// Game Server: GSProcessor.java
package org.l2jmobius.gameserver.blockchain;

import org.json.JSONObject;

public class GSProcessor
{
    public static String processRequest(String requestString)
    {
        JSONObject requestObject = new JSONObject(requestString);
        JSONObject responseObject = new JSONObject();
        
        String subject = requestObject.getString("subject");
        JSONObject data = requestObject.getJSONObject("data");
        
        switch (subject)
        {
            
        }
        
        return responseObject.toString();
    }
}
