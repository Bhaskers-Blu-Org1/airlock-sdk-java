package com.ibm.airlock.sdk.log;


import org.apache.commons.logging.LogFactory;

/**
 * Created by Denis Voloshin on 03/11/2017.
 */

public class JavaLog implements com.ibm.airlock.common.log.Log {

    @Override
    public int e(String tag, String msg) {

        LogFactory.getLog(tag).error(msg);
        return 1;
    }

    @Override
    public int e(String tag, String msg, Throwable tr) {
        LogFactory.getLog(tag).error(msg, tr);
        return 1;
    }

    @Override
    public int w(String tag, String msg) {
        LogFactory.getLog(tag).warn(msg);
        return 1;
    }

    @Override
    public int d(String tag, String msg) {
        LogFactory.getLog(tag).debug(msg);
        return 1;
    }

    @Override
    public int i(String tag, String msg) {
        LogFactory.getLog(tag).info(msg);
        return 1;
    }
}
