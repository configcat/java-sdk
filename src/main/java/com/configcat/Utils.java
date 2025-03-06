package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

final class Utils {
    private Utils() { /* prevent from instantiation*/ }

    static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public static String sha256(byte[] byteArray) {
        return new String(Hex.encodeHex(DigestUtils.sha256(byteArray)));
    }

    public static String sha256(String text) {
        return new String(Hex.encodeHex(DigestUtils.sha256(text)));
    }

    public static String sha1(String text) {
        return new String(Hex.encodeHex(DigestUtils.sha1(text)));
    }

    public static String sha1(byte[] byteArray) {
        return new String(Hex.encodeHex(DigestUtils.sha1(byteArray)));
    }

    public static DecimalFormat getDecimalFormat() {
        DecimalFormat decimalFormat = new DecimalFormat("0.#####");
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.UK));
        return decimalFormat;
    }

    public static Config deserializeConfig(String json) {
        Config config = Utils.gson.fromJson(json, Config.class);
        String salt = config.getPreferences().getSalt();
        Segment[] segments = config.getSegments();
        if (segments == null) {
            segments = new Segment[]{};
        }
        for (Setting setting : config.getEntries().values()) {
            setting.setConfigSalt(salt);
            setting.setSegments(segments);
        }
        return config;
    }
}
