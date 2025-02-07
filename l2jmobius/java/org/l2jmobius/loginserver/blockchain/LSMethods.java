// Login Server: ProcessorMethods.java
package org.l2jmobius.loginserver.blockchain;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.loginserver.GameServerTable;
import org.l2jmobius.loginserver.GameServerTable.GameServerInfo;
import org.l2jmobius.loginserver.GameServerThread;
import org.l2jmobius.loginserver.network.gameserverpackets.FiskPayResponseReceive;

public class LSMethods {
    private static final Logger LOGGER = Logger.getLogger(LSMethods.class.getName());
    private static final AtomicInteger counter = new AtomicInteger(0);

    protected static JSONObject getAccounts(String walletAddress) {

        JSONObject result;

        try (Connection con = DatabaseFactory.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT login FROM accounts WHERE wallet_address = ?")) {
            JSONArray accounts = new JSONArray();

            ps.setString(1, walletAddress);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                accounts.put(rs.getString("login"));
            }

            result = new JSONObject().put("data", accounts);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            result = new JSONObject().put("fail", "getAccounts SQL error");
        }

        return result;
    }

    protected static CompletableFuture<JSONObject> sendRequestToGS(JSONObject requestObject) {
        CompletableFuture<JSONObject> future = new CompletableFuture<>();

        String serverId = requestObject.getString("id");
        int gsID = Integer.parseInt(serverId);

        GameServerTable gsTable = GameServerTable.getInstance();

        if (gsTable == null) {
            return CompletableFuture
                    .completedFuture(new JSONObject().put("fail", "Could not get the GameServerTable instance"));
        }

        GameServerInfo gsInfo = gsTable.getRegisteredGameServerById(gsID);

        if (gsInfo == null) {
            return CompletableFuture.completedFuture(new JSONObject().put("fail", "Could not find Game Server info"));
        }

        GameServerThread gsThread = gsInfo.getGameServerThread();

        if (gsThread == null) {
            return CompletableFuture.completedFuture(new JSONObject().put("fail", "Could not find Game Server thread"));
        }

        int uniqueID = getNextID();

        FiskPayResponseReceive.registerCallback(uniqueID, responseString -> {
            JSONObject response;

            if (responseString == null || responseString.isEmpty()) {
                LOGGER.log(Level.WARNING, "Received an empty or null responseString");
                response = new JSONObject().put("fail", "responseString is empty or null");
            } else {
                try {
                    response = new JSONObject(responseString);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Invalid responseString: " + responseString, e);
                    response = new JSONObject().put("fail", "responseString is not a JSONObject string");
                }
            }

            if (!future.isDone()) {
                future.complete(response);
            }
        });

        gsThread.sendFiskPayRequest(uniqueID, requestObject.toString()); // Forward the request

        return future.completeOnTimeout(new JSONObject().put("fail", "Request to Game Server timed out"), 10,
                TimeUnit.SECONDS);
    }

    private static int getNextID() {
        return counter.updateAndGet(value -> (value == 1000000) ? 0 : value + 1);
    }
}
