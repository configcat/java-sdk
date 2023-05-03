package com.configcat.models;

import com.google.gson.annotations.SerializedName;

public class Preferences {
    @SerializedName(value = "u")
    private String baseUrl;
    @SerializedName(value = "r")
    private int redirect;

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getRedirect() {
        return redirect;
    }
}

