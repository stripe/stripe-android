package com.stripe.http;

import java.net.URL;
import java.util.concurrent.Executor;

public interface AsyncHTTPInterface {
	public void sendAsynchronousRequest(URL url, String method, String body, Executor executor, ResponseHandler responseHandler);
}
