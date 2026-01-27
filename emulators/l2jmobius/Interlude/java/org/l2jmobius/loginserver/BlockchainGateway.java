/*
 * Copyright (c) 2026 FiskPay
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
package org.l2jmobius.loginserver;

import com.fiskpay.l2.Connector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.loginserver.GameServerTable.GameServerInfo;
import org.l2jmobius.loginserver.config.BlockchainConfig;
import org.l2jmobius.loginserver.network.gameserverpackets.FiskPayResponseReceive;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author Scrab
 */
public class BlockchainGateway implements Connector.Interface
{
    private static final Logger LOGGER = Logger.getLogger(BlockchainGateway.class.getName());
    private static final Logger BLOCKCHAIN_LOGGER = Logger.getLogger("blockchain");
    
    private static final String SYMBOL = BlockchainConfig.SYMBOL;
    private static final String WALLET = BlockchainConfig.WALLET;
    private static final String PASSWORD = BlockchainConfig.PASSWORD;
    
    private static final AtomicInteger _counter = new AtomicInteger(0);
    
    private final Set<String> _onlineServers = ConcurrentHashMap.newKeySet();
    private final CompletableFuture<Void> _connectionResult = new CompletableFuture<>();
    
    private boolean _signedIn = false;
    
    private Connector _connector;
    
    @Override
    public void onLogDeposit(String txHash, String from, String symbol, String amount, String srvId, String character)
    {
        logDeposit(txHash, from, symbol, amount, srvId, character).thenAccept((logResult) ->
        {
            if (logResult.getBoolean("ok") == true)
            {
                BLOCKCHAIN_LOGGER.info("Deposit on " + getServerName(srvId) + ": " + from + " -> " + character + " = " + amount + " " + symbol);
            }
            else
            {
                BLOCKCHAIN_LOGGER.warning("--------------------------------------- Failed Deposit Start ---------------------------------------");
                BLOCKCHAIN_LOGGER.warning("TxHash:   " + txHash);
                BLOCKCHAIN_LOGGER.warning("From:     " + from);
                BLOCKCHAIN_LOGGER.warning("To:       " + character);
                BLOCKCHAIN_LOGGER.warning("Server:   " + getServerName(srvId));
                BLOCKCHAIN_LOGGER.warning("Amount:   " + amount);
                BLOCKCHAIN_LOGGER.warning("Token:    " + symbol);
                BLOCKCHAIN_LOGGER.warning("Message:  " + logResult.getString("error"));
                BLOCKCHAIN_LOGGER.warning("Action:   You must manualy reward " + amount + " " + symbol + " to player");
                BLOCKCHAIN_LOGGER.warning("---------------------------------------- Failed Deposit End ----------------------------------------");
            }
        });
    }
    
    @Override
    public void onLogWithdraw(String txHash, String to, String symbol, String amount, String srvId, String character, String refund)
    {
        final JSONObject logResult = logWithdraw(txHash, to, symbol, amount, srvId, character, refund);
        
        if (logResult.getBoolean("ok") == true)
        {
            BLOCKCHAIN_LOGGER.info("Withdrawal on " + getServerName(srvId) + ": " + character + " -> " + to + " = " + amount + " " + symbol);
        }
        else
        {
            BLOCKCHAIN_LOGGER.warning("-------------------------------------- Failed Withdrawal Start -------------------------------------");
            BLOCKCHAIN_LOGGER.warning("TxHash:   " + txHash);
            BLOCKCHAIN_LOGGER.warning("From:     " + character);
            BLOCKCHAIN_LOGGER.warning("To:       " + to);
            BLOCKCHAIN_LOGGER.warning("Server:   " + getServerName(srvId));
            BLOCKCHAIN_LOGGER.warning("Amount:   " + amount);
            BLOCKCHAIN_LOGGER.warning("Token:    " + symbol);
            BLOCKCHAIN_LOGGER.warning("Message:  " + logResult.getString("error"));
            BLOCKCHAIN_LOGGER.warning("Action:   You may manualy remove " + amount + " " + symbol + " from player");
            BLOCKCHAIN_LOGGER.warning("--------------------------------------- Failed Withdrawal End --------------------------------------");
        }
    }
    
