package com.ibm.airlock.sdk;

import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.net.ConnectionManager;
import com.ibm.airlock.common.test.AbstractBaseTest;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.sdk.cache.InstanceContext;
import com.ibm.airlock.sdk.cache.PersistenceEncryptor;
import com.ibm.airlock.sdk.cache.pref.FilePreferencesFactory;
import com.ibm.airlock.sdk.log.JavaLog;
import com.ibm.airlock.sdk.net.JavaOkHttpClientBuilder;
import org.json.JSONException;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Denis Voloshin on 26/11/2017.
 */

public class JavaSdkBaseTest extends AbstractBaseTest {

    static {
        System.setProperty("java.util.prefs.PreferencesFactory", FilePreferencesFactory.class.getName());
        PersistenceEncryptor.enableEncryption(false);
    }


    public JavaSdkBaseTest() {
        Logger.setLogger(new JavaLog());
    }

    @Override
    public void setLocale(Locale locale) {
        Locale.setDefault(locale);
    }

    @Override
    public void setUpMockups() throws JSONException {
        mockedContext = Mockito.spy(new InstanceContext("111-111-111-111", FilePreferencesFactory.getAirlockCacheDirectory(), slurp(getDefaultFile(), 1024), this.m_version));
        manager = new AirlockProductInstanceManager(mockedContext);
        MockitoAnnotations.initMocks(this);
    }

    @Override
    public void customSetUp(String version, ArrayList<String> groups, String locale, String randoms, boolean setUpMockups, boolean reset, boolean cleanStreams)
            throws IOException, AirlockInvalidFileException, JSONException {
        //set up mockes
        if (setUpMockups) setUpMockups();
        //reset
        if (reset) {
            if (!((InstanceContext) mockedContext).getAppVersion().equals(version)) {
                manager.reset(new InstanceContext("111-111-111-111", FilePreferencesFactory.getAirlockCacheDirectory(), slurp(getDefaultFile(), 1024), version));
            }
            manager.reset(mockedContext);
        }
        //init sdk
        manager.initSDK(mockedContext, slurp(getDefaultFile(), 1024), version);
        //set user groups
        if (groups != null) manager.setDeviceUserGroups(groups);
        //set locale
        if (locale != null) Locale.setDefault(new Locale(locale));
        //write required randoms
        if (randoms != null)
            manager.getCacheManager().getPersistenceHandler().write(Constants.SP_RANDOMS, randoms);
        //clean streams
        if (cleanStreams) manager.getStreamsManager().clearAllStreams();
    }

    @Override
    protected ConnectionManager getConnectionManager() {
        return new ConnectionManager(new JavaOkHttpClientBuilder(new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }),true);
    }

    @Override
    protected ConnectionManager getConnectionManager(String m_key) {
        return new ConnectionManager(new JavaOkHttpClientBuilder(new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }), m_key,true);
    }


    @Override
    public String getDataFileContent(String pathInDataFolder) throws IOException {
        return (new JavaSdkTestDataManager()).getFileContent(pathInDataFolder);
    }

    @Override
    public String[] getDataFileNames(String directoryPathInDataFolder) throws IOException {
        return (new JavaSdkTestDataManager()).getFileNamesListFromDirectory(directoryPathInDataFolder);
    }

    @Override
    public String getTestName() {
        return null;
    }
}
