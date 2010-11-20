package com.google.code.vanilladata.core.log;

import java.util.logging.Level;

public interface LogHandler {
    public boolean isEnabled(String logName, Level level);

    public void log(String logName, Level level, String message, Throwable thrown);
}
