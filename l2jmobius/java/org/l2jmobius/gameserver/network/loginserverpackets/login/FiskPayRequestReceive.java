// Game Server: FiskPayRequestReceive.java
package org.l2jmobius.gameserver.network.loginserverpackets.login;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.network.base.BaseReadablePacket;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.blockchain.GSProcessor;

public class FiskPayRequestReceive extends BaseReadablePacket
{
    public FiskPayRequestReceive(byte[] decrypt)
    {
        super(decrypt);
        readByte(); // Packet id, it is already processed.
        
        int requestId = readInt(); // Read request ID
        String requestString = readString(); // Read Login Server request data

        ThreadPool.execute(() ->
        {
            String responseString = GSProcessor.processRequest(requestString); // Create a response depending on the Login Server request
            LoginServerThread.getInstance().sendFiskPayResponse(requestId, responseString); // Send response back to the Login Server
        });
    }
}
