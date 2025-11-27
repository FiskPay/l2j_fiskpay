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

package org.l2jmobius.gameserver.network.loginserverpackets.login;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.network.base.BaseReadablePacket;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.blockchain.GSProcessor;

/**
 * @author Scrab
 */
public class FiskPayRequestReceive extends BaseReadablePacket
{
    public FiskPayRequestReceive(byte[] decrypt)
    {
        super(decrypt);
        readByte(); // Packet id, it is already processed.
        
        final int requestId = readInt(); // Read request ID
        final String requestString = readString(); // Read Login Server request data

        ThreadPool.execute(() ->
        {
            final String responseString = GSProcessor.processRequest(requestString); // Create a response depending on the Login Server request
            LoginServerThread.getInstance().sendFiskPayResponse(requestId, responseString); // Send response back to the Login Server
        });
    }
}
