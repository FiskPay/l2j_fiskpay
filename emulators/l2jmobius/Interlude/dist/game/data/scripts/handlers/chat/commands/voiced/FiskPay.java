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
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package handlers.chat.commands.voiced;

import java.security.SecureRandom;

import org.l2jmobius.gameserver.BlockchainEndpoint;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.PledgeCrest;
import org.l2jmobius.commons.util.StringUtil;

/**
 * @author Scrab
 */
public class FiskPay implements IVoicedCommandHandler
{
    private static final String[] VOICED_COMMANDS =
    {
        "crypto",
        "otp"
    };

    private static final int CREST_ID_UPPER = 700100;
    private static final int CREST_ID_LOWER = 700101;

    private static final int ONE_TIME_PASSWORD_LENGTH = 6;
    private static final String ONE_TIME_PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public boolean onCommand(String command, Player activeChar, String params)
    {
        if (activeChar == null || !BlockchainEndpoint.isSet())
        {
            return false;
        }

        if (command.equals("crypto"))
        {
            activeChar.sendPacket(new PledgeCrest(CREST_ID_UPPER, BlockchainEndpoint.getQRCodeDataUpper()));
            activeChar.sendPacket(new PledgeCrest(CREST_ID_LOWER, BlockchainEndpoint.getQRCodeDataLower()));

            final NpcHtmlMessage html = new NpcHtmlMessage();
            final StringBuilder sb = new StringBuilder();

            StringUtil.append(sb, "<html>");
            StringUtil.append(sb, "<title>Crypto Panel</title>");
            StringUtil.append(sb, "<body><center><br><br><img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\">");
            StringUtil.append(sb, "Scan the QR code to open the panel");
            StringUtil.append(sb, "<br>");
            StringUtil.append(sb, "<img src=\"Crest.crest_" + ServerConfig.SERVER_ID + "_" + CREST_ID_UPPER + "\" width=256 height=128>");
            StringUtil.append(sb, "<img src=\"Crest.crest_" + ServerConfig.SERVER_ID + "_" + CREST_ID_LOWER + "\" width=256 height=128>");
            StringUtil.append(sb, "<br>");
            StringUtil.append(sb, "</center></body>");
            StringUtil.append(sb, "</html>");

            html.setHtml(sb.toString());
            activeChar.sendPacket(html);
        }
        else if (command.equals("otp"))
        {
            final String username = activeChar.getAccountName();
            final String oneTimePassword = generateOneTimePassword();

            activeChar.sendPacket(new CreatureSay(null, ChatType.WHISPER, "Info", "--------------- New One Time Password --------------"));
            activeChar.sendPacket(new CreatureSay(null, ChatType.WHISPER, "Info", "Account Username:  " + username));
            activeChar.sendPacket(new CreatureSay(null, ChatType.WHISPER, "Info", "One-Time Password: " + oneTimePassword));
            activeChar.sendPacket(new CreatureSay(null, ChatType.WHISPER, "Info", "Is valid for: 1 minute"));

            LoginServerThread.getInstance().sendFiskPayOTP(username, oneTimePassword); // Forward OTP to Login Server for temporary validation storage.
        }

        return true;
    }

    private static String generateOneTimePassword()
    {
        final StringBuilder sb = new StringBuilder(ONE_TIME_PASSWORD_LENGTH);

        for (int i = 0; i < ONE_TIME_PASSWORD_LENGTH; i++)
        {
            sb.append(ONE_TIME_PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(ONE_TIME_PASSWORD_CHARS.length())));
        }

        return sb.toString();
    }

    @Override
    public String[] getCommandList()
    {
        return VOICED_COMMANDS;
    }
}
