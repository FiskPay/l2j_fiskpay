// Login Server: FiskPayListener.java
package org.l2jmobius.loginserver;

import com.fiskpay.l2j.Connector;
import com.fiskpay.l2j.Listener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.l2jmobius.loginserver.blockchain.LSProcessor;

import java.util.Calendar;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class FiskPayListener implements Listener
{
    private static final Logger LOGGER = Logger.getLogger(FiskPayListener.class.getName());
    private static final Logger BLOCKCHAIN_LOGGER = Logger.getLogger("blockchain");
    
    private static final String SYMBOL = "TOS";
    private static final String WALLET = "0x33a6657b336A2bef47769BCdcAdFCED8E78246AC";
    private static final String PASSWORD = "aa";
    
    private static volatile JSONArray _onlineServers = new JSONArray();
    
    private Connector _connector;
    
    @Override
    public void onLogDeposit(String txHash, String from, String symbol, String amount, String server, String character)
    {
        boolean success = LSProcessor.logDeposit(txHash, from, symbol, amount, server, character);
        
        if (success)
        {
            BLOCKCHAIN_LOGGER.info(getDateTime() + " | Deposit on " + getServerName(server) + ": " + from + " -> " + character + " = " + amount + " " + symbol);
        }
        else
        {
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | --------------------------------------- Failed Deposit Start ---------------------------------------");
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | TxHash: " + txHash);
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | From:   " + from);
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | To:     " + character);
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | Server: " + getServerName(server));
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | Amount: " + amount);
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | Token:  " + symbol);
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | ---------------------------------------- Failed Deposit End ----------------------------------------");
        }
    }
    
    @Override
    public void onLogWithdraw(String txHash, String to, String symbol, String amount, String server, String character, String refund)
    {
        boolean success = LSProcessor.logWithdraw(txHash, to, symbol, amount, server, character, refund);
        
        if (success)
        {
            BLOCKCHAIN_LOGGER.info(getDateTime() + " | Withdrawal on " + getServerName(server) + ": " + character + " -> " + to + " = " + amount + " " + symbol);
        }
        else
        {
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | -------------------------------------- Failed Withdrawal Start -------------------------------------");
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | TxHash: " + txHash);
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | From:   " + character);
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | To:     " + to);
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | Server: " + getServerName(server));
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | Amount: " + amount);
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | Token:  " + symbol);
            BLOCKCHAIN_LOGGER.warning(getDateTime() + " | --------------------------------------- Failed Withdrawal End --------------------------------------");
        }
    }
    
    @Override
    public void onRequest(JSONObject requestObject, Callback cb)
    {
        if (requestObject.has("id") && requestObject.get("id") instanceof String && requestObject.has("subject") && requestObject.get("subject") instanceof String && requestObject.has("data") && requestObject.get("data") instanceof JSONObject)
        {
            String serverId = requestObject.getString("id");
            
            if (serverId == "ls") // Request must be handled by the Login Server
            {
                cb.resolve(LSProcessor.processLSRequest(requestObject)); // Proccess the request and send it to the FiskPay server
            }
            else if (Pattern.matches("(^([1-9]|[1-9][0-9]|1[01][0-9]|12[0-7])$)", serverId)) // Forward the request to the appropriate Game Server. When a response is received from the Game Server, Login Server will handle it
            {
                LSProcessor.processGSRequest(requestObject).thenAccept(cb::resolve); // Forward the request to the Game Server and send the response back to the FiskPay server
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
        
        _connector.login(SYMBOL, WALLET, PASSWORD, _onlineServers).thenAccept(result ->
        {
            LOGGER.info(getClass().getSimpleName() + ": " + result);
        }).exceptionally(e ->
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
    
    public void updateServers(JSONArray onlineServers)
    {
        _onlineServers = onlineServers;
        _connector.onlineServers(onlineServers);
    }
    
    public static FiskPayListener getInstance()
    {
        return SingletonHolder.INSTANCE;
    }
    
    private FiskPayListener()
    {
        
        LOGGER.info(getClass().getSimpleName() + ": Connecting to FiskPay...");
        _connector = new Connector(this);
    }
    
    private static class SingletonHolder
    {
        private static final FiskPayListener INSTANCE = new FiskPayListener();
    }
    
    private static String getServerName(String id)
    {
        switch (id)
        {
            case "1":
                return "Bartz";
            case "2":
                return "Sieghardt";
            case "3":
                return "Kain";
            case "4":
                return "Lionna";
            case "5":
                return "Erica";
            case "6":
                return "Gustin";
            case "7":
                return "Devianne";
            case "8":
                return "Hindemith";
            case "9":
                return "Teon";
            case "10":
                return "Franz";
            case "11":
                return "Luna";
            case "12":
                return "Kastien";
            case "13":
                return "Airin";
            case "14":
                return "Staris";
            case "15":
                return "Ceriel";
            case "16":
                return "Fehyshar";
            case "17":
                return "Elhwynna";
            case "18":
                return "Ellikia";
            case "19":
                return "Shikken";
            case "20":
                return "Scryde";
            case "21":
                return "Frikios";
            case "22":
                return "Ophylia";
            case "23":
                return "Shakdun";
            case "24":
                return "Tarziph";
            case "25":
                return "Aria";
            default:
                return "Unknown";
        }
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