    @Override
    public void onRequest(JSONObject requestObject, Callback cb)
    {
        if (requestObject.has("id") && requestObject.get("id") instanceof String && requestObject.has("subject") && requestObject.get("subject") instanceof String && requestObject.has("data") && requestObject.get("data") instanceof JSONObject)
        {
            final String srvId = requestObject.getString("id");
            
            if ("ls".equals(srvId)) // Request must be handled by the Login Server
            {
                cb.resolve(processLSRequest(requestObject)); // Proccess the request and send it to the FiskPay server
            }
            else if (Pattern.matches("^([1-9]|[1-9][0-9]|1[01][0-9]|12[0-7])$", srvId)) // Forward the request to the appropriate Game Server. When a response is received from the Game Server, Login Server will handle it
            {
                if (_onlineServers.contains(srvId))
                {
                    processGSRequest(requestObject).thenAccept(cb::resolve); // Forward the request to the Game Server and send the response back to the FiskPay server
                }
                else
                {
                    cb.resolve(new JSONObject().put("ok", false).put("error", "Game server " + getServerName(srvId) + " is not available"));
                }
            }
            else
            {
                cb.resolve(new JSONObject().put("ok", false).put("error", "Request object has an illegal id value"));
            }
        }
    }
    
    @Override
    public void onConnect()
    {
        LOGGER.info(getClass().getSimpleName() + ": Connection established");
        LOGGER.info(getClass().getSimpleName() + ": Signing in...");
        
        _connector.login(SYMBOL, WALLET, PASSWORD, new JSONArray(_onlineServers)).thenAccept((responseObject) ->
        {
            if (responseObject.getBoolean("ok") == true)
            {
                LOGGER.info(getClass().getSimpleName() + ": Signed in successfully");
                _signedIn = true;
            }
            else if (responseObject.has("error") == true)
            {
                LOGGER.info(getClass().getSimpleName() + ": " + responseObject.getString("error"));
            }
            else
            {
                LOGGER.info(getClass().getSimpleName() + ": Maybe completeExceptionally triggered in Connector.java? ");
            }
            
            if (!_connectionResult.isDone())
            {
                _connectionResult.complete(null);
            }
            
        }).exceptionally((e) ->
        {
            LOGGER.warning(getClass().getSimpleName() + ": Error during sign in");
            LOGGER.warning(getClass().getSimpleName() + ": " + e.getMessage());
            
            return null;
        });
    }
    
    @Override
    public void onDisconnect()
    {
        LOGGER.warning(getClass().getSimpleName() + ": Service temporary unavailable");
    }
    
    @Override
    public void onError(Exception e)
    {
        LOGGER.warning(getClass().getSimpleName() + ": Connector error");
        LOGGER.warning(getClass().getSimpleName() + ": " + e.getMessage());
    }
    
    public void renewServers(int serverId, boolean isConnected)
    {
        renewServers(Integer.toString(serverId), isConnected);
    }
    
    public void renewServers(String srvId, boolean isConnected)
    {
        if (isConnected && !_onlineServers.contains(srvId))
        {
            _onlineServers.add(srvId);
            setConfig(srvId, WALLET, SYMBOL);
        }
        else if (!isConnected && _onlineServers.contains(srvId))
        {
            _onlineServers.remove(srvId);
        }
        
        _connector.renewServers(new JSONArray(_onlineServers));
    }
    
    public boolean isSignedIn()
    {
        return _signedIn;
    }
    
    public static BlockchainGateway getInstance()
    {
        return SingletonHolder.INSTANCE;
    }
    
