package com.google.code.vanilladata.core.log;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: Peter
 * Date: 20-Nov-2010
 * Time: 15:58:11
 * To change this template use File | Settings | File Templates.
 */
public class LoggerHandler implements LogHandler {
    public boolean isEnabled(String logName, Level level) {
        return Logger.getLogger(logName).isLoggable(level);
    }

    public void log(String logName, Level level, String message, Throwable thrown) {
        Logger.getLogger(logName).log(level, message, thrown);
    }
}
