package com.ibm.airlock.sdk;

import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.engine.StateFullContext;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.sdk.cache.InstanceContext;
import com.ibm.airlock.sdk.cache.PersistenceEncryptor;
import com.ibm.airlock.sdk.cache.pref.FilePreferencesFactory;
import com.ibm.airlock.sdk.config.ConfigurationManager;
import com.ibm.airlock.sdk.debug.AirlockDebugger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.MalformedParametersException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by Denis Voloshin on 04/09/2017.
 */
public class AirlockMultiProductsManager extends AbstractMultiProductManager {

    static {
        PersistenceEncryptor.enableEncryption(ConfigurationManager.isCacheShouldBeEncrypted());
    }


    private final String TAG = this.getClass().getSimpleName();

    private static final String MULTI_PRODUCTS_CONTEXT_NAME = "multi.products.context.name";
    private static final String PRODUCT_STATES_STORAGE = "products.states.pref";
    private static final String PRODUCT_IDENTIFICATION = "products.identifications";
    private static final String SHARED_CONTEXT = "shared.context";
    public static final String URLS_LAST_UPDATES_STORAGE = "urls.lastUpdates.pref";

    private ConcurrentHashMap<String, ProductMetaData> products;
    private ConcurrentHashMap<String, AirlockProductInstanceManager> airlockProductsMap;
    private static StateFullContext multiProductsSharedScope;
    private Preferences productsStateStorage;


    private static final class AirlockMultiProductsManagerLazyHolder {
        private static final AirlockMultiProductsManager INSTANCE = new AirlockMultiProductsManager();
    }


    /**
     * Return an AirlockMultiProductsManager instance.
     *
     * @return Returns an AirlockMultiProductsManager instance.
     */
    public static AirlockMultiProductsManager getInstance() {
        return AirlockMultiProductsManagerLazyHolder.INSTANCE;
    }


    AirlockMultiProductsManager() {
        this.airlockProductsMap = new ConcurrentHashMap();
        multiProductsSharedScope = new StateFullContext(MULTI_PRODUCTS_CONTEXT_NAME);
        this.products = new ConcurrentHashMap();
        initProductsStateStorage();
    }


    public void initProductsStateStorage() {
        this.productsStateStorage = Preferences.userRoot().node(FilePreferencesFactory.getAirlockCacheDirectory() +
                File.separator + PRODUCT_STATES_STORAGE);
    }

    public Collection<ProductMetaData> getAllProducts() {
        return new ArrayList(products.values());
    }

    public static StateFullContext getMultiProductsSharedScope() {
        return multiProductsSharedScope;
    }

    @Override
    public void restoreProductsState() {


        copyCacheFromSSD();
        // re-init stage after cache loading complete

        this.initProductsStateStorage();


        String productIdentificationsAsStr = null;
        try {
            productIdentificationsAsStr = this.productsStateStorage.get(PRODUCT_IDENTIFICATION, "[]");
        } catch (Exception e) {
            Logger.log.w(TAG, e.getMessage());
        }

        products = new ConcurrentHashMap();
        if (productIdentificationsAsStr != null) {
            JSONArray productsIdToNameMapAs = new JSONArray(productIdentificationsAsStr);

            //go through all products and restore their previous state.
            for (int i = 0; i < productsIdToNameMapAs.length(); i++) {
                ProductMetaData productIdentification = new ProductMetaData(productsIdToNameMapAs.getJSONObject(i));
                products.put(productIdentification.getInstanceId(), productIdentification);
                lazyProductRestore(productIdentification.getInstanceId(),
                        productIdentification.getSeasonId(),
                        productIdentification.getProductId(),
                        productIdentification.getProductName(),
                        productIdentification.getAppVersion());
            }
        }

        String sharedContext = null;
        try {
            sharedContext = this.productsStateStorage.get(SHARED_CONTEXT, "{}");
        } catch (Exception e) {
            Logger.log.w(TAG, e.getMessage());
        }

        if (sharedContext != null) {
            //restore shared context
            multiProductsSharedScope.update(new JSONObject(sharedContext));
        }
    }


