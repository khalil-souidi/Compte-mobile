package com.example.tp_compte.Config;

import com.example.tp_compte.api.ApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class RetrofitClient {

    private static Retrofit jsonRetrofit;
    private static Retrofit xmlRetrofit;
    private static final String BASE_URL = "http://10.0.2.2:8080/";

    public static Retrofit getJsonRetrofitInstance() {
        if (jsonRetrofit == null) {
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd") // Match the format of your backend dates
                    .create();

            jsonRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson)) // JSON support
                    .client(createHttpClient("application/json"))
                    .build();
        }
        return jsonRetrofit;
    }

    public static Retrofit getXmlRetrofitInstance() {
        if (xmlRetrofit == null) {
            xmlRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(SimpleXmlConverterFactory.createNonStrict()) // XML support
                    .client(createHttpClient("application/xml"))
                    .build();
        }
        return xmlRetrofit;
    }

    public static ApiService getJsonApiService() {
        return getJsonRetrofitInstance().create(ApiService.class);
    }
    public static ApiService getXmlApiService() {
        return getXmlRetrofitInstance().create(ApiService.class);
    }

    private static OkHttpClient createHttpClient(String acceptType) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                            .addHeader("Accept", acceptType)
                            .build();
                    return chain.proceed(request);
                })
                .build();
    }
}
