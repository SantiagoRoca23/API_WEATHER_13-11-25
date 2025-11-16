package com.example.api_weather.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.api_weather.api.GeocodingClient;
import com.example.api_weather.api.GeocodingApi;
import com.example.api_weather.api.RetrofitClient;
import com.example.api_weather.api.WeatherApi;
import com.example.api_weather.model.GeocodingResponse;
import com.example.api_weather.model.OpenMeteoResponse;
import com.example.api_weather.model.WeatherResponse;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherViewModel extends ViewModel {
    private MutableLiveData<WeatherResponse> weatherData = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private WeatherApi weatherApi;
    private GeocodingApi geocodingApi;

    public WeatherViewModel() {
        weatherApi = RetrofitClient.getInstance().getWeatherApi();
        geocodingApi = GeocodingClient.getInstance().getGeocodingApi();
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
        // Primero buscar las coordenadas de la ciudad
        geocodingApi.searchLocation(cityName, 1, "es", "json")
                .enqueue(new Callback<GeocodingResponse>() {
                    @Override
                    public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                        if (response.isSuccessful() && response.body() != null && 
                            response.body().getResults() != null && !response.body().getResults().isEmpty()) {
                            GeocodingResponse.Location location = response.body().getResults().get(0);
                            // Ahora obtener el clima usando las coordenadas
                            fetchWeatherByCoordinates(location.getLatitude(), location.getLongitude(), 
                                    location.getName(), location.getCountryCode());
                        } else {
                            isLoading.setValue(false);
                            errorMessage.setValue("Ciudad no encontrada. Verifica el nombre.");
                        }
                    }

                    @Override
                    public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Error de conexión: " + t.getMessage());
                    }
                });
    }

    private void fetchWeatherByCoordinates(double lat, double lon, String cityName, String countryCode) {
        String currentParams = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,pressure_msl,uv_index";
        String dailyParams = "temperature_2m_max,temperature_2m_min";
        weatherApi.getCurrentWeather(lat, lon, currentParams, dailyParams, "auto")
                .enqueue(new Callback<OpenMeteoResponse>() {
                    @Override
                    public void onResponse(Call<OpenMeteoResponse> call, Response<OpenMeteoResponse> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            // Convertir OpenMeteoResponse a WeatherResponse para mantener compatibilidad
                            WeatherResponse weatherResponse = convertToWeatherResponse(response.body(), cityName, countryCode);
                            weatherData.setValue(weatherResponse);
                            errorMessage.setValue(null);
                        } else {
                            String errorMsg = "Error al obtener datos del clima";
                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    errorMsg = "Error: " + errorBody.substring(0, Math.min(100, errorBody.length()));
                                } else if (response.message() != null) {
                                    errorMsg = "Error " + response.code() + ": " + response.message();
                                }
                            } catch (IOException e) {
                                errorMsg = "Error " + response.code() + ": " + response.message();
                            }
                            errorMessage.setValue(errorMsg);
                        }
                    }

                    @Override
                    public void onFailure(Call<OpenMeteoResponse> call, Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Error de conexión: " + t.getMessage());
                    }
                });
    }

    public void fetchWeatherByCoordinates(double lat, double lon) {
        fetchWeatherByCoordinates(lat, lon, null, null);
    }

    private WeatherResponse convertToWeatherResponse(OpenMeteoResponse openMeteoResponse, String cityName, String countryCode) {
        WeatherResponse weatherResponse = new WeatherResponse();
        
        // Coordenadas
        com.example.api_weather.model.Coord coord = new com.example.api_weather.model.Coord();
        coord.setLat(openMeteoResponse.getLatitude());
        coord.setLon(openMeteoResponse.getLongitude());
        weatherResponse.setCoord(coord);
        
        // Weather (convertir weather_code a descripción)
        java.util.List<com.example.api_weather.model.Weather> weatherList = new java.util.ArrayList<>();
        com.example.api_weather.model.Weather weather = new com.example.api_weather.model.Weather();
        if (openMeteoResponse.getCurrent() != null) {
            int weatherCode = openMeteoResponse.getCurrent().getWeatherCode();
            weather.setId(weatherCode);
            weather.setMain(getWeatherMain(weatherCode));
            weather.setDescription(getWeatherDescription(weatherCode));
            weather.setIcon(getWeatherIcon(weatherCode));
        }
        weatherList.add(weather);
        weatherResponse.setWeather(weatherList);
        
        // Main (temperatura, humedad, presión)
        com.example.api_weather.model.Main main = new com.example.api_weather.model.Main();
        if (openMeteoResponse.getCurrent() != null) {
            double temp = openMeteoResponse.getCurrent().getTemperature2m();
            main.setTemp(temp);
            
            // Calcular sensación térmica basada en temperatura, humedad, viento y UV index
            double feelsLike = calculateFeelsLike(
                temp,
                openMeteoResponse.getCurrent().getRelativeHumidity2m(),
                openMeteoResponse.getCurrent().getWindSpeed10m(),
                openMeteoResponse.getCurrent().getUvIndex()
            );
            main.setFeelsLike(feelsLike);
            
            // Obtener temperatura máxima y mínima del día actual
            double tempMin = openMeteoResponse.getCurrent().getTemperature2m();
            double tempMax = openMeteoResponse.getCurrent().getTemperature2m();
            
            if (openMeteoResponse.getDaily() != null) {
                if (openMeteoResponse.getDaily().getTemperature2mMin() != null && 
                    !openMeteoResponse.getDaily().getTemperature2mMin().isEmpty()) {
                    tempMin = openMeteoResponse.getDaily().getTemperature2mMin().get(0);
                }
                if (openMeteoResponse.getDaily().getTemperature2mMax() != null && 
                    !openMeteoResponse.getDaily().getTemperature2mMax().isEmpty()) {
                    tempMax = openMeteoResponse.getDaily().getTemperature2mMax().get(0);
                }
            }
            
            main.setTempMin(tempMin);
            main.setTempMax(tempMax);
            main.setHumidity(openMeteoResponse.getCurrent().getRelativeHumidity2m());
            main.setPressure((int) Math.round(openMeteoResponse.getCurrent().getPressureMsl()));
        }
        weatherResponse.setMain(main);
        
        // Wind
        com.example.api_weather.model.Wind wind = new com.example.api_weather.model.Wind();
        if (openMeteoResponse.getCurrent() != null) {
            wind.setSpeed(openMeteoResponse.getCurrent().getWindSpeed10m());
        }
        weatherResponse.setWind(wind);
        
        // Clouds (Open-Meteo no proporciona datos de nubes directamente)
        com.example.api_weather.model.Clouds clouds = new com.example.api_weather.model.Clouds();
        clouds.setAll(0);
        weatherResponse.setClouds(clouds);
        
        // Sys
        com.example.api_weather.model.Sys sys = new com.example.api_weather.model.Sys();
        if (countryCode != null) {
            sys.setCountry(countryCode);
        }
        weatherResponse.setSys(sys);
        
        // Otros campos
        weatherResponse.setName(cityName != null ? cityName : "Ubicación");
        weatherResponse.setTimezone(openMeteoResponse.getUtcOffsetSeconds());
        weatherResponse.setCod(200);
        
        return weatherResponse;
    }

    private String getWeatherMain(int weatherCode) {
        // Códigos WMO Weather interpretation codes
        if (weatherCode == 0) return "Clear";
        if (weatherCode <= 3) return "Clouds";
        if (weatherCode <= 49) return "Fog";
        if (weatherCode <= 59) return "Drizzle";
        if (weatherCode <= 69) return "Rain";
        if (weatherCode <= 79) return "Snow";
        if (weatherCode <= 84) return "Rain";
        if (weatherCode <= 86) return "Snow";
        if (weatherCode <= 99) return "Thunderstorm";
        return "Unknown";
    }

    private String getWeatherDescription(int weatherCode) {
        // Descripciones en español basadas en códigos WMO
        switch (weatherCode) {
            case 0: return "cielo despejado";
            case 1: return "mayormente despejado";
            case 2: return "parcialmente nublado";
            case 3: return "nublado";
            case 45: return "niebla";
            case 48: return "niebla con escarcha";
            case 51: return "llovizna ligera";
            case 53: return "llovizna moderada";
            case 55: return "llovizna densa";
            case 56: return "llovizna helada ligera";
            case 57: return "llovizna helada densa";
            case 61: return "lluvia ligera";
            case 63: return "lluvia moderada";
            case 65: return "lluvia intensa";
            case 66: return "lluvia helada ligera";
            case 67: return "lluvia helada intensa";
            case 71: return "nieve ligera";
            case 73: return "nieve moderada";
            case 75: return "nieve intensa";
            case 77: return "granos de nieve";
            case 80: return "chubascos ligeros";
            case 81: return "chubascos moderados";
            case 82: return "chubascos intensos";
            case 85: return "chubascos de nieve ligeros";
            case 86: return "chubascos de nieve intensos";
            case 95: return "tormenta";
            case 96: return "tormenta con granizo";
            case 99: return "tormenta intensa con granizo";
            default: return "desconocido";
        }
    }

    private String getWeatherIcon(int weatherCode) {
        // Mapear códigos a iconos similares a OpenWeatherMap
        if (weatherCode == 0) return "01d";
        if (weatherCode <= 2) return "02d";
        if (weatherCode == 3) return "03d";
        if (weatherCode <= 49) return "50d";
        if (weatherCode <= 59) return "09d";
        if (weatherCode <= 69) return "10d";
        if (weatherCode <= 79) return "13d";
        if (weatherCode <= 84) return "09d";
        if (weatherCode <= 86) return "13d";
        if (weatherCode <= 99) return "11d";
        return "01d";
    }

    /**
     * Calcula la sensación térmica basada en temperatura, humedad, viento y UV index
     * @param temp Temperatura en °C
     * @param humidity Humedad relativa en %
     * @param windSpeed Velocidad del viento en m/s
     * @param uvIndex Índice UV
     * @return Sensación térmica en °C
     */
    private double calculateFeelsLike(double temp, int humidity, double windSpeed, double uvIndex) {
        double feelsLike = temp;
        
        // Convertir velocidad del viento de m/s a km/h
        double windSpeedKmh = windSpeed * 3.6;
        
        // Para temperaturas altas (> 27°C): usar Heat Index
        if (temp > 27.0 && humidity > 40) {
            feelsLike = calculateHeatIndex(temp, humidity);
        }
        // Para temperaturas bajas (< 10°C): usar Wind Chill
        else if (temp < 10.0 && windSpeedKmh > 4.8) {
            feelsLike = calculateWindChill(temp, windSpeedKmh);
        }
        // Para temperaturas medias: ajuste por viento y humedad
        else {
            // Ajuste por viento (el viento enfría cuando hace calor moderado también)
            if (windSpeedKmh > 5.0) {
                // El viento reduce la sensación térmica
                double windEffect = Math.min(windSpeedKmh * 0.1, 3.0); // Máximo 3°C de reducción
                feelsLike -= windEffect;
            }
            
            // Ajuste por humedad (alta humedad aumenta sensación de calor en temperaturas cálidas)
            if (temp > 20.0 && humidity > 60) {
                double humidityEffect = (humidity - 60) * 0.05; // Aumenta sensación térmica
                feelsLike += Math.min(humidityEffect, 2.0); // Máximo 2°C de aumento
            }
            
            // Ajuste por humedad baja (baja humedad reduce sensación de calor)
            if (temp > 20.0 && humidity < 40) {
                double humidityEffect = (40 - humidity) * 0.03; // Reduce sensación térmica
                feelsLike -= Math.min(humidityEffect, 1.5); // Máximo 1.5°C de reducción
            }
        }
        
        // Ajuste por UV index (UV alto aumenta sensación de calor)
        if (uvIndex > 5.0 && temp > 20.0) {
            double uvEffect = (uvIndex - 5.0) * 0.2; // Aumenta sensación térmica
            feelsLike += Math.min(uvEffect, 1.5); // Máximo 1.5°C de aumento
        }
        
        return Math.round(feelsLike * 10.0) / 10.0; // Redondear a 1 decimal
    }

    /**
     * Calcula el Heat Index (índice de calor) para temperaturas altas
     * Fórmula de Rothfusz
     */
    private double calculateHeatIndex(double temp, int humidity) {
        // Convertir temperatura a Fahrenheit para la fórmula
        double tempF = (temp * 9.0 / 5.0) + 32.0;
        
        // Fórmula de Rothfusz (simplificada)
        double hi = -42.379 + 
                    2.04901523 * tempF + 
                    10.14333127 * humidity - 
                    0.22475541 * tempF * humidity - 
                    6.83783e-3 * tempF * tempF - 
                    5.481717e-2 * humidity * humidity + 
                    1.22874e-3 * tempF * tempF * humidity + 
                    8.5282e-4 * tempF * humidity * humidity - 
                    1.99e-6 * tempF * tempF * humidity * humidity;
        
        // Convertir de vuelta a Celsius
        double hiC = (hi - 32.0) * 5.0 / 9.0;
        
        // Si el resultado es menor que la temperatura, usar la temperatura
        return Math.max(hiC, temp);
    }

    /**
     * Calcula el Wind Chill (sensación térmica por viento) para temperaturas bajas
     * Fórmula de Environment Canada
     */
    private double calculateWindChill(double temp, double windSpeedKmh) {
        // La fórmula requiere velocidad del viento en km/h y temperatura en °C
        if (windSpeedKmh < 4.8) {
            return temp; // Sin efecto del viento si es muy bajo
        }
        
        // Fórmula de Environment Canada
        double wc = 13.12 + 
                    0.6215 * temp - 
                    11.37 * Math.pow(windSpeedKmh, 0.16) + 
                    0.3965 * temp * Math.pow(windSpeedKmh, 0.16);
        
        // Si el resultado es mayor que la temperatura, usar la temperatura
        return Math.min(wc, temp);
    }
}


