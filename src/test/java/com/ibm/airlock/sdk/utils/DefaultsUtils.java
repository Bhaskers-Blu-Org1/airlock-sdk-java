package com.ibm.airlock.sdk.utils;

import org.json.JSONArray;
import org.json.JSONObject;

public class DefaultsUtils {
    public static String getSeasonId(String defaultsFile){
        JSONObject defaultsJSON = new JSONObject(defaultsFile);
        return defaultsJSON.optString("seasonId");
    }

    public static String getProductId(String defaultsFile){
        JSONObject defaultsJSON = new JSONObject(defaultsFile);
        return defaultsJSON.optString("productId");
    }

    public static String getProductName(String defaultsFile){
        JSONObject defaultsJSON = new JSONObject(defaultsFile);
        return defaultsJSON.optString("productName");
    }
}
