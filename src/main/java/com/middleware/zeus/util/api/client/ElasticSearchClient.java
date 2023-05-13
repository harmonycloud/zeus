package com.middleware.zeus.util.api.client;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @author damiao
 * @since 2020-05-08 16:03
 */
public class ElasticSearchClient {


    static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }};

    public static RestHighLevelClient getHighLevelClient(String protocol, String clusterAddress,String userName,String password,Integer port) throws Exception{
        RestHighLevelClient client = initESClient(protocol, clusterAddress, userName, password,port);
        return client;
    }

    private static RestHighLevelClient initESClient(String protocol, String clusterAddress, String userName,
        String password, Integer port) throws Exception {

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));

        SSLContext sc = null;
        sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        SSLIOSessionStrategy sessionStrategy = new SSLIOSessionStrategy(sc, new NullHostNameVerifier());

        RestClientBuilder builder = RestClient.builder(new HttpHost(clusterAddress, port, protocol))
            .setHttpClientConfigCallback(httpAsyncClientBuilder -> {
                httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                if ("https".equalsIgnoreCase(protocol)) {
                    httpAsyncClientBuilder.setSSLStrategy(sessionStrategy);
                }
                return httpAsyncClientBuilder;
            }).setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(10 * 1000)
                .setSocketTimeout(10 * 1000).setConnectionRequestTimeout(10 * 1000));

        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }

    public static class NullHostNameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String arg0, SSLSession arg1) {
            return true;
        }
    }
}
