package com.ai.operations.infrastructure.utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public final class OkHttpUtil {

    private OkHttpUtil() {
    }

    public static String get(OkHttpClient client, String url) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP request failed: " + response.code());
            }
            return response.body() == null ? "" : response.body().string();
        }
    }
}

