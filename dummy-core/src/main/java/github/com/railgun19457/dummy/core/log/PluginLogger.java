package github.com.railgun19457.dummy.core.log;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class PluginLogger {

    private final Logger logger;

    public PluginLogger(Logger logger) {
        this.logger = logger;
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warn(String message) {
        logger.warning(message);
    }

    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
}
