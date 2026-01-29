/*
* Copyright (c) 2026 FiskPay
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

package handlers.voicedcommandhandlers;

import org.l2jmobius.gameserver.BlockchainEndpoint;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.PledgeCrest;
import org.l2jmobius.commons.util.StringUtil;

/**
 * @author Scrab
 */
public class Crypto implements IVoicedCommandHandler
{
    private static final String[] VOICED_COMMANDS =
    {
        "crypto"
    };
    
    private static final int CREST_ID = 700100;
    
    @Override
    public boolean onCommand(String command, Player activeChar, String params)
    {
        if (activeChar == null || !BlockchainEndpoint.isSet())
        {
            return false;
        }
        
        if (command.equals("crypto"))
        {
            activeChar.sendPacket(new PledgeCrest(CREST_ID, BlockchainEndpoint.getQRCodeData()));
            
            final NpcHtmlMessage html = new NpcHtmlMessage();
            final StringBuilder sb = new StringBuilder();
            
            StringUtil.append(sb, "<html>");
            StringUtil.append(sb, "<title>Crypto Panel</title>");
            StringUtil.append(sb, "<body><center><br><br><img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\">");
            StringUtil.append(sb, "Scan the QR code to open the panel");
            StringUtil.append(sb, "<br>");
            StringUtil.append(sb, "<img src=\"Crest.crest_" + ServerConfig.SERVER_ID + "_" + CREST_ID + "\" width=\"256\" height=\"256\">");
            StringUtil.append(sb, "<br>");
            StringUtil.append(sb, "</center></body>");
            StringUtil.append(sb, "</html>");
            
            html.setHtml(sb.toString());
            activeChar.sendPacket(html);
        }
        
        return true;
    }
    
    @Override
    public String[] getCommandList()
    {
        return VOICED_COMMANDS;
    }
}