package com.quietinbox.api;

import com.quietinbox.models.*;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.*;

/**
 * Retrofit API interface for QuietInbox backend
 */
public interface ApiService {

    // Authentication
    @POST("v1/auth/register")
    Call<TokenResponse> register(@Body RegisterRequest request);

    @POST("v1/auth/login")
    @FormUrlEncoded
    Call<TokenResponse> login(
        @Field("email") String email,
        @Field("password") String password
    );

    // User
    @GET("v1/user/me")
    Call<User> getCurrentUser(@Header("Authorization") String token);

    @POST("v1/user/upgrade-pro")
    Call<UpgradeResponse> upgradeToPro(@Header("Authorization") String token);

    // Profiles
    @GET("v1/profile")
    Call<List<Profile>> getProfiles(@Header("Authorization") String token);

    @POST("v1/profile")
    Call<Profile> createProfile(
        @Header("Authorization") String token,
        @Body Profile profile
    );

    @PUT("v1/profile/{id}")
    Call<Profile> updateProfile(
        @Header("Authorization") String token,
        @Path("id") long id,
        @Body Profile profile
    );

    // VIPs
    @GET("v1/vip")
    Call<List<VIP>> getVIPs(@Header("Authorization") String token);

    @POST("v1/vip")
    Call<VIP> createVIP(
        @Header("Authorization") String token,
        @Body VIP vip
    );

    @DELETE("v1/vip/{id}")
    Call<Void> deleteVIP(
        @Header("Authorization") String token,
        @Path("id") long id
    );

    // Sync
    @POST("v1/sync/push")
    Call<SyncResponse> pushSync(
        @Header("Authorization") String token,
        @Body SyncPushRequest request
    );

    @GET("v1/sync/pull")
    Call<SyncPullResponse> pullSync(
        @Header("Authorization") String token,
        @Query("since") String since
    );

    // Recommendations
    @GET("v1/recommendations/deferral-windows")
    Call<List<DeferralRecommendation>> getDeferralRecommendations(
        @Header("Authorization") String token
    );

    // Health check
    @GET("health")
    Call<HealthResponse> healthCheck();
}
