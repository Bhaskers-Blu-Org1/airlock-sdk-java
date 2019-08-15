package com.ibm.airlock.sdk.cache;


import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.cache.*;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.sdk.cache.pref.FilePreferencesFactory;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static com.ibm.airlock.sdk.AirlockMultiProductsManager.URLS_LAST_UPDATES_STORAGE;


/**
 * To manage Shared preference
 *
 * @author Denis Voloshin
 */
public class InstancePersistenceHandler extends BasePersistenceHandler {

    private final String TAG = "InstancePersistenceHandler";

    public static int DEFAULT_IN_MEMORRY_EXPIRATION_PERIOD = 10 * 1000; // on minute

    private final Set<String> instanceRuntimeFiles = new HashSet<>(Arrays.asList(
            new String[]{Constants.SP_CURRENT_CONTEXT,
                    Constants.SP_FIRED_NOTIFICATIONS, Constants.SP_NOTIFICATIONS_HISTORY,
                    Constants.SP_SYNCED_FEATURES_LIST,
                    Constants.SP_SERVER_FEATURE_LIST,
                    Constants.SP_PRE_SYNCED_FEATURES_LIST
            }
    ));

    private final Hashtable<String, ReentrantReadWriteLock> presistanceFilesReadWriteLocks = new Hashtable<>();
    private final ReentrantReadWriteLock streamsReadWriteLock = new ReentrantReadWriteLock(true);

    private final Set<String> instancePreferenceKeys = new HashSet<>(Arrays.asList(
            new String[]{Constants.SP_LAST_CALCULATE_TIME, Constants.SP_LAST_SYNC_TIME
            }
    ));

    protected InstancePreferences instancePreferences;


    private String productName;

    public InstancePersistenceHandler(String productName, Context context) {
        InstanceContext instanceContext = (InstanceContext) context;
        this.productName = productName;

        this.preferences = new InstancePreferences(FilePreferencesFactory.getAirlockCacheDirectory() + File.separator +
                instanceContext.getProductName() + File.separator + instanceContext.getSeasonId());

        init(context);

        this.instancePreferences = new InstancePreferences(FilePreferencesFactory.getAirlockCacheDirectory() + File.separator +
                instanceContext.getProductName() + File.separator + instanceContext.getSeasonId() +
                File.separator + ((InstanceContext) context).getAppVersion() + File.separator + instanceContext.getInstanceId());

        //init read/write locks
        for (String filePersistPreference : filePersistPreferences) {
            if (!presistanceFilesReadWriteLocks.containsKey(filePersistPreference)) {
                presistanceFilesReadWriteLocks.put(filePersistPreference, new ReentrantReadWriteLock(true));
            }
        }

        for (String filePersistPreference : instanceRuntimeFiles) {
            if (!presistanceFilesReadWriteLocks.containsKey(filePersistPreference)) {
                presistanceFilesReadWriteLocks.put(filePersistPreference, new ReentrantReadWriteLock(true));
            }
        }

        //If first time the app starts or it is a tests mock app (files dir is null) - do not read from file system
        if (isInitialized() && context.getFilesDir() != null) {
            new Thread(new FilePreferencesReader()).start();
        }
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        InstanceContext instanceContext = (InstanceContext) context;
        this.preferences = new InstancePreferences(FilePreferencesFactory.getAirlockCacheDirectory() + File.separator +
                instanceContext.getProductName() + File.separator + instanceContext.getAppVersion());

        this.instancePreferences = new InstancePreferences(FilePreferencesFactory.getAirlockCacheDirectory() + File.separator +
                instanceContext.getProductName() + File.separator + instanceContext.getSeasonId() +
                File.separator + ((InstanceContext) context).getAppVersion() + File.separator + instanceContext.getInstanceId());
    }


    private String getResourcePersistanceLocation(String resourceName) {
        if (instanceRuntimeFiles.contains(resourceName)) {
            InstanceContext instanceContext = (InstanceContext) context;
            return context.getFilesDir() + File.separator + instanceContext.getAppVersion()
                    + File.separator + instanceContext.getInstanceId();
        }
        return context.getFilesDir().getAbsolutePath();
    }

    @Override
    public void init(Context c, AirlockCallback callback) {
        callback.onSuccess("");
    }

    public synchronized void init(Context context) {
        this.context = context;
        this.inMemoryPreferences = new InMemoryCache();
    }

    public synchronized void reset(Context c) {
        clearRuntimeData();
        if (preferences != null) {
            clearInstanceRuntimeFiles();
            clearStreams();
        }
    }

