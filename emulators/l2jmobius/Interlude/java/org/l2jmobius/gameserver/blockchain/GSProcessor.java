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

package org.l2jmobius.gameserver.blockchain;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Scrab
 */
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
                final String username = info.getString(0);

                responseObject = GSMethods.getAccountCharacters(username);
                break;
            }
            case "getCharacterBalance":
            {
                final String character = info.getString(0);

                responseObject = GSMethods.getCharacterBalance(character);
                break;
            }
            case "isCharacterOffline":
            {
                final String character = info.getString(0);

                responseObject = GSMethods.isCharacterOffline(character);
                break;
            }
            case "getCharacterUsername":
            {
                final String character = info.getString(0);

                responseObject = GSMethods.getCharacterUsername(character);
                break;
            }
            case "addToCharacter":
            {
                final String character = info.getString(0);
                final String amount = info.getString(1);

                responseObject = GSMethods.addToCharacter(character, amount);
                break;
            }
            case "removeFromCharacter":
            {
                final String character = info.getString(0);
                final String amount = info.getString(1);

                responseObject = GSMethods.removeFromCharacter(character, amount);
                break;
            }
            case "getGameServerMode":
            {
                responseObject = GSMethods.getGameServerMode();
                break;
            }
            case "setConfig":
            {
                final String rwdId = info.getString(0);
                final String wallet = info.getString(1);
                final String symbol = info.getString(2);
                
                responseObject = GSMethods.setConfig(rwdId, wallet, symbol);
                break;
            }
            case "getGameServerBalance":
            {
                responseObject = GSMethods.getGameServerBalance();
                break;
            }
            default:
            {
                responseObject = new JSONObject().put("ok", false).put("error", "GS request subject unknown");
                break;
            }
        }
        
        return responseObject.toString();
    }
}
