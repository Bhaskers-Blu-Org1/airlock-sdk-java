package com.ibm.airlock.sdk;

import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.BaseAirlockProductManager;
import com.ibm.airlock.common.cache.*;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.engine.StateFullContext;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.net.ConnectionManager;
import com.ibm.airlock.common.streams.StreamsManager;
import com.ibm.airlock.common.util.*;
import com.ibm.airlock.sdk.cache.InstanceContext;
import com.ibm.airlock.sdk.cache.InstancePersistenceHandler;
import com.ibm.airlock.sdk.cache.InstancePreferences;
import com.ibm.airlock.sdk.cache.pref.FilePreferencesFactory;
import com.ibm.airlock.sdk.log.JavaLog;
import com.ibm.airlock.sdk.net.JavaOkHttpClientBuilder;
import com.ibm.airlock.sdk.notifications.InstanceNotificationsManager;
import com.ibm.airlock.sdk.util.ProductLocaleProvider;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import static com.ibm.airlock.sdk.AirlockMultiProductsManager.URLS_LAST_UPDATES_STORAGE;

/**
 * Created by Denis Voloshin on 06/11/2017.
 */
public class AirlockProductInstanceManager extends BaseAirlockProductManager {

    private static final String TAG = "AirlockProductManager";

    static {
        Base64.init(new Base64Decoder() {
            @Override
            public byte[] decode(String str) {
                return DatatypeConverter.parseBase64Binary(str);
            }
        });
        Logger.setLogger(new JavaLog());
        System.setProperty("java.util.prefs.PreferencesFactory", FilePreferencesFactory.class.getName());
        InMemoryCache.setDefaultExpirationPeriod(10 * 60 * 1000); // 10 minute
        CacheManager.setFeaturesMapTimeToLive(10 * 1000); // 10 seconds
    }

    private LocaleProvider localeProvider;


    private String productName;

    private InstanceContext instanceContext;
    @Nullable
    private JSONObject lastCalculatedUserProfile;
    @Nullable
    private JSONObject lastCalculatedAirlockContext;

    public AirlockProductInstanceManager(Context instanceContext) {
        this((InstanceContext) instanceContext);
    }


    public boolean isInitialized() {
        return init;
    }

    public AirlockProductInstanceManager(InstanceContext instanceContext) {
        super(instanceContext.getAppVersion());
        productName = instanceContext.getProductName();
        this.instanceContext = instanceContext;
        this.init = false;
    }

    public void initSDKFromCache(String contextLocation, String productName, String seasonId, String appVersion, String instanceId) throws AirlockInvalidFileException, IOException {
        /**
         * Allows multiple initSDK calls, skip initialization logic if it's already done.
         */
        if (init) {
            return;
        }

        InstanceContext instanceContext = new InstanceContext(instanceId, contextLocation, seasonId, productName, appVersion);
        InstancePersistenceHandler persistenceHandler = new InstancePersistenceHandler(productName, instanceContext);


        //no defaults file stored, seems like the initSDKFromCache called before the defaults file has been loaded.
        if (persistenceHandler.read(Constants.SP_DEFAULT_FILE, "{}").equals("{}")) {
            return;
        }

        this.notificationsManager = new InstanceNotificationsManager(instanceContext, persistenceHandler, appVersion, cacheManager.getAirlockContextManager());
        this.streamsManager = new StreamsManager(persistenceHandler, appVersion);

        setLocale(persistenceHandler);

        this.connectionManager = new ConnectionManager(new JavaOkHttpClientBuilder(), persistenceHandler.read(Constants.SP_PRODUCT_KEY, null));
        this.cacheManager.init(productName,
                instanceContext,
                persistenceHandler.read(Constants.SP_DEFAULT_FILE, "{}"),
                appVersion, persistenceHandler,
                this.streamsManager,
                this.notificationsManager,
                this.connectionManager);
        init = true;
    }


    public void initSDK(String defaultFile, String key) throws AirlockInvalidFileException, IOException {
        initSDK(instanceContext, defaultFile, appVersion, key);
    }


    @Override
    public void initSDK(Context productContext, String defaultFile, String appVersion) throws AirlockInvalidFileException, IOException {
        initSDK(productContext, defaultFile, appVersion, null);
    }

    @Override
    public Feature calculateFeatures(@Nullable JSONObject context, String locale) throws AirlockNotInitializedException, JSONException {
        return this.cacheManager.calculateFeatures(context, locale);
    }

    private void setLocale(PersistenceHandler persistenceHandler) {
        ProductLocaleProvider productLocaleProvider = null;
        try {
            productLocaleProvider = new ProductLocaleProvider(persistenceHandler.read(Constants.SP_CURRENT_LOCALE, Locale.getDefault().toString()));
        } catch (Exception e) {
            Logger.log.e(TAG, e.getMessage());
        }

        if (productLocaleProvider != null) {
            this.cacheManager.setLocaleProvider(productLocaleProvider);
        } else {
            this.cacheManager.setLocaleProvider(new ProductLocaleProvider(Locale.getDefault().toString()));
        }
    }

