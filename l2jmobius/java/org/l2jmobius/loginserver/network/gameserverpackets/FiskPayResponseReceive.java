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

package org.l2jmobius.loginserver.network.gameserverpackets;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.l2jmobius.commons.network.base.BaseReadablePacket;

public class FiskPayResponseReceive extends BaseReadablePacket
{
    private static final ConcurrentHashMap<Integer, Consumer<String>> CALLBACKS = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService CLEANER = Executors.newSingleThreadScheduledExecutor();
    
    public FiskPayResponseReceive(byte[] decrypt)
    {
        super(decrypt);
        readByte(); // Packet id, it is already processed.
        
        int requestId = readInt(); // Read request ID
        String responseData = readString(); // Read GameServer response
        
        Consumer<String> callback = CALLBACKS.remove(requestId);
        
        if (callback != null)
        {
            callback.accept(responseData); // Resolve the Login Server callback with the Game Server response data
        }
    }
    
    public static void registerCallback(int requestId, Consumer<String> callback)
    {
        CALLBACKS.put(requestId, callback);
        CLEANER.schedule(() -> CALLBACKS.remove(requestId), 20, TimeUnit.SECONDS);
    }
}
