package com.sagar.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Simple Retrofit service builder for the AI backend.
 * Set BASE_URL to your deployed backend (must end with '/').
 */
public class StudyApiService {

    // TODO: Replace with your Railway URL (must include trailing slash), e.g.:
    // https://your-railway-app.up.railway.app/
    private static final String BASE_URL = "https://your-railway-app.up.railway.app/";

    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit != null) return retrofit;

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build();

        Gson gson = new GsonBuilder().create();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        return retrofit;
    }
}