    public synchronized void destroy() {
        clearInstanceRuntimeFiles();
        clearStreams();

        // remove instance preferences node
        try {
            instancePreferences.removeNode();
        } catch (BackingStoreException e) {
            Logger.log.e(TAG, e.getLocalizedMessage());
        }

        InstanceContext instanceContext = (InstanceContext) context;
        // delete version folder if any instance is left
        File versionFolder = new File(instanceContext.getFilesDir() + File.separator + instanceContext.getAppVersion());


        // try to delete hidden files
        if (versionFolder != null && versionFolder.listFiles() != null && versionFolder.listFiles().length > 0) {
            for (File file : versionFolder.listFiles()) {
                if (file.getName().startsWith(".")) {
                    file.delete();
                }
            }
        }

        if (versionFolder != null && versionFolder.listFiles() != null && versionFolder.listFiles().length == 0) {
            try {
                Preferences.userRoot().node(instanceContext.getFilesDir() + File.separator + instanceContext.getAppVersion()).removeNode();
            } catch (BackingStoreException e) {
                Logger.log.e(TAG, e.getLocalizedMessage());
            }
        }

        if (instanceContext.getFilesDir().listFiles() != null) {

            // delete season folder if any version is left
            boolean seasonFolderShouldBeDeleted = true;

            for (File file : instanceContext.getFilesDir().listFiles()) {
                if (file.isDirectory()) {
                    seasonFolderShouldBeDeleted = false;
                }
            }

            if (seasonFolderShouldBeDeleted) {
                for (String fileName : filePersistPreferences) {
                    new File(instanceContext.getFilesDir(), fileName).delete();
                }
                try {
                    ((InstancePreferences) preferences).removeNode();
                } catch (BackingStoreException e) {
                    Logger.log.e(TAG, e.getLocalizedMessage());
                }

            }
        }
    }

    private void clearStreams() {

        if (context != null) {
            InstanceContext instanceContext = (InstanceContext) context;
            File folder = new File(instanceContext.getFilesDir() + File.separator + instanceContext.getAppVersion()
                    + File.separator + instanceContext.getInstanceId());
            if (folder.listFiles() != null && folder.listFiles().length > 0) {
                for (File file : folder.listFiles()) {
                    if (file.getName().startsWith(Constants.STREAM_PREFIX)) {
                        file.delete();
                    }

                }
            }
        }
    }


    public void clearInstanceRuntimeFiles() {
        InstanceContext instanceContext = (InstanceContext) context;
        //On test scenario's context could be null or the filesDir could be null
        if (context != null) {

            File folder = new File(instanceContext.getFilesDir() + File.separator + instanceContext.getAppVersion()
                    + File.separator + instanceContext.getInstanceId());
            for (String file : instanceRuntimeFiles) {
                new File(folder, file).delete();

            }
        }
        inMemoryPreferences.clear();
    }

    public String getProductName() {
        return productName;
    }

    /**
     * The reason this has a seperate method is because it is called when app stops - so we need to persist synchronously
     *
     * @param jsonAsString
     */
    public void writeStream(String streamName, String jsonAsString) {
        if (jsonAsString != null && !jsonAsString.isEmpty()) {
            //if it is a tests mock app (files dir is null) - do not write to file system
            if (this.context.getFilesDir() != null) {
                final long startTime = System.currentTimeMillis();
                try {

                    InstanceContext instanceContext = (InstanceContext) context;
                    File outputFile = new File(instanceContext.getFilesDir() + File.separator + instanceContext.getAppVersion()
                            + File.separator + instanceContext.getInstanceId(), Constants.STREAM_PREFIX + streamName);

                    outputFile.createNewFile();
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    if (fos == null) {
                        //On tests that use mock context the FileOutputStream could be null...
                        return;
                    }
                    fos.write(jsonAsString.getBytes());
                    fos.close();
                    Logger.log.d(TAG, "Write to file system of : " + streamName + " took : " + (System.currentTimeMillis() - startTime));
                } catch (IOException e) {
                    Logger.log.w(TAG, "Failed to persist content of: " + streamName + " to file system. Error: " + e.getMessage());
                }
            }
        } else {
            deleteStream(streamName);
        }
    }

    public void deleteStream(String name) {
        //if it is a tests mock app (files dir is null) - do not write to file system
        if (this.context.getFilesDir() != null) {
            context.deleteFile(Constants.STREAM_PREFIX + name);
        }
    }

    @Override
    public JSONObject readJSON(String key) {
        return super.readJSON(key);
    }


    @Override
    public long read(String key, long defaultValue) {
        long value = defaultValue;

        if (this.instancePreferenceKeys.contains(key)) {
            value = instancePreferences.getLong(key, defaultValue);
        } else {
            if (preferences != null) {
                value = preferences.getLong(key, defaultValue);
            }
        }
        return value;
    }

    @Override
    public String read(String key, String defaultValue) {
        String value;
        if (filePersistPreferences.contains(key)) {
            return readFromMemory(key, defaultValue);
        } else {
            value = defaultValue;

            if (this.instancePreferenceKeys.contains(key)) {
                value = instancePreferences.getString(key, defaultValue);
            } else {
                if (preferences != null) {
                    value = preferences.getString(key, defaultValue);
                }
            }
        }
        return value;
    }


    /**
     * The reason this has a seperate method is because it is called when app stopps - so we need to persist synchronously
     */
    public JSONObject readStream(String name) {

        JSONObject value = null;
        name = Constants.STREAM_PREFIX + name;
        InstanceContext instanceContext = (InstanceContext) context;
        String streamValue = (String) readSinglePreferenceFromFileSystem(name, instanceContext.getFilesDir() + File.separator + instanceContext.getAppVersion()
                + File.separator + instanceContext.getInstanceId());
        if (streamValue != null) {
            try {
                value = new JSONObject(streamValue);
            } catch (JSONException e) {
                //DO nothing
            }
        }
        if (value == null) {
            value = new JSONObject();
        }
        return value;
    }

