package com.middleware.zeus.util.api.common.interceptor;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class RedirectInterceptor implements Interceptor {
    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        // TODO this intercept solve missing params question when native okhttp3 request be redirected
        // may solve a okhttp3 bug
        Response response = chain.proceed(chain.request());
        if (response.isRedirect()) {
            Request request = response.request().newBuilder().url(response.header("Location")).build();
            response.close();
            response = chain.proceed(request);
        }
        return response;
    }
}