    private void lazyProductRestore(String instanceId, String seasonId, String productId, String productName, String appVersion) {
        InstanceContext instanceContext = new InstanceContext(instanceId, FilePreferencesFactory.getAirlockCacheDirectory(), seasonId, productName, appVersion);
        AirlockProductInstanceManager productAirlockManager = new AirlockProductInstanceManager(instanceContext);
        airlockProductsMap.put(instanceId, productAirlockManager);
    }

    private void loadProductFromCache(AirlockProductInstanceManager productAirlockManager, ProductMetaData productMetaData) {
        try {
            productAirlockManager.initSDKFromCache(FilePreferencesFactory.getAirlockCacheDirectory(), productMetaData.getProductName(),
                    productMetaData.getSeasonId(), productMetaData.getAppVersion(), productMetaData.getInstanceId());
            if (productAirlockManager.isInitialized()) {
                productAirlockManager.setSharedContext(multiProductsSharedScope);
            }
        } catch (AirlockInvalidFileException | IOException e) {
            Logger.log.e(TAG, e.getMessage());
        }
    }


    public void copyCacheToSSD() throws IOException {

        if (ConfigurationManager.getSSDCacheVolume() == null) {
            String error = "SSD volume is not defined";
            Logger.log.w(TAG, error);
            throw new IOException(error);
        }

        try {
            compressCacheFolder(new File(ConfigurationManager.getSSDCacheVolume()).getAbsolutePath() + File.separator +
                    ConfigurationManager.CACHE_ZIP_FILE_NAME + "_" + (new Date(System.currentTimeMillis()).toString()).
                    replaceAll(" ", "-") + ".zip");
        } catch (IOException e) {
            Logger.log.e(TAG, e.getMessage());
            throw e;
        }
    }

    public void copyCacheFromSSD() {

        if (ConfigurationManager.getSSDCacheVolume() == null) {
            return;
        }

        try {
            Preferences.userRoot().node(FilePreferencesFactory.getAirlockCacheDirectory() +
                    File.separator + PRODUCT_STATES_STORAGE).removeNode();
        } catch (BackingStoreException e) {
            Logger.log.e(TAG, e.getMessage());
        }

        try {
            // clear current cache folder.
            extractCacheFolder(new File(FilePreferencesFactory.getAirlockCacheDirectory()).getAbsolutePath());
        } catch (IOException e) {
            Logger.log.e(TAG, e.getMessage());
        }
    }


    private void extractCacheFolder(String cacheFolder) throws IOException {

        File ssdZip = lookupForZipFile();

        if (ssdZip != null) {
            byte[] buffer = new byte[1024];
            ZipInputStream zis = new ZipInputStream(new FileInputStream(ssdZip));
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                String fileName = zipEntry.getName();
                File newFile = new File(cacheFolder + File.separator + fileName);
                newFile.getParentFile().mkdirs();
                if (!newFile.exists()) {
                    newFile.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } else {
            Logger.log.w(TAG, "No airlock cache found on SSD");
        }
    }

    @CheckForNull
    private File lookupForZipFile() {
        if (ConfigurationManager.getSSDCacheVolume() == null) {
            return null;
        }
        File[] files = new File(ConfigurationManager.getSSDCacheVolume()).listFiles();
        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (file.getName().endsWith(".zip")) {
                return file;
            }
        }
        return null;
    }


