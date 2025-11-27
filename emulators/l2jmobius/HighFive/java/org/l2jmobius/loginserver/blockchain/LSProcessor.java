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

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.json.JSONObject;

/**
 * @author Scrab
 */
public class LSProcessor
{
    public static JSONObject processLSRequest(JSONObject requestObject)
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
                
                return LSMethods.getAccounts(walletAddress); // Get wallet accounts from Login Server database
            }
            case "getClientBal":
            {
                return LSMethods.getClientBalance(); // Get total balance from all Game Servers
            }
            case "linkAcc": // This subject's requestObject is validated on the FiskPay Service, no checks needed.
            {
                final String username = data.getString("username");
                final String password = data.getString("password");
                final String walletAddress = data.getString("walletAddress");
                
                return LSMethods.linkAccount(username, password, walletAddress); // Links the account to the wallet address
            }
            case "unlinkAcc": // This subject's requestObject is validated on the FiskPay Service, no checks needed.
            {
                final String username = data.getString("username");
                final String password = data.getString("password");
                final String walletAddress = data.getString("walletAddress");
                
                return LSMethods.unlinkAccount(username, password, walletAddress); // Unlinks the account from the wallet address
            }
            default:
            {
                return new JSONObject().put("ok", false).put("error", "Unknown subject: " + subject);
            }
        }
    }
    
    public static CompletableFuture<JSONObject> processGSRequest(JSONObject requestObject)
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
                
                return LSMethods.getAccountCharacters(srvId, username);
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
                
                return LSMethods.getCharacterBalance(srvId, character);
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
                
                return LSMethods.isCharacterOffline(srvId, character);
            }
            case "getGSMode":
            {
                return LSMethods.getGameServerMode(srvId);
            }
            case "requestWithdraw": // This subject's requestObject is validated on the FiskPay Service, no checks needed
            {
                final String walletAddress = data.getString("walletAddress");
                final String character = data.getString("character");
                final String refund = data.getString("refund");
                final String amount = data.getString("amount");
                
                if (!LSMethods.isNewWithdraw(srvId, character, refund, amount))
                {
                    return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "Trying to exploit? Help us grow, report your findings"));
                }
                
                return LSMethods.getCharacterUsername(srvId, character).thenCompose((responseObject0) ->
                {
                    if (responseObject0.has("error"))
                    {
                        return CompletableFuture.completedFuture(responseObject0);
                    }
                    
                    final String username = responseObject0.getString("data");
                    
                    if (!LSMethods.isWalletOwner(username, walletAddress))
                    {
                        return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "Wallet ownership verification failed"));
                    }
                    
                    return LSMethods.removeFromCharacter(srvId, character, amount).thenCompose((responseObject1) ->
                    {
                        if (responseObject1.has("error"))
                        {
                            return CompletableFuture.completedFuture(responseObject1);
                        }
                        
                        if (!LSMethods.createNewWithdraw(srvId, character, refund, amount))
                        {
                            return LSMethods.addToCharacter(srvId, character, amount).thenApply((responseObject2) ->
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
                return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "LS request subject unknown"));
            }
        }
    }
    
    public static CompletableFuture<JSONObject> logDeposit(String txHash, String from, String symbol, String amount, String srvId, String character)
    {
        if(LSMethods.logDepositToDB(txHash, from, symbol, amount, srvId, character) == true)
        {
            return LSMethods.addToCharacter(srvId, character, amount);
        }
        
        return CompletableFuture.completedFuture(new JSONObject().put("ok", false).put("error", "logDepositToDB in LSMethods.java had an error"));
    }
    
    public static JSONObject logWithdraw(String txHash, String to, String symbol, String amount, String srvId, String character, String refund)
    {
        if(LSMethods.logWithdrawToDB(txHash, to, symbol, amount, srvId, character) == true)
        {
            return LSMethods.finalizeWithdraw(srvId, character, refund, amount);
        }
        
        return new JSONObject().put("ok", false).put("error", "logWithdrawToDB in LSMethods.java had an error");
    }
    
    public static void setConfig(String srvId, String wallet, String symbol)
    {
        LSMethods.setConfig(srvId, wallet, symbol);
    }
    
    public static void refundExpitedWithdraws(String srvId)
    {
        LSMethods.refundExpitedWithdraws(srvId);
    }
    
    public static void updateGameServerBalanceToDB(String srvId)
    {
        LSMethods.updateGameServerBalanceToDB(srvId);
    }
}
