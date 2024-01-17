package com.configcat;

import com.google.gson.annotations.SerializedName;

class Preferences {

    @SerializedName(value = "u")
    private String baseUrl;
    @SerializedName(value = "r")
    private int redirect;
    @SerializedName(value = "s")
    private String salt;

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getRedirect() {
        return redirect;
    }

    public String getSalt() {
        return salt;
    }
}

