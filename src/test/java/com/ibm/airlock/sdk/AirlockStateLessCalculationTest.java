package com.ibm.airlock.sdk;

import com.ibm.airlock.common.data.Feature;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;


public class AirlockStateLessCalculationTest extends AirlockStateLessBaseTest {


    @Test
    public void calculationWithDifferentLocales() {
        JSONObject context = new JSONObject(readFile(new File(AirlockStateLessCalculationPerformanceTest.class.
                getResource("Product_context.json").getFile())));
        Feature feature = calc(product, context, "en_US");
        FeaturesMap map = new FeaturesMap(feature);
        JSONObject config = map.getFeature("analytics.SmartRatings").getConfiguration();
        Assert.assertNotNull(config);
        Assert.assertEquals(config.has("positiveAnswerMessage"), true);
        Assert.assertEquals(config.getString("positiveAnswerMessage"), "Will you share the love in the Google Play Store?");


        feature = calc(product, context, "de_DE");
        map = new FeaturesMap(feature);
        config = map.getFeature("analytics.SmartRatings").getConfiguration();
        Assert.assertNotNull(config);
        Assert.assertEquals(config.has("positiveAnswerMessage"), true);
        Assert.assertEquals(config.getString("positiveAnswerMessage"), "Würden Sie auch im Google Play Store eine positive Bewertung hinterlassen?");

        AirlockMultiProductsManager.getInstance().removeAirlockProductManager(instanceId);
    }
}
