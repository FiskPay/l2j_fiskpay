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
                
                String walletAddress = data.getString("walletAddress");
                
                if (!Pattern.matches("^0x[a-fA-F0-9]{40}$", walletAddress))
                {
                    return new JSONObject().put("fail", "Improper walletAddress");
                }
                
                return LSMethods.getAccounts(walletAddress); // Get accounts from Login Server database
            }
            case "getClientBal":
            {
                return LSMethods.getClientBalance(); // Get total balance from all Game Servers
            }
            case "addAcc": // This subject's requestObject is validated on the FiskPay Service, no checks needed.
            {
                String username = data.getString("username");
                String password = data.getString("password");
                String walletAddress = data.getString("walletAddress");
                
                return LSMethods.addAccount(username, password, walletAddress); // Links the account to the wallet address
            }
            case "removeAcc": // This subject's requestObject is validated on the FiskPay Service, no checks needed.
            {
                String username = data.getString("username");
                String password = data.getString("password");
                String walletAddress = data.getString("walletAddress");
                
                return LSMethods.removeAccount(username, password, walletAddress); // Unlinks the account from the wallet address
            }
            default:
            {
                return new JSONObject().put("fail", "Unknown subject: " + subject);
            }
        }
    }
    
    public static CompletableFuture<JSONObject> processGSRequest(JSONObject requestObject)
    {
        String srvId = requestObject.getString("id");
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
                
                String username = data.getString("username");
                
                return LSMethods.getAccountCharacters(srvId, username);
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
                
                String character = data.getString("character");
                
                return LSMethods.getCharacterBalance(srvId, character);
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
                
                String character = data.getString("character");
                
                return LSMethods.isCharacterOffline(srvId, character);
            }
            case "isAvailable":
            {
                return LSMethods.isGameServerAvailable(srvId);
            }
            case "doWithdraw": // This subject's requestObject is validated on the FiskPay Service, no checks needed
            {
                String walletAddress = data.getString("walletAddress");
                String character = data.getString("character");
                String refund = data.getString("refund");
                String amount = data.getString("amount");
                
                if (!LSMethods.isNewWithdraw(srvId, character, refund, amount))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("fail", "Trying to exploit? Help us grow, report your findings"));
                }
                
                return LSMethods.getCharacterUsername(srvId, character).thenCompose((responseObject0) ->
                {
                    if (responseObject0.has("fail"))
                    {
                        return CompletableFuture.completedFuture(responseObject0);
                    }
                    
                    String username = responseObject0.getString("data");
                    
                    if (!LSMethods.isWalletOwner(username, walletAddress))
                    {
                        return CompletableFuture.completedFuture(new JSONObject().put("fail", "Wallet ownership verification failed"));
                    }
                    
                    return LSMethods.removeFromCharacter(srvId, character, amount).thenCompose((responseObject1) ->
                    {
                        if (responseObject1.has("fail"))
                        {
                            return CompletableFuture.completedFuture(responseObject1);
                        }
                        
                        if (!LSMethods.createNewWithdraw(srvId, character, refund, amount))
                        {
                            return LSMethods.addToCharacter(srvId, character, amount).thenApply((responseObject2) ->
                            {
                                if (responseObject2.has("fail"))
                                {
                                    return responseObject2;
                                }
                                
                                if (responseObject2.getBoolean("data"))
                                {
                                    return new JSONObject().put("fail", "Refund could not be created. Withdrawal reverted");
                                }
                                
                                return new JSONObject().put("fail", "REFUND COULD NOT BE CREATED AND WITHDRAWAL NOT REVERTED");
                            });
                        }
                        
                        return CompletableFuture.completedFuture(new JSONObject().put("data", true));
                    });
                });
            }
            default:
            {
                return CompletableFuture.completedFuture(new JSONObject().put("fail", "subject unknown"));
            }
        }
    }
    
    public static CompletableFuture<JSONObject> logDeposit(String txHash, String from, String symbol, String amount, String srvId, String character)
    {
        LSMethods.logDepositToDB(txHash, from, symbol, amount, srvId, character);
        
        return LSMethods.addToCharacter(srvId, character, amount);
    }
    
    public static JSONObject logWithdraw(String txHash, String to, String symbol, String amount, String srvId, String character, String refund)
    {
        LSMethods.logWithdrawToDB(txHash, to, symbol, amount, srvId, character);
        
        return LSMethods.finalizeWithdraw(srvId, character, refund, amount);
    }
    
    public static void refundPlayers()
    {
        LSMethods.refundPlayers();
    }
        
    public static void updateGameServerBalance(String serverId)
    {
        LSMethods.updateGameServerBalance(serverId);
    }
}