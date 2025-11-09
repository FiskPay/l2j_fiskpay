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

package org.l2jmobius.loginserver;

import com.fiskpay.l2.Connector;

import org.json.JSONArray;
import org.json.JSONObject;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.loginserver.blockchain.LSProcessor;

import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author Scrab
 */
public class BlockchainClient implements Connector.Interface
{
    private static final Logger LOGGER = Logger.getLogger(BlockchainClient.class.getName());
    private static final Logger BLOCKCHAIN_LOGGER = Logger.getLogger("blockchain");
    
    private static final String SYMBOL = Config.BLOCKCHAIN_SYMBOL;
    private static final String WALLET = Config.BLOCKCHAIN_WALLET;
    private static final String PASSWORD = Config.BLOCKCHAIN_PASSWORD;
    
    private final Set<String> _onlineServers = ConcurrentHashMap.newKeySet();
    private final CompletableFuture<Void> _connectionResult = new CompletableFuture<>();
    
    private boolean _signedIn = false;
    
    private Connector _connector;
    
    @Override
    public void onLogDeposit(String txHash, String from, String symbol, String amount, String srvId, String character)
    {
        LSProcessor.logDeposit(txHash, from, symbol, amount, srvId, character).thenAccept((logResult) ->
        {            
            if (logResult.getBoolean("ok") == true)
            {
                BLOCKCHAIN_LOGGER.info("Deposit on " + getServerName(srvId) + ": " + from + " -> " + character + " = " + amount + " " + symbol);
            }
            else
            {
                BLOCKCHAIN_LOGGER.warning( "--------------------------------------- Failed Deposit Start ---------------------------------------");
                BLOCKCHAIN_LOGGER.warning( "TxHash:   " + txHash);
                BLOCKCHAIN_LOGGER.warning( "From:     " + from);
                BLOCKCHAIN_LOGGER.warning( "To:       " + character);
                BLOCKCHAIN_LOGGER.warning( "Server:   " + getServerName(srvId));
                BLOCKCHAIN_LOGGER.warning( "Amount:   " + amount);
                BLOCKCHAIN_LOGGER.warning( "Token:    " + symbol);
                BLOCKCHAIN_LOGGER.warning( "Message:  " + logResult.getString("error"));
                BLOCKCHAIN_LOGGER.warning( "Action:   You must manualy reward " + amount + " " + symbol + " to player");
                BLOCKCHAIN_LOGGER.warning( "---------------------------------------- Failed Deposit End ----------------------------------------");
            }
        });
    }
    
    @Override
    public void onLogWithdraw(String txHash, String to, String symbol, String amount, String srvId, String character, String refund)
    {
        final JSONObject logResult = LSProcessor.logWithdraw(txHash, to, symbol, amount, srvId, character, refund);
        
        if (logResult.getBoolean("ok") == true)
        {
            BLOCKCHAIN_LOGGER.info("Withdrawal on " + getServerName(srvId) + ": " + character + " -> " + to + " = " + amount + " " + symbol);
        }
        else
        {
            BLOCKCHAIN_LOGGER.warning( "-------------------------------------- Failed Withdrawal Start -------------------------------------");
            BLOCKCHAIN_LOGGER.warning( "TxHash:   " + txHash);
            BLOCKCHAIN_LOGGER.warning( "From:     " + character);
            BLOCKCHAIN_LOGGER.warning( "To:       " + to);
            BLOCKCHAIN_LOGGER.warning( "Server:   " + getServerName(srvId));
            BLOCKCHAIN_LOGGER.warning( "Amount:   " + amount);
            BLOCKCHAIN_LOGGER.warning( "Token:    " + symbol);
            BLOCKCHAIN_LOGGER.warning( "Message:  " + logResult.getString("error"));
            BLOCKCHAIN_LOGGER.warning( "Action:   You may manualy remove " + amount + " " + symbol + " from player");
            BLOCKCHAIN_LOGGER.warning( "--------------------------------------- Failed Withdrawal End --------------------------------------");
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
                cb.resolve(LSProcessor.processLSRequest(requestObject)); // Proccess the request and send it to the FiskPay server
            }
            else if (Pattern.matches("^([1-9]|[1-9][0-9]|1[01][0-9]|12[0-7])$", srvId)) // Forward the request to the appropriate Game Server. When a response is received from the Game Server, Login Server will handle it
            {
                if (_onlineServers.contains(srvId))
                {
                    LSProcessor.processGSRequest(requestObject).thenAccept(cb::resolve); // Forward the request to the Game Server and send the response back to the FiskPay server
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
            LSProcessor.setConfig(srvId, WALLET, SYMBOL);
        }
        else if (!isConnected && _onlineServers.contains(srvId))
        {
            _onlineServers.remove(srvId);
        }
        
        _connector.renewServers(new JSONArray(_onlineServers));
    }
    
    public boolean isSigned()
    {
        return _signedIn;
    }
    
    public static BlockchainClient getInstance()
    {
        return SingletonHolder.INSTANCE;
    }
    
    private BlockchainClient()
    {
        LOGGER.info(getClass().getSimpleName() + ": Connecting...");
        
        _connector = new Connector(this);
        
        ThreadPool.scheduleAtFixedRate(() ->
        {
            for (String srvId : _onlineServers)
            {
                LSProcessor.refundExpitedWithdraws(srvId);
            }
        }, 0, 150000);
        
        ThreadPool.scheduleAtFixedRate(() ->
        {
            for (String srvId : _onlineServers)
            {
                LSProcessor.updateGameServerBalanceToDB(srvId);
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
        private static final BlockchainClient INSTANCE = new BlockchainClient();
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
    
    private static String getDateTime()
    {
        Calendar currentDate = Calendar.getInstance();
        
        final int day = currentDate.get(Calendar.DAY_OF_MONTH);
        final int month = currentDate.get(Calendar.MONTH) + 1; // Month is 0-based in Calendar
        final int year = currentDate.get(Calendar.YEAR);
        final int hour = currentDate.get(Calendar.HOUR_OF_DAY);
        final int minute = currentDate.get(Calendar.MINUTE);
        final int second = currentDate.get(Calendar.SECOND);
        
        // Format the date and time similar to the JS version
        final String datetime = String.format("%02d/%02d/%d @ %02d:%02d:%02d", day, month, year, hour, minute, second);
        
        return datetime;
    }
}