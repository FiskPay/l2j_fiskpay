/*
* Copyright (c) 2025 FiskPay
* 
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be
* included in all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
* IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package org.l2jmobius.loginserver.blockchain;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Arrays;
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
    private static final AtomicInteger _counter = new AtomicInteger(0);
    
    protected static JSONObject getAccounts(String walletAddress)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            final JSONArray accounts = new JSONArray();
            
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
    
    protected static JSONObject linkAccount(String username, String password, String walletAddress)
    {
        try (Connection con = DatabaseFactory.getConnection();)
        {
            final byte[] rawPassword = MessageDigest.getInstance("SHA").digest(password.getBytes(StandardCharsets.UTF_8));
            final String inputPassword = Base64.getEncoder().encodeToString(rawPassword);

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
                
                return new JSONObject().put("fail", "Linking wallet address to account failed");
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return new JSONObject().put("fail", "linkAccount SQL error");
        }
    }
    
    protected static JSONObject unlinkAccount(String username, String password, String walletAddress)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            final byte[] rawPassword = MessageDigest.getInstance("SHA").digest(password.getBytes(StandardCharsets.UTF_8));
            final String inputPassword = Base64.getEncoder().encodeToString(rawPassword);

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
                    else
                    {
                        return new JSONObject().put("fail", "Account not found");
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
                
                return new JSONObject().put("fail", "Unlinking wallet address from account failed");
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return new JSONObject().put("fail", "unlinkAccount SQL error");
        }
    }
    
    protected static JSONObject finalizeWithdraw(String srvId, String character, String refund, String amount)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM fiskpay_temporary WHERE server_id = ? AND character_name = ? AND refund = ? AND amount = ?;"))
            {
                ps.setInt(1, Integer.parseInt(srvId));
                ps.setString(2, character);
                ps.setInt(3, Integer.parseInt(refund));
                ps.setLong(4, Long.parseLong(amount));
                
                if (ps.executeUpdate() > 0)
                {
                    return new JSONObject().put("data", true);
                }
                
                return new JSONObject().put("fail", "Withdraw not finalized");
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return new JSONObject().put("fail", "finalizeWithdraw sql error");
        }
    }
    
    protected static boolean isWalletOwner(String username, String walletAddress)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM accounts WHERE login = ? AND wallet_address = ? LIMIT 1;"))
            {
                ps.setString(1, username);
                ps.setString(2, walletAddress);
                
                try (ResultSet rs = ps.executeQuery())
                {
                    return rs.next();
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return false;
        }
    }
    
    protected static boolean isNewWithdraw(String srvId, String character, String refund, String amount)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM fiskpay_temporary WHERE server_id = ? AND character_name = ? AND refund = ? AND amount = ? LIMIT 1;"))
            {
                ps.setInt(1, Integer.parseInt(srvId));
                ps.setString(2, character);
                ps.setInt(3, Integer.parseInt(refund));
                ps.setLong(4, Long.parseLong(amount));
                
                try (ResultSet rs = ps.executeQuery())
                {
                    return !rs.next();
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return false;
        }
    }
    
    protected static boolean createNewWithdraw(String srvId, String character, String refund, String amount)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO fiskpay_temporary (server_id, character_name, refund, amount) VALUES (?, ?, ?, ?);"))
            {
                ps.setInt(1, Integer.parseInt(srvId));
                ps.setString(2, character);
                ps.setInt(3, Integer.parseInt(refund));
                ps.setLong(4, Long.parseLong(amount));
                
                return ps.executeUpdate() > 0;
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return false;
        }
    }
    
    protected static void logDepositToDB(String txHash, String from, String symbol, String amount, String srvId, String character)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO fiskpay_deposits (transaction_hash, server_id, character_name, wallet_address, amount) VALUES (?, ?, ?, ?, ?);"))
            {
                ps.setString(1, txHash);
                ps.setInt(2, Integer.parseInt(srvId));
                ps.setString(3, character);
                ps.setString(4, from);
                ps.setLong(5, Long.parseLong(amount));
                ps.executeUpdate();
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
        }
    }
    
    protected static void logWithdrawToDB(String txHash, String to, String symbol, String amount, String srvId, String character)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO fiskpay_withdrawals (transaction_hash, server_id, character_name, wallet_address, amount) VALUES (?, ?, ?, ?, ?);"))
            {
                ps.setString(1, txHash);
                ps.setInt(2, Integer.parseInt(srvId));
                ps.setString(3, character);
                ps.setString(4, to);
                ps.setLong(5, Long.parseLong(amount));
                ps.executeUpdate();
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
        }
    }
    
    protected static CompletableFuture<JSONObject> getAccountCharacters(String srvId, String username)
    {
        return sendRequestToGS(srvId, "getAccountCharacters", new JSONArray().put(username));
    }
    
    protected static CompletableFuture<JSONObject> getCharacterBalance(String srvId, String character)
    {
        return sendRequestToGS(srvId, "getCharacterBalance", new JSONArray().put(character));
    }
    
    protected static CompletableFuture<JSONObject> isCharacterOffline(String srvId, String character)
    {
        return sendRequestToGS(srvId, "isCharacterOffline", new JSONArray().put(character));
    }
    
    protected static CompletableFuture<JSONObject> getCharacterUsername(String srvId, String character)
    {
        return sendRequestToGS(srvId, "getCharacterUsername", new JSONArray().put(character));
    }
    
    protected static CompletableFuture<JSONObject> addToCharacter(String srvId, String character, String amount)
    {
        return sendRequestToGS(srvId, "addToCharacter", new JSONArray(Arrays.asList(character, amount)));
    }
    
    protected static CompletableFuture<JSONObject> removeFromCharacter(String srvId, String character, String amount)
    {
        return sendRequestToGS(srvId, "removeFromCharacter", new JSONArray(Arrays.asList(character, amount)));
    }
    
    protected static CompletableFuture<JSONObject> isGameServerAvailable(String srvId)
    {
        return sendRequestToGS(srvId, "isGameServerAvailable", new JSONArray());
    }
    
    protected static void setConfig(String srvId, String wallet, String symbol)
    {
        String rwdId = "0";
        
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("SELECT reward_id FROM gameservers WHERE server_id = ? LIMIT 1;"))
            {
                ps.setInt(1, Integer.parseInt(srvId));
                
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next())
                    {
                        rwdId = rs.getString("reward_id");
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            LOGGER.log(Level.WARNING, "Blockchain in-game reward item could not be set");
        }
                
        sendRequestToGS(srvId, "setConfig", new JSONArray().put(rwdId).put(wallet).put(symbol)).thenAccept((responseObject) ->
        {
            if (!(responseObject.has("data") && responseObject.getBoolean("data")))
            {
                LOGGER.log(Level.WARNING, "Failed to send blockchain configuration to Game Server " + srvId);
                
                if (responseObject.has("fail"))
                {
                    LOGGER.log(Level.WARNING, "Fail reason: " + responseObject.getString("fail"));
                }
            }
        });
    }
    
    protected static void updateGameServerBalance(String srvId)
    {
        sendRequestToGS(srvId, "fetchGameServerBalance", new JSONArray()).thenAccept((responseObject) ->
        {
            if (responseObject.has("data"))
            {
                final String balance = responseObject.getString("data");
                
                try (Connection con = DatabaseFactory.getConnection();)
                {
                    try (PreparedStatement ps = con.prepareStatement("UPDATE gameservers SET balance = ? WHERE server_id = ?;"))
                    {
                        ps.setLong(1, Long.parseLong(balance));
                        ps.setInt(2, Integer.parseInt(srvId));
                        ps.executeUpdate();
                    }
                }
                catch (Exception e)
                {
                    LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
                }
            }
            else
            {
                LOGGER.log(Level.WARNING, "Failed to update Game Server balance. Server id: " + srvId);
            }
        });
    }
    
    protected static void refundPlayers(String srvId)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM fiskpay_temporary WHERE server_id = ? AND refund < ?;"))
            {
                final int timestamp = (int) (System.currentTimeMillis() / 1000);
                
                ps.setInt(1, Integer.parseInt(srvId));
                ps.setInt(2, timestamp);
                
                try (ResultSet rs = ps.executeQuery())
                {
                    while (rs.next())
                    {
                        final String character = rs.getString("character_name");
                        final String amount = rs.getString("amount");
                        final String refund = rs.getString("refund");
                        
                        addToCharacter(srvId, character, amount).thenAccept(addObject ->
                        {
                            if (addObject.has("data") && addObject.getBoolean("data"))
                            {
                                JSONObject finalizeObject = finalizeWithdraw(srvId, character, refund, amount);
                                
                                if (!(finalizeObject.has("data") && finalizeObject.getBoolean("data")))
                                {
                                    LOGGER.log(Level.WARNING, "Failed to refund player (finalize): " + character + " on server: " + srvId);
                                    
                                    if (finalizeObject.has("fail"))
                                    {
                                        LOGGER.log(Level.WARNING, "Fail reason: " + finalizeObject.getString("fail"));
                                    }
                                }
                            }
                            else
                            {
                                LOGGER.log(Level.WARNING, "Failed to refund player (add): " + character + " on server: " + srvId);
                                
                                if (addObject.has("fail"))
                                {
                                    LOGGER.log(Level.WARNING, "Fail reason: " + addObject.getString("fail"));
                                }
                            }
                        });
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error in refundPlayers: " + e.getMessage(), e);
        }
    }
    
    private static int getNextID()
    {
        return _counter.updateAndGet((value) -> (value == 1000000) ? 0 : value + 1);
    }
    
    private static CompletableFuture<JSONObject> sendRequestToGS(String srvId, String subject, JSONArray info)
    {
        final CompletableFuture<JSONObject> future = new CompletableFuture<>();
        
        final GameServerTable gsTable = GameServerTable.getInstance();
        
        if (gsTable == null)
        {
            return CompletableFuture.completedFuture(new JSONObject().put("fail", "Request to Game Server " + srvId + "failed. Could not get the GameServerTable instance"));
        }
        
        final GameServerInfo gsInfo = gsTable.getRegisteredGameServerById(Integer.parseInt(srvId));
        
        if (gsInfo == null)
        {
            return CompletableFuture.completedFuture(new JSONObject().put("fail", "Request to Game Server " + srvId + "failed. Could not find Game Server info"));
        }
        
        final GameServerThread gsThread = gsInfo.getGameServerThread();
        
        if (gsThread == null)
        {
            return CompletableFuture.completedFuture(new JSONObject().put("fail", "Request to Game Server " + srvId + "failed. Could not find Game Server thread"));
        }
        
        final int uniqueID = getNextID();
        
        FiskPayResponseReceive.registerCallback(uniqueID, responseString ->
        {
            JSONObject response;
            
            if (responseString == null || responseString.isEmpty())
            {
                LOGGER.log(Level.WARNING, "Received an empty or null responseString from Game Server" + srvId);
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
                    LOGGER.log(Level.WARNING, "Invalid responseString from Game Server " + srvId + ". Response: " + responseString, e);
                    response = new JSONObject().put("fail", "responseString is not a JSONObject string");
                }
            }
            
            if (!future.isDone())
            {
                future.complete(response);
            }
        });
        
        final JSONObject requestObject = new JSONObject();
        
        requestObject.put("subject", subject);
        requestObject.put("info", info);
        
        final String requestString = requestObject.toString();
        
        gsThread.sendFiskPayRequest(uniqueID, requestString); // Forward the request
        
        return future.completeOnTimeout(new JSONObject().put("fail", "Request to Game Server " + srvId + " with subject " + subject + " timed out"), 10, TimeUnit.SECONDS);
    }
}
