package com.google.code.vanilladata.core.log;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class Log {
    public static final AtomicReference<LogHandler> LOG_HANDLER
            = new AtomicReference<LogHandler>(new LoggerHandler());

    public static Log log(String name) {
        return new Log(name);
    }

    private final String name;

    public Log(String name) {
        this.name = name;
    }

    public static Log log(Class clazz) {
        return log(clazz.getName());
    }

    public boolean isTraceEnabled() {
        return LOG_HANDLER.get().isEnabled(name, Level.FINEST);
    }

    public void trace(String message, Throwable throwable) {
        LOG_HANDLER.get().log(name, Level.FINEST, message, throwable);
    }

    public void error(String message, Throwable throwable) {
        LOG_HANDLER.get().log(name, Level.SEVERE, message, throwable);
    }

    public void info(String message) {
        LOG_HANDLER.get().log(name, Level.FINE, message, null);
    }
}
