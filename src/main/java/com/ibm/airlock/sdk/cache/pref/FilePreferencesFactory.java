package com.ibm.airlock.sdk.cache.pref;


import com.ibm.airlock.sdk.config.ConfigurationManager;

import java.io.File;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

public class FilePreferencesFactory implements PreferencesFactory {

    Preferences rootPreferences;

    public Preferences systemRoot() {
        return userRoot();
    }

    public Preferences userRoot() {
        if (rootPreferences == null) {
            new File(getAirlockCacheDirectory()).mkdir();
            rootPreferences = new FilePreferences(null, "");
        }
        return rootPreferences;
    }

    public static String getAirlockCacheDirectory() {
        return ConfigurationManager.getCacheVolume() == null
                ? "cache"+File.separator+"airlock" : ConfigurationManager.getCacheVolume() + File.separator + "airlock";
    }
}