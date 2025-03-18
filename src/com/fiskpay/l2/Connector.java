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

package com.fiskpay.l2;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;

public class Connector
{
    private Socket _socket;
    private Listener _listener;

    public Connector(Listener listener)
    {
        _listener = listener;

        try
        {
            Thread.sleep(10000); // This delay is used to make sure that the previous connection is properly closed when restarting the Login Server.
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        try {
            _socket = IO.socket("wss://ds.fiskpay.com:42099");

            _socket.on("logDeposit", (args) -> 
            {
                if (args.length == 6)
                {
                    String txHash = (String) args[0];
                    String from = (String) args[1];
                    String symbol = (String) args[2];
                    String amount = (String) args[3];
                    String server = (String) args[4];
                    String character = (String) args[5];

                    _listener.onLogDeposit(txHash, from, symbol, amount, server, character);
                }
            });

            _socket.on("logWithdrawal", (args) ->
            {
                if (args.length == 7)
                {
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

            _socket.on("connect", (_) ->
            {
                _listener.onConnect();
            });

            _socket.on("disconnect", (_) ->
            {
                _listener.onDisconnect();
            });

            _socket.on("request", (args) ->
            {
                if (args.length == 2)
                {
                    JSONObject requestObject = (JSONObject) args[0];
                    Ack ack = (Ack) args[1];

                    _listener.onRequest(requestObject, (responseObject) ->
                    {
                        ack.call(responseObject);
                    });
                }
            });

            _socket.connect();
        }
        catch (Exception e)
        {
            _listener.onError(e);
        }
    }

    public CompletableFuture<String> login(String tokenSymbol, String clientWalletAddress, String clientPassword, JSONArray onlineServers)
    {
        CompletableFuture<String> future = new CompletableFuture<>();
        JSONObject sendObject = new JSONObject();

        sendObject.put("symbol", tokenSymbol);
        sendObject.put("wallet", clientWalletAddress);
        sendObject.put("password", clientPassword);
        sendObject.put("servers", onlineServers);

        _socket.emit("login", sendObject, new Ack()
        {
            @Override
            public void call(Object... response)
            {
                try
                {
                    JSONObject responseObject = (JSONObject) response[0];

                    if (responseObject.has("fail"))
                    {
                        future.complete(responseObject.getString("fail"));
                    }
                    else
                    {
                        future.complete("Signed in successfully");
                    }
                }
                catch (Exception e)
                {
                    if (!future.isDone())
                    {
                        future.completeExceptionally(e);
                    }
                }
            }
        });

        return future.completeOnTimeout("Login request timed out", 5, TimeUnit.SECONDS);
    }

    public void onlineServers(JSONArray onlineServers)
    {
        _socket.emit("onlineServers", onlineServers);
    }
}