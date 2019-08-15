package com.ibm.airlock.sdk;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.sdk.cache.InstanceContext;
import com.ibm.airlock.sdk.cache.pref.FilePreferencesFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(Parallelized.class)
public class AirlockMultiInstancesProductContextTest {

    private String productName;
    private String instanceId;
    private String appVersion = "8.16";
    private static AirlockMultiProductsManager airlockMultiProductsManager;
    private static String[][] products = new String[][]{
            {"Product1", "cba6de52-808d-4f03-81cb-e78df5cb97911"},
            {"Product1", "cba6de52-808d-4f03-81cb-e78df5cb97922"},
            {"Product1", "cba6de52-808d-4f03-81cb-e78df5cb97933"},
            {"Product1", "cba6de52-808d-4f03-81cb-e78df5cb97944"},
            {"Product3", "cba6de52-808d-4f03-81cb-e78df5cb97955"},
            {"Product2", "cba6de52-808d-4f03-81cb-e78df5cb97966"}
    };


    public AirlockMultiInstancesProductContextTest(String productName, String instanceId) {
        this.productName = productName;
        this.instanceId = instanceId;
        airlockMultiProductsManager = AirlockMultiProductsManager.getInstance();
    }


    @Parameterized.Parameters(name = "{index}:{0}")
    public static Collection primeNumbers() {
        return Arrays.asList(products);
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
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        return initAndPull(this.productName, productName + "_default.json");
    }

    private AirlockProductManager initAndPull(String productName, String default_file) {


        File file = new File(AirlockMultiInstancesProductContextTest.class.getResource(default_file).getFile());
        String defaultsFile = readFile(file);
        InstanceContext context = new InstanceContext(instanceId, FilePreferencesFactory.getAirlockCacheDirectory(), getSeasonId(defaultsFile), productName, appVersion);
        Assert.assertNotNull(airlockMultiProductsManager.createProduct(context));

        final AirlockProductManager airlockProductManager = airlockMultiProductsManager.getAirlockProductManager(this.instanceId);
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
    public synchronized void addInstanceContextField() {

        String airlockContext = readFile(new File(AirlockMultiInstancesProductContextTest.class.getResource(this.productName + "_context.json").getFile()));

        JSONObject airlockContextJSON = new JSONObject(airlockContext);
        JSONObject device = airlockContextJSON.getJSONObject("device");
        device.remove("localeCountryCode");
        airlockContextJSON.put("device", device);

        AirlockProductManager airlockProductManager = initAndPull();
        airlockProductManager.updateProductContext("{\"device\": {\"locale\": \"en_US\", \"localeCountryCode\": \"DE\" }}");


        try {
            airlockProductManager.calculateFeatures(null, airlockContextJSON);
            airlockProductManager.syncFeatures();
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }


        Assert.assertEquals(airlockProductManager.
                getFeature("modules.Airlock Control Over Modules").isOn(), false);

        airlockProductManager.updateProductContext("{\"device\": {\"locale\": \"en_US\", \"localeCountryCode\": \"US\" }}");


        try {
            airlockProductManager.calculateFeatures(null, new JSONObject(airlockContext));
            airlockProductManager.syncFeatures();
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }


        Assert.assertEquals(airlockProductManager.
                getFeature("modules.Airlock Control Over Modules").isOn(), true);


        JSONObject contextWithSharedValue = new JSONObject(airlockContext);
        ((JSONObject) contextWithSharedValue.get("device")).remove("localeCountryCode");

        try {
            airlockProductManager.calculateFeatures(null, contextWithSharedValue);
            airlockProductManager.syncFeatures();
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertEquals(airlockProductManager.
                getFeature("modules.Airlock Control Over Modules").isOn(), true);

        File file = new File(AirlockMultiInstancesProductContextTest.class.getResource(productName + "_default.json").getFile());
        InstanceContext context = new InstanceContext(this.instanceId, FilePreferencesFactory.getAirlockCacheDirectory(), getSeasonId(readFile(file)), productName, appVersion);

        try {
            airlockProductManager.reset(context, false);
            airlockProductManager.initSDK(context, readFile(file), appVersion);
        } catch (AirlockInvalidFileException | IOException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertNotNull(airlockProductManager.getCacheManager().
                getAirlockContextManager().getCurrentContext().toString());

        Assert.assertEquals(airlockProductManager.getCacheManager().
                getAirlockContextManager().getCurrentContext().toString(), (new JSONObject("{\"device\": {\"locale\": \"en_US\", \"localeCountryCode\": \"US\" }}")).toString());

    }

    @Test
    public synchronized void removeInstanceContextField() {
        String context = readFile(new File(AirlockMultiInstancesProductContextTest.class.getResource(this.productName + "_context.json").getFile()));

        AirlockProductManager airlockProductManager = initAndPull();
        airlockProductManager.updateProductContext("{\"device\": {\"locale\": \"en_US\", \"localeCountryCode\": \"DE\" }}");

        JSONObject modifiedContext = new JSONObject(context);
        ((JSONObject) modifiedContext.get("device")).remove("localeCountryCode");


        calcSync(airlockProductManager, modifiedContext);

        Assert.assertEquals(airlockProductManager.
                getFeature("modules.Airlock Control Over Modules").isOn(), false);

        airlockProductManager.updateProductContext("{\"device\": {\"locale\": \"en_US\", \"localeCountryCode\": \"US\" }}");

        calcSync(airlockProductManager, modifiedContext);

        Assert.assertEquals(airlockProductManager.
                getFeature("modules.Airlock Control Over Modules").isOn(), true);

        airlockProductManager.removeProductContextField("device.localeCountryCode");

        calcSync(airlockProductManager, modifiedContext);

        Assert.assertEquals(airlockProductManager.
                getFeature("modules.Airlock Control Over Modules").isOn(), false);

    }


    @AfterClass
    public static void destroyAllProducts() {
        Collection<AbstractMultiProductManager.ProductMetaData> products = AirlockMultiProductsManager.getInstance().getAllProducts();
        for (AbstractMultiProductManager.ProductMetaData product : products) {
            airlockMultiProductsManager.getAirlockProductManager(product.getInstanceId()).destroy();
        }
        //Assert.assertEquals(0, new File(FilePreferencesFactory.getAirlockCacheDirectory() + File.separator + "Product1").listFiles().length);

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