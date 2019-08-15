package com.ibm.airlock.sdk;

import com.github.peterwippermann.junit4.parameterizedsuite.ParameterizedSuite;
import com.ibm.airlock.common.test.AbstractBaseTest;
import com.ibm.airlock.common.test.functional.*;
import com.ibm.airlock.common.test.golds_machine.GoldsTester;
import com.ibm.airlock.common.test.regressions.BranchesDiffBugRegTest;
import com.ibm.airlock.common.test.regressions.PercentageUpgradeRegTest;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;

/**
 * Created by Denis Voloshin on 26/11/2017.
 */

@RunWith(ParameterizedSuite.class)
@Suite.SuiteClasses({
        InMemoryCacheTest.class,
        EncryptedServerMinMaxVersionTest.class,
        SecuredConnectionTest.class,
        MinMaxVersionTest.class,
        PercentageUpgradeRegTest.class,
        GoldsTester.class,
        FeatureOrderingTest.class,
        SetLocaleTest.class,
        ManagerBasicTest.class,
        StreamsDevTest.class,
        RePullProdFeaturesTest.class,
        DevProdSeparationTest.class,
        FeaturesListTreeTest.class,
        StreamsQATest.class,
        UserGroupsTest.class,
        AnalyticsTest.class,
        PercentageManagerTest.class,
        BranchesDiffBugRegTest.class
})
public class JavaTestsSuiteTest {
    @Parameterized.Parameters(name = "Create tests helper")
    public static Object[] params() {
        return new Object[][] {{new JavaSdkBaseTest()}};
    }

    @Parameterized.Parameter
    public AbstractBaseTest baseTest ;
}
