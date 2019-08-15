package com.ibm.airlock.sdk.net;

import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.net.OkHttpClientBuilder;
import com.ibm.airlock.common.net.interceptors.ResponseDecryptor;
import com.ibm.airlock.common.net.interceptors.ResponseExtractor;
import com.ibm.airlock.sdk.config.ConfigurationManager;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JavaOkHttpClientBuilder implements OkHttpClientBuilder {

    private  X509TrustManager trustManager = null;

    public JavaOkHttpClientBuilder(){
        super();
    }

    public JavaOkHttpClientBuilder(X509TrustManager p_trustManager) {
        trustManager = p_trustManager;
    }

    /**
     * Enables TLSv1.2 protocol (which is disabled by default)
     *
     * @param client OKHttp client builder
     */
    private  OkHttpClient.Builder enableTls12O(OkHttpClient.Builder client) {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.
                    getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                return client;
            }

            sc.init(null, new TrustManager[]{trustManager}, null);

            client.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()), trustManager);
            ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .build();

            List<ConnectionSpec> specs = new ArrayList<>();
            specs.add(cs);
            specs.add(ConnectionSpec.COMPATIBLE_TLS);
            specs.add(ConnectionSpec.CLEARTEXT);

            client.connectionSpecs(specs);
        } catch (Exception exc) {
        }

        return client;
    }

    public OkHttpClient create() {
        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .cache(null)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS);


        // set proxy if defined
        if (ConfigurationManager.getProxyPort() != -1 && ConfigurationManager.getProxyUrl() != null) {
            client.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ConfigurationManager.getProxyUrl(),
                    ConfigurationManager.getProxyPort())));
            Logger.log.i(JavaOkHttpClientBuilder.class.getName(),
                    "Airlock SDK will communicate through proxy server:" +
                            (ConfigurationManager.getProxyUrl() + ":" + ConfigurationManager.getProxyPort()));
        }

        return enableTls12O(client).build();
    }

    @Override
    public OkHttpClient create(String encryptionKey) {
        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .cache(null)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(new ResponseExtractor()).addInterceptor(new ResponseDecryptor(encryptionKey));
        return enableTls12O(client).build();
    }
}
