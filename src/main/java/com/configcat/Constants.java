package com.configcat;

final class Constants {
    private Constants() { /* prevent from instantiation*/ }

    static final long DISTANT_FUTURE = Long.MAX_VALUE;
    static final long DISTANT_PAST = 0;
    static final String CONFIG_JSON_NAME = "config_v6.json";
    static final String SERIALIZATION_FORMAT_VERSION = "v2";
    static final String VERSION = "9.4.2";

    static final String SDK_KEY_PROXY_PREFIX = "configcat-proxy/";
    static final String SDK_KEY_PREFIX = "configcat-sdk-1";

    static final int SDK_KEY_SECTION_LENGTH = 22;

}