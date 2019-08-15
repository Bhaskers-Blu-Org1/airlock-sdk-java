package com.ibm.airlock.sdk.debug;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.debug.AirlockProductDebugger;
import com.ibm.airlock.common.engine.ScriptInitException;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.net.AirlockDAO;
import com.ibm.airlock.common.streams.AirlockStream;
import com.ibm.airlock.common.streams.StreamsManager;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.sdk.AirlockProductInstanceManager;
import com.ibm.airlock.sdk.cache.InstanceContext;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Denis Voloshin on 07/11/2017.
 */
public class AirlockDebugger implements AirlockProductDebugger {

    @Nullable
    private AirlockProductInstanceManager airlockProductManagerDelegator;

    public AirlockDebugger(AirlockProductInstanceManager airlockProductManagerDelegator) {
        this.airlockProductManagerDelegator = airlockProductManagerDelegator;
    }


    @Override
    public List<String> getSelectedDeviceUserGroups() {
        return this.airlockProductManagerDelegator.getCacheManager().getPersistenceHandler().getDeviceUserGroups();
    }

    @Override
    public JSONArray getUserGroups() throws InterruptedException, TimeoutException, IOException {
        final CountDownLatch latch = new CountDownLatch(1);
        final JSONArray userGroups = new JSONArray();

        final List<Exception> errors = new ArrayList<>();
        airlockProductManagerDelegator.getServerUserGroups(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                Logger.log.e(getClass().getName(), e.getMessage());
                errors.add(e);
                latch.countDown();
            }

            @Override
            public void onSuccess(String msg) {
                userGroups.put(new JSONArray(msg));
                latch.countDown();
            }
        });
        if (!latch.await(5000, TimeUnit.SECONDS)) {
            throw new TimeoutException();
        }

        if (errors.size() > 0) {
            throw new IOException(errors.get(0));
        }

        if (userGroups.length() > 0) {
            return (JSONArray) userGroups.get(0);
        }

        return userGroups;
    }

    @Override
    public void setDeviceUserGroups(@Nullable String... userGroups) throws TimeoutException {
        if (userGroups != null) {
            StreamsManager sm = this.airlockProductManagerDelegator.getStreamsManager();
            this.airlockProductManagerDelegator.getCacheManager().getPersistenceHandler().
                    storeDeviceUserGroups(Arrays.asList(userGroups), sm);
        }
    }

    @Override
    public void pullFeatures() throws AirlockNotInitializedException, InterruptedException, TimeoutException, IOException {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<Exception> errors = new ArrayList<>();
        AirlockCallback callback = new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                errors.add(e);
                latch.countDown();
            }

            @Override
            public void onSuccess(String s) {
                latch.countDown();
            }
        };
        this.airlockProductManagerDelegator.pullFeatures(callback);
        if (!latch.await(5000, TimeUnit.SECONDS)) {
            throw new TimeoutException();
        }
        if (errors.size() > 0) {
            throw new IOException(errors.get(0));
        }

    }

    @Override
    public void initSDK(String directory, String defaultConfiguration, String productVersion) throws AirlockInvalidFileException, IOException {
        this.airlockProductManagerDelegator.initSDK(new InstanceContext(directory), defaultConfiguration, productVersion);
    }

    @Override
    public JSONObject getLastCalculatedContext() {
        return ((AirlockProductInstanceManager) this.airlockProductManagerDelegator).getLastCalculatedAirlockContext();
    }

    @Override
    public JSONObject getTranslatedStringsTable() {
        return this.airlockProductManagerDelegator.getCacheManager().getPersistenceHandler().readJSON(Constants.SP_RAW_TRANSLATIONS);
    }

    @Override
    public JSONObject getStreamsJavaScriptUils() {
        return this.airlockProductManagerDelegator.getCacheManager().getPersistenceHandler().readJSON(Constants.SP_FEATURE_UTILS_STREAMS);
    }

    @Override
    public JSONObject getJavaScriptUils() {
        return this.airlockProductManagerDelegator.getCacheManager().getPersistenceHandler().readJSON(Constants.SP_RAW_JS_FUNCTIONS);
    }

    @Override
    public JSONObject getRawFeatureConfiguration() {
        return this.airlockProductManagerDelegator.getCacheManager().getPersistenceHandler().readJSON(Constants.SP_RAW_RULES);
    }

    @Override
    public JSONArray getDevelopmentBranches() throws InterruptedException, TimeoutException, IOException {
        final CountDownLatch latch = new CountDownLatch(1);
        final JSONArray branches = new JSONArray();
        final List<Exception> errors = new ArrayList<>();
        AirlockDAO.pullBranches(this.airlockProductManagerDelegator.getCacheManager(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.log.e(getClass().getName(), e.getMessage());
                errors.add(e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                //read the response to the string
                if (response.body() == null || response.body().toString().isEmpty() || !response.isSuccessful()) {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }

                try {
                    final JSONObject branchesFullResponse = new JSONObject(response.body().string());
                    response.body().close();
                    final JSONArray branchesArray = branchesFullResponse.getJSONArray("branches");
                    branches.put(branchesArray);
                } catch (Exception e) {

                }

                latch.countDown();
            }
        });
        if (!latch.await(5000, TimeUnit.SECONDS)) {
            throw new TimeoutException();
        }

        if (errors.size() > 0) {
            throw new IOException(errors.get(0));
        }

        if (branches.length() > 0) {
            return (JSONArray) branches.get(0);
        }

        return branches;
    }

    @Override
    public List<String> getStreams() {
        return this.airlockProductManagerDelegator.getStreamsManager().getStreamNames();
    }

    @Override
    public JSONObject getStream(String name) {
        return this.airlockProductManagerDelegator.getStreamsManager().getStreamByName(name).toJSON();
    }

    @Override
    public void clearStream(String name) {
        AirlockStream stream = this.airlockProductManagerDelegator.getStreamsManager().getStreamByName(name);
        if (stream != null) {
            stream.clearEvents();
            stream.clearProcessingData();
            stream.clearTrace();
        }
    }

    @Override
    public void runStreamProcessing(String name) throws ScriptInitException {
        AirlockStream stream = this.airlockProductManagerDelegator.getStreamsManager().getStreamByName(name);
        if (stream != null) {
            this.airlockProductManagerDelegator.getStreamsManager().calculateAndSaveStreams(null, true, new String[]{stream.getName()});
        }
    }

    @Override
    public void setStreamRolloutPercentage(String name, long percentage) {
        AirlockStream stream = this.airlockProductManagerDelegator.getStreamsManager().getStreamByName(name);
        stream.setRolloutPercentage(percentage);
    }

    @Override
    public void setDevelopmentBranch(String branchName) {
        this.airlockProductManagerDelegator.getCacheManager().getPersistenceHandler().write(Constants.SP_DEVELOP_BRANCH_NAME, branchName);
    }

    @Override
    public String getSelectedDevelopmentBranch() {
        return this.airlockProductManagerDelegator.getCacheManager().getPersistenceHandler().read(Constants.SP_DEVELOP_BRANCH_NAME, "");
    }

    @Override
    public void enableResponsiveMode(boolean enable) {
        this.airlockProductManagerDelegator.setDataProviderType(enable ? AirlockDAO.DataProviderType.CACHED_MODE : AirlockDAO.DataProviderType.DIRECT_MODE);
    }

    @Override
    public void disableAllDevelopmentBranches() {
        this.airlockProductManagerDelegator.getCacheManager().getPersistenceHandler().write(Constants.SP_DEVELOP_BRANCH_NAME, "");
    }

    @Override
    public List<Feature> getCurrentFeaturesState() {
        return this.airlockProductManagerDelegator.getCacheManager().getSyncFeatureList().getRootFeatures();
    }

    @Override
    public void setFeatureRolloutPercentage(String featureName, int percentage) {
        this.airlockProductManagerDelegator.getCacheManager().getPersistenceHandler().getFeaturesRandomMap().put(featureName, "1");
    }
}
