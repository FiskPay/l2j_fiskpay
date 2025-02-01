package com.fiskpay.l2j;

import java.util.concurrent.CompletableFuture;

import org.json.JSONArray;
import org.json.JSONObject;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;

public class Connector {

    private Socket _socket;
    private Listener _listener;

    public Connector(Listener listener) {

        _listener = listener;

        try {

            _socket = IO.socket("wss://ds.fiskpay.com:42099");

            _socket.on("logDeposit", args -> {

                if (args.length == 6) {

                    String txHash = (String) args[0];
                    String from = (String) args[1];
                    String symbol = (String) args[2];
                    String amount = (String) args[3];
                    String server = (String) args[4];
                    String character = (String) args[5];

                    _listener.onLogDeposit(txHash, from, symbol, amount, server, character);
                }
            });

            _socket.on("logWithdrawal", args -> {

                if (args.length == 7) {

                    String txHash = (String) args[0];
                    String to = (String) args[1];
                    String symbol = (String) args[2];
                    String amount = (String) args[3];
                    String server = (String) args[4];
                    String character = (String) args[5];
                    String refund = (String) args[6];

                    _listener.onLogWithdraw(txHash, to, symbol, amount, server, character, refund);
                }
            });

            _socket.on("connect", _ -> {
                _listener.onConnect();
            });

            _socket.on("disconnect", _ -> {
                _listener.onDisconnect();
            });

            _socket.on("request", args -> {

                if (args.length == 2) {

                    JSONObject requestObject = (JSONObject) args[0];
                    Ack ack = (Ack) args[1];

                    _listener.onRequest(requestObject, (responseObject) -> {
                        ack.call(responseObject);
                    });
                }
            });

            _socket.connect();
        } catch (Exception e) {
            _listener.onError(e);
        }
    }

    public CompletableFuture<String> login(String tokenSymbol, String clientWalletAddress, String clientPassword,
            JSONArray onlineServers) {

        CompletableFuture<String> future = new CompletableFuture<>();
        JSONObject sendObject = new JSONObject();

        sendObject.put("symbol", tokenSymbol);
        sendObject.put("wallet", clientWalletAddress);
        sendObject.put("password", clientPassword);
        sendObject.put("servers", onlineServers);

        _socket.emit("login", sendObject, new Ack() {

            @Override
            public void call(Object... response) {

                try {

                    JSONObject responseObject = (JSONObject) response[0];

                    if (responseObject.has("fail")) {
                        future.complete(responseObject.getString("fail"));
                    } else {
                        future.complete("Signed in successfully");
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        });

        return future;

    }

    public void onlineServers(JSONArray onlineServers) {

        _socket.emit("onlineServers", onlineServers);
    }
}
