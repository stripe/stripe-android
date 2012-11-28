package com.stripe.http;

import android.os.Build;
import com.stripe.util.StripeLog;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.concurrent.Executor;

public class AsyncHTTPTask implements AsyncHTTPInterface {
    private static final String UTF8 = "UTF-8";
    private static final int FROYO = 8;

    public AsyncHTTPTask() {

        // See: Avoiding Bugs In Earlier Releases
        // http://developer.android.com/reference/java/net/HttpURLConnection.html
        if (Build.VERSION.SDK == null || Integer.parseInt(Build.VERSION.SDK) < FROYO) {
            StripeLog.d("Turning off connection keepAlive due to pre-FROYO problems.");
            System.setProperty("http.keepAlive", "false");
        }
    }

    public void sendAsynchronousRequest(final URL url, final String method, final String body, Executor executor, final ResponseHandler responseHandler) {


        AsyncTask<Void, Void, ResponseWrapper> task = new AsyncTask<Void, Void, ResponseWrapper>() {
            @Override
            protected ResponseWrapper doInBackground(Void... params) {
                StripeLog.d("Calling %s %s in background thread.", method, url.toString());

                HttpsURLConnection conn = null;
                try {
                    conn = (HttpsURLConnection) url.openConnection();

                    StripeLog.d("Connection open, setting headers:");

                    if (url.getUserInfo() != null) {
                        String encoded = Base64.encodeBytes((url.getUserInfo()).getBytes(UTF8));
                        setAndLogHeader(conn, "Authorization", "Basic " + encoded);
                    }

                    setAndLogHeader(conn, "Host", "api.stripe.com");
                    setAndLogHeader(conn, "Accept", "*/*");

                    conn.setRequestMethod(method);
                    conn.setDoInput(true);

                    if (body != null) {
                        setRequestBody(conn, body);
                    }

                    String responseBody = handleResponse(conn, conn.getResponseCode());
                    return new ResponseWrapper(conn.getResponseCode(), responseBody);
                } catch (Exception e) {
                    StripeLog.e(e);
                    throw new RuntimeException(e);
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                        StripeLog.d("Connection disconnected.");
                    }
                }
            }

            @Override
            protected void onPostExecute(ResponseWrapper responseWrapper) {
                responseHandler.handle(responseWrapper.responseCode, responseWrapper.responseBody);
            }
        };

        if (executor != null) {
            StripeLog.d("Using custom executor.");
            task.executeOnExecutor(executor);
        } else {
            StripeLog.d("Using default executor.");
            task.execute();
        }
    }

    private String handleResponse(HttpsURLConnection conn, int responseCode) throws IOException {
        InputStream inputStream;

        StripeLog.d("Received HTTP status: %d", responseCode);
        boolean isOK = responseCode == HttpURLConnection.HTTP_OK;
        if (isOK) {
            inputStream = conn.getInputStream();
        } else {
            inputStream = conn.getErrorStream();
        }

        if (inputStream != null) {
            StripeLog.d("Retrieving response with content length: %d", conn.getContentLength());
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(inputStream));
                CharBuffer buffer = CharBuffer.allocate(1024);

                StringBuilder response = new StringBuilder();
                while (in.read(buffer) >= 0) {
                    response.append(buffer.array());
                }
                StripeLog.d("Retrieval finished.");
                return response.toString();
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } else {
            StripeLog.d("No response body received.");
            return null;
        }
    }

    private void setRequestBody(HttpsURLConnection conn, String body) throws IOException {
        int contentLength = body.getBytes().length;
        conn.setFixedLengthStreamingMode(contentLength);
        setAndLogHeader(conn, "Content-Type", "application/x-www-form-urlencoded");
        setAndLogHeader(conn, "Content-Length", String.valueOf(contentLength));
        conn.setDoOutput(true);

        StripeLog.d("Starting to send request content...");
        DataOutputStream wr = null;
        try {
            wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(body);
            wr.flush();
            StripeLog.d("Finished sending.");
        } finally {
            if (wr != null) {
                wr.close();
            }
        }
    }

    private void setAndLogHeader(HttpsURLConnection conn, String field, String value) {
        conn.setRequestProperty(field, value);
        StripeLog.d("%s=%s", field, value);
    }

    private class ResponseWrapper implements Serializable {
        private static final long serialVersionUID = -9053668422164180287L;
        public final int responseCode;
        public final String responseBody;

        private ResponseWrapper(int responseCode, String responseBody) {
            this.responseCode = responseCode;
            this.responseBody = responseBody;
        }
    }
}
