package com.ibm.airlock.sdk;

import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.engine.AirlockEnginePerformanceMetric;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@RunWith(Parameterized.class)
public class AirlockSequentialCalculationsTest extends AirlockStateLessBaseTest {


    public AirlockSequentialCalculationsTest(String productName) {
        product = initAndPull(this.productName, "StressCalculationDefaults.json");
        product.setDeviceUserGroups(Arrays.asList(new String[]{"QA"}));
    }

    @Parameterized.Parameters(name = "{index}:{0}")
    public static Collection primeNumbers() {
        return Arrays.asList(new Object[][]{
                {"StressCalculationDefaults"},
        });
    }


    @Test
    public void mutipleCalculation() {
        int trails = 100;
        for (int j = 0; j < trails; j++) {
            calc(product, new JSONObject(readFile(new File(AirlockStateLessCalculationPerformanceTest.class.
                    getResource("Product_context.json").getFile()))));
            if (j % 10 == 0) {
                System.out.println("So far done: " + j);
            }
        }
    }


    protected void calc(AirlockProductManager airlockProductManager, JSONObject context) {
        try {
            airlockProductManager.calculateFeatures(null, context);
            Assert.assertEquals(2, airlockProductManager.getFeature("modules.Airlock Control Over ModulesQAtest").getChildren().size());
            airlockProductManager.syncFeatures();
            Assert.assertEquals(2, airlockProductManager.getFeature("modules.Airlock Control Over ModulesQAtest").getChildren().size());
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }
    }
}