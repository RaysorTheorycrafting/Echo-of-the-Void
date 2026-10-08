package com.eotv.echoofthevoid.diagnostics;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

final class UncannyLogCaptureAppender extends AbstractAppender {
    private static final String NAME = "EchoOfTheVoidDiagnosticCapture";
    private static final int MAX_MESSAGE_LENGTH = 8_192;
    private static final int MAX_STACK_LENGTH = 32_768;
    private static final ThreadLocal<Boolean> RECORDING = ThreadLocal.withInitial(() -> false);
    private static UncannyLogCaptureAppender installed;

    private UncannyLogCaptureAppender() {
        super(NAME, null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
    }

    static synchronized void install() {
        if (installed != null) {
            return;
        }
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        UncannyLogCaptureAppender appender = new UncannyLogCaptureAppender();
        appender.start();
        configuration.addAppender(appender);
        configuration.getRootLogger().addAppender(appender, Level.WARN, null);
        installed = appender;
        context.updateLoggers();
    }

    static synchronized void uninstall() {
        if (installed == null) {
            return;
        }
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.getConfiguration().getRootLogger().removeAppender(NAME);
        installed.stop();
        installed = null;
        context.updateLoggers();
    }

    @Override
    public void append(LogEvent event) {
        if (event == null || RECORDING.get()) {
            return;
        }
        boolean globalError = event.getLevel().isMoreSpecificThan(Level.ERROR);
        boolean modWarning = event.getLevel().isMoreSpecificThan(Level.WARN)
                && event.getLoggerName() != null
                && event.getLoggerName().startsWith("com.eotv.echoofthevoid");
        if (!globalError && !modWarning) {
            return;
        }

        RECORDING.set(true);
        try {
            DiagnosticSeverity severity = event.getLevel().isMoreSpecificThan(Level.FATAL)
                    ? DiagnosticSeverity.FATAL
                    : globalError ? DiagnosticSeverity.ERROR : DiagnosticSeverity.WARNING;
            String message = event.getMessage() == null ? "" : event.getMessage().getFormattedMessage();
            Throwable thrown = event.getThrown();
            UncannyDiagnostics.record(
                    severity,
                    "log",
                    thrown == null ? "log_message" : "uncaught_or_logged_exception",
                    UncannyDiagnostics.fields(
                            "logger", event.getLoggerName(),
                            "level", event.getLevel().name(),
                            "thread", event.getThreadName(),
                            "message", bounded(UncannyDiagnostics.sanitizeCapturedText(message), MAX_MESSAGE_LENGTH),
                            "exception", thrown == null ? "" : bounded(
                                    UncannyDiagnostics.sanitizeCapturedText(stackTrace(thrown)), MAX_STACK_LENGTH)));
        } finally {
            RECORDING.set(false);
        }
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter output = new StringWriter();
        throwable.printStackTrace(new PrintWriter(output));
        return output.toString();
    }

    private static String bounded(String value, int maximumLength) {
        if (value == null || value.length() <= maximumLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maximumLength) + "...[truncated]";
    }
}