    public void initSDK(Context productContext, RuntimeLoader runtimeLoader, String encryptionKey) throws AirlockInvalidFileException, IOException {
        /**
         * Allows multiple initSDK calls, skip initialization logic if it's already done.
         */
        if (init) {
            return;
        }

        PersistenceHandler persistenceHandler = new InstancePersistenceHandler(this.productName, productContext);

        this.notificationsManager = new InstanceNotificationsManager(productContext, persistenceHandler, appVersion, cacheManager.getAirlockContextManager());
        this.streamsManager = new StreamsManager(persistenceHandler, appVersion);
        this.connectionManager = new ConnectionManager(new JavaOkHttpClientBuilder(), encryptionKey);

        setLocale(persistenceHandler);

        runtimeLoader.loadRuntimeFilesOnStartup(this);

        this.cacheManager.init(this.productName, productContext,
                null, appVersion, persistenceHandler,
                this.streamsManager,
                this.notificationsManager,
                this.connectionManager);
        init = true;
    }

    public void initSDK(Context productContext, String defaultFile, String appVersion, @Nullable String key) throws AirlockInvalidFileException, IOException {
        /**
         * Allows multiple initSDK calls, skip initialization logic if it's already done.
         */
        if (init) {
            return;
        }

        if (key == null) {
            key = "";
        }

        this.appVersion = appVersion;
        PersistenceHandler persistenceHandler = new InstancePersistenceHandler(this.productName, productContext);
        persistenceHandler.write(Constants.SP_PRODUCT_KEY, key);
        this.notificationsManager = new InstanceNotificationsManager(productContext, persistenceHandler, appVersion, cacheManager.getAirlockContextManager());
        this.streamsManager = new StreamsManager(persistenceHandler, appVersion);
        this.connectionManager = new ConnectionManager(new JavaOkHttpClientBuilder(), key);

        setLocale(persistenceHandler);

        this.cacheManager.init(this.productName, productContext,
                defaultFile, appVersion, persistenceHandler,
                this.streamsManager,
                this.notificationsManager,
                this.connectionManager);
        init = true;
    }

    /**
     * Calculates the status of the features according to the pullFeatures results.
     * No feature status changes are exposed until the syncFeatures method is called.
     *
     * @param userProfile    the user profile
     * @param airlockContext the airlock context
     * @throws AirlockNotInitializedException if the Airlock SDK has not been initialized
     * @throws JSONException                  if the pullFeature results, the userProfile or the deviceProfile is not in the correct JSON format.
     */
    @Override
    public void calculateFeatures(@Nullable JSONObject userProfile, @Nullable JSONObject airlockContext, @Nullable Set<String> featureToProcess) throws AirlockNotInitializedException, JSONException {
        this.lastCalculatedUserProfile = userProfile;
        this.lastCalculatedAirlockContext = airlockContext;
        super.calculateFeatures(userProfile, airlockContext, featureToProcess);
    }

    public void reset() {
        reset(instanceContext, true);
    }

    public void destroy() {
        if (isInitialized()) {
            ((InstancePersistenceHandler) cacheManager.getPersistenceHandler()).destroy();
        }
    }

    @Override
    public void reset(Context context, boolean simulateUninstall) {
        try {
            PersistenceHandler sp = cacheManager.getPersistenceHandler();

            if (sp == null) {
                InstanceContext javaAirlockProductContext = (InstanceContext) context;
                sp = new InstancePersistenceHandler(javaAirlockProductContext.getProductName(), context);
                cacheManager.setPersistenceHandler(sp);
            }

            if (sp != null) {
                if (simulateUninstall) {
                    sp.reset(context);
                } else {
                    sp.clearInMemory();
                }
            }
        } catch (Exception e) {
            Logger.log.d(TAG, AirlockMessages.ERROR_SP_NOT_INIT_CANT_CLEAR);
            // continue, this is because the SP is not init
        }
        cacheManager.resetFeatureLists();
        if (streamsManager != null) {
            streamsManager.clearStreams();
        }
        init = false;
    }


    @Override
    public void updateProductContext(String context, boolean clearPreviousContext) {
        updateContext(context, clearPreviousContext);
    }


    @Override
    public void updateProductContext(String context) {
        updateContext(context, false);
    }

    @Override
    public void removeProductContextField(String fieldPath) {
        removeContextField(fieldPath);
    }

    @Override
    public void setSharedContext(StateFullContext stateFullContext) {
        if (this.cacheManager != null) {
            this.cacheManager.setAirlockSharedContext(stateFullContext);
        }
    }

    public Locale getLocale() {
        return super.getLocale();
    }

    public String getProductName() {
        return instanceContext.getProductName();
    }

    @Nullable
    public JSONObject getLastCalculatedUserProfile() {
        return lastCalculatedUserProfile;
    }

    @Nullable
    public JSONObject getLastCalculatedAirlockContext() {
        return lastCalculatedAirlockContext;
    }
}
