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
                
                if (!Pattern.matches("^0x[a-fA-F0-9]{40}$", data.getString("walletAddress")))
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
                
                return LSMethods.sendRequestToGS(requestObject);
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
                
                return LSMethods.sendRequestToGS(requestObject);
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
                
                return LSMethods.sendRequestToGS(requestObject);
            }
            case "doWithdraw": // This subject's requestObject is validated on the FiskPay Service, no checks needed.
            {                
                return LSMethods.sendRequestToGS(requestObject).thenCompose((resultObject)->{

                    if(resultObject.has("fail")){
                         return CompletableFuture.completedFuture(resultObject);
                    }

                    if(!LSMethods.createRefundToDB(data.getString("address"), data.getString("amount"), data.getString("id"), data.getString("character"), data.getString("refund"))){

                        return LSMethods.deliverToCharacter(data.getString("id"), data.getString("character"), data.getString("amount")).thenApply((reverted)->{

                            if(reverted){
                                return new JSONObject().put("fail", "Refund could not be created. Withdrawal reverted");
                            }

                            return new JSONObject().put("fail", "REFUND COULD NOT BE CREATED AND WITHDRAWAL NOT REVERTED");
                        });
                    }

                    return CompletableFuture.completedFuture(new JSONObject().put("data", true));
                });
            }
            default:
            {
                return CompletableFuture.completedFuture(new JSONObject().put("fail", "subject unknown"));
            }
        }
    }
    
    public static CompletableFuture<Boolean> logDeposit(String txHash, String from, String symbol, String amount, String srvId, String character)
    {
        LSMethods.logDepositToDB(txHash, from, symbol, amount, srvId, character);
        
        return LSMethods.deliverToCharacter(srvId, character, amount);
    }
    
    public static boolean logWithdraw(String txHash, String to, String symbol, String amount, String srvId, String character, String refund)
    {
        LSMethods.logWithdrawToDB(txHash, to, symbol, amount, srvId, character);
        
        return LSMethods.removeRefundFromDB(to, amount, srvId, character, refund); 
    }
}
