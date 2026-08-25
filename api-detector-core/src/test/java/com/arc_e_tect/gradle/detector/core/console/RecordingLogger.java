package com.arc_e_tect.gradle.detector.core.console;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-written {@link Logger} test double that records every message passed to
 * {@link #lifecycle(String)}/{@link #lifecycle(String, Object...)} and
 * {@link #info(String)}/{@link #info(String, Object...)}, and no-ops everything else - this
 * codebase's tests use hand-written fakes rather than a mocking framework.
 */
class RecordingLogger implements Logger {

    private final List<String> lifecycleMessages = new ArrayList<>();
    private final List<String> infoMessages = new ArrayList<>();

    /** Creates a new {@code RecordingLogger}. */
    RecordingLogger() {}

    /**
     * Every message passed to {@code lifecycle(...)}, in call order.
     *
     * @return the recorded lifecycle messages
     */
    List<String> lifecycleMessages() {
        return lifecycleMessages;
    }

    /**
     * Every message passed to {@code info(...)}, in call order.
     *
     * @return the recorded info messages
     */
    List<String> infoMessages() {
        return infoMessages;
    }

    @Override
    public void lifecycle(String message) {
        lifecycleMessages.add(message);
    }

    @Override
    public void lifecycle(String message, Object... objects) {
        lifecycleMessages.add(message);
    }

    @Override
    public void lifecycle(String message, Throwable throwable) {
        lifecycleMessages.add(message);
    }

    @Override
    public boolean isLifecycleEnabled() {
        return true;
    }

    @Override
    public void quiet(String message) {}

    @Override
    public void quiet(String message, Object... objects) {}

    @Override
    public void quiet(String message, Throwable throwable) {}

    @Override
    public boolean isQuietEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(LogLevel level) {
        return false;
    }

    @Override
    public void log(LogLevel level, String message) {}

    @Override
    public void log(LogLevel level, String message, Object... objects) {}

    @Override
    public void log(LogLevel level, String message, Throwable throwable) {}

    @Override
    public String getName() {
        return "RecordingLogger";
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void trace(String msg) {}

    @Override
    public void trace(String format, Object arg) {}

    @Override
    public void trace(String format, Object arg1, Object arg2) {}

    @Override
    public void trace(String format, Object... arguments) {}

    @Override
    public void trace(String msg, Throwable t) {}

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return false;
    }

    @Override
    public void trace(Marker marker, String msg) {}

    @Override
    public void trace(Marker marker, String format, Object arg) {}

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {}

    @Override
    public void trace(Marker marker, String format, Object... argArray) {}

    @Override
    public void trace(Marker marker, String msg, Throwable t) {}

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void debug(String msg) {}

    @Override
    public void debug(String format, Object arg) {}

    @Override
    public void debug(String format, Object arg1, Object arg2) {}

    @Override
    public void debug(String format, Object... arguments) {}

    @Override
    public void debug(String msg, Throwable t) {}

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return false;
    }

    @Override
    public void debug(Marker marker, String msg) {}

    @Override
    public void debug(Marker marker, String format, Object arg) {}

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {}

    @Override
    public void debug(Marker marker, String format, Object... arguments) {}

    @Override
    public void debug(Marker marker, String msg, Throwable t) {}

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String msg) {
        infoMessages.add(msg);
    }

    @Override
    public void info(String format, Object arg) {}

    @Override
    public void info(String format, Object arg1, Object arg2) {}

    @Override
    public void info(String format, Object... arguments) {
        infoMessages.add(format);
    }

    @Override
    public void info(String msg, Throwable t) {
        infoMessages.add(msg);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return false;
    }

    @Override
    public void info(Marker marker, String msg) {}

    @Override
    public void info(Marker marker, String format, Object arg) {}

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {}

    @Override
    public void info(Marker marker, String format, Object... arguments) {}

    @Override
    public void info(Marker marker, String msg, Throwable t) {}

    @Override
    public boolean isWarnEnabled() {
        return false;
    }

    @Override
    public void warn(String msg) {}

    @Override
    public void warn(String format, Object arg) {}

    @Override
    public void warn(String format, Object... arguments) {}

    @Override
    public void warn(String format, Object arg1, Object arg2) {}

    @Override
    public void warn(String msg, Throwable t) {}

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return false;
    }

    @Override
    public void warn(Marker marker, String msg) {}

    @Override
    public void warn(Marker marker, String format, Object arg) {}

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {}

    @Override
    public void warn(Marker marker, String format, Object... arguments) {}

    @Override
    public void warn(Marker marker, String msg, Throwable t) {}

    @Override
    public boolean isErrorEnabled() {
        return false;
    }

    @Override
    public void error(String msg) {}

    @Override
    public void error(String format, Object arg) {}

    @Override
    public void error(String format, Object arg1, Object arg2) {}

    @Override
    public void error(String format, Object... arguments) {}

    @Override
    public void error(String msg, Throwable t) {}

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return false;
    }

    @Override
    public void error(Marker marker, String msg) {}

    @Override
    public void error(Marker marker, String format, Object arg) {}

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {}

    @Override
    public void error(Marker marker, String format, Object... arguments) {}

    @Override
    public void error(Marker marker, String msg, Throwable t) {}
}
