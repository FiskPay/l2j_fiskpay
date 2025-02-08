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
                if (!data.has("walletAddress"))
                {
                    return new JSONObject().put("fail", "walletAddress undefined");
                }
                
                if (!(data.get("walletAddress") instanceof String))
                {
                    return new JSONObject().put("fail", "walletAddress not a String");
                }
                
                return LSMethods.getAccounts(data.getString("walletAddress")); // Get accounts from Login Server
            }
            case "getClientBal":
            {
                // to do
                // return LSMethods.getClientBalance(); // Get total balance from all Game Servers
            }
            case "addAcc":
            {
                if (!data.has("username"))
                {
                    return new JSONObject().put("fail", "username undefined");
                }
                
                if (!data.has("password"))
                {
                    return new JSONObject().put("fail", "password undefined");
                }
                
                if (!data.has("walletAddress"))
                {
                    return new JSONObject().put("fail", "walletAddress undefined");
                }
                
                if (!(data.get("username") instanceof String))
                {
                    return new JSONObject().put("fail", "username not a String");
                }
                
                if (!(data.get("password") instanceof String))
                {
                    return new JSONObject().put("fail", "password not a String");
                }
                
                if (!(data.get("walletAddress") instanceof String))
                {
                    return new JSONObject().put("fail", "walletAddress not a String");
                }
                
                // to do
                // return LSMethods.addAccount(); // Links the account to the wallet address
            }
            case "removeAcc":
            {
                if (!data.has("username"))
                {
                    return new JSONObject().put("fail", "username undefined");
                }
                
                if (!data.has("password"))
                {
                    return new JSONObject().put("fail", "password undefined");
                }
                
                if (!data.has("walletAddress"))
                {
                    return new JSONObject().put("fail", "walletAddress undefined");
                }
                
                if (!(data.get("username") instanceof String))
                {
                    return new JSONObject().put("fail", "username not a String");
                }
                
                if (!(data.get("password") instanceof String))
                {
                    return new JSONObject().put("fail", "password not a String");
                }
                
                if (!(data.get("walletAddress") instanceof String))
                {
                    return new JSONObject().put("fail", "walletAddress not a String");
                }
                
                // to do
                // return LSMethods.removeAccount(); // Unlinks the account from the wallet address
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
            case "doWithdraw":
            {
                if (!data.has("address"))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "address undefined"));
                }

                if (!data.has("amount"))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "amount undefined"));
                }

                if (!data.has("character"))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "character undefined"));
                }

                if (!data.has("refund"))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "refund undefined"));
                }
                
                if (!(data.get("address") instanceof String))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "address not a String"));
                }

                if (!(data.get("amount") instanceof String))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "amount not a String"));
                }

                if (!(data.get("character") instanceof String))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "character not a String"));
                }

                if (!(data.get("refund") instanceof String))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "refund not a String"));
                }
                
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
