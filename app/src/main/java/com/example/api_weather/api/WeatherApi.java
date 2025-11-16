package com.example.api_weather.api;

import com.example.api_weather.model.OpenMeteoResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApi {
    @GET("forecast")
    Call<OpenMeteoResponse> getCurrentWeather(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("current") String current,
            @Query("daily") String daily,
            @Query("timezone") String timezone
    );
}






