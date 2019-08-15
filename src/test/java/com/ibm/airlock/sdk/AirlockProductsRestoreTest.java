package com.ibm.airlock.sdk;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.sdk.cache.InstanceContext;
import com.ibm.airlock.sdk.cache.pref.FilePreferences;
import com.ibm.airlock.sdk.cache.pref.FilePreferencesFactory;
import com.ibm.airlock.sdk.config.ConfigurationManager;
import com.ibm.airlock.sdk.utils.DefaultsUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(Parallelized.class)
public class AirlockProductsRestoreTest {

    private String productName;
    private String appVersion = "8.16";
    private String instanceId;


    public AirlockProductsRestoreTest(String productName, String instanceId) {
        this.productName = productName;
        this.instanceId = instanceId;
    }

    @Parameterized.Parameters(name = "{index}:{0}")
    public static Collection primeNumbers() {
        return Arrays.asList(new Object[][]{
                {"Product1", "cba6de52-808d-4f03-81cb-e78df5cb9744"},
        });
    }


    private void calcSync(AirlockProductManager airlockProductManager, JSONObject context) {
        try {
            airlockProductManager.calculateFeatures(null, context);
            airlockProductManager.syncFeatures();
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }
    }

    private AirlockProductManager initAndPull() {
        return initAndPull(this.productName, productName + "_default.json");
    }

