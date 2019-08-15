package com.ibm.airlock.sdk;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.sdk.cache.InstanceContext;
import com.ibm.airlock.sdk.cache.pref.FilePreferencesFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AirlockMultiInstancesCacheTest {

    private String appVersion = "8.16";
    private static AirlockMultiProductsManager airlockMultiProductsManager;
    private static String[][] products = new String[][]{
            {"Product2", "cba6de52-808d-4f03-81cb-e78df5cb9791"},
            {"Product2", "cba6de52-808d-4f03-81cb-e78df5cb9792"}
    };

    @BeforeClass
    public static void deleteAllInstances() {
        Collection<AbstractMultiProductManager.ProductMetaData> products = AirlockMultiProductsManager.getInstance().getAllProducts();
        for (AbstractMultiProductManager.ProductMetaData product : products) {
            airlockMultiProductsManager.getAirlockProductManager(product.getInstanceId()).destroy();
        }
        AirlockMultiProductsManager.getInstance().reset();
    }


    public AirlockMultiInstancesCacheTest() {
        airlockMultiProductsManager = AirlockMultiProductsManager.getInstance();
    }

    private AirlockProductManager initAndPull(String instanceId, String productName) {
        return initAndPull(instanceId, productName, productName + "_default.json");
    }


    protected AirlockProductManager intProduct(String instanceId, String productName, String defualt_file) {
        File file = new File(AirlockMultiInstancesCacheTest.class.getResource(defualt_file).getFile());
        String defaultsFile = readFile(file);
        InstanceContext context = new InstanceContext(instanceId, FilePreferencesFactory.getAirlockCacheDirectory(), getSeasonId(defaultsFile), productName, appVersion);
        final AirlockProductManager airlockProductManager = airlockMultiProductsManager.createProduct(context);
        Assert.assertNotNull(airlockProductManager);

        try {
            airlockProductManager.initSDK(context, readFile(file), appVersion);
        } catch (AirlockInvalidFileException | IOException e) {
            Assert.fail(e.getMessage());
        }

        return airlockProductManager;

    }

    private AirlockProductManager initAndPull(String instanceId, String productName, String defualts_file) {
        AirlockProductManager airlockProductManager = intProduct(instanceId, productName, defualts_file);
        CountDownLatch done = new CountDownLatch(1);
        try {
            airlockProductManager.pullFeatures(new AirlockCallback() {
                @Override
                public void onFailure(Exception e) {
                    Assert.fail(e.getMessage());
                    done.countDown();
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

    @CheckForNull
    public String getSeasonId(@Nullable String defaultFile) throws JSONException {
        if (defaultFile == null) {
            return null;
        }
        JSONObject defaultFileJson = new JSONObject(defaultFile);
        return defaultFileJson.optString("seasonId");
    }


    @Test
    public synchronized void crossInstanceLastCalcCacheTest() throws InterruptedException {

        String airlockContext = readFile(new File(AirlockMultiInstancesCacheTest.class.getResource("Product" + "_context.json").getFile()));

        AirlockProductManager airlockProductManager = initAndPull(products[0][1], products[0][0]);
        Assert.assertNotEquals(0, airlockProductManager.getLastPullTime().getTime());

        try {
            airlockProductManager.calculateFeatures(null, new JSONObject(airlockContext), null);
            airlockProductManager.syncFeatures();
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }


        // wait for cache be updated, happens in background thread
        Thread.sleep(1000);

        long lastCalcTime = airlockProductManager.getLastCalculateTime().getTime();
        Assert.assertNotEquals(0, lastCalcTime);
        airlockProductManager = intProduct(products[1][1], products[1][0], products[1][0] + "_default.json");
        Assert.assertEquals(0, airlockProductManager.getLastCalculateTime().getTime());
        Assert.assertNotEquals(0, airlockProductManager.getLastPullTime().getTime());
    }

    @Test
    public synchronized void crossInstanceLastPullCacheTest() {

        String airlockContext = readFile(new File(AirlockMultiInstancesCacheTest.class.getResource("Product" + "_context.json").getFile()));

        JSONObject airlockContextJSON = new JSONObject(airlockContext);
        JSONObject device = airlockContextJSON.getJSONObject("device");
        device.remove("localeCountryCode");
        airlockContextJSON.put("device", device);

        AirlockProductManager airlockProductManager = initAndPull(products[0][1], products[0][0]);

        try {
            airlockProductManager.calculateFeatures(null, airlockContextJSON, null);
            airlockProductManager.syncFeatures();
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }


        long lastPullTime = airlockProductManager.getLastPullTime().getTime();
        Assert.assertNotEquals(0, lastPullTime);
        airlockProductManager = intProduct(products[1][1], products[1][0], products[1][0] + "_default.json");
        Assert.assertEquals(lastPullTime, airlockProductManager.getLastPullTime().getTime());
    }


    @Test
    public synchronized void newInstanceInheritsCurrentSeasonLocaleTest() {

        String airlockContext = readFile(new File(AirlockMultiInstancesCacheTest.class.getResource("Product" + "_context.json").getFile()));

        Locale originalLocale = Locale.getDefault();


        JSONObject airlockContextJSON = new JSONObject(airlockContext);
        JSONObject device = airlockContextJSON.getJSONObject("device");
        device.remove("localeCountryCode");
        airlockContextJSON.put("device", device);

        AirlockProductManager airlockProductManager = initAndPull(products[0][1], products[0][0]);

        try {
            airlockProductManager.calculateFeatures(null, airlockContextJSON, null);
            airlockProductManager.syncFeatures();
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }


        long lastPullTime = airlockProductManager.getLastPullTime().getTime();
        Assert.assertNotEquals(0, lastPullTime);
        airlockProductManager = intProduct(products[1][1], products[1][0], products[1][0] + "_default.json");
        Assert.assertEquals(lastPullTime, airlockProductManager.getLastPullTime().getTime());
        Assert.assertEquals(Locale.getDefault().toString().toLowerCase(), airlockProductManager.getLocale().toString().toLowerCase());

        Locale.setDefault(originalLocale);
    }


//    @AfterClass
//    public static void destroyAllProducts() {
//        for (String[] product : products) {
//            airlockMultiProductsManager.getAirlockProductManager(product[1]).destroy();
//        }
//        //Assert.assertEquals(0, new File(FilePreferencesFactory.getAirlockCacheDirectory() + File.separator + "Product2").listFiles().length);
//    }

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