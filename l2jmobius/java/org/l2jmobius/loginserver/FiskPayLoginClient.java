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
import com.fiskpay.l2.Listener;

import org.json.JSONArray;
import org.json.JSONObject;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.loginserver.blockchain.LSProcessor;

import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class FiskPayLoginClient implements Listener
{
    private static final Logger LOGGER = Logger.getLogger(FiskPayLoginClient.class.getName());
    private static final Logger BLOCKCHAIN_LOGGER = Logger.getLogger("blockchain");
    
    private static final String SYMBOL = "USDT"; // USDT or TOS
    private static final String WALLET = "WALLET_ADDRESS_HERE"; // Add your public address here
    private static final String PASSWORD = "SUPER_SECRET_PASSWORD"; // Register here: https://l2.fiskpay.com/admin/Your_Public_Address_Here
    
    private static final Set<String> _onlineServers = ConcurrentHashMap.newKeySet();
    
    private Connector _connector;
    
    @Override
    public void onLogDeposit(String txHash, String from, String symbol, String amount, String server, String character)
    {
        LSProcessor.logDeposit(txHash, from, symbol, amount, server, character).thenAccept((logResult) ->
        {
            String nowDate = getDateTime();
            
            if (!logResult.has("fail"))
            {
                BLOCKCHAIN_LOGGER.info(nowDate + " | Deposit on " + getServerName(server) + ": " + from + " -> " + character + " = " + amount + " " + symbol);
            }
            else
            {
                BLOCKCHAIN_LOGGER.warning(nowDate + " | --------------------------------------- Failed Deposit Start ---------------------------------------");
                BLOCKCHAIN_LOGGER.warning(nowDate + " | TxHash:   " + txHash);
                BLOCKCHAIN_LOGGER.warning(nowDate + " | From:     " + from);
                BLOCKCHAIN_LOGGER.warning(nowDate + " | To:       " + character);
                BLOCKCHAIN_LOGGER.warning(nowDate + " | Server:   " + getServerName(server));
                BLOCKCHAIN_LOGGER.warning(nowDate + " | Amount:   " + amount);
                BLOCKCHAIN_LOGGER.warning(nowDate + " | Token:    " + symbol);
                BLOCKCHAIN_LOGGER.warning(nowDate + " | Message:  " + logResult.getString("fail"));
                BLOCKCHAIN_LOGGER.warning(nowDate + " | Action:   You must manualy reward " + amount + " " + symbol + " to player");
                BLOCKCHAIN_LOGGER.warning(nowDate + " | ---------------------------------------- Failed Deposit End ----------------------------------------");
            }
        });
    }
    
    @Override
    public void onLogWithdraw(String txHash, String to, String symbol, String amount, String server, String character, String refund)
    {
        String nowDate = getDateTime();
        JSONObject logResult = LSProcessor.logWithdraw(txHash, to, symbol, amount, server, character, refund);
        
        if (!logResult.has("fail"))
        {
            BLOCKCHAIN_LOGGER.info(nowDate + " | Withdrawal on " + getServerName(server) + ": " + character + " -> " + to + " = " + amount + " " + symbol);
        }
        else
        {
            BLOCKCHAIN_LOGGER.warning(nowDate + " | -------------------------------------- Failed Withdrawal Start -------------------------------------");
            BLOCKCHAIN_LOGGER.warning(nowDate + " | TxHash:   " + txHash);
            BLOCKCHAIN_LOGGER.warning(nowDate + " | From:     " + character);
            BLOCKCHAIN_LOGGER.warning(nowDate + " | To:       " + to);
            BLOCKCHAIN_LOGGER.warning(nowDate + " | Server:   " + getServerName(server));
            BLOCKCHAIN_LOGGER.warning(nowDate + " | Amount:   " + amount);
            BLOCKCHAIN_LOGGER.warning(nowDate + " | Token:    " + symbol);
            BLOCKCHAIN_LOGGER.warning(nowDate + " | Message:  " + logResult.getString("fail"));
            BLOCKCHAIN_LOGGER.warning(nowDate + " | Action:   You may manualy remove " + amount + " " + symbol + " from player");
            BLOCKCHAIN_LOGGER.warning(nowDate + " | --------------------------------------- Failed Withdrawal End --------------------------------------");
        }
    }
    
    @Override
    public void onRequest(JSONObject requestObject, Callback cb)
    {
        if (requestObject.has("id") && requestObject.get("id") instanceof String && requestObject.has("subject") && requestObject.get("subject") instanceof String && requestObject.has("data") && requestObject.get("data") instanceof JSONObject)
        {
            String serverId = requestObject.getString("id");
            
            if ("ls".equals(serverId)) // Request must be handled by the Login Server
            {
                cb.resolve(LSProcessor.processLSRequest(requestObject)); // Proccess the request and send it to the FiskPay server
            }
            else if (Pattern.matches("^([1-9]|[1-9][0-9]|1[01][0-9]|12[0-7])$", serverId)) // Forward the request to the appropriate Game Server. When a response is received from the Game Server, Login Server will handle it
            {
                if (_onlineServers.contains(serverId))
                {
                    LSProcessor.processGSRequest(requestObject).thenAccept(cb::resolve); // Forward the request to the Game Server and send the response back to the FiskPay server
                }
                else
                {
                    cb.resolve(new JSONObject().put("fail", "Game server " + getServerName(serverId) + " is not available"));
                }
            }
            else
            {
                cb.resolve(new JSONObject().put("fail", "Request object has an illegal id value"));
            }
        }
    }
    
    @Override
    public void onConnect()
    {
        LOGGER.info(getClass().getSimpleName() + ": Connection to FiskPay established");
        LOGGER.info(getClass().getSimpleName() + ": Signing in....");
        
        _connector.login(SYMBOL, WALLET, PASSWORD, new JSONArray(_onlineServers)).thenAccept((message) ->
        {
            LOGGER.info(getClass().getSimpleName() + ": " + message);
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
        LOGGER.warning(getClass().getSimpleName() + ": FiskPay temporary unavailable");
    }
    
    @Override
    public void onError(Exception e)
    {
        LOGGER.warning(getClass().getSimpleName() + ": Connector error");
        LOGGER.warning(getClass().getSimpleName() + ": " + e.getMessage());
    }
    
    public void updateServers(int serverId, boolean isConnected)
    {
        String id = Integer.toString(serverId);
        
        if (isConnected && !_onlineServers.contains(id))
        {
            _onlineServers.add(id);
        }
        else if (!isConnected && _onlineServers.contains(id))
        {
            _onlineServers.remove(id);
        }
        
        _connector.onlineServers(new JSONArray(_onlineServers));
    }
    
    public static FiskPayLoginClient getInstance()
    {
        return SingletonHolder.INSTANCE;
    }
    
    private FiskPayLoginClient()
    {
        LOGGER.info(getClass().getSimpleName() + ": Connecting to FiskPay...");
        
        _connector = new Connector(this);

        ThreadPool.scheduleAtFixedRate(() ->
        {
            LSProcessor.refundPlayers();
        }, 0, 150000);

        ThreadPool.scheduleAtFixedRate(() ->
        {
            for (String srvId : _onlineServers)
            {
                LSProcessor.updateGameServerBalance(srvId);
            }
        }, 75000, 150000);
    }
    
    private static class SingletonHolder
    {
        private static final FiskPayLoginClient INSTANCE = new FiskPayLoginClient();
    }
    
    private static String getServerName(String id)
    {        
        return getServerName(Integer.parseInt(id));
    }

    private static String getServerName(int id)
    {
        
        GameServerTable gsTable = GameServerTable.getInstance();
        
        if (gsTable != null)
        {
            return gsTable.getServerNameById(id);
        }
        
        return "Unknown";
    }
    
    private static String getDateTime()
    {
        Calendar currentDate = Calendar.getInstance();
        
        int day = currentDate.get(Calendar.DAY_OF_MONTH);
        int month = currentDate.get(Calendar.MONTH) + 1; // Month is 0-based in Calendar
        int year = currentDate.get(Calendar.YEAR);
        int hour = currentDate.get(Calendar.HOUR_OF_DAY);
        int minute = currentDate.get(Calendar.MINUTE);
        int second = currentDate.get(Calendar.SECOND);
        
        // Format the date and time similar to the JS version
        String datetime = String.format("%02d/%02d/%d @ %02d:%02d:%02d", day, month, year, hour, minute, second);
        
        return datetime;
    }
}