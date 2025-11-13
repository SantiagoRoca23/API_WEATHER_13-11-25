package com.example.api_weather;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.api_weather.model.WeatherResponse;
import com.example.api_weather.viewmodel.WeatherViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etCityName;
    private MaterialButton btnSearch;
    private ProgressBar progressBar;
    private TextView tvError;
    private MaterialCardView cardWeather;
    
    private TextView tvCityName;
    private TextView tvCountry;
    private ImageView ivWeatherIcon;
    private TextView tvTemperature;
    private TextView tvDescription;
    private TextView tvFeelsLike;
    private TextView tvTempMin;
    private TextView tvTempMax;
    private TextView tvHumidity;
    private TextView tvPressure;
    private TextView tvWind;
    private TextView tvVisibility;
    
    private WeatherViewModel weatherViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupViewModel();
        setupListeners();
    }

    private void initViews() {
        etCityName = findViewById(R.id.etCityName);
        btnSearch = findViewById(R.id.btnSearch);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);
        cardWeather = findViewById(R.id.cardWeather);
        
        tvCityName = findViewById(R.id.tvCityName);
        tvCountry = findViewById(R.id.tvCountry);
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvDescription = findViewById(R.id.tvDescription);
        tvFeelsLike = findViewById(R.id.tvFeelsLike);
        tvTempMin = findViewById(R.id.tvTempMin);
        tvTempMax = findViewById(R.id.tvTempMax);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvPressure = findViewById(R.id.tvPressure);
        tvWind = findViewById(R.id.tvWind);
        tvVisibility = findViewById(R.id.tvVisibility);
    }

    private void setupViewModel() {
        weatherViewModel = new ViewModelProvider(this).get(WeatherViewModel.class);
        
        // Observar datos del clima
        weatherViewModel.getWeatherData().observe(this, weatherResponse -> {
            if (weatherResponse != null) {
                displayWeatherData(weatherResponse);
            }
        });
        
        // Observar errores
        weatherViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showError(errorMessage);
            }
        });
        
        // Observar estado de carga
        weatherViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                btnSearch.setEnabled(!isLoading);
            }
        });
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> {
            String cityName = etCityName.getText().toString().trim();
            if (TextUtils.isEmpty(cityName)) {
                Toast.makeText(this, "Por favor ingresa el nombre de una ciudad", Toast.LENGTH_SHORT).show();
                return;
            }
            hideError();
            weatherViewModel.fetchWeatherByCity(cityName);
        });
    }

    private void displayWeatherData(WeatherResponse weatherResponse) {
        cardWeather.setVisibility(View.VISIBLE);
        
        // Ciudad y país
        tvCityName.setText(weatherResponse.getName());
        if (weatherResponse.getSys() != null && weatherResponse.getSys().getCountry() != null) {
            tvCountry.setText(weatherResponse.getSys().getCountry());
        }
        
        // Icono del clima
        if (weatherResponse.getWeather() != null && !weatherResponse.getWeather().isEmpty()) {
            String icon = weatherResponse.getWeather().get(0).getIcon();
            String iconUrl = "https://openweathermap.org/img/wn/" + icon + "@4x.png";
            Glide.with(this)
                    .load(iconUrl)
                    .into(ivWeatherIcon);
            
            // Descripción
            String description = weatherResponse.getWeather().get(0).getDescription();
            if (description != null) {
                description = capitalizeFirstLetter(description);
                tvDescription.setText(description);
            }
        }
        
        // Temperatura
        if (weatherResponse.getMain() != null) {
            double temp = weatherResponse.getMain().getTemp();
            tvTemperature.setText(String.format("%.0f°C", temp));
            
            double feelsLike = weatherResponse.getMain().getFeelsLike();
            tvFeelsLike.setText(String.format("Sensación: %.0f°C", feelsLike));
            
            double tempMin = weatherResponse.getMain().getTempMin();
            tvTempMin.setText(String.format("%.0f°C", tempMin));
            
            double tempMax = weatherResponse.getMain().getTempMax();
            tvTempMax.setText(String.format("%.0f°C", tempMax));
            
            int humidity = weatherResponse.getMain().getHumidity();
            tvHumidity.setText(String.format("%d%%", humidity));
            
            int pressure = weatherResponse.getMain().getPressure();
            tvPressure.setText(String.format("%d hPa", pressure));
        }
        
        // Viento
        if (weatherResponse.getWind() != null) {
            double windSpeed = weatherResponse.getWind().getSpeed();
            tvWind.setText(String.format("%.1f m/s", windSpeed));
        }
        
        // Visibilidad
        int visibility = weatherResponse.getVisibility();
        if (visibility > 0) {
            double visibilityKm = visibility / 1000.0;
            tvVisibility.setText(String.format("%.1f km", visibilityKm));
        } else {
            tvVisibility.setText("N/A");
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
        cardWeather.setVisibility(View.GONE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
        tvError.setText("");
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