    private void compressCacheFolder(String zipFilePath) throws IOException {
        Files.createDirectories(Paths.get(zipFilePath).getParent());
        Path ssd = Paths.get(zipFilePath).getParent();
        Files.walk(ssd).forEach(path -> {
            try {
                Files.delete(path);
            } catch (IOException e) {
            }
        });

        // create SSD directory if deleted.
        Files.createDirectories(Paths.get(zipFilePath).getParent());

        Path zip_on_ssd = Paths.get(zipFilePath);

        // zip file to the temp archive
        Path temp_zip = Paths.get(
                new File(new File(FilePreferencesFactory.getAirlockCacheDirectory()).getAbsolutePath()).getParentFile().getAbsolutePath()
                        + File.separator + "airlock.zip");
        if (!Files.exists(temp_zip)) {
            Files.createFile(temp_zip);
        }

        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(temp_zip))) {
            Path pp = Paths.get(FilePreferencesFactory.getAirlockCacheDirectory());
            Files.walk(pp)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            Logger.log.e(TAG, e.getMessage());
                        }
                    });
            zs.close();
        }

        Files.copy(temp_zip, zip_on_ssd);
        Files.delete(temp_zip);
    }

    @Override
    protected void restoreProduct(String instanceId, String seasonId, String productId, String productName, String appVersion) {
        loadProductFromCache(airlockProductsMap.get(instanceId), products.get(instanceId));
    }

    @Override
    public void runtimeReset() {
        this.airlockProductsMap = new ConcurrentHashMap<>();
        multiProductsSharedScope = new StateFullContext(MULTI_PRODUCTS_CONTEXT_NAME);
        this.products = new ConcurrentHashMap();
        multiProductsSharedScope = new StateFullContext(MULTI_PRODUCTS_CONTEXT_NAME);
    }

    @Override
    public void reset() {
        runtimeReset();
        products.clear();
        updateProductsIdentifications();
    }


    /**
     * The method enables to add/update a context field.
     * A context field will be updated if such name already exists.
     *
     * @param instanceId         a airlock instance id the context field will be updated for
     * @param contextFieldAsJson a cross-products context field is provided as a string in JSON format
     */
    public void addProductContextField(String instanceId, String contextFieldAsJson) {
        if (getAirlockProductManager(products.get(instanceId)) != null) {
            if (getAirlockProductManager(products.get(instanceId)) != null) {
                getAirlockProductManager(products.get(instanceId)).updateProductContext(contextFieldAsJson);
            }
        }
    }


    /**
     * The method enables to remove a product context field.
     *
     * @param instanceId a airlock product instance id the context field will be removed from
     * @param fieldPath  a context field path provided in the json path syntax ex: key1.key2.key3
     */
    public void removeProductContextField(String instanceId, String fieldPath) {
        if (products.containsKey(instanceId)) {
            if (getAirlockProductManager(products.get(instanceId)) != null) {
                getAirlockProductManager(products.get(instanceId)).removeProductContextField(fieldPath);
            }
        }
    }

    /**
     * The method enables to add a new cross-products context field.
     * A context field will be updated if such name already exists.
     *
     * @param contextFieldAsJson a cross-products context field is provided as a string in JSON format
     * @param clearPreviousValue indicates whether the existing value will be overridden
     */
    public void updateSharedContext(String contextFieldAsJson, boolean clearPreviousValue) {
        multiProductsSharedScope.update(new JSONObject(contextFieldAsJson), clearPreviousValue);
        new Thread(() -> {
            try {
                cacheSharedContext();
            } catch (Exception e) {
                Logger.log.w(TAG, e.getMessage());
            }
        }).start();
    }

    /**
     * The method enables to add a new cross-products context field.
     * A context field will be updated if such name already exists.
     *
     * @param contextFieldAsJson a cross-products context field is provided as a string in JSON format
     */
    public void updateSharedContext(String contextFieldAsJson) {
        updateSharedContext(contextFieldAsJson, false);
    }


    /**
     * The method enables to remove a cross-products context field.
     *
     * @param fieldPath a cross-products context field path provided in the json path syntax ex: key1.key2.key3
     */
    public void removeSharedContextField(String fieldPath) {
        multiProductsSharedScope.removeContextField(fieldPath);
    }


    @Override
    public AirlockProductInstanceManager createProduct(InstanceContext instanceContext) throws JSONException, MalformedParametersException {
        return createProduct(instanceContext.getInstanceId(), instanceContext.getSeasonId(), instanceContext.getProductId(),
                instanceContext.getProductName(), instanceContext.getAppVersion());
    }


    @Override
    protected AirlockProductInstanceManager createProduct(String instanceId, String seasonId, String productId, String productName, String appVersion) throws JSONException, MalformedParametersException {
        if (productName == null) {
            throw new MalformedParametersException();
        }
        AirlockProductInstanceManager productAirlockManager;

        if (airlockProductsMap.containsKey(instanceId)) {
            productAirlockManager = getAirlockProductManager(instanceId);
        } else {
            InstanceContext instanceContext = new InstanceContext(instanceId, FilePreferencesFactory.getAirlockCacheDirectory(), seasonId, productName, appVersion);
            productAirlockManager = new AirlockProductInstanceManager(instanceContext);
            ProductMetaData product = new ProductMetaData(instanceId, seasonId, productId, productName, appVersion);
            products.put(instanceId, product);
            airlockProductsMap.put(instanceId, productAirlockManager);
            productAirlockManager.setSharedContext(multiProductsSharedScope);
            updateProductsIdentifications();
        }

        return productAirlockManager;
    }

    private void cacheSharedContext() {
        try {
            this.productsStateStorage.put(SHARED_CONTEXT, multiProductsSharedScope.toString());
            this.productsStateStorage.flush();
        } catch (Exception e) {
            Logger.log.e(TAG, e.getMessage());
        }
    }

    private void updateProductsIdentifications() {
        JSONArray jsonArray = new JSONArray();
        for (ProductMetaData productIdentification : this.products.values()) {
            jsonArray.put(productIdentification.toJSON());
        }
        try {
            this.productsStateStorage.put(PRODUCT_IDENTIFICATION, jsonArray.toString());
            this.productsStateStorage.flush();
        } catch (Exception e) {
            Logger.log.e(TAG, e.getMessage());
        }
    }


    @Override
    public void removeAirlockProductManager(String instanceId) {
        instanceId = instanceId.trim();
        if (airlockProductsMap.containsKey(instanceId)) {
            AirlockProductInstanceManager airlockProductInstanceManager = airlockProductsMap.remove(instanceId);
            if (airlockProductInstanceManager != null) {
                airlockProductInstanceManager.destroy();
            }
            removeProductIdentification(instanceId);
            updateProductsIdentifications();
        }
    }

    private void removeProductIdentification(String instanceId) {
        products.remove(instanceId);
    }


    @CheckForNull
    public AirlockProductInstanceManager getAirlockProductManager(ProductMetaData product) {
        String instanceId = product.getInstanceId();
        if (instanceId != null) {
            instanceId = instanceId.trim();
            if (airlockProductsMap.containsKey(instanceId)) {
                if (!airlockProductsMap.get(instanceId).isInitialized()) {
                    AirlockProductInstanceManager airlockProductInstanceManager = airlockProductsMap.get(instanceId);
                    loadProductFromCache(airlockProductInstanceManager, product);
                }
                return airlockProductsMap.get(instanceId);
            }
        }
        return null;
    }


    @CheckForNull
    public AirlockDebugger getAirlockDebugger(ProductMetaData product) {
        if (product != null && product.getInstanceId() != null) {
            String instanceId = product.getInstanceId();
            return new AirlockDebugger(getAirlockProductManager(instanceId.trim()));
        }
        return null;
    }

    @Override
    public AirlockProductInstanceManager getAirlockProductManager(String instanceId) {
        if (products.containsKey(instanceId)) {
            return getAirlockProductManager(products.get(instanceId));
        }
        return null;
    }

    @Override
    public AirlockDebugger getAirlockDebugger(String productId) {
        return getAirlockDebugger(new ProductMetaData(productId));
    }
}
