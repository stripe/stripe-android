package com.stripe.http;

abstract public class ResponseHandler {
    abstract public void handle(int responseCode, String responseBody);
}
