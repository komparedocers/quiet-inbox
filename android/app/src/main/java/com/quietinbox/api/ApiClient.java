package com.quietinbox.api;

import android.content.Context;
import android.util.Log;

import com.quietinbox.utils.ConfigLoader;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * API Client for backend communication
 */
public class ApiClient {
    private static final String TAG = "ApiClient";
    private static ApiClient instance;
    private final ApiService apiService;
    private final String baseUrl;

    private ApiClient(Context context) {
        ConfigLoader config = ConfigLoader.getInstance(context);
        this.baseUrl = config.getBackendUrl();

        // Logging interceptor
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // OkHttp client with timeouts
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(config.getConnectTimeout(), TimeUnit.SECONDS)
            .readTimeout(config.getReadTimeout(), TimeUnit.SECONDS)
            .writeTimeout(config.getReadTimeout(), TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build();

        // Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

        apiService = retrofit.create(ApiService.class);

        Log.d(TAG, "API Client initialized with base URL: " + baseUrl);
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
        return instance;
    }

    public ApiService getService() {
        return apiService;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
