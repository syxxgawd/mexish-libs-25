package net.mexish.libs.commons.logging;

import net.mexish.libs.commons.util.Strings;
import net.mexish.libs.impl.Impls;

public abstract class Logging {

    public static final Logging IMP = Impls.get(Logging.class);

    public abstract void debug(String message);

    public void debug(String message, Object... args) {
        debug(Strings.formatText(message, args));
    }

    public abstract void info(String message);

    public void info(String message, Object... args) {
        info(Strings.formatText(message, args));
    }

    public abstract void warn(String message);

    public void warn(String message, Object... args) {
        warn(Strings.formatText(message, args));
    }

    public abstract void error(String message);

    public void error(String message, Object... args) {
        error(Strings.formatText(message, args));
    }

    public abstract void stacktrace(Throwable t, String message);

    public void stacktrace(Throwable t, String message, Object... args) {
        stacktrace(t, Strings.formatText(message, args));
    }

}
