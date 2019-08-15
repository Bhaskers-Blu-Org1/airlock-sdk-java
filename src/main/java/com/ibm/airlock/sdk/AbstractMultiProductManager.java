package com.ibm.airlock.sdk;

import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.util.Base64;
import com.ibm.airlock.common.util.Base64Decoder;
import com.ibm.airlock.sdk.cache.InstanceContext;
import com.ibm.airlock.sdk.cache.pref.FilePreferencesFactory;
import com.ibm.airlock.sdk.debug.AirlockDebugger;
import com.ibm.airlock.sdk.log.JavaLog;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.xml.bind.DatatypeConverter;
import java.lang.reflect.MalformedParametersException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractMultiProductManager {

    static {
        Base64.init(new Base64Decoder() {
            @Override
            public byte[] decode(String str) {
                return DatatypeConverter.parseBase64Binary(str);
            }
        });
        Logger.setLogger(new JavaLog());
        System.setProperty("java.util.prefs.PreferencesFactory", FilePreferencesFactory.class.getName());
    }

    private static final String PRODUCT_NAME = "productName";

    private static final String PRODUCT_ID = "productId";

    private static final String SEASON_ID = "seasonId";

    protected abstract void restoreProductsState();

    protected abstract void restoreProduct(String instanceId, String seasonId, String productId, String productName, String appVersion);

    protected abstract void runtimeReset();

    protected abstract void reset();

    protected abstract AirlockProductInstanceManager createProduct(InstanceContext instanceContext);

    protected abstract AirlockProductInstanceManager createProduct(String instanceId, String seasonId, String productId, String productName, String appVersion) throws JSONException, MalformedParametersException;

    protected abstract void removeAirlockProductManager(String instanceId);

    protected abstract AirlockDebugger getAirlockDebugger(ProductMetaData product);

    protected abstract AirlockProductInstanceManager getAirlockProductManager(String instanceId);

    protected abstract AirlockDebugger getAirlockDebugger(String instanceId);

    @CheckForNull
    public String getProductName(@Nullable String defaultFile) throws JSONException {
        if (defaultFile == null) {
            return null;
        }
        JSONObject defaultFileJson = new JSONObject(defaultFile);
        return defaultFileJson.optString(PRODUCT_NAME);
    }

    @CheckForNull
    public String getProductId(@Nullable String defaultFile) throws JSONException {
        if (defaultFile == null) {
            return null;
        }
        JSONObject defaultFileJson = new JSONObject(defaultFile);
        return defaultFileJson.optString(PRODUCT_ID);
    }

    @CheckForNull
    public String getSeasonId(@Nullable String defaultFile) throws JSONException {
        if (defaultFile == null) {
            return null;
        }
        JSONObject defaultFileJson = new JSONObject(defaultFile);
        return defaultFileJson.optString(SEASON_ID);
    }


    public static class ProductMetaData {

        static final String PRODUCT_ID = "productId";
        static final String SEASON_ID = "seasonId";
        static final String INSTANCE_ID = "instanceId";
        static final String PRODUCT_NAME = "productName";
        static final String APP_VERSION = "appVersion";
        static final String PRODUCT_PERSISTENCE_DIR = "persistanceDir";

        private final String productId;
        private final String seasonId;
        private final String instanceId;
        private final String productName;
        private String appVersion = "N/A";
        private String persistenceDirectory;

        public ProductMetaData(JSONObject json) {
            this.instanceId = json.optString(INSTANCE_ID);
            this.productId = json.optString(PRODUCT_ID);
            this.seasonId = json.optString(SEASON_ID);
            this.productName = json.optString(PRODUCT_NAME);
            this.appVersion = json.optString(APP_VERSION);
            this.persistenceDirectory = json.optString(PRODUCT_PERSISTENCE_DIR);
        }

        public ProductMetaData(String instanceId, String seasonId, String productId, String productName, String appVersion) {
            this.instanceId = instanceId;
            this.seasonId = seasonId;
            this.productId = productId;
            this.appVersion = appVersion;
            this.productName = productName;
        }

        public ProductMetaData(String instanceId) {
            this(instanceId, "N/A", "N/A", "N/A", "N/A");

        }

        public String getSeasonId() {
            return seasonId;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public String getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public String getAppVersion() {
            return appVersion;
        }

        public JSONObject toFullJSON() {
            JSONObject fullJSON = toJSON();
            fullJSON.put(PRODUCT_PERSISTENCE_DIR, this.persistenceDirectory);
            return fullJSON;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put(SEASON_ID, seasonId);
            json.put(INSTANCE_ID, instanceId);
            json.put(PRODUCT_ID, productId);
            json.put(PRODUCT_NAME, productName);
            json.put(APP_VERSION, appVersion);

            return json;
        }
    }
}
