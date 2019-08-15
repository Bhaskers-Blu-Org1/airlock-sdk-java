package com.ibm.airlock.sdk;

import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.net.BaseOkHttpClientBuilder;
import com.ibm.airlock.common.net.ConnectionManager;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.sdk.net.JavaOkHttpClientBuilder;
import org.json.JSONException;
import org.junit.Ignore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Denis Voloshin on 27/11/2017.
 */

public class JavaMultiProductBaseTest extends JavaSdkBaseTest{



    public JavaMultiProductBaseTest() {
        super();
    }

    @Override
    public void setUp(String serverUrl, String productName, String version) throws IOException, AirlockInvalidFileException, JSONException {
        m_serverUrl = serverUrl ;
        m_productName = productName ;
        m_version = version ;
        //manager = AirlockMultiProductsManager.getInstance().createProduct(m_productName,m_version);
        customSetUp(m_version, null, "en", null, true, true, true);
    }

    @Override
    public void customSetUp(String version, ArrayList<String> groups, String locale, String randoms, boolean setUpMockups, boolean reset, boolean cleanStreams) throws IOException, AirlockInvalidFileException, JSONException {
        //set up mockes
        if (setUpMockups) {
            setUpMockups();
        }
        //reset
        if (reset) {
            manager.reset(mockedContext);
        }
        //init sdk
        manager.initSDK(mockedContext,slurp(getDefaultFile(),1024),version);
        //set user groups
        if (groups!=null) manager.setDeviceUserGroups(groups);
        //set locale
        if (locale!=null) Locale.setDefault(new Locale(locale));
        //write required randoms
        if (randoms!=null) manager.getCacheManager().getPersistenceHandler().write(Constants.SP_RANDOMS,randoms);
        //clean streams
        if (cleanStreams) manager.getStreamsManager().clearAllStreams();
    }


    protected ConnectionManager getConnectionManager() {
        return new ConnectionManager(new JavaOkHttpClientBuilder());
    }

    protected ConnectionManager getConnectionManager(String m_key) {
        return new ConnectionManager(new JavaOkHttpClientBuilder(), m_key);
    }

}
