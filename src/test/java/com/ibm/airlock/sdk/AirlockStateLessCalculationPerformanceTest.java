package com.ibm.airlock.sdk;

import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.engine.AirlockEnginePerformanceMetric;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@RunWith(Parallelized.class)
public class AirlockStateLessCalculationPerformanceTest extends AirlockStateLessBaseTest {

    private static long totalAvarageTime;
    private int numberOfConcurrentContextCalcs = 10;


    public AirlockStateLessCalculationPerformanceTest(String productName) {
        product = initAndPull(this.productName, "Product_default.json");
    }

    @Parameterized.Parameters(name = "{index}:{0}")
    public static Collection primeNumbers() {
        return Arrays.asList(new Object[][]{
                {"StateLessCalcTest"},
        });
    }

    private long mutipleCalculationWithThreadNumber(int p_numberOfConcurrentContextCalcs) {
        AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().startMeasuring();
        LinkedList<JSONObject> contexts = new LinkedList();

        for (int i = 0; i < p_numberOfConcurrentContextCalcs; i++) {
            contexts.add(new JSONObject(readFile(new File(AirlockStateLessCalculationPerformanceTest.class.
                    getResource("Product_context.json").getFile()))));
        }

        calcNoTimer(product, contexts.get(0));

        long start = System.currentTimeMillis();
        CountDownLatch doneAll = new CountDownLatch(p_numberOfConcurrentContextCalcs);
        for (int i = 0; i < p_numberOfConcurrentContextCalcs; i++) {
            final JSONObject context = contexts.get(0);
            new Thread() {
                public void run() {
                    calc(product, context);
                    doneAll.countDown();
                }
            }.start();
        }
        long totalTime = 0;
        try {
            if (!doneAll.await(100, TimeUnit.SECONDS)) {
                Assert.fail("time-out happened");
            } else {
                totalTime = (System.currentTimeMillis() - start);
                System.out.println("Total time of [" + p_numberOfConcurrentContextCalcs + "] :" + (System.currentTimeMillis() - start));
                System.out.println("Average time per thread [" + p_numberOfConcurrentContextCalcs + "] :" + (System.currentTimeMillis() - start) / p_numberOfConcurrentContextCalcs);
                System.out.println("Total time of per thread [" + p_numberOfConcurrentContextCalcs + "] :" + totalAvarageTime / p_numberOfConcurrentContextCalcs);
            }
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }

        System.out.println("Reports : " + AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().getReport().toString());
        AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().stopMeasuring();
        for (String calcTime : AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().getReport().values()) {
            Assert.assertTrue(Integer.parseInt(calcTime) < 500);
        }
        return totalTime;
    }


    @Test
    public void mutipleCalculation() {
        JSONObject average = new JSONObject();
        int trails = 1;
        for (int j = 0; j < trails; j++) {
            JSONObject result = new JSONObject();
            for (int i = 1; i <= numberOfConcurrentContextCalcs; i++) {
                result.put(i + "", mutipleCalculationWithThreadNumber(22));
                totalAvarageTime = 0;
            }
            average.put(j + "", result);
        }
        Hashtable<Integer, Integer> averageResults = new Hashtable();
        for (int i = 1; i <= numberOfConcurrentContextCalcs; i++) {
            int sum = 0;
            for (int j = 0; j < trails; j++) {
                sum += average.getJSONObject(j + "").getInt(i + "");
            }
            averageResults.put(i, sum / trails);
        }

        System.out.println("mutipleCalculation result:" + averageResults.toString());
    }


    protected void calc(AirlockProductManager airlockProductManager, JSONObject context) {
        try {
            long start = System.currentTimeMillis();
            airlockProductManager.calculateFeatures(null, context);
            totalAvarageTime = totalAvarageTime + (System.currentTimeMillis() - start);
        } catch (AirlockNotInitializedException e) {
            Assert.fail(e.getMessage());
        }
    }
}