package com.ibm.airlock.sdk.cache;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.SharedPreferences;
import org.json.JSONObject;

import java.io.*;

/**
 * Created by Denis Voloshin on 23/07/2017.
 */
public class InstanceContext implements Context {

    private File contextFolder;
    private String productName;
    private String rootFolder;
    private String appVersion;
    private String instanceId;
    private String productId;
    private String seasonId;


    public InstanceContext(String instanceId, String rootFolder, String defaults, String appVersion) {

        JSONObject defaultsJson = new JSONObject(defaults);
        this.rootFolder = rootFolder;
        this.seasonId = getSeasonId(defaultsJson);
        this.instanceId = instanceId;
        this.productId = getProductId(defaultsJson);
        this.productName = getProductName(defaultsJson);
        this.appVersion = appVersion;

        this.contextFolder = new File(rootFolder + File.separator + productName + File.separator + seasonId);
        if (!this.contextFolder.exists()) {
            this.contextFolder.mkdirs();
        }
    }

    public InstanceContext(String instanceId, String rootFolder, String seasonId, String productName, String appVersion) {
        this(rootFolder + File.separator + productName + File.separator + seasonId);
        this.rootFolder = rootFolder;
        this.productName = productName;
        this.appVersion = appVersion;
        this.instanceId = instanceId;
        this.seasonId = seasonId;
    }

    public InstanceContext(String contextFolder) {
        this.contextFolder = new File(contextFolder);
        if (!this.contextFolder.exists()) {
            this.contextFolder.mkdirs();
        }
    }

    public String getProductId() {
        return productId;
    }

    public String getSeasonId() {
        return seasonId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getProductName() {
        return productName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    @Override
    public File getFilesDir() {
        return this.contextFolder;
    }

    @Override
    public SharedPreferences getSharedPreferences(String spName, int modePrivate) {
        return null;
    }

    @Override
    public void deleteFile(String key) {
        new File(key).delete();
    }

    @Override
    public FileInputStream openFileInput(String preferenceName) throws FileNotFoundException {
        return new FileInputStream(new File(preferenceName));
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return new FileOutputStream(new File(name));
    }

    @Override
    public Object getSystemService(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream openRawResource(int name) {
        throw new UnsupportedOperationException();
    }

    public static String getSeasonId(JSONObject defaultsJSON) {
        return defaultsJSON.optString("seasonId");
    }

    public static String getProductId(JSONObject defaultsJSON) {
        return defaultsJSON.optString("productId");
    }

    public static String getProductName(JSONObject defaultsJSON) {
        return defaultsJSON.optString("productName");
    }
}
