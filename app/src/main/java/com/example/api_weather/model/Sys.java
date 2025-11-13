package com.example.api_weather.model;

import com.google.gson.annotations.SerializedName;

public class Sys {
    @SerializedName("type")
    private int type;
    
    @SerializedName("id")
    private int id;
    
    @SerializedName("country")
    private String country;
    
    @SerializedName("sunrise")
    private long sunrise;
    
    @SerializedName("sunset")
    private long sunset;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public long getSunrise() {
        return sunrise;
    }

    public void setSunrise(long sunrise) {
        this.sunrise = sunrise;
    }

    public long getSunset() {
        return sunset;
    }

    public void setSunset(long sunset) {
        this.sunset = sunset;
    }
}

