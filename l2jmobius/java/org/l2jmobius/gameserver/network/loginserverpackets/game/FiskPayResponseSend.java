// Game Server: FiskPayResponsePacket.java
package org.l2jmobius.gameserver.network.loginserverpackets.game;

import org.l2jmobius.commons.network.base.BaseWritablePacket;

public class FiskPayResponseSend extends BaseWritablePacket
{
    public FiskPayResponseSend(int requestId, String responseData)
    {
        writeByte(0x0C);
        writeInt(requestId);
        writeString(responseData);
    }
}