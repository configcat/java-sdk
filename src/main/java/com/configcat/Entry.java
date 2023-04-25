package com.configcat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class Entry {

    //TODO javadoc
    public static final  class  DateTimeUtils{
        public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));

        public static boolean isValidDate(String date){
            try {
                DATE_TIME_FORMATTER.parse(date);
            }catch (DateTimeParseException e){
                return false;
            }
            return true;
        }
        public static long parseToLong(String dateTime){
            return Instant.EPOCH.until(Instant.from(DateTimeUtils.DATE_TIME_FORMATTER.parse(dateTime)), ChronoUnit.MILLIS);

        }

        public  static String format(long timeInMilliseconds){
            return DateTimeUtils.DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(timeInMilliseconds));
        }
    }
    private Config config;
    private String eTag;
    private String configJson;
    private String fetchTimeRaw;

    public Config getConfig() {
        return config;
    }

    public String getETag() {
        return eTag;
    }

    public long getFetchTime() {
        return fetchTimeRaw == null || fetchTimeRaw.isEmpty() ? 0 : DateTimeUtils.parseToLong(fetchTimeRaw);
    }

    public String getConfigJson() {
        return configJson;
    }

    public String getFetchTimeRaw() {
        return fetchTimeRaw;
    }

    public Entry withFetchTime(String fetchTimeRaw) {
        return new Entry(getConfig(), getETag(), getConfigJson(), fetchTimeRaw);
    }

    public Entry(Config config, String eTag, String configJson, String fetchTimeRaw) {
        this.config = config;
        this.eTag = eTag;
        this.configJson = configJson;
        this.fetchTimeRaw = fetchTimeRaw;
    }

    boolean isEmpty() {
        return EMPTY.equals(this);
    }

    public static final Entry EMPTY = new Entry(Config.EMPTY, "", "", "");

    public String serializedForCache() {
        return getFetchTimeRaw() + "\n" + getETag() + "\n" + getConfigJson();
    }

    public static Entry deserializeFromCache(String cacheValue) throws Exception {
        if(cacheValue == null || cacheValue.isEmpty()){
            return null;
        }

        int fetchTimeIndex = cacheValue.indexOf("\n");
        int eTagIndex = cacheValue.indexOf("\n", fetchTimeIndex + 1);
        if (fetchTimeIndex < 0 || eTagIndex < 0) {
            throw new Exception("Number of values is fewer than expected.");
        }
        String fetchTimeRaw = cacheValue.substring(0, fetchTimeIndex);
        if (!DateTimeUtils.isValidDate(fetchTimeRaw)){
            throw new Exception("Invalid fetch time format.");
        }

        String eTag = cacheValue.substring(fetchTimeIndex + 1, eTagIndex);
        if(eTag.isEmpty()){
            throw new Exception("Invalid eTag format.");
        }
        String configJson = cacheValue.substring(eTagIndex + 1, cacheValue.length());
        if(configJson.isEmpty()){
            throw new Exception("Invalid config jsom format.");
        }
        try {
            Config config = Utils.gson.fromJson(configJson, Config.class);
            return new Entry(config, eTag, configJson, fetchTimeRaw);
        }catch (Exception e){
            throw new Exception("Invalid config jsom format.");
        }

    }
}
