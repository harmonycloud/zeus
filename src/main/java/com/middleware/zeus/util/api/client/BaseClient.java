package com.middleware.zeus.util.api.client;

import com.alibaba.fastjson.JSON;
import com.middleware.zeus.util.api.common.ApiException;
import com.middleware.zeus.util.api.common.ApiResponse;
import com.middleware.zeus.util.api.common.Pair;
import com.middleware.zeus.util.api.common.RequestParams;
import com.middleware.zeus.util.api.common.auth.Authentication;
import com.middleware.zeus.util.api.common.auth.HttpBasicAuth;
import com.middleware.zeus.util.api.common.auth.HttpBearerAuth;
import com.middleware.zeus.util.api.common.interceptor.RedirectInterceptor;
import okhttp3.*;
import okhttp3.internal.http.HttpMethod;
import okhttp3.internal.tls.OkHostnameVerifier;
import okio.BufferedSink;
import okio.Okio;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseClient {
    // metadata
    protected String basePath;
    protected String baseUrl;
    protected String protocol;
    protected String host;
    protected int port;

    // auth
    protected Map<String, Authentication> authentications;

    // configuration
    protected boolean verifyingSsl;
    protected InputStream sslCaCert;
    protected KeyManager[] keyManagers;
    protected String tempFolderPath = null;

    // httpclient
    protected OkHttpClient httpClient;
    protected final Map<String, String> defaultHeaderMap = new HashMap<String, String>();
    protected final Map<String, String> defaultCookieMap = new HashMap<String, String>();

    protected BaseClient(String basePath) {
        this.basePath = basePath;
        this.authentications = new HashMap<>();
        initHttpClient();
    }

    public BaseClient(String protocol, String host, int port, String basePath) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.basePath = basePath;
        this.baseUrl = this.protocol + "://" + this.host + ":" + this.port + this.basePath;

        this.authentications = new HashMap<>();
        initHttpClient();
    }

    public BaseClient resetAddr(String protocol, String host, Integer port) {
        this.protocol = protocol;
        this.host = host;
        if (port != null) {
            this.port = port;
            this.baseUrl = this.protocol + "://" + this.host + ":" + this.port + this.basePath;
        } else {
            this.baseUrl = this.protocol + "://" + this.host + this.basePath;
        }
        return this;
    }

    protected void initHttpClient() {
        this.httpClient = new OkHttpClient().newBuilder().
                followRedirects(false).
                followSslRedirects(false).
                addInterceptor(new RedirectInterceptor()).
                build();
    }

    protected void applySslSetting() {
        try {
            TrustManager[] trustManagers;
            HostnameVerifier hostnameVerifier;
            if (!verifyingSsl) {
                trustManagers = new TrustManager[]{
                        new X509TrustManager() {
                            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {

                            }

                            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {

                            }

                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }
                };
                hostnameVerifier = (s, sslSession) -> true;
            } else {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                if (sslCaCert == null) {
                    trustManagerFactory.init((KeyStore) null);
                } else {
                    char[] password = null; // Any password will work.
                    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                    Collection<? extends Certificate> certificates =
                            certificateFactory.generateCertificates(sslCaCert);
                    if (certificates.isEmpty()) {
                        throw new IllegalArgumentException("expected non-empty set of trusted certificates");
                    }
                    KeyStore caKeyStore = newEmptyKeyStore(password);
                    int index = 0;
                    for (Certificate certificate : certificates) {
                        String certificateAlias = "ca" + index++;
                        caKeyStore.setCertificateEntry(certificateAlias, certificate);
                    }
                    trustManagerFactory.init(caKeyStore);
                }
                trustManagers = trustManagerFactory.getTrustManagers();
                hostnameVerifier = OkHostnameVerifier.INSTANCE;
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
            httpClient =
                    httpClient
                            .newBuilder()
                            .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0])
                            .hostnameVerifier(hostnameVerifier)
                            .build();

        } catch (GeneralSecurityException generalSecurityException) {
            generalSecurityException.printStackTrace();
        }
    }

    protected KeyStore newEmptyKeyStore(char[] password) throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, password);
            return keyStore;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public Call buildCall(String path, String method, RequestParams requestParams, Object body, String[] authNames) {
        return httpClient.newCall(buildRequest(path, method, requestParams.getQuery(), body, requestParams.getHeader(),
                requestParams.getCookie(), requestParams.getForm(), authNames, requestParams.isUseBasePath()));
    }

    public Call buildCall(String path, String method, Object body, String[] authNames) {
        RequestParams requestParams = new RequestParams();
        return httpClient.newCall(buildRequest(path, method, requestParams.getQuery(), body, requestParams.getHeader(),
                requestParams.getCookie(), requestParams.getForm(), authNames, true));
    }

    public Call buildCall(String path, String method, List<Pair> query, Object body,
                          Map<String, String> header, Map<String, String> cookie, Map<String, Object> form, String[] authNames) {
        return httpClient.newCall(buildRequest(path, method, query, body, header, cookie, form, authNames, true));
    }

    protected Request buildRequest(String path, String method, List<Pair> query, Object body,
                                   Map<String, String> header, Map<String, String> cookie, Map<String, Object> form,
                                   String[] authNames, boolean useBasePath) {
        updateParamsForAuth(authNames, query, header, cookie);

        final String url = buildUrl(path, query, useBasePath);
        final Request.Builder builder = new Request.Builder().url(url);
        processHeaderParams(header, builder);
        processCookieParams(cookie, builder);

        String contentType = header.get("Content-Type");
        if (contentType == null) {
            contentType = "application/json";
        }

        RequestBody reqBody;
        if (!HttpMethod.permitsRequestBody(method)) {
            reqBody = null;
        } else if ("application/x-www-form-urlencoded".equals(contentType)) {
            reqBody = buildRequestBodyFormEncoding(form);
        } else if ("multipart/form-data".equals(contentType)) {
            reqBody = buildRequestBodyMultipart(form);
        } else if (body == null) {
            if ("DELETE".equals(method)) {
                // allow calling DELETE without sending a request body
                reqBody = null;
            } else {
                // use an empty request body (for POST, PUT and PATCH)
                reqBody = RequestBody.create("", MediaType.parse(contentType));
            }
        } else {
            reqBody = serialize(body, contentType);
        }

        return builder.method(method, reqBody).build();
    }

    protected void updateParamsForAuth(String[] authNames, List<Pair> query, Map<String, String> header, Map<String, String> cookie) {
        for (String authName : authNames) {
            Authentication auth = authentications.get(authName);
            if (auth == null) {
                throw new RuntimeException("Authentication undefined: " + authName);
            }
            auth.applyToParams(query, header, cookie);
        }
    }

    protected RequestBody serialize(Object body, String contentType) {
        if (body instanceof byte[]) {
            // Binary (byte array) body parameter support.
            return RequestBody.create((byte[]) body, MediaType.parse(contentType));
        } else if (body instanceof File) {
            // File body parameter support.
            return RequestBody.create((File) body, MediaType.parse(contentType));
        } else if (isJsonMime(contentType)) {
            String content;
            if (body != null) {
                content = JSON.toJSONString(body);
            } else {
                content = "";
            }
            return RequestBody.create(content, MediaType.parse(contentType));
        } else {
            throw new IllegalArgumentException("Content type \"" + contentType + "\" is not supported");
        }
    }

    protected boolean isJsonMime(String mime) {
        String jsonMime = "(?i)^(application/json|[^;/ \t]+/[^;/ \t]+[+]json)[ \t]*(;.*)?$";
        return mime != null && (mime.matches(jsonMime) || mime.equals("*/*"));
    }

    protected void processHeaderParams(Map<String, String> header, Request.Builder builder) {
        Set<String> keySet = header.keySet();
        for (String key : keySet) {
            builder.header(key, header.get(key));
        }
        Set<String> defaultKeySet = defaultHeaderMap.keySet();
        for (String key : defaultKeySet) {
            builder.header(key, defaultHeaderMap.get(key));
        }
    }

    protected void processCookieParams(Map<String, String> cookie, Request.Builder builder) {
        Set<String> keySet = cookie.keySet();
        for (String key : keySet) {
            builder.addHeader("Cookie", String.format("%s=%s", key, cookie.get(key)));
        }
        Set<String> defaultKeySet = defaultCookieMap.keySet();
        for (String key : defaultKeySet) {
            builder.addHeader("Cookie", String.format("%s=%s", key, defaultCookieMap.get(key)));
        }
    }

    protected RequestBody buildRequestBodyMultipart(Map<String, Object> form) {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        Set<String> keySet = form.keySet();
        for (String key : keySet) {
            if (form.get(key) instanceof File) {
                File file = (File) form.get(key);
                Headers partHeaders =
                        Headers.of(
                                "Content-Disposition",
                                "form-data; name=\"" + key + "\"; filename=\"" + file.getName() + "\"");
                MediaType mediaType = MediaType.parse(guessContentTypeFromFile(file));
                builder.addPart(partHeaders, RequestBody.create(file, mediaType));
            } else {
                Headers partHeaders =
                        Headers.of("Content-Disposition", "form-data; name=\"" + key + "\"");
                builder.addPart(
                        partHeaders, RequestBody.create(parameterToString(form.get(key)), null));
            }
        }
        return builder.build();
    }

    protected String guessContentTypeFromFile(File file) {
        String contentType = URLConnection.guessContentTypeFromName(file.getName());
        if (contentType == null) {
            return "application/octet-stream";
        } else {
            return contentType;
        }
    }

    protected RequestBody buildRequestBodyFormEncoding(Map<String, Object> form) {
        FormBody.Builder builder = new FormBody.Builder();
        Set<String> keySet = form.keySet();
        for (String key : keySet) {
            builder.add(key, parameterToString(form.get(key)));
        }
        return builder.build();
    }

    protected String parameterToString(Object param) {
        if (param == null) {
            return "";
        } else if (param instanceof Date) {
            // Serialize to json string and remove the " enclosing characters
            return JSON.toJSONString(param);
        } else if (param instanceof Collection) {
            StringBuilder b = new StringBuilder();
            for (Object o : (Collection) param) {
                if (b.length() > 0) {
                    b.append(",");
                }
                b.append(String.valueOf(o));
            }
            return b.toString();
        } else {
            return String.valueOf(param);
        }
    }

    protected String buildUrl(String path, List<Pair> queryParams, boolean useBasePath) {
        final StringBuilder url = new StringBuilder();
        // 如果不使用basePath，替换成空
        if (!useBasePath) {
            this.baseUrl = this.baseUrl.replace(this.basePath, "");
        }
        url.append(this.baseUrl).append(path);

        if (queryParams != null && !queryParams.isEmpty()) {
            String prefix = path.contains("?") ? "&" : "?";
            for (Pair param : queryParams) {
                if (param.getValue() != null) {
                    if (prefix != null) {
                        url.append(prefix);
                        prefix = null;
                    } else {
                        url.append("&");
                    }
                    url.append(escapeString(param.getName())).append("=").append(escapeString(param.getValue()));
                }
            }
        }

        return url.toString();
    }

    protected String escapeString(String str) {
        try {
            return URLEncoder.encode(str, "utf8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }

    public <T> ApiResponse<T> execute(Call call, Type returnType) throws ApiException {
        try {
            Response response = call.execute();
            T data = handleResponse(response, returnType);
            return new ApiResponse<T>(response.code(), response.headers().toMultimap(), data);
        } catch (IOException e) {
            throw new ApiException(e);
        }
    }

    protected <T> T handleResponse(Response response, Type returnType) throws ApiException {
        if (response.isSuccessful()) {
            if (returnType == null || response.code() == 204) {
                // returning null if the returnType is not defined,
                // or the status code is 204 (No Content)
                if (response.body() != null) {
                    try {
                        response.body().close();
                    } catch (Exception e) {
                        throw new ApiException(
                                response.message(), e, response.code(), response.headers().toMultimap());
                    }
                }
                return null;
            } else {
                return deserialize(response, returnType);
            }
        } else {
            String respBody = null;
            if (response.body() != null) {
                try {
                    respBody = response.body().string();
                } catch (IOException e) {
                    throw new ApiException(
                            response.message(), e, response.code(), response.headers().toMultimap());
                }
            }
            throw new ApiException(
                    response.message(), response.code(), response.headers().toMultimap(), respBody);
        }
    }

    protected <T> T deserialize(Response response, Type returnType) throws ApiException {
        if (response == null || returnType == null) {
            return null;
        }

        if ("byte[]".equals(returnType.toString())) {
            // Handle binary response (byte array).
            try {
                return (T) response.body().bytes();
            } catch (IOException e) {
                throw new ApiException(e);
            }
        } else if (returnType.equals(File.class)) {
            // Handle file downloading.
            return (T) downloadFileFromResponse(response);
        }

        String respBody;
        try {
            if (response.body() != null) respBody = response.body().string();
            else respBody = null;
        } catch (IOException e) {
            throw new ApiException(e);
        }

        if (respBody == null || "".equals(respBody)) {
            return null;
        }

        String contentType = response.headers().get("Content-Type");
        if (contentType == null) {
            // ensuring a default content type
            contentType = "application/json";
        }
        if (isJsonMime(contentType)) {
            return JSON.parseObject(respBody, returnType);
        } else if (returnType.equals(String.class)) {
            // Expecting string, return the raw response body.
            return (T) respBody;
        } else {
            throw new ApiException(
                    "Content type \"" + contentType + "\" is not supported for type: " + returnType,
                    response.code(),
                    response.headers().toMultimap(),
                    respBody);
        }
    }

    protected File downloadFileFromResponse(Response response) throws ApiException {
        try {
            File file = prepareDownloadFile(response);
            BufferedSink sink = Okio.buffer(Okio.sink(file));
            sink.writeAll(response.body().source());
            sink.close();
            return file;
        } catch (IOException e) {
            throw new ApiException(e);
        }
    }

    protected File prepareDownloadFile(Response response) throws IOException {
        String filename = null;
        String contentDisposition = response.header("Content-Disposition");
        if (contentDisposition != null && !"".equals(contentDisposition)) {
            // Get filename from the Content-Disposition header.
            Pattern pattern = Pattern.compile("filename=['\"]?([^'\"\\s]+)['\"]?");
            Matcher matcher = pattern.matcher(contentDisposition);
            if (matcher.find()) {
                filename = sanitizeFilename(matcher.group(1));
            }
        }
        String prefix;
        String suffix = null;
        if (filename == null) {
            prefix = "download-";
            suffix = "";
        } else {
            int pos = filename.lastIndexOf(".");
            if (pos == -1) {
                prefix = filename + "-";
            } else {
                prefix = filename.substring(0, pos) + "-";
                suffix = filename.substring(pos);
            }
            // File.createTempFile requires the prefix to be at least three characters long
            if (prefix.length() < 3) prefix = "download-";
        }

        if (tempFolderPath == null) return File.createTempFile(prefix, suffix);
        else return File.createTempFile(prefix, suffix, new File(tempFolderPath));
    }

    protected String sanitizeFilename(String filename) {
        return filename.replaceAll(".*[/\\\\]", "");
    }

    public BaseClient addHttpBasicAuth(String key, String username, String password) {
        this.authentications.put(key, new HttpBasicAuth(username, password));
        return this;
    }

    public BaseClient addBearerTokenAuth(String key, String token) {
        this.authentications.put(key, new HttpBearerAuth("Bearer").setBearerToken(token));
        return this;
    }

    public BaseClient setSslCaCertAndKeyManagers(InputStream sslCaCert, KeyManager[] keyManagers) {
        this.sslCaCert = sslCaCert;
        this.keyManagers = keyManagers;
        applySslSetting();
        return this;
    }

    public String getProtocol() {
        return protocol;
    }

    public boolean isVerifyingSsl() {
        return verifyingSsl;
    }

    public BaseClient setVerifyingSsl(boolean verifyingSsl) {
        this.verifyingSsl = verifyingSsl;
        applySslSetting();
        return this;
    }

    public Map<String, Authentication> getAuthentications() {
        return authentications;
    }

    public void setAuthentications(Map<String, Authentication> authentications) {
        this.authentications = authentications;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public InputStream getSslCaCert() {
        return sslCaCert;
    }

    public BaseClient setSslCaCert(InputStream sslCaCert) {
        this.sslCaCert = sslCaCert;
        applySslSetting();
        return this;
    }

    public KeyManager[] getKeyManagers() {
        return keyManagers;
    }

    public String getTempFolderPath() {
        return tempFolderPath;
    }

    public BaseClient setTempFolderPath(String tempFolderPath) {
        this.tempFolderPath = tempFolderPath;
        return this;
    }

    public BaseClient setKeyManagers(KeyManager[] keyManagers) {
        this.keyManagers = keyManagers;
        applySslSetting();
        return this;
    }
}
