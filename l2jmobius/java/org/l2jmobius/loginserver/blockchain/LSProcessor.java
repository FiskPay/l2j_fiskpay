// Login Server: LSProcessor.java
package org.l2jmobius.loginserver.blockchain;

import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;

public class LSProcessor
{
    public static JSONObject processLSRequest(JSONObject requestObject)
    {
        String subject = requestObject.getString("subject");
        JSONObject data = requestObject.getJSONObject("data");
        
        switch (subject)
        {
            case "getAccs":
            {
                if (data.has("walletAddress"))
                {
                    if (data.get("walletAddress") instanceof String)
                    {
                        return LSMethods.getAccounts(data.getString("walletAddress")); // Get accounts from Login Server database
                    }
                    
                    return new JSONObject().put("fail", "walletAddress not a String");
                }
                
                return new JSONObject().put("fail", "walletAddress undefined");
            }
            default:
            {
                return new JSONObject().put("fail", "subject unknown");
            }
        }
    }
    
    public static CompletableFuture<JSONObject> processGSRequest(JSONObject requestObject)
    {
        String subject = requestObject.getString("subject");
        JSONObject data = requestObject.getJSONObject("data");
        
        switch (subject)
        {
            case "getChars": // Forward request to game server
            {
                if (data.has("username"))
                {
                    if (data.get("username") instanceof String)
                    {
                        return LSMethods.sendRequestToGS(requestObject); // Calls an async method that sends a request to Game Server, and return the result
                    }
                    
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "username not a String"));
                }
                
                return CompletableFuture.completedFuture(new JSONObject().put("fail", "username undefined"));
            }
            default:
            {
                return CompletableFuture.completedFuture(new JSONObject().put("fail", "subject unknown"));
            }
        }
    }
    
    public static boolean logDeposit(String txHash, String from, String symbol, String amount, String server, String character)
    {
        return true;
    }
    
    public static boolean logWithdraw(String txHash, String to, String symbol, String amount, String server, String character, String refund)
    {
        return true;
    }
}