    public void write(String key, long value) {
        if (instancePreferenceKeys.contains(key)) {
            instancePreferences.edit().putLong(key, value);
            instancePreferences.edit().commit();
        } else {
            preferences.edit().putLong(key, value);
            preferences.edit().commit();
        }
    }


    public void write(String key, String value) {
        if (filePersistPreferences.contains(key)) {
            if (value != null && !value.isEmpty()) {
                if (saveAsJSONPreferences.contains(key)) {
                    try {
                        inMemoryPreferences.put(key, new JSONObject(value), DEFAULT_IN_MEMORRY_EXPIRATION_PERIOD);
                    } catch (JSONException e) {
                        Logger.log.e(TAG, "Failed to convert content of: " + key + " to JSONObject.");
                    }
                } else {
                    inMemoryPreferences.put(key, value, DEFAULT_IN_MEMORRY_EXPIRATION_PERIOD);
                }
                new Thread(new FilePreferencesPersister(key, value)).start();

            } else {
                //remove data
                inMemoryPreferences.remove(key);
                //context.deleteFile(key);
                File fileDel = new File(context.getFilesDir(), key);
                fileDel.delete();
            }
        } else {
            if (key.equals(Constants.SP_SEASON_ID)) {
                updateSeasonIdAndClearRuntimeData(value);
                return;
            }
            if (instancePreferenceKeys.contains(key)) {
                instancePreferences.edit().putString(key, value);
                instancePreferences.edit().commit();
            } else {
                preferences.edit().putString(key, value);
                preferences.edit().commit();
            }
        }
    }

    private ReentrantReadWriteLock getWriteReadLocker(String resourceName) {
        if (presistanceFilesReadWriteLocks.containsKey(resourceName)) {
            return presistanceFilesReadWriteLocks.get(resourceName);
        } else {
            return streamsReadWriteLock;
        }
    }


    private synchronized Object readSinglePreferenceFromFileSystem(String preferenceName, String folder) {
        //because of synchronization it is possible to reach this method but the value is inMemory...
        if (inMemoryPreferences.containsKey(preferenceName)) {
            return inMemoryPreferences.get(preferenceName);
        }
        Object preferenceValue = null;
        InputStream fis = null;
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        try {
            // get read/write lock for the specific file.
            getWriteReadLocker(preferenceName).readLock().lock();

            File fileInput = new File(folder, preferenceName);
            if (!fileInput.exists()) {
                return preferenceValue;
            }
            fis = PersistenceEncryptor.decryptAES(fileInput);
            if (fis != null) {
                byte[] buffer = new byte[fis.available()];
                int length;
                while ((length = fis.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }

                if (saveAsJSONPreferences.contains(preferenceName)) {
                    preferenceValue = new JSONObject(result.toString("UTF-8"));
                } else {
                    preferenceValue = result.toString("UTF-8");
                }
                inMemoryPreferences.put(preferenceName, preferenceValue);
            }
        } catch (IOException | JSONException e) {
            Logger.log.e(TAG, e.getMessage(), e);
        } finally {
            try {
                getWriteReadLocker(preferenceName).readLock().unlock();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                fis.close();
            } catch (Throwable ignore) {
            }
        }
        return preferenceValue;
    }

    @CheckForNull
    @Nullable
    public synchronized Object readSinglePreferenceFromFileSystem(String preferenceName) {
        if (instanceRuntimeFiles.contains(preferenceName)) {
            InstanceContext instanceContext = (InstanceContext) context;
            return readSinglePreferenceFromFileSystem(preferenceName, instanceContext.getFilesDir() + File.separator + instanceContext.getAppVersion()
                    + File.separator + instanceContext.getInstanceId());
        }

        return readSinglePreferenceFromFileSystem(preferenceName, context.getFilesDir().getAbsolutePath());
    }


    public class FilePreferencesPersister implements Runnable {
        private final String key;
        private final String value;

        public FilePreferencesPersister(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public void run() {
            try {
                // wait for read/write lock for the specific file.
                getWriteReadLocker(key).writeLock().lock();

                File outputFile = new File(new File(getResourcePersistanceLocation(key)), key);
                outputFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(outputFile);

                if (fos == null) {
                    //On tests that use mock context the FileOutputStream could be null...
                    return;
                }
                fos.write(value.getBytes());
                fos.flush();
                fos.close();
                PersistenceEncryptor.encryptAES(outputFile);
            } catch (IOException e) {
                Logger.log.w(TAG, "Failed to persist content of: " + key + " to file system. Error: " + e.getMessage());
            } finally {
                getWriteReadLocker(key).writeLock().unlock();
            }
        }
    }

    private class FilePreferencesReader implements Runnable {
        public void run() {
            for (String preferenceName : filePersistPreferences) {
                readSinglePreferenceFromFileSystem(preferenceName);
            }
        }
    }
}