    private BlockchainGateway()
    {
        LOGGER.info(getClass().getSimpleName() + ": Connecting...");
        
        _connector = new Connector(this);
        
        ThreadPool.scheduleAtFixedRate(() ->
        {
            for (String srvId : _onlineServers)
            {
                updateGameServerBalanceToDB(srvId);
            }
        }, 0, 150000);
        
        ThreadPool.scheduleAtFixedRate(() ->
        {
            for (String srvId : _onlineServers)
            {
                refundExpiredWithdrawals(srvId);
            }
        }, 75000, 150000);
        
        _connectionResult.completeOnTimeout(null, 10, TimeUnit.SECONDS);
        
        try
        {
            _connectionResult.get();
        }
        catch (Exception e)
        {
            LOGGER.warning(getClass().getSimpleName() + ": Error while connecting to service");
            LOGGER.warning(getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
    
    private static class SingletonHolder
    {
        private static final BlockchainGateway INSTANCE = new BlockchainGateway();
    }
    
    private static String getServerName(String srvId)
    {
        return getServerName(Integer.parseInt(srvId));
    }
    
    private static String getServerName(int serverId)
    {
        final GameServerTable gsTable = GameServerTable.getInstance();
        
        if (gsTable != null)
        {
            return gsTable.getServerNameById(serverId);
        }
        
        return "Unknown";
    }
    
    private static JSONObject processLSRequest(JSONObject requestObject)
    {
        final String subject = requestObject.getString("subject");
        final JSONObject data = requestObject.getJSONObject("data");
        
        switch (subject)
        {
            case "getAccs":
            {
                if (!data.has("walletAddress"))
                {
                    return new JSONObject().put("ok", false).put("error", "walletAddress undefined");
                }
                
                if (!(data.get("walletAddress") instanceof String))
                {
                    return new JSONObject().put("ok", false).put("error", "walletAddress not a String");
                }
                
                final String walletAddress = data.getString("walletAddress");
                
                if (!Pattern.matches("^0x[a-fA-F0-9]{40}$", walletAddress))
                {
                    return new JSONObject().put("ok", false).put("error", "Improper walletAddress");
                }
                
                return getAccounts(walletAddress); // Get wallet accounts from Login Server database
            }
            case "getClientBal":
            {
                return getClientBalance(); // Get total balance from all Game Servers
            }
            case "linkAcc": // This subject's requestObject is validated on the FiskPay Service, no checks needed.
            {
                final String username = data.getString("username");
                final String password = data.getString("password");
                final String walletAddress = data.getString("walletAddress");
                
                return linkAccount(username, password, walletAddress); // Links the account to the wallet address
            }
            case "unlinkAcc": // This subject's requestObject is validated on the FiskPay Service, no checks needed.
            {
                final String username = data.getString("username");
                final String password = data.getString("password");
                final String walletAddress = data.getString("walletAddress");
                
                return unlinkAccount(username, password, walletAddress); // Unlinks the account from the wallet address
            }
            default:
            {
                return new JSONObject().put("ok", false).put("error", "Unknown request to Login Server. Subject: " + subject);
            }
        }
    }
    
    private static CompletableFuture<JSONObject> processGSRequest(JSONObject requestObject)
    {
        final String srvId = requestObject.getString("id");
        final String subject = requestObject.getString("subject");
        final JSONObject data = requestObject.getJSONObject("data");
        
        switch (subject)
        {
            case "getChars":
            {
                if (!data.has("username"))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "username undefined"));
                }
                
                if (!(data.get("username") instanceof String))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "username not a String"));
                }
                
                final String username = data.getString("username");
                
