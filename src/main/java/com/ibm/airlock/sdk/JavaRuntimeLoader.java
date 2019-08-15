package com.ibm.airlock.sdk;

import com.ibm.airlock.common.cache.RuntimeLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class JavaRuntimeLoader extends RuntimeLoader {

    JavaRuntimeLoader(String pathToFiles, String encryprionKey) {
        super(pathToFiles, encryprionKey);
    }

    @Override
    protected InputStream getInputStream(String name){
        File fileInput = new File(pathToFiles, name);
        try {
            return new FileInputStream(fileInput);
        } catch (FileNotFoundException e) {
            return null;
        }
    }
}
