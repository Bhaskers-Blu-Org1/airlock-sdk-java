package com.ibm.airlock.sdk;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.engine.AirlockEnginePerformanceMetric;
import com.ibm.airlock.sdk.cache.InstanceContext;
import com.ibm.airlock.sdk.cache.pref.FilePreferencesFactory;
import com.ibm.airlock.sdk.utils.DefaultsUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class AirlockParallelCalculationsTest {

    private static long totalAvarageTime;

    private int numberOfCalculationsPerProduct = 10;
    private static int numberOfCalculationsSoFar = 0;
    private String appVersion = "8.16";
    LinkedList<AirlockProductManager> productsInstances = new LinkedList();
    private static String[][] products = new String[][]{
            {"Product1", "cba6de52-808d-4f03-81cb-e78df5cb9791"},
            {"Product2", "cba6de52-808d-4f03-81cb-e78df5cb9792"},
            {"Product3", "cba6de52-808d-4f03-81cb-e78df5cb9793"},
            {"Product4", "cba6de52-808d-4f03-81cb-e78df5cb9794"},
            {"Product5", "cba6de52-808d-4f03-81cb-e78df5cb9795"},
            {"Product6", "cba6de52-808d-4f03-81cb-e78df5cb9796"},
            {"Product7", "cba6de52-808d-4f03-81cb-e78df5cb9797"},
            {"Product8", "cba6de52-808d-4f03-81cb-e78df5cb9798"}
    };

    public AirlockParallelCalculationsTest() {
        for (String[] product : products) {
            productsInstances.add(initAndPull(product[1], product[0], "Product_default.json"));
        }
    }


    private long mutipleCalculationWithThreadNumber(int numberOfConcurrentProducts) {
        AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().startMeasuring();
        LinkedList<JSONObject> contexts = new LinkedList();
        for (int i = 0; i < numberOfConcurrentProducts; i++) {
            contexts.add(new JSONObject(readFile(new File(AirlockParallelCalculationsTest.class.
                    getResource("Product_context.json").getFile()))));
        }

        for (int i = 0; i < productsInstances.size(); i++) {
            final JSONObject context = contexts.get(i);
            calcNoTimer(productsInstances.get(i), context);
        }

        long start = System.currentTimeMillis();
        CountDownLatch doneAll = new CountDownLatch(numberOfConcurrentProducts);


        for (int i = 0; i < productsInstances.size(); i++) {
            final JSONObject context = contexts.get(i);
            final AirlockProductManager product = productsInstances.get(i);
            product.updateProductContext(context.toString());
            new Thread() {
                public void run() {
                    for (int i = 0; i < numberOfCalculationsPerProduct; i++) {
                        try {
                            int delta = new Random().nextInt(1000);
                            Thread.sleep(1000 + delta);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        new Thread() {
                            public void run() {
                                calc(product, context);
                                numberOfCalculationsSoFar++;
                                if (numberOfCalculationsSoFar % 20 == 0) {
                                    System.out.println("So far :" + numberOfCalculationsSoFar + " calcs");
                                    System.out.println("Performance:" + totalAvarageTime / numberOfCalculationsSoFar);
                                }
                            }
                        }.start();
                    }
                    doneAll.countDown();
                }
            }.start();
        }
        long totalTime = 0;
        try {
            if (!doneAll.await(100000, TimeUnit.SECONDS)) {
                Assert.fail("time-out happened");
            } else {
                totalTime = (System.currentTimeMillis() - start);
                System.out.println("Total time of [" + numberOfCalculationsSoFar+ "] calc:" +totalAvarageTime);
                System.out.println("Calc per second:" + products.length);
                System.out.println("Average time of per thread [" + numberOfCalculationsSoFar + "] :" + totalAvarageTime / numberOfCalculationsSoFar);
            }
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }

        System.out.println("Reports : " + AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().getReport().toString());
        AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().stopMeasuring();
        return totalTime;
    }


    @Test
    public void mutipleCalculation() {
        mutipleCalculationWithThreadNumber(productsInstances.size());
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
            airlockProductManager.calculateFeatures((JSONObject)null, (JSONObject)null);
            airlockProductManager.syncFeatures();
            totalAvarageTime = totalAvarageTime + (System.currentTimeMillis() - start);
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }
    }

    private void calcSync(AirlockProductManager airlockProductManager, JSONObject context) {
        try {
            airlockProductManager.calculateFeatures(null, context);
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }
    }

    private AirlockProductManager init(String productName, String productId) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        return initAndPull(productId, productName, productName + "_default.json");
    }

    private AirlockProductManager initAndPull(String instanceId, String productName, String default_file) {


        File file = new File(AirlockParallelCalculationsTest.class.getResource(default_file).getFile());
        String defaultsFile = readFile(file);

        String seasonId = DefaultsUtils.getSeasonId(defaultsFile);
        String productId = DefaultsUtils.getProductId(defaultsFile);

        Assert.assertNotNull(AirlockMultiProductsManager.getInstance().createProduct(instanceId,seasonId,productId, productName, appVersion));
        final AirlockProductManager airlockProductManager = AirlockMultiProductsManager.getInstance().getAirlockProductManager(instanceId);
        Assert.assertNotNull(airlockProductManager);

        try {
            InstanceContext context = new InstanceContext(instanceId,FilePreferencesFactory.getAirlockCacheDirectory(),seasonId, productName, appVersion);
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