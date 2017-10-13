package com.stripe.android;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Wraps a SSLSocketFactory and enables more TLS versions on older versions of Android.
 * Most of the code is taken from stripe-java.
 */
class StripeSSLSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory under;
    private final boolean tlsv11Supported;
    private final boolean tlsv12Supported;

    private static final String TLS_V11_PROTO = "TLSv1.1";
    private static final String TLS_V12_PROTO = "TLSv1.2";

    /**
     * Constructor for a socket factory instance.
     */
    StripeSSLSocketFactory() {
        this.under = HttpsURLConnection.getDefaultSSLSocketFactory();

        // For Android prior to 4.1, TLSv1.1 and TLSv1.2 might not be supported
        boolean tlsv11Supported = false, tlsv12Supported = false;

        String[] supportedProtocols;
        try {
            supportedProtocols = SSLContext.getDefault().getSupportedSSLParameters().getProtocols();
        } catch (NoSuchAlgorithmException e) {
            supportedProtocols = new String[0];
        }

        for (String proto : supportedProtocols) {
            if (proto.equals(TLS_V11_PROTO)) {
                tlsv11Supported = true;
            } else if (proto.equals(TLS_V12_PROTO)) {
                tlsv12Supported = true;
            }
        }

        this.tlsv11Supported = tlsv11Supported;
        this.tlsv12Supported = tlsv12Supported;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return this.under.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return this.under.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(
            Socket s,
            String host,
            int port,
            boolean autoClose) throws IOException {
        return fixupSocket(this.under.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return fixupSocket(this.under.createSocket(host, port));
    }

    @Override
    public Socket createSocket(
            String host,
            int port,
            InetAddress localHost,
            int localPort) throws IOException {
        return fixupSocket(this.under.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return fixupSocket(this.under.createSocket(host, port));
    }

    @Override
    public Socket createSocket(
            InetAddress address,
            int port,
            InetAddress localAddress,
            int localPort) throws IOException {
        return fixupSocket(
                this.under.createSocket(address, port, localAddress, localPort));
    }

    private Socket fixupSocket(Socket sock) {
        if (!(sock instanceof SSLSocket)) {
            return sock;
        }

        SSLSocket sslSock = (SSLSocket) sock;

        Set<String> protos = new HashSet<>(Arrays.asList(sslSock.getEnabledProtocols()));
        if (tlsv11Supported) {
            protos.add(TLS_V11_PROTO);
        }
        if (tlsv12Supported) {
            protos.add(TLS_V12_PROTO);
        }

        sslSock.setEnabledProtocols(protos.toArray(new String[0]));
        return sslSock;
    }
}
