package com.gritacademy.exjobbet.Retrofit;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit;

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            // Set up logging interceptor
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Log request and response body

            // Add the interceptor to the OkHttpClient
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build();

            // Create Retrofit instance with the logging interceptor
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://api.datamuse.com/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create()) // Gson converter
                    .build();
        }
        return retrofit;
    }
}