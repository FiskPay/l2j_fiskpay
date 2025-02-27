// Login Server: FiskPayResponseReceive.java
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
