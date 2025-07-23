package com.middleware.zeus.config;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLContext;
import java.net.Socket;
import java.security.cert.X509Certificate;


/**
 * @author xutianhong
 * @Date 2025/7/23 16:36
 */
public class TrustAllSocketFactory extends SocketFactory {

    private static final SSLSocketFactory sslSocketFactory;

    static {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // Skip client certificate validation
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    // Skip server certificate validation
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, null);
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create TrustAllSocketFactory", e);
        }
    }

    public static SocketFactory getDefault() {
        return new TrustAllSocketFactory();
    }

    @Override
    public Socket createSocket(String host, int port) throws java.io.IOException {
        return sslSocketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, java.net.InetAddress localAddress, int localPort) throws java.io.IOException {
        return sslSocketFactory.createSocket(host, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(java.net.InetAddress host, int port) throws java.io.IOException {
        return sslSocketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(java.net.InetAddress address, int port, java.net.InetAddress localAddress, int localPort) throws java.io.IOException {
        return sslSocketFactory.createSocket(address, port, localAddress, localPort);
    }
}
