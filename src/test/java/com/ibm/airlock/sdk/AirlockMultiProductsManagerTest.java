package com.ibm.airlock.sdk;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.engine.AirlockEnginePerformanceMetric;
import com.ibm.airlock.sdk.cache.InstanceContext;
import com.ibm.airlock.sdk.cache.pref.FilePreferencesFactory;
import com.ibm.airlock.sdk.utils.DefaultsUtils;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;

@RunWith(Parameterized.class)
public class AirlockMultiProductsManagerTest {

    private static long totalAvarageTime;
    private String productName;
    private String instanceId;
    private String appVersion = "8.16";
    private static AirlockMultiProductsManager spiedAirlockMultiProductsManager;
    private static Object[][] products = new Object[][]{
            {"Product1", "cba6de52-808d-4f03-81cb-e78df5cb9791"},
            {"Product2", "cba6de52-808d-4f03-81cb-e78df5cb9792"},
            {"Product3", "cba6de52-808d-4f03-81cb-e78df5cb9793"}
    };


    public AirlockMultiProductsManagerTest(String productName, String instanceId) {
        this.productName = productName;
        this.instanceId = instanceId;
        spiedAirlockMultiProductsManager = Mockito.spy(new AirlockMultiProductsManager());

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String defaultFile = (String) args[0];

                if (defaultFile == null || defaultFile.isEmpty()) {
                    return null;
                }

                JSONObject defaultFileJson = new JSONObject(defaultFile);
                String productName = defaultFileJson.optString("productName");

                for (Object[] product : products) {
                    if (product[0].equals(productName)) {
                        return (String) product[1];
                    }
                }
                return null;
            }
        }).when(spiedAirlockMultiProductsManager).getProductId(anyString());
    }


    @Parameterized.Parameters(name = "{index}:{0}")
    public static Collection primeNumbers() {
        return Arrays.asList(products);
    }

    private long mutipleCalculationWithThreadNumber(int numberOfConcurrentProducts) {
        AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().startMeasuring();
        LinkedList<JSONObject> contexts = new LinkedList();
        LinkedList<AirlockProductManager> products = new LinkedList();
        for (int i = 0; i < numberOfConcurrentProducts; i++) {
            contexts.add(new JSONObject(readFile(new File(AirlockMultiProductsManagerTest.class.
                    getResource("Product_context.json").getFile()))));
        }

        for (int i = 0; i < numberOfConcurrentProducts; i++) {
            products.add(initAndPull("ProductMetaData" + (i + 1), "Product_default.json"));
        }

        for (int i = 0; i < products.size(); i++) {
            final JSONObject context = contexts.get(i);
            calcNoTimer(products.get(i), context);
        }

        long start = System.currentTimeMillis();
        CountDownLatch doneAll = new CountDownLatch(numberOfConcurrentProducts);


        for (int i = 0; i < products.size(); i++) {
            final JSONObject context = contexts.get(i);
            final AirlockProductManager product = products.get(i);
            new Thread() {
                public void run() {
                    calc(product, context);
                    doneAll.countDown();
                }
            }.start();
        }
        long totalTime = 0;
        try {
            if (!doneAll.await(10, TimeUnit.SECONDS)) {
                Assert.fail("time-out happened");
            } else {
                totalTime = (System.currentTimeMillis() - start);
                System.out.println("Total time of [" + numberOfConcurrentProducts + "] :" + (System.currentTimeMillis() - start));
                System.out.println("Total time per thread [" + numberOfConcurrentProducts + "] :" + (System.currentTimeMillis() - start) / numberOfConcurrentProducts);
                System.out.println("Average time of per thread [" + numberOfConcurrentProducts + "] :" + totalAvarageTime / numberOfConcurrentProducts);
            }
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }

        System.out.println("Reports : " + AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().getReport().toString());
        AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().stopMeasuring();
        return totalTime;
    }


    @Test
    @Ignore
    public void mutipleCalculation() {
        JSONObject average = new JSONObject();
        int trails = 1;
        for (int j = 0; j < trails; j++) {
            JSONObject result = new JSONObject();
            for (int i = 1; i <= 3; i++) {
                result.put(i + "", mutipleCalculationWithThreadNumber(i));
                totalAvarageTime = 0;
            }
            average.put(j + "", result);
        }
        Hashtable<Integer, Integer> averageResults = new Hashtable();
        for (int i = 1; i <= 3; i++) {
            int sum = 0;
            for (int j = 0; j < trails; j++) {
                sum += average.getJSONObject(j + "").getInt(i + "");
            }
            averageResults.put(i, sum / trails);
        }

        System.out.println("mutipleCalculation result:" + averageResults.toString());
    }

    private void calcNoTimer(AirlockProductManager airlockProductManager, JSONObject context) {
        try {
            airlockProductManager.calculateFeatures(null, context);
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }
    }

    private void calc(AirlockProductManager airlockProductManager, JSONObject context) {
        try {
            long start = System.currentTimeMillis();
            airlockProductManager.calculateFeatures(null, context);
            //System.out.println("Calculation time [" + productName + "]: " + (System.currentTimeMillis() - start));
            totalAvarageTime = totalAvarageTime + (System.currentTimeMillis() - start);
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }
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

    private AirlockProductManager initAndPull(String productName, String defualt_file) {


        File file = new File(AirlockMultiInstancesProductContextTest.class.getResource(defualt_file).getFile());
        String defaultsFile = readFile(file);

        InstanceContext context = new InstanceContext(instanceId, FilePreferencesFactory.getAirlockCacheDirectory(), defaultsFile, appVersion);
        Assert.assertNotNull(spiedAirlockMultiProductsManager.createProduct(context));
        String seasonId = DefaultsUtils.getSeasonId(defaultsFile);
        String productId = DefaultsUtils.getProductId(defaultsFile);

        final AirlockProductManager airlockProductManager = spiedAirlockMultiProductsManager.getAirlockProductManager(
                new AbstractMultiProductManager.ProductMetaData(this.instanceId, seasonId, productId, productName, appVersion));
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


    @Test
    public synchronized void restoreSharedContext() {
        String context = readFile(new File(AirlockMultiProductsManagerTest.class.getResource(this.productName + "_context.json").getFile()));

        spiedAirlockMultiProductsManager.reset();
        spiedAirlockMultiProductsManager.restoreProductsState();
        AirlockProductManager airlockProductManager = initAndPull();
        Assert.assertNotNull(spiedAirlockMultiProductsManager.getAirlockProductManager(this.instanceId));

        calcSync(airlockProductManager, new JSONObject(context));

        Feature feature = airlockProductManager.getFeature("modules.Airlock Control Over Modules");
        Assert.assertEquals(feature.isOn(), true);
        Assert.assertEquals(feature.getTraceInfo(), "Configurations: [defaultConfiguration]");

        JSONObject modifiedContext = new JSONObject(context);
        ((JSONObject) modifiedContext.get("device")).remove("locale");
        spiedAirlockMultiProductsManager.updateSharedContext("{\"device\": {\"locale\": \"en_DE\"}}");


        spiedAirlockMultiProductsManager.runtimeReset();
        spiedAirlockMultiProductsManager.restoreProductsState();
        Assert.assertEquals("{\"device\":{\"locale\":\"en_DE\"}}", spiedAirlockMultiProductsManager.getMultiProductsSharedScope().toString().toString());


    }

    @Test
    public void getAirlockDebugger() {
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