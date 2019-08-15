package com.ibm.airlock.sdk.config;

import java.net.MalformedURLException;
import java.net.URL;

public class ConfigurationManager {

    public static final String CACHE_ZIP_FILE_NAME = "airlock-cache";
    public static final String CACHE_VOLUME = "CACHE_VOLUME";
    public static final String SSD_CACHE_VOLUME = "SSD_CACHE_VOLUME";
    private static final String ENABLE_CACHE_ENCRYPTION = "ENABLE_CACHE_ENCRYPTION";
    private static final String FORWARD_PROXY_ADDRESS = "FORWARD_PROXY_ADDRESS";
    private static final String VAS_ADDRESS = "VAS_ADDRESS";
    private static final String VAS_VERSION = "VAS_VERSION";

    private static String getForwardProxyAddress() {
        return System.getenv(FORWARD_PROXY_ADDRESS) == null ||
                (System.getenv(FORWARD_PROXY_ADDRESS) != null && System.getenv(FORWARD_PROXY_ADDRESS).isEmpty())
                ? null : System.getenv(FORWARD_PROXY_ADDRESS);
    }

    public static String getVASAddress() {
        return System.getenv(VAS_ADDRESS) == null ||
                (System.getenv(VAS_ADDRESS) != null && System.getenv(VAS_ADDRESS).isEmpty())
                ? null : System.getenv(VAS_ADDRESS);
    }

    public static String getVASVersion() {
        return System.getenv(VAS_VERSION) == null ||
                (System.getenv(VAS_VERSION) != null && System.getenv(VAS_VERSION).isEmpty())
                ? "v1.0" : System.getenv(VAS_VERSION);
    }

    public static int getProxyPort() {
        if (getForwardProxyAddress() == null) {
            return -1;
        }
        try {
            URL aURL = new URL(getForwardProxyAddress());
            return aURL.getPort();
        } catch (MalformedURLException e) {
            return -1;
        }
    }

    public static String getProxyUrl() {
        if (getForwardProxyAddress() == null) {
            return null;
        }
        try {
            URL aURL = new URL(getForwardProxyAddress());
            return aURL.getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }


    public static String getCacheVolume() {
        return System.getenv(CACHE_VOLUME) == null ||
                (System.getenv(CACHE_VOLUME) != null && System.getenv(CACHE_VOLUME).isEmpty())
                ? null : System.getenv(CACHE_VOLUME);
    }

    public static String getSSDCacheVolume() {
        return System.getenv(SSD_CACHE_VOLUME) == null ||
                (System.getenv(SSD_CACHE_VOLUME) != null && System.getenv(SSD_CACHE_VOLUME).isEmpty())
                ? null : System.getenv(SSD_CACHE_VOLUME);
    }

    public static boolean isCacheShouldBeEncrypted() {
        try {
            return System.getenv(ENABLE_CACHE_ENCRYPTION) == null ? false : Boolean.parseBoolean(System.getenv(ENABLE_CACHE_ENCRYPTION));
        } catch (Exception e) {
            return false;
        }
    }
}
