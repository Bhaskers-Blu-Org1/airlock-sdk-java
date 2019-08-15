package com.ibm.airlock.sdk;

import com.ibm.airlock.common.test.AbstractTestDataManager;
import org.junit.Ignore;

import java.io.*;

/**
 * Created by Denis Voloshin on 26/11/2017.
 */

public class JavaSdkTestDataManager implements AbstractTestDataManager {

    @Override
    public String getFileContent(String filePathUnderDataFolder) throws IOException {
        InputStream st =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(filePathUnderDataFolder);
        //byte[] encoded = Files.readAllBytes(Paths.get(workingDir.getAbsolutePath()+BUILD_FOLDER+filePathUnderDataFolder));
        return  convertStreamToString(st);
    }
    @Override
    public String[] getFileNamesListFromDirectory(String dirPathUnderDataFolder) throws IOException {
        File directory =  new File(Thread.currentThread().getContextClassLoader().getResource(dirPathUnderDataFolder).getFile());
        return directory.list();
    }

    public String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }
}
