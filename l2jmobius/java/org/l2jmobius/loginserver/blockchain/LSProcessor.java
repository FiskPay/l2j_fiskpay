// Login Server: LSProcessor.java
package org.l2jmobius.loginserver.blockchain;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

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
                if (!data.has("walletAddress"))
                {
                    return new JSONObject().put("fail", "walletAddress undefined");
                }
                
                if (!(data.get("walletAddress") instanceof String))
                {
                    return new JSONObject().put("fail", "walletAddress not a String");
                }
                
                if (!Pattern.matches("/^0x[a-fA-F0-9]{40}$/", data.getString("walletAddress")))
                {
                    return new JSONObject().put("fail", "Improper walletAddress");
                }
                
                return LSMethods.getAccounts(data.getString("walletAddress")); // Get accounts from Login Server database
            }
            case "getClientBal":
            {
                return LSMethods.getClientBalance(); // Get total balance from all Game Servers
            }
            case "addAcc": // This subject's requestObject is validated on the FiskPay Service, no checks needed.
            {
                return LSMethods.addAccount(data.getString("username"), data.getString("password"), data.getString("walletAddress")); // Links the account to the wallet address
            }
            case "removeAcc": // This subject's requestObject is validated on the FiskPay Service, no checks needed.
            {                
                return LSMethods.removeAccount(data.getString("username"), data.getString("password"), data.getString("walletAddress")); // Unlinks the account from the wallet address
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
            case "getChars":
            {
                if (!data.has("username"))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "username undefined"));
                }
                
                if (!(data.get("username") instanceof String))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "username not a String"));
                }
                
                return LSMethods.sendRequestToGS(requestObject); // Calls an async method that sends a "getChars" request to Game Server, and return the result
            }
            case "getCharBal":
            {
                if (!data.has("character"))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "character undefined"));
                }
                
                if (!(data.get("character") instanceof String))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "character not a String"));
                }
                
                return LSMethods.sendRequestToGS(requestObject); // Calls an async method that sends a "getCharBal" request to Game Server, and return the result
            }
            case "isOffline":
            {
                if (!data.has("character"))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "character undefined"));
                }
                
                if (!(data.get("character") instanceof String))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "character not a String"));
                }
                
                return LSMethods.sendRequestToGS(requestObject); // Calls an async method that sends a "isOffline" request to Game Server, and return the result
            }
            case "doWithdraw": // This subject's requestObject is validated on the FiskPay Service, no checks needed.
            {                
                return LSMethods.sendRequestToGS(requestObject); // Calls an async method that sends a "doWithdraw" request to Game Server, and return the result
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