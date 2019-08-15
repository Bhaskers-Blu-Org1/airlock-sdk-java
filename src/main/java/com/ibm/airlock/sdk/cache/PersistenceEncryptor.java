package com.ibm.airlock.sdk.cache;

import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.util.Decryptor;

import javax.annotation.CheckForNull;
import java.io.*;
import java.security.GeneralSecurityException;

@SuppressWarnings("SpellCheckingInspection")
public class PersistenceEncryptor {

    private static final String TAG = "PersistenceEncryptor";
    private static final String ENCRYPTION_KEY = "KRXD4SU1UQRCNRUR";
    private static boolean enableEncryption = true;


    public static void enableEncryption(boolean enableEncryption_p) {
        enableEncryption = enableEncryption_p;
    }

    @CheckForNull
    public static InputStream decryptAES(File file) {
        try {
            byte[] fileContent = read(file);
            if (fileContent.length > 0) {
                return new ByteArrayInputStream(decrypt(read(file)));
            } else {
                return new ByteArrayInputStream(fileContent);
            }
        } catch (Exception e) {
            Logger.log.e(TAG, e.getMessage(), e);
            //if decryption fails returns the original file.
            try {
                return new ByteArrayInputStream(read(file));
            } catch (IOException e1) {
                Logger.log.e(TAG, e.getMessage(), e);

            }
        }
        return null;
    }

    private static byte[] encrypt(byte[] data) throws GeneralSecurityException, IOException {
        return enableEncryption ? Decryptor.encryptAES(data, ENCRYPTION_KEY.getBytes()) : data;
    }

    private static byte[] decrypt(byte[] data) throws GeneralSecurityException {
        return enableEncryption ? Decryptor.decryptAES(data, ENCRYPTION_KEY.getBytes()) : data;
    }


    @CheckForNull
    public static void encryptAES(File file) {
        try {
            byte[] encryptedValue = encrypt(read(file));
            FileOutputStream os = new FileOutputStream(file);
            os.write(encryptedValue);
            os.close();
        } catch (Exception e) {
            Logger.log.e(TAG, e.getMessage(), e);
        }
    }

    private static byte[] read(File file) throws IOException {
        ByteArrayOutputStream ous = null;
        InputStream ios = null;
        try {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            ios = new FileInputStream(file);
            int read;
            while ((read = ios.read(buffer)) != -1) {
                ous.write(buffer, 0, read);
            }
        } finally {
            try {
                if (ous != null)
                    ous.close();
            } catch (IOException ignored) {
            }

            try {
                if (ios != null)
                    ios.close();
            } catch (IOException ignored) {
            }
        }
        return ous.toByteArray();
    }
}
