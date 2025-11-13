package com.example.api_weather.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.api_weather.api.RetrofitClient;
import com.example.api_weather.api.WeatherApi;
import com.example.api_weather.model.WeatherResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherViewModel extends ViewModel {
    private static final String API_KEY = "aa5bf967f1153ae617e8a7f29a818208";
    private MutableLiveData<WeatherResponse> weatherData = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private WeatherApi weatherApi;

    public WeatherViewModel() {
        weatherApi = RetrofitClient.getInstance().getWeatherApi();
    }

    public LiveData<WeatherResponse> getWeatherData() {
        return weatherData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void fetchWeatherByCity(String cityName) {
        isLoading.setValue(true);
        weatherApi.getCurrentWeather(cityName, API_KEY, "metric", "es")
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            weatherData.setValue(response.body());
                        } else {
                            errorMessage.setValue("Error: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Error de conexión: " + t.getMessage());
                    }
                });
    }

    public void fetchWeatherByCoordinates(double lat, double lon) {
        isLoading.setValue(true);
        weatherApi.getCurrentWeatherByCoordinates(lat, lon, API_KEY, "metric", "es")
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            weatherData.setValue(response.body());
                        } else {
                            errorMessage.setValue("Error: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Error de conexión: " + t.getMessage());
                    }
                });
    }
}


