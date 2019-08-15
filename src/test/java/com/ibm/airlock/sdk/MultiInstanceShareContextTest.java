package com.ibm.airlock.sdk;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.sdk.cache.InstanceContext;
import com.ibm.airlock.sdk.cache.pref.FilePreferencesFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
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
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@RunWith(Parameterized.class)
public class MultiInstanceShareContextTest {

    private String productName;
    private String instanceId;
    private String appVersion = "8.16";
    private static AirlockMultiProductsManager airlockMultiProductsManager;
    private static Object[][] products = new Object[][]{
            {"SharedContext", "cba6de52-808d-4f03-81cb-e78df5cb9791"},
//            {"SharedContext", "cba6de52-808d-4f03-81cb-e78df5cb9792"},
//            {"SharedContext", "cba6de52-808d-4f03-81cb-e78df5cb9793"},
//            {"SharedContext", "cba6de52-808d-4f03-81cb-e78df5cb9794"},
//            {"SharedContext2", "cba6de52-808d-4f03-81cb-e78df5cb9795"},
//            {"SharedContext2", "cba6de52-808d -4f03-81cb-e78df5cb9796"}
    };


    public MultiInstanceShareContextTest(String productName, String instanceId) {
        this.productName = productName;
        this.instanceId = instanceId;
        airlockMultiProductsManager = new AirlockMultiProductsManager();
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
        return initAndPull(this.productName, "Product" + "_default.json");
    }

    private AirlockProductManager initAndPull(String productName, String defualt_file) {


        File file = new File(MultiInstanceShareContextTest.class.getResource(defualt_file).getFile());
        String defaultsFile = readFile(file);
        InstanceContext context = new InstanceContext(instanceId, FilePreferencesFactory.getAirlockCacheDirectory(), getSeasonId(defaultsFile), productName, appVersion);
        Assert.assertNotNull(airlockMultiProductsManager.createProduct(context));

        final AirlockProductManager airlockProductManager = airlockMultiProductsManager.getAirlockProductManager(this.instanceId);
        Assert.assertNotNull(airlockProductManager);

        try {
            airlockProductManager.reset(context, true);
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

    @CheckForNull
    public String getSeasonId(@Nullable String defaultFile) throws JSONException {
        if (defaultFile == null) {
            return null;
        }
        JSONObject defaultFileJson = new JSONObject(defaultFile);
        return defaultFileJson.optString("seasonId");
    }


    @Test
    public void addSharedContextField() {

        String context = readFile(new File(MultiInstanceShareContextTest.class.getResource("Product" + "_context.json").getFile()));
        AirlockProductManager airlockProductManager = initAndPull();

        calcSync(airlockProductManager, new JSONObject(context));

        Feature feature = airlockProductManager.getFeature("modules.Airlock Control Over Modules");
        Assert.assertEquals(feature.isOn(), true);
        Assert.assertEquals(feature.getTraceInfo(), "Configurations: [defaultConfiguration]");

        JSONObject modifiedContext = new JSONObject(context);
        ((JSONObject) modifiedContext.get("device")).remove("locale");


        airlockMultiProductsManager.updateSharedContext("{\"device\": {\"locale\": \"en_DE\"}}");
        calcSync(airlockProductManager, modifiedContext);
        feature = airlockProductManager.getFeature("performance.Limit");
        Assert.assertEquals(feature.isOn(), false);
        Assert.assertEquals(feature.getTraceInfo(), "Rule returned false");


        airlockProductManager.updateProductContext("{\"device\": {\"locale\": \"en_US\"}}");
        calcSync(airlockProductManager, modifiedContext);

        feature = airlockProductManager.getFeature("performance.Limit");
        Assert.assertEquals(feature.isOn(), true);
        Assert.assertEquals(feature.getTraceInfo(), "Configurations: [defaultConfiguration]");
    }

    @Test
    public void doubleUpdateCurrentContextShouldNotChangeIt() {


        String context = readFile(new File(MultiInstanceShareContextTest.class.getResource("Product" + "_context.json").getFile()));
        AirlockProductManager airlockProductManager = initAndPull();


        airlockMultiProductsManager.updateSharedContext("{\"context\":[\"value1\",\"value2\"]}");
        airlockMultiProductsManager.updateSharedContext("{\"context\":[\"value1\",\"value2\"]}");

        Assert.assertEquals((new JSONObject("{}").toString()),
                airlockProductManager.getCacheManager().getAirlockContextManager().getCurrentContext().toString());

        Assert.assertEquals((new JSONObject("{}").toString()),
                airlockProductManager.getCacheManager().getAirlockContextManager().getRuntimeContext().toString());

        calcSync(airlockProductManager, new JSONObject(context));

        Assert.assertEquals((new JSONObject("{}").toString()),
                airlockProductManager.getCacheManager().getAirlockContextManager().getCurrentContext().toString());

        Assert.assertEquals((new JSONObject("{\"context\":[\"value1\",\"value2\"]}").toString()),
                airlockProductManager.getCacheManager().getAirlockContextManager().getRuntimeContext().toString());

        Assert.assertEquals((new JSONObject("{\"context\":[\"value1\",\"value2\"]}").toString()),
                airlockMultiProductsManager.getMultiProductsSharedScope().toString().toString());




        airlockMultiProductsManager.updateSharedContext("{\"context\":[\"value3\",\"value4\"]}");
        airlockMultiProductsManager.updateSharedContext("{\"context\":[\"value3\",\"value4\"]}");

        calcSync(airlockProductManager, new JSONObject(context));

        Assert.assertEquals((new JSONObject("{\"context\":[\"value1\",\"value2\",\"value3\",\"value4\"]}").toString()),
                airlockMultiProductsManager.getMultiProductsSharedScope().toString().toString());

    }


    @Test
    public void removeSharedContextField() {
        String context = readFile(new File(MultiInstanceShareContextTest.class.getResource("Product" + "_context.json").getFile()));
        AirlockProductManager airlockProductManager = initAndPull();


        airlockMultiProductsManager.updateSharedContext("{\"device\": {\"locale\": \"en_US\"}}");
        JSONObject modifiedContext = new JSONObject(context);
        ((JSONObject) modifiedContext.get("device")).remove("locale");

        calcSync(airlockProductManager, modifiedContext);

        Feature feature = airlockProductManager.getFeature("performance.Limit");
        Assert.assertEquals(feature.isOn(), true);
        Assert.assertEquals(feature.getTraceInfo(), "Configurations: [defaultConfiguration]");


        airlockMultiProductsManager.removeSharedContextField("device.locale");
        calcSync(airlockProductManager, modifiedContext);
        feature = airlockProductManager.getFeature("performance.Limit");
        Assert.assertEquals(feature.isOn(), false);
        Assert.assertEquals(feature.getTraceInfo(), "Rule returned false");
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