package com.ibm.airlock.sdk.util;

import com.ibm.airlock.common.util.LocaleProvider;

import java.lang.reflect.MalformedParametersException;
import java.util.Locale;

public class ProductLocaleProvider implements LocaleProvider {

    private Locale locale;

    public ProductLocaleProvider(String locale) throws MalformedParametersException {
        if (locale == null || locale.isEmpty()) {
        } else if (locale.split("_").length < 2) {
            this.locale = new Locale(locale);
        } else {
            this.locale = new Locale(locale.split("_")[0], locale.split("_")[1]);
        }
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }
}
