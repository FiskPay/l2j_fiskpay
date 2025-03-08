// Game Server: GSProcessor.java
package org.l2jmobius.gameserver.blockchain;

import org.json.JSONArray;
import org.json.JSONObject;

public class GSProcessor
{
    public static String processRequest(String requestString)
    {
        JSONObject requestObject = new JSONObject(requestString);
        JSONObject responseObject = new JSONObject();
        
        String subject = requestObject.getString("subject");
        JSONArray info = requestObject.getJSONArray("info");
        
        switch (subject)
        {
            case "getAccountCharacters":
            {
                String username =  info.getString(0);
                responseObject = GSMethods.getAccountCharacters(username);
                break;
            }
            case "getCharacterBalance":
            {
                String character =  info.getString(0);
                responseObject = GSMethods.getCharacterBalance(character);
                break;
            }
            case "isCharacterOffline":
            {
                String character =  info.getString(0);
                responseObject = GSMethods.isCharacterOffline(character);
                break;
            }
            case "getCharacterUsername":
            {
                String character =  info.getString(0);
                responseObject = GSMethods.getCharacterUsername(character);
                break;
            }
            case "addToCharacter":
            {
                String character =  info.getString(0);
                String amount =  info.getString(1);
                responseObject = GSMethods.addToCharacter(character, amount);
                break;
            }
            case "removeFromCharacter":
            {
                String character =  info.getString(0);
                String amount =  info.getString(1);
                responseObject = GSMethods.removeFromCharacter(character, amount);
                break;
            }
            case "isGameServerAvailable":
            {
                responseObject = GSMethods.isGameServerAvailable();
                break;
            }
            case "fetchGameServerBalance":
            {
                responseObject = GSMethods.fetchGameServerBalance();
                break;
            }
            default:
            {
                responseObject =  new JSONObject().put("fail","GS request subject unknown");
                break;
            }
        }
        
        return responseObject.toString();
    }
}