                return getAccountCharacters(srvId, username);
            }
            case "getCharBal":
            {
                if (!data.has("character"))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "character undefined"));
                }
                
                if (!(data.get("character") instanceof String))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "character not a String"));
                }
                
                final String character = data.getString("character");
                
                return getCharacterBalance(srvId, character);
            }
            case "isOffline":
            {
                if (!data.has("character"))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "character undefined"));
                }
                
                if (!(data.get("character") instanceof String))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "character not a String"));
                }
                
                final String character = data.getString("character");
                
                return isCharacterOffline(srvId, character);
            }
            case "getGSMode":
            {
                return getGameServerMode(srvId);
            }
            case "requestWithdraw": // This subject's requestObject is validated on the FiskPay Service, no checks needed
            {
                final String walletAddress = data.getString("walletAddress");
                final String character = data.getString("character");
                final String refund = data.getString("refund");
                final String amount = data.getString("amount");
                
                if (!isNewWithdraw(srvId, character, refund, amount))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "Trying to exploit? Help us grow, report your findings"));
                }
                
                return getCharacterUsername(srvId, character).thenCompose((responseObject0) ->
                {
                    if (responseObject0.has("error"))
                    {
                        return CompletableFuture.completedFuture(responseObject0);
                    }
                    
                    final String username = responseObject0.getString("data");
                    
                    if (!isWalletOwner(username, walletAddress))
                    {
                        return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "Wallet ownership verification failed"));
                    }
                    
                    return removeFromCharacter(srvId, character, amount).thenCompose((responseObject1) ->
                    {
                        if (responseObject1.has("error"))
                        {
                            return CompletableFuture.completedFuture(responseObject1);
                        }
                        
                        if (!createNewWithdraw(srvId, character, refund, amount))
                        {
                            return addToCharacter(srvId, character, amount, "0").thenApply((responseObject2) ->
                            {
                                if (responseObject2.getBoolean("ok") == true)
                                {
                                    return new JSONObject().put("ok", false).put("error", "Refund could not be created. Withdrawal reverted");
                                }
                                
                                return responseObject2;
                            });
                        }
                        
                        return CompletableFuture.completedFuture(new JSONObject().put("ok", true));
                    });
                });
            }
            default:
            {
                return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "Unknown request to forward to Game Server. Subject: " + subject));
            }
        }
    }
    
    private static CompletableFuture<JSONObject> logDeposit(String txHash, String from, String symbol, String amount, String srvId, String character)
    {
        if (logDepositToDB(txHash, from, symbol, amount, srvId, character) == true)
        {
            return addToCharacter(srvId, character, amount, "1");
        }
        
        return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "logDepositToDB in java had an error"));
    }
    
    private static JSONObject logWithdraw(String txHash, String to, String symbol, String amount, String srvId, String character, String refund)
    {
        if (logWithdrawToDB(txHash, to, symbol, amount, srvId, character) == true)
        {
            return finalizeWithdraw(srvId, character, refund, amount);
        }
        
        return new JSONObject().put("ok", false).put("error", "logWithdrawToDB in java had an error");
    }
    
    private static JSONObject getAccounts(String walletAddress)
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
                
                return new JSONObject().put("ok", true).put("data", accounts);
            }
            catch (Exception e)
            {
                
                LOGGER.log(Level.WARNING, "getAccounts could not be fetched from database");
                LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
                
                return new JSONObject().put("ok", false).put("error", "getAccounts sql query error");
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error with database connection");
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            
            return new JSONObject().put("ok", false).put("error", "getAccounts database connection error");
        }
    }
    
    private static JSONObject getClientBalance()
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("SELECT SUM(balance) AS balance FROM gameservers;"))
            {
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next() && rs.getString("balance") != null)
                    {
                        return new JSONObject().put("ok", true).put("data", rs.getString("balance"));
                    }
                    
                    return new JSONObject().put("ok", true).put("data", "0");
                }
            }
            catch (Exception e)
            {
                
                LOGGER.log(Level.WARNING, "getClientBalance could not be fetched from database");
                LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
                
                return new JSONObject().put("ok", false).put("error", "getClientBalance sql query error");
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error with database connection");
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            
            return new JSONObject().put("ok", false).put("error", "getClientBalance database connection error");
        }
    }
    
    private static JSONObject linkAccount(String username, String password, String walletAddress)
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
                return new JSONObject().put("ok", false).put("error", "Username - password mismatch");
            }
            
            if (!databaseWallet.equals("not linked"))
            {
                return new JSONObject().put("ok", false).put("error", "Account " + username + " already linked to an Ethereum address");
            }
            
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET wallet_address = ? WHERE login = ?;"))
            {
                ps.setString(1, walletAddress);
                ps.setString(2, username);
                
                if (ps.executeUpdate() > 0)
                {
                    return new JSONObject().put("ok", true);
                }
                
                return new JSONObject().put("ok", false).put("error", "No error, but ps.executeUpdate() returned zero (linkAccount)");
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "linkAccount could not be finalized to database");
                LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
                
                return new JSONObject().put("ok", false).put("error", "linkAccount sql query error");
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error with database connection");
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            
            return new JSONObject().put("ok", false).put("error", "linkAccount database connection error");
        }
    }
    
    private static JSONObject unlinkAccount(String username, String password, String walletAddress)
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
                        return new JSONObject().put("ok", false).put("error", "Username - password mismatch");
                    }
                }
            }
            
            if (!databasePassword.equals(inputPassword))
            {
                return new JSONObject().put("ok", false).put("error", "Username - password mismatch");
            }
            
            if (!databaseWallet.equals(walletAddress))
            {
                return new JSONObject().put("ok", false).put("error", "Account " + username + " not linked to your Ethereum address");
            }
            
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET wallet_address = ? WHERE login = ?;"))
            {
                ps.setString(1, "not linked");
                ps.setString(2, username);
                
                if (ps.executeUpdate() > 0)
                {
                    return new JSONObject().put("ok", true);
                }
                
                return new JSONObject().put("ok", false).put("error", "No error, but ps.executeUpdate() returned zero (unlinkAccount)");
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "unlinkAccount could not be finalized to database");
                LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
                
                return new JSONObject().put("ok", false).put("error", "unlinkAccount sql query error");
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error with database connection");
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            
            return new JSONObject().put("ok", false).put("error", "unlinkAccount database connection error");
        }
    }
    
    private static JSONObject finalizeWithdraw(String srvId, String character, String refund, String amount)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM fiskpay_temporary WHERE server_id = ? AND character_name = ? AND refund = ? AND amount = ?;"))
            {
                ps.setInt(1, Integer.parseInt(srvId));
                ps.setString(2, character);
                ps.setInt(3, Integer.parseInt(refund));
                ps.setInt(4, Integer.parseInt(amount));
                
                if (ps.executeUpdate() > 0)
                {
                    return new JSONObject().put("ok", true);
                }
                
                return new JSONObject().put("ok", false).put("error", "No error, but ps.executeUpdate() returned zero (finalizeWithdraw)");
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "Withdraw could not be finalized to database");
                LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
                
                return new JSONObject().put("ok", false).put("error", "finalizeWithdraw sql query error");
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error with database connection");
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            
            return new JSONObject().put("ok", false).put("error", "finalizeWithdraw db connection error");
        }
    }
    
    private static boolean isWalletOwner(String username, String walletAddress)
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
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "Wallet owner could not be fetched from database");
                LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error with database connection");
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
        }
        
        return false;
    }
    
    private static boolean isNewWithdraw(String srvId, String character, String refund, String amount)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM fiskpay_temporary WHERE server_id = ? AND character_name = ? AND refund = ? AND amount = ? LIMIT 1;"))
            {
                ps.setInt(1, Integer.parseInt(srvId));
                ps.setString(2, character);
                ps.setInt(3, Integer.parseInt(refund));
                ps.setInt(4, Integer.parseInt(amount));
                
                try (ResultSet rs = ps.executeQuery())
                {
                    return !rs.next();
                }
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "New withdraw check could not be fetched from database");
                LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error with database connection");
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
        }
        
        return false;
    }
    
    private static boolean createNewWithdraw(String srvId, String character, String refund, String amount)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO fiskpay_temporary (server_id, character_name, refund, amount) VALUES (?, ?, ?, ?);"))
            {
                ps.setInt(1, Integer.parseInt(srvId));
                ps.setString(2, character);
                ps.setInt(3, Integer.parseInt(refund));
                ps.setInt(4, Integer.parseInt(amount));
                
                return ps.executeUpdate() > 0;
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "New withdraw could not be added to database");
                LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error with database connection");
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
        }
        
        return false;
    }
    
    private static boolean logDepositToDB(String txHash, String from, String symbol, String amount, String srvId, String character)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO fiskpay_deposits (transaction_hash, server_id, character_name, wallet_address, amount) VALUES (?, ?, ?, ?, ?);"))
            {
                ps.setString(1, txHash);
                ps.setInt(2, Integer.parseInt(srvId));
                ps.setString(3, character);
                ps.setString(4, from);
                ps.setInt(5, Integer.parseInt(amount));
                ps.executeUpdate();
                
                return true;
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "Deposit could not be logged to database");
                LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error with database connection");
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
        }
        
        return false;
    }
    
    private static boolean logWithdrawToDB(String txHash, String to, String symbol, String amount, String srvId, String character)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO fiskpay_withdrawals (transaction_hash, server_id, character_name, wallet_address, amount) VALUES (?, ?, ?, ?, ?);"))
            {
                ps.setString(1, txHash);
                ps.setInt(2, Integer.parseInt(srvId));
                ps.setString(3, character);
                ps.setString(4, to);
                ps.setInt(5, Integer.parseInt(amount));
                ps.executeUpdate();
                
                return true;
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "Withdraw could not be logged to database");
                LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error with database connection");
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
        }
        
        return false;
    }
    
    private static CompletableFuture<JSONObject> getAccountCharacters(String srvId, String username)
    {
        return sendRequestToGS(srvId, "getAccountCharacters", new JSONArray().put(username));
    }
    
    private static CompletableFuture<JSONObject> getCharacterBalance(String srvId, String character)
    {
        return sendRequestToGS(srvId, "getCharacterBalance", new JSONArray().put(character));
    }
    
    private static CompletableFuture<JSONObject> isCharacterOffline(String srvId, String character)
    {
        return sendRequestToGS(srvId, "isCharacterOffline", new JSONArray().put(character));
    }
    
    private static CompletableFuture<JSONObject> getCharacterUsername(String srvId, String character)
    {
        return sendRequestToGS(srvId, "getCharacterUsername", new JSONArray().put(character));
    }
    
    private static CompletableFuture<JSONObject> addToCharacter(String srvId, String character, String amount, String isDeposit)
    {
        return sendRequestToGS(srvId, "addToCharacter", new JSONArray().put(character).put(amount).put(isDeposit));
    }
    
    private static CompletableFuture<JSONObject> removeFromCharacter(String srvId, String character, String amount)
    {
        return sendRequestToGS(srvId, "removeFromCharacter", new JSONArray().put(character).put(amount));
    }
    
    private static CompletableFuture<JSONObject> getGameServerMode(String srvId)
    {
        return sendRequestToGS(srvId, "getGameServerMode", new JSONArray());
    }
    
    private static void setConfig(String srvId, String wallet, String symbol)
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
                catch (Exception e)
                {
                    LOGGER.log(Level.WARNING, "Blockchain in-game reward item could not be fetched from database");
                    LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error with database connection");
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
        }
        
        sendRequestToGS(srvId, "setConfig", new JSONArray().put(wallet).put(symbol).put(rwdId)).thenAccept((responseObject) ->
        {
            if (responseObject.getBoolean("ok") == true)
            {
                // Everything worked
            }
            else
            {
                LOGGER.log(Level.WARNING, "Failed to send blockchain configuration to Game Server " + srvId);
                LOGGER.log(Level.WARNING, "Fail reason: " + responseObject.getString("error"));
            }
        });
    }
    
    private static void updateGameServerBalanceToDB(String srvId)
    {
        sendRequestToGS(srvId, "getGameServerBalance", new JSONArray()).thenAccept((responseObject) ->
        {
            if (responseObject.getBoolean("ok") == true)
            {
                try (Connection con = DatabaseFactory.getConnection();)
                {
                    try (PreparedStatement ps = con.prepareStatement("UPDATE gameservers SET balance = ? WHERE server_id = ?;"))
                    {
                        ps.setInt(1, Integer.parseInt(responseObject.getString("data")));
                        ps.setInt(2, Integer.parseInt(srvId));
                        ps.executeUpdate();
                    }
                    catch (Exception e)
                    {
                        LOGGER.log(Level.WARNING, "Could not update gameserver balance to database");
                        LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
                    }
                }
                catch (Exception e)
                {
                    LOGGER.log(Level.WARNING, "Error with database connection");
                    LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
                }
            }
            else
            {
                LOGGER.log(Level.WARNING, "Failed to update Game Server balance. Server id: " + srvId);
                LOGGER.log(Level.WARNING, "Fail reason: " + responseObject.getString("error"));
            }
        });
    }
    
    private static void refundExpiredWithdrawals(String srvId)
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
                        
                        addToCharacter(srvId, character, amount, "0").thenAccept(responseObject1 ->
                        {
                            if (responseObject1.getBoolean("ok") == true)
                            {
                                JSONObject responseObject2 = finalizeWithdraw(srvId, character, refund, amount);
                                
                                if (responseObject2.getBoolean("ok") == true)
                                {
                                    // Everything worked
                                }
                                else
                                {
                                    LOGGER.log(Level.WARNING, "Failed to refund player (finalize): " + character + " on server: " + srvId);
                                    LOGGER.log(Level.WARNING, "Fail reason: " + responseObject2.getString("error"));
                                }
                            }
                            else
                            {
                                LOGGER.log(Level.WARNING, "Failed to refund player (add): " + character + " on server: " + srvId);
                                LOGGER.log(Level.WARNING, "Fail reason: " + responseObject1.getString("error"));
                            }
                        });
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error with database connection");
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
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
            return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "Request to Game Server " + srvId + "failed. Could not get the GameServerTable instance"));
        }
        
        final GameServerInfo gsInfo = gsTable.getRegisteredGameServerById(Integer.parseInt(srvId));
        
        if (gsInfo == null)
        {
            return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "Request to Game Server " + srvId + "failed. Could not find Game Server info"));
        }
        
        final GameServerThread gsThread = gsInfo.getGameServerThread();
        
        if (gsThread == null)
        {
            return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "Request to Game Server " + srvId + "failed. Could not find Game Server thread"));
        }
        
        final int uniqueID = getNextID();
        
        // Then will happen this part of the code (result of the trigger) - START
        FiskPayResponseReceive.registerCallback(uniqueID, responseString ->
        {
            JSONObject responseObject;
            
            try
            {
                responseObject = new JSONObject(responseString);
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "Invalid responseString from Game Server " + srvId);
                LOGGER.log(Level.WARNING, "Response: " + responseString);
                
                responseObject = new JSONObject().put("ok", false).put("error", "responseString is not a JSONObject string");
            }
            
            if (!future.isDone())
            {
                future.complete(responseObject);
            }
        });
        // Then will happen this part of the code (result of the trigger) - END
        
        // First will happen this part of the code (trigger) - START
        final JSONObject requestObject = new JSONObject();
        
        requestObject.put("subject", subject);
        requestObject.put("info", info);
        
        final String requestString = requestObject.toString();
        
        gsThread.sendFiskPayRequest(uniqueID, requestString); // Forward the request
        // First will happen this part of the code (trigger) - END
        
        return future.completeOnTimeout(new JSONObject().put("ok", false).put("error", "Request to Game Server " + srvId + " with subject " + subject + " timed out"), 10, TimeUnit.SECONDS);
    }
}