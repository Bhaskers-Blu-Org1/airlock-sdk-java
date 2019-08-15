package com.ibm.airlock.sdk;

import com.github.peterwippermann.junit4.parameterizedsuite.ParameterizedSuite;
import com.ibm.airlock.common.test.AbstractBaseTest;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;

/**
 * Created by Denis Voloshin on 27/11/2017.
 */

@RunWith(ParameterizedSuite.class)
@Suite.SuiteClasses({})
public class JavaMultiProductTestsSuite {
    @Parameterized.Parameters(name = "Create tests helper")
    public static Object[] params() {
        return new Object[][] {{new JavaMultiProductBaseTest()}};
    }

    @Parameterized.Parameter
    public AbstractBaseTest baseTest ;
}
