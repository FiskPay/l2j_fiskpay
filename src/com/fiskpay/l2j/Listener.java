package com.fiskpay.l2j;

import org.json.JSONObject;

public interface Listener
{
    void onLogDeposit(String txHash, String from, String symbol, String amount, String server, String character);

    void onLogWithdraw(String txHash, String to, String symbol, String amount, String server, String character, String refund);

    void onRequest(JSONObject requestObject, Callback cb);

    void onConnect();

    void onDisconnect();

    void onError(Exception e);

    @FunctionalInterface
    interface Callback
    {
        void resolve(JSONObject callbackObject);
    }
}