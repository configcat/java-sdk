package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class Utils {
    private Utils() { /* prevent from instantiation*/ }

    public static final Gson gson = new GsonBuilder().create();
}
