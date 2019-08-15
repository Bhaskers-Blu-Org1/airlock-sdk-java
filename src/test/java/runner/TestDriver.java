package runner;


import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.sdk.AirlockMultiProductsManager;
import com.ibm.airlock.sdk.AirlockProductInstanceManager;
import com.ibm.airlock.sdk.cache.InstanceContext;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;


/**
 * The class is a quick start to get familiar with Airlock SDK for Java platform
 * Created by OrenP on 25/07/2017.
 */
public class TestDriver {


    // airlock test product names
    private final static String ANDROID_FLAGSHIP_PHONE = "Android Flagship Phone";
    private final static String IOS_TWC_STORM_RADAR = "iOS TWC Storm Radar";
    private static String pathToProject = System.getProperty("user.dir") + "/";

    public static void main(String[] args) throws AirlockNotInitializedException {

        final AirlockMultiProductsManager airlockProductsManager = AirlockMultiProductsManager.getInstance();
        AirlockProductInstanceManager productIOS = null;
        AirlockProductInstanceManager productAndroid = null;
        AirlockProductInstanceManager productAndroidStorm = null;

        try {

            // create different instance contexts for each airlock product

            InstanceContext contextIOS1 = new InstanceContext("cba6de52-808d-4f03-81cb-e78df5cb9791", "airlock-sdk-java/cache",
                    readAndroidFile(pathToProject + "config/iOS TWC Storm Radar.json"), "1.2");
            productIOS = airlockProductsManager.createProduct(contextIOS1);
            AirlockMultiProductsManager.getInstance().getAirlockProductManager("cba6de52-808d-4f03-81cb-e78df5cb9791").
                    initSDK(contextIOS1, readAndroidFile(pathToProject + "config/iOS TWC Storm Radar.json"), "1.2");


            InstanceContext contextIOS2 = new InstanceContext("cba6de52-808d-4f03-81cb-e78df5cb9792", "airlock-sdk-java/cache",
                    readAndroidFile(pathToProject + "config/iOS TWC Storm Radar.json"), "1.2");

            productIOS = airlockProductsManager.createProduct(contextIOS2);
            AirlockMultiProductsManager.getInstance().getAirlockProductManager("cba6de52-808d-4f03-81cb-e78df5cb9792").
                    initSDK(contextIOS2, readAndroidFile(pathToProject + "config/iOS TWC Storm Radar.json"), "1.2");


            InstanceContext contextAndroid = new InstanceContext("cba6de52-808d-4f03-81cb-e78df5cb9793", "airlock-sdk-java/cache",
                    readAndroidFile(pathToProject + "config/Android Flagship Phone.json"), "8.16");


            productAndroid = airlockProductsManager.createProduct(contextAndroid);
            AirlockMultiProductsManager.getInstance().getAirlockProductManager("cba6de52-808d-4f03-81cb-e78df5cb9793").
                    initSDK(contextAndroid, readAndroidFile(pathToProject + "config/Android Flagship Phone.json"), "8.16");


            InstanceContext contextIOS4 = new InstanceContext("cba6de52-808d-4f03-81cb-e78df5cb9794", "airlock-sdk-java/cache",
                    readAndroidFile(pathToProject + "config/iOS TWC Storm Radar.json"), "1.2");
            productIOS = airlockProductsManager.createProduct(contextIOS4);
            AirlockMultiProductsManager.getInstance().getAirlockProductManager("cba6de52-808d-4f03-81cb-e78df5cb9791").
                    initSDK(contextIOS4, readAndroidFile(pathToProject + "config/iOS TWC Storm Radar.json"), "1.2");


        } catch (AirlockInvalidFileException e) {
            // handle error
        } catch (IOException e) {
            // handle error
        }


        // pull remote configuration by instance Id
        airlockProductsManager.getAirlockProductManager("cba6de52-808d-4f03-81cb-e78df5cb9793").pullFeatures(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                System.err.println("Error happened while fetching from " + ANDROID_FLAGSHIP_PHONE + " " + e.getMessage());
            }

            @Override
            public void onSuccess(String msg) {
                try {

                    System.out.println("Remote config for " + ANDROID_FLAGSHIP_PHONE + " product is done");
                    String content = null;

                    // read sample airlock context from FS
                    try {
                        content = new String(Files.readAllBytes(Paths.get(pathToProject + "context/context.json")));
                    } catch (IOException e) {
                        // handle error
                    }
                    AirlockProductManager airlockProductManager = airlockProductsManager.getAirlockProductManager("cba6de52-808d-4f03-81cb-e78df5cb9793");

                    JSONObject airlockContext = content == null ? null : new JSONObject(content);
                    airlockProductManager.calculateFeatures(new JSONObject(), airlockContext, null);
                    System.out.println("calculation  for " + IOS_TWC_STORM_RADAR + " product is done");
                    airlockProductManager.syncFeatures();
                    System.out.println("sync  for " + ANDROID_FLAGSHIP_PHONE + " product is done");

                    Feature feature = airlockProductManager.getFeature(AirlockConstants.modules.AIRLOCK_CONTROL_OVER_MODULES);
                    System.out.println("In " + ANDROID_FLAGSHIP_PHONE + " " + AirlockConstants.modules.AIRLOCK_CONTROL_OVER_MODULES + " " + feature.getChildren().toString());

                } catch (AirlockNotInitializedException e) {
                    // handle error
                }
            }
        });


        // pull remote configuration
        airlockProductsManager.getAirlockProductManager("cba6de52-808d-4f03-81cb-e78df5cb9791").pullFeatures(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                System.err.println("Error happened while fetching from " + IOS_TWC_STORM_RADAR + " " + e.getMessage());
            }


            @Override
            public void onSuccess(String msg) {
                try {
                    System.out.println("Remote config for " + IOS_TWC_STORM_RADAR + " product is done");
                    String content = null;

                    // read sample airlock context from FS
                    try {
                        content = new String(Files.readAllBytes(Paths.get(pathToProject + "context/context.json")));
                    } catch (IOException e) {
                        // handle error
                    }
                    AirlockProductManager airlockProductManager = airlockProductsManager.getAirlockProductManager("cba6de52-808d-4f03-81cb-e78df5cb9791");


                    JSONObject airlockContext = content == null ? null : new JSONObject(content);
                    airlockProductManager.calculateFeatures(new JSONObject(), airlockContext, null);
                    System.out.println("calculation  for " + IOS_TWC_STORM_RADAR + " product is done");
                    airlockProductManager.syncFeatures();
                    System.out.println("sync  for " + IOS_TWC_STORM_RADAR + " product is done");
                    Feature feature = airlockProductManager.getFeature(AirlockConstants.PushConfiguration.PUSH_NOTIFICATIONS);
                    System.out.println("In " + IOS_TWC_STORM_RADAR + " " + AirlockConstants.PushConfiguration.PUSH_NOTIFICATIONS + " " + feature.toString());

                } catch (AirlockNotInitializedException e) {
                    // handle error
                }
            }
        });
    }


    public static String readAndroidFile(String filePath) throws AirlockInvalidFileException, IOException {
        File file = new File(filePath);
        InputStream inStream = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
        StringBuilder sBuilder = new StringBuilder();
        String strLine;
        while ((strLine = br.readLine()) != null) {
            sBuilder.append(strLine);
        }
        br.close();
        return sBuilder.toString();
    }
}