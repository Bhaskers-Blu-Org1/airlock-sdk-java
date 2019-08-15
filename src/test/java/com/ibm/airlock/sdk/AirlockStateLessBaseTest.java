package com.ibm.airlock.sdk;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.sdk.cache.InstanceContext;
import com.ibm.airlock.sdk.cache.pref.FilePreferencesFactory;
import com.ibm.airlock.sdk.utils.DefaultsUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AirlockStateLessBaseTest {

    protected String productName;
    protected String appVersion = "8.16";
    protected String instanceId = "cba6de52-808d-4f03-81cb-e78df5cbqwqwq";
    protected AirlockProductManager product;

    public AirlockStateLessBaseTest() {
        this.productName = "StateLessCalcTest";
        product = initAndPull(productName, "Product_default.json");
    }

    protected void calcNoTimer(AirlockProductManager airlockProductManager, JSONObject context) {
        try {
            airlockProductManager.calculateFeatures(context,"en_US" );
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }
    }


    protected void calcSync(AirlockProductManager airlockProductManager, JSONObject context) {
        try {
            airlockProductManager.calculateFeatures(context,"en_US");
            airlockProductManager.syncFeatures();
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }
    }

    protected AirlockProductManager initAndPull() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        return initAndPull(this.productName, productName + "_default.json");
    }

    protected AirlockProductManager initAndPull(String productName, String defualt_file) {

        File file = new File(AirlockStateLessCalculationPerformanceTest.class.getResource(defualt_file).getFile());

        String defaultsFile = readFile(file);

        String seasonId = DefaultsUtils.getSeasonId(defaultsFile);
        String productId = DefaultsUtils.getProductId(defaultsFile);

        InstanceContext context = new InstanceContext(instanceId, FilePreferencesFactory.getAirlockCacheDirectory(), defaultsFile, appVersion);
        AirlockProductInstanceManager product = AirlockMultiProductsManager.getInstance().createProduct(context);
        AirlockProductManager airlockProductManager = AirlockMultiProductsManager.getInstance().getAirlockProductManager(instanceId);
        Assert.assertNotNull(airlockProductManager);

        try {
            airlockProductManager.reset(context, true);
            airlockProductManager.initSDK(context, readFile(file), "8.16");
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


    protected String readFile(File file) {

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

    protected Feature calc(AirlockProductManager airlockProductManager, JSONObject context, String locale) {
        try {
            return airlockProductManager.calculateFeatures(context, locale);
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }
        return null;
    }

    protected class FeaturesMap {
        private Hashtable<String, Feature> features = new Hashtable<>();
        private int onFeatures = 0;
        private int offFeatures = 0;

        public FeaturesMap(Feature parent) {
            traverse(parent);
        }

        private void traverse(Feature parent) {
            if (parent != null) {
                features.put(parent.getName(), parent);
                if (parent.isOn()) {
                    onFeatures++;
                } else {
                    offFeatures++;
                }
            }
            if (parent.getChildren() != null && parent.getChildren().size() > 0) {
                for (Feature feature : parent.getChildren()) {
                    traverse(feature);
                }
            }
        }

        public Feature getFeature(String name) {
            return features.get(name);
        }

        public Hashtable<String, Feature> getFeatures() {
            return features;
        }

        public int getOnFeatures() {
            return onFeatures;
        }

        public int getOffFeatures() {
            return offFeatures;
        }
    }
}
