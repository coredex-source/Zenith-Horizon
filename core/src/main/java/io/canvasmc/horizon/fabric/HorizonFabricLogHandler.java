package io.canvasmc.horizon.fabric;

import io.canvasmc.horizon.logger.Logger;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.loader.impl.util.log.LogHandler;
import net.fabricmc.loader.impl.util.log.LogLevel;
import org.jspecify.annotations.NonNull;

public final class HorizonFabricLogHandler implements LogHandler {
    private final Logger logger;

    public HorizonFabricLogHandler(final @NonNull Logger logger) {
        this.logger = logger;
    }

    @Override
    public void log(long time, LogLevel level, LogCategory category, String msg, Throwable exc, boolean fromReplay, boolean wasSuppressed) {
        switch (level) {
            case ERROR -> {
                if (exc == null) logger.error("{}", msg);
                else logger.error(exc, "{}", msg);
            }
            case WARN -> {
                if (exc == null) logger.warn("{}", msg);
                else logger.warn(exc, "{}", msg);
            }
            case INFO -> {
                if (exc == null) logger.info("{}", msg);
                else logger.info(exc, "{}", msg);
            }
            case DEBUG -> {
                if (exc == null) logger.debug("{}", msg);
                else logger.debug(exc, "{}", msg);
            }
            case TRACE -> {
                if (exc == null) logger.trace("{}", msg);
                else logger.trace(exc, "{}", msg);
            }
        }
    }

    @Override
    public boolean shouldLog(LogLevel level, LogCategory category) {
        return switch (level) {
            case ERROR -> logger.isErrorEnabled();
            case WARN -> logger.isWarnEnabled();
            case INFO -> logger.isInfoEnabled();
            case DEBUG -> logger.isDebugEnabled();
            case TRACE -> logger.isTraceEnabled();
        };
    }

    @Override
    public void close() {
    }
}
