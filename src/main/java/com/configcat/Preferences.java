package com.configcat;

import com.google.gson.annotations.SerializedName;


/**
 * The config preferences.
 */
class Preferences {

    @SerializedName(value = "u")
    private String baseUrl;
    @SerializedName(value = "r")
    private int redirect;
    /**
     * The config salt which was used to hash sensitive data.
     */
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

