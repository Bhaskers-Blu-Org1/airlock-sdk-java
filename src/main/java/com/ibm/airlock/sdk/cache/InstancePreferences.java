package com.ibm.airlock.sdk.cache;

import com.ibm.airlock.common.cache.Cacheable;
import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.InMemoryCache;
import com.ibm.airlock.common.cache.SharedPreferences;
import com.ibm.airlock.common.log.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static com.ibm.airlock.sdk.cache.InstancePersistenceHandler.DEFAULT_IN_MEMORRY_EXPIRATION_PERIOD;

/**
 * Created by Denis Voloshin on 05/11/2017.
 */
public class InstancePreferences implements SharedPreferences {

    private static String TAG = "JavaSharedPreferences";
    private static String PRODUCT_PREF = "airlock.product.pref";

    Preferences preferences;
    private String productPreferencesFolder;
    private InstancePreferencesEditor editor;
    private Cacheable<String, Object> inMemoryCache;

    public InstancePreferences(String productName, Context context) {
        this.preferences = Preferences.userRoot().node(context.getFilesDir() + File.separator + productName + File.separator + PRODUCT_PREF);
        this.editor = new InstancePreferencesEditor(preferences, this);
        this.inMemoryCache = new InMemoryCache();
    }

    public InstancePreferences(String prefFolder, String prefFileName) {
        this.preferences = Preferences.userRoot().node(prefFolder + File.separator + prefFileName);
        this.editor = new InstancePreferencesEditor(preferences, this);
        this.inMemoryCache = new InMemoryCache();
    }

    public InstancePreferences(String productPreferencesFolder) {
        this.productPreferencesFolder = productPreferencesFolder;
        this.preferences = Preferences.userRoot().node(productPreferencesFolder + File.separator + PRODUCT_PREF);
        this.editor = new InstancePreferencesEditor(preferences, this);
        this.inMemoryCache = new InMemoryCache();
    }


    public boolean isExists() throws BackingStoreException{
        return Preferences.userRoot().nodeExists(this.productPreferencesFolder + File.separator + PRODUCT_PREF);

    }

    public void removeNode() throws BackingStoreException {
        if (Preferences.userRoot().nodeExists(this.productPreferencesFolder + File.separator + PRODUCT_PREF)) {
            Preferences.userRoot().node(this.productPreferencesFolder + File.separator + PRODUCT_PREF).removeNode();
        }
        if (Preferences.userRoot().nodeExists(this.productPreferencesFolder)) {
            Preferences.userRoot().node(this.productPreferencesFolder).removeNode();
        }
    }


    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return preferences.getLong(key, defValue);
    }

    @CheckForNull
    @Override
    public String getString(String key, @Nullable String defValue) {
        String result = null;
        try {
            if (inMemoryCache.containsKey(key)) {
                return (String) inMemoryCache.get(key);
            }
            //result = (String) ExtendedPreferences.getObject(preferences, key);
            result = preferences.get(key, defValue);
        } catch (Exception e) {
            Logger.log.w(TAG, e.getMessage());
        }
        if (result != null || (result == null && defValue != null)) {
            inMemoryCache.put(key, result == null ? defValue : result, DEFAULT_IN_MEMORRY_EXPIRATION_PERIOD);
        }
        return result == null ? defValue : result;
    }

    @Override
    public Editor edit() {
        return editor;
    }

    @Override
    public int getInt(String key, int defValue) {
        return preferences.getInt(key, defValue);
    }

    @Override
    public Set<String> getStringSet(String key, @Nullable Object o) {
        if (this.preferences.get(key, o == null ? null : o.toString()) == null) {
            return null;
        }
        try {
            JSONArray array = new JSONArray(this.preferences.get(key, o == null ? null : o.toString()));
            Set<String> set = new HashSet<String>() {
            };
            int len = array.length();
            for (int i = 0; i < len; i++) {
                set.add(array.get(i).toString());
            }
            return set;
        } catch (JSONException e) {
            return null;
        }
    }

    public class InstancePreferencesEditor implements Editor {

        private Preferences preferences;
        private InstancePreferences sharedPreferences;

        public InstancePreferencesEditor(Preferences preferences, InstancePreferences sharedPreferences) {
            this.preferences = preferences;
            this.sharedPreferences = sharedPreferences;
        }

        @Override
        public Editor remove(String key) {
            try {
                this.preferences.remove(key);
                this.sharedPreferences.inMemoryCache.remove(key);
            } catch (Exception e) {
                Logger.log.w(TAG, e.getMessage());
            }
            return this;
        }

        @Override
        public Editor clear() {
            try {
                String[] keys = this.preferences.keys();
                for (String key : keys) {
                    preferences.remove(key);
                }
                this.sharedPreferences.inMemoryCache.clear();

            } catch (BackingStoreException e) {
                Logger.log.e(TAG, e.getMessage());
            }
            return this;
        }

        @Override
        public boolean commit() {
            try {
                this.preferences.flush();
            } catch (BackingStoreException e) {
                return false;
            }
            return true;
        }

        @Override
        public void apply() {
            commit();
        }

        @Override
        public Editor putInt(String key, int value) {
            this.preferences.putInt(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            this.preferences.putLong(key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            this.preferences.putFloat(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            this.preferences.putBoolean(key, value);
            return this;
        }

        @Override
        public void putString(String key, String value) {
            if (value == null) {
                return;
            }
            try {
                this.preferences.put(key, value);
                this.sharedPreferences.inMemoryCache.put(key, value, DEFAULT_IN_MEMORRY_EXPIRATION_PERIOD);
            } catch (Exception e) {
                Logger.log.e(TAG, e.getMessage());
            }
        }
    }
}
