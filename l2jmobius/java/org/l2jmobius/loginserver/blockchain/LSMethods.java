// Login Server: ProcessorMethods.java
package org.l2jmobius.loginserver.blockchain;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.loginserver.GameServerTable;
import org.l2jmobius.loginserver.GameServerTable.GameServerInfo;
import org.l2jmobius.loginserver.GameServerThread;
import org.l2jmobius.loginserver.network.gameserverpackets.FiskPayResponseReceive;

public class LSMethods
{
    private static final Logger LOGGER = Logger.getLogger(LSMethods.class.getName());
    private static final AtomicInteger counter = new AtomicInteger(0);
    
    protected static JSONObject getAccounts(String walletAddress)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            JSONArray accounts = new JSONArray();
            
            try (PreparedStatement ps = con.prepareStatement("SELECT login FROM accounts WHERE wallet_address = ?;"))
            {
                ps.setString(1, walletAddress);
                
                try (ResultSet rs = ps.executeQuery())
                {
                    while (rs.next())
                    {
                        accounts.put(rs.getString("login"));
                    }
                }
                
                return new JSONObject().put("data", accounts);
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return new JSONObject().put("fail", "getAccounts SQL error");
        }
    }
    
    protected static JSONObject getClientBalance()
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("SELECT SUM(balance) AS balance FROM gameservers;"))
            {
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next() && rs.getString("balance") != null)
                    {
                        return new JSONObject().put("data", rs.getString("balance"));
                    }
                    
                    return new JSONObject().put("data", "0");                 
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return new JSONObject().put("fail", "getClientBalance SQL error");
        }
    }
    
    protected static JSONObject addAccount(String username, String password, String walletAddress)
    {
        try (Connection con = DatabaseFactory.getConnection();)
        {
            byte[] rawPassword = MessageDigest.getInstance("SHA").digest(password.getBytes(StandardCharsets.UTF_8));
            String inputPassword = Base64.getEncoder().encodeToString(rawPassword);
            String databasePassword = "";
            String databaseWallet = "";
            
            try (PreparedStatement ps = con.prepareStatement("SELECT password, wallet_address FROM accounts WHERE login = ? LIMIT 1;"))
            {
                ps.setString(1, username);
                
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next())
                    {
                        databasePassword = rs.getString("password");
                        databaseWallet = rs.getString("wallet_address");
                    }
                }
            }
            
            if (!databasePassword.equals(inputPassword))
            {
                return new JSONObject().put("fail", "Username - password mismatch");
            }
            
            if (!databaseWallet.equals("not linked"))
            {
                return new JSONObject().put("fail", "Account " + username + " already linked to an Ethereum address");
            }
            
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET wallet_address = ? WHERE login = ?;"))
            {
                ps.setString(1, walletAddress);
                ps.setString(2, username);
                
                if (ps.executeUpdate() > 0)
                {
                    return new JSONObject().put("data", true);
                }
                
                return new JSONObject().put("data", false);
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return new JSONObject().put("fail", "addAccount SQL error");
        }
    }
    
    protected static JSONObject removeAccount(String username, String password, String walletAddress)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            byte[] rawPassword = MessageDigest.getInstance("SHA").digest(password.getBytes(StandardCharsets.UTF_8));
            String inputPassword = Base64.getEncoder().encodeToString(rawPassword);
            String databasePassword = "";
            String databaseWallet = "";
            
            try (PreparedStatement ps = con.prepareStatement("SELECT password, wallet_address FROM accounts WHERE login = ? LIMIT 1;"))
            {
                ps.setString(1, username);
                
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next())
                    {
                        databasePassword = rs.getString("password");
                        databaseWallet = rs.getString("wallet_address");
                    }
                }
            }
            
            if (!databasePassword.equals(inputPassword))
            {
                return new JSONObject().put("fail", "Username - password mismatch");
            }
            
            if (!databaseWallet.equals(walletAddress))
            {
                return new JSONObject().put("fail", "Account " + username + " not linked to your Ethereum address");
            }
            
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET wallet_address = ? WHERE login = ?;"))
            {
                ps.setString(1, "not linked");
                ps.setString(2, username);
                
                if (ps.executeUpdate() > 0)
                {
                    return new JSONObject().put("data", true);
                }
                
                return new JSONObject().put("data", false);
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return new JSONObject().put("fail", "removeAccount SQL error");
        }
    }
    
    protected static CompletableFuture<JSONObject> sendRequestToGS(JSONObject requestObject)
    {
        CompletableFuture<JSONObject> future = new CompletableFuture<>();
        
        GameServerTable gsTable = GameServerTable.getInstance();
        
        if (gsTable == null)
        {
            return CompletableFuture.completedFuture(new JSONObject().put("fail", "Could not get the GameServerTable instance"));
        }
        
        GameServerInfo gsInfo = gsTable.getRegisteredGameServerById(Integer.parseInt(requestObject.getString("id")));
        
        if (gsInfo == null)
        {
            return CompletableFuture.completedFuture(new JSONObject().put("fail", "Could not find Game Server info"));
        }
        
        GameServerThread gsThread = gsInfo.getGameServerThread();
        
        if (gsThread == null)
        {
            return CompletableFuture.completedFuture(new JSONObject().put("fail", "Could not find Game Server thread"));
        }
        
        int uniqueID = getNextID();
        
        FiskPayResponseReceive.registerCallback(uniqueID, responseString ->
        {
            JSONObject response;
            
            if (responseString == null || responseString.isEmpty())
            {
                LOGGER.log(Level.WARNING, "Received an empty or null responseString");
                response = new JSONObject().put("fail", "responseString is empty or null");
            }
            else
            {
                try
                {
                    response = new JSONObject(responseString);
                }
                catch (Exception e)
                {
                    LOGGER.log(Level.WARNING, "Invalid responseString: " + responseString, e);
                    response = new JSONObject().put("fail", "responseString is not a JSONObject string");
                }
            }
            
            if (!future.isDone())
            {
                future.complete(response);
            }
        });
        
        gsThread.sendFiskPayRequest(uniqueID, requestObject.toString()); // Forward the request
        
        return future.completeOnTimeout(new JSONObject().put("fail", "Request to Game Server timeout"), 10, TimeUnit.SECONDS);
    }
    
    private static int getNextID()
    {
        return counter.updateAndGet(value -> (value == 1000000) ? 0 : value + 1);
    }
}