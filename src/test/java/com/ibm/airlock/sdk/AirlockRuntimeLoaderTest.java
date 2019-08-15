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
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class AirlockRuntimeLoaderTest {


    private String appVersion = "8.16";

    public AirlockRuntimeLoaderTest() {

    }
    
    @Test
    public void testInit() {
        Assert.assertNotNull(AirlockMultiProductsManager.getInstance().createProduct("startupInstanceId","startupSeason","startupProductId", "startupProductName", appVersion));
        final AirlockProductManager airlockProductManager = AirlockMultiProductsManager.getInstance().getAirlockProductManager("startupInstanceId");
        Assert.assertNotNull(airlockProductManager);

        try {
            InstanceContext context = new InstanceContext("startupInstanceId",FilePreferencesFactory.getAirlockCacheDirectory(),"startupSeason", "startupProductName", appVersion);
            airlockProductManager.reset(context, true);
            airlockProductManager.initSDK(context, new JavaRuntimeLoader(System.getProperty("user.dir") + File.separator + "src/test/resources/runtime/encrypted", "TNHI3XTLNXCMDIZ6"), "TNHI3XTLNXCMDIZ6");
        } catch (AirlockInvalidFileException | IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}