    private AirlockProductManager initAndPull(String productName, String defualt_file) {

        File file = new File(AirlockProductsRestoreTest.class.getResource(defualt_file).getFile());
        String defaultsFile = readFile(file);

        InstanceContext context = new InstanceContext(instanceId, FilePreferencesFactory.getAirlockCacheDirectory(), defaultsFile, appVersion);

        final AirlockProductInstanceManager airlockProduct = AirlockMultiProductsManager.getInstance().createProduct(context);
        Assert.assertNotNull(airlockProduct);

        final AirlockProductManager airlockProductManager = AirlockMultiProductsManager.getInstance().getAirlockProductManager(this.instanceId);
        Assert.assertNotNull(airlockProductManager);

        try {
            //airlockProductManager.reset(context, true);
            airlockProductManager.initSDK(context, readFile(file), appVersion);
        } catch (AirlockInvalidFileException | IOException e) {
            Assert.fail(e.getMessage());
        }

        CountDownLatch done = new CountDownLatch(1);
        try {
            airlockProductManager.pullFeatures(new AirlockCallback() {
                @Override
                public void onFailure(Exception e) {
                    done.countDown();
                    Assert.fail(e.getMessage());
                }

                @Override
                public void onSuccess(String msg) {
                    done.countDown();
                }
            });
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            if (!done.await(10000, TimeUnit.SECONDS)) {
                //Assert.fail("Timeout reached");
            }
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        return airlockProductManager;
    }


    @Test
    public synchronized void restoreSharedContext() {
        String context = readFile(new File(AirlockProductsRestoreTest.class.getResource(this.productName + "_context.json").getFile()));

        AirlockMultiProductsManager.getInstance().reset();
        AirlockMultiProductsManager.getInstance().restoreProductsState();
        AirlockProductManager airlockProductManager = initAndPull();
        Assert.assertNotNull(AirlockMultiProductsManager.getInstance().getAirlockProductManager(this.instanceId));

        calcSync(airlockProductManager, new JSONObject(context));

        Feature feature = airlockProductManager.getFeature("modules.Airlock Control Over Modules");
        Assert.assertEquals(feature.isOn(), true);
        Assert.assertEquals(feature.getTraceInfo(), "Configurations: [defaultConfiguration]");

        JSONObject modifiedContext = new JSONObject(context);
        ((JSONObject) modifiedContext.get("device")).remove("locale");
        AirlockMultiProductsManager.getInstance().updateSharedContext("{\"device\": {\"locale\": \"en_DE\"}}");

        AirlockMultiProductsManager.getInstance().runtimeReset();
        AirlockMultiProductsManager.getInstance().restoreProductsState();
        Assert.assertEquals(
                "{\"device\":{\"locale\":\"en_DE\"}}", AirlockMultiProductsManager.getInstance().getMultiProductsSharedScope().toString().toString());


    }



    @Test
    public synchronized void restoreAirlockFromSSD() {

        try {
            injectEnvironmentVariable(ConfigurationManager.CACHE_VOLUME, "cache");
            injectEnvironmentVariable(ConfigurationManager.SSD_CACHE_VOLUME, "SSD");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        AirlockMultiProductsManager.getInstance().restoreProductsState();
        AirlockProductInstanceManager airlockProductInstanceManager = AirlockMultiProductsManager.getInstance().getAirlockProductManager(this.instanceId);
        Assert.assertNotNull(airlockProductInstanceManager);
        Assert.assertEquals(airlockProductInstanceManager.getFeature("video.Video Categories").isOn(), true);

        try {
            injectEnvironmentVariable(ConfigurationManager.CACHE_VOLUME, "");
            injectEnvironmentVariable(ConfigurationManager.SSD_CACHE_VOLUME, "");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }


    @Test
    public synchronized void restoreAirlockProducts() {
        AirlockMultiProductsManager.getInstance().reset();
        AirlockMultiProductsManager.getInstance().restoreProductsState();
        String context = readFile(new File(AirlockProductsRestoreTest.class.getResource(this.productName + "_context.json").getFile()));
        AirlockProductManager airlockProductManager = initAndPull();
        Assert.assertNotNull(AirlockMultiProductsManager.getInstance().getAirlockProductManager(this.instanceId));

        calcSync(airlockProductManager, new JSONObject(context));

        Assert.assertEquals(airlockProductManager.getFeature("video.Video Categories").isOn(), true);
        AirlockMultiProductsManager.getInstance().runtimeReset();
        AirlockMultiProductsManager.getInstance().restoreProductsState();

        airlockProductManager = AirlockMultiProductsManager.getInstance().getAirlockProductManager(this.instanceId);
        Assert.assertNotNull(airlockProductManager);
        Assert.assertEquals(airlockProductManager.getFeature("video.Video Categories").isOn(), true);
    }

    @Test
    public void copyCacheToSSD() {

        try {
            injectEnvironmentVariable(ConfigurationManager.CACHE_VOLUME, "cache");
            injectEnvironmentVariable(ConfigurationManager.SSD_CACHE_VOLUME, "SSD");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            doCopyCacheToSSD();
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        restoreAirlockProducts();

        try {
            injectEnvironmentVariable(ConfigurationManager.CACHE_VOLUME, "");
            injectEnvironmentVariable(ConfigurationManager.SSD_CACHE_VOLUME, "");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }


    @Test
    public void copyCacheFromSSDWithEmptyConfiguration() {
        try {
            injectEnvironmentVariable(ConfigurationManager.CACHE_VOLUME, "");
            injectEnvironmentVariable(ConfigurationManager.SSD_CACHE_VOLUME, "");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }


        doCopyCacheFromSSD();
    }


    @Test
    public void copyCacheToSSDWithEmptyConfiguration() {
        try {
            injectEnvironmentVariable(ConfigurationManager.CACHE_VOLUME, "");
            injectEnvironmentVariable(ConfigurationManager.SSD_CACHE_VOLUME, "");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        try {
            doCopyCacheToSSD();
        } catch (IOException e) {
            Assert.assertTrue(true);
            return;
        }
        Assert.fail("has to be thrown IOException");
    }


    private void doCopyCacheFromSSD() {
        copyCacheToSSD();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        AirlockMultiProductsManager.getInstance().copyCacheFromSSD();
        String context = readFile(new File(AirlockProductsRestoreTest.class.getResource(this.productName + "_context.json").getFile()));
        AirlockProductManager airlockProductManager = initAndPull();
        Assert.assertNotNull(AirlockMultiProductsManager.getInstance().getAirlockProductManager(this.instanceId));

        calcSync(airlockProductManager, new JSONObject(context));

        Feature feature = airlockProductManager.getFeature("modules.Airlock Control Over Modules");
        Assert.assertEquals(feature.isOn(), true);
        Assert.assertEquals(feature.getTraceInfo(), "Configurations: [defaultConfiguration]");
    }


    private void doCopyCacheToSSD() throws IOException {
        String context = readFile(new File(AirlockProductsRestoreTest.class.getResource(this.productName + "_context.json").getFile()));
        AirlockProductManager airlockProductManager = initAndPull();
        Assert.assertNotNull(AirlockMultiProductsManager.getInstance().getAirlockProductManager(this.instanceId));

        calcSync(airlockProductManager, new JSONObject(context));

        String sddFilePath = null;
        if (ConfigurationManager.getSSDCacheVolume() != null) {
            sddFilePath = new File(ConfigurationManager.getSSDCacheVolume()).getAbsolutePath();
            new File(sddFilePath).mkdir();
        }


        AirlockMultiProductsManager.getInstance().copyCacheToSSD();


        Assert.assertTrue(new File(sddFilePath).listFiles().length > 0);
        Assert.assertTrue(new File(sddFilePath).listFiles()[0].getName().contains(ConfigurationManager.CACHE_ZIP_FILE_NAME));
    }

    @Test
    public void copyCacheFromSSD() {

        try {
            injectEnvironmentVariable(ConfigurationManager.CACHE_VOLUME, "cache");
            injectEnvironmentVariable(ConfigurationManager.SSD_CACHE_VOLUME, "SSD");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        doCopyCacheFromSSD();

        try {
            injectEnvironmentVariable(ConfigurationManager.CACHE_VOLUME, "");
            injectEnvironmentVariable(ConfigurationManager.SSD_CACHE_VOLUME, "");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }


    @Test
    public void restoreDamagedCachedProduct() {
        String context = readFile(new File(AirlockProductsRestoreTest.class.getResource(this.productName + "_context.json").getFile()));
        AirlockProductManager airlockProductManager = initAndPull();
        Assert.assertNotNull(AirlockMultiProductsManager.getInstance().getAirlockProductManager(this.instanceId));
        calcSync(airlockProductManager, new JSONObject(context));


        AirlockMultiProductsManager.getInstance().runtimeReset();

        // delete default file to simulate damaged product cache
        File default_file = new File(new File(FilePreferencesFactory.getAirlockCacheDirectory()).getAbsolutePath() + File.separator +
                this.productName + File.separator + "4620cc87-0253-4fb5-9027-f4cb7c69b1a2" + File.separator + "airlock.defaultFile");
        default_file.delete();

        // delete SSD
        File ssd = new File(new File(new File(FilePreferencesFactory.getAirlockCacheDirectory()).getAbsolutePath()).getParentFile().getAbsolutePath()
                + File.separator + "SSD");
        if (ssd.exists() && ssd.listFiles().length > 0) {
            for (File file : ssd.listFiles()) {
                file.delete();
            }
        }

        AirlockMultiProductsManager.getInstance().restoreProductsState();
        Assert.assertEquals(AirlockMultiProductsManager.getInstance().
                getAirlockProductManager(this.instanceId).isInitialized(), false);
        AirlockMultiProductsManager.getInstance().runtimeReset();
    }

    private static void injectEnvironmentVariable(String key, String value)
            throws Exception {

        Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");

        Field unmodifiableMapField = getAccessibleField(processEnvironment, "theUnmodifiableEnvironment");
        Object unmodifiableMap = unmodifiableMapField.get(null);
        injectIntoUnmodifiableMap(key, value, unmodifiableMap);

        Field mapField = getAccessibleField(processEnvironment, "theEnvironment");
        Map<String, String> map = (Map<String, String>) mapField.get(null);
        map.put(key, value);
    }

    private static Field getAccessibleField(Class<?> clazz, String fieldName)
            throws NoSuchFieldException {

        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    private static void injectIntoUnmodifiableMap(String key, String value, Object map)
            throws ReflectiveOperationException {

        Class unmodifiableMap = Class.forName("java.util.Collections$UnmodifiableMap");
        Field field = getAccessibleField(unmodifiableMap, "m");
        Object obj = field.get(map);
        ((Map<String, String>) obj).put(key, value);
    }


    private String readFile(File file) {

        StringBuilder fileContents = new StringBuilder((int) file.length());
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String lineSeparator = System.getProperty("line.separator");

        try {
            while (scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine() + lineSeparator);
            }
            return fileContents.toString();
        } finally {
            scanner.close();
        }
    }
}