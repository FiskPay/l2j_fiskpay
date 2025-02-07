// Login Server: FiskPayRequestSend.java
package org.l2jmobius.loginserver.network.loginserverpackets;

import org.l2jmobius.commons.network.base.BaseWritablePacket;

public class FiskPayRequestSend extends BaseWritablePacket
{
    public FiskPayRequestSend(int requestId, String requestData)
    {
        writeByte(0x07);
        writeInt(requestId);
        writeString(requestData);
    }
}
