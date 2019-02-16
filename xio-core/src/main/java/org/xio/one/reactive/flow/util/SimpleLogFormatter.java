package org.xio.one.reactive.flow.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

public class SimpleLogFormatter extends Formatter {

    // format string for printing the log record
    static String getLoggingProperty(String name) {
        return LogManager.getLogManager().getProperty(name);
    }

    private String format;

    public SimpleLogFormatter() {
        format="[%1$tF %1$tT] [%4$-7s] [%7$-20s] %5$s %n";
    }

    public SimpleLogFormatter(String format) {
        this.format = format;
    }

    @Override
    public String format(LogRecord record) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(
                record.getInstant(), ZoneId.systemDefault());
        String source;
        if (record.getSourceClassName() != null) {
            source = record.getSourceClassName();
            if (record.getSourceMethodName() != null) {
                source += " " + record.getSourceMethodName();
            }
        } else {
            source = record.getLoggerName();
        }
        String message = formatMessage(record);
        String throwable = "";
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }
        return String.format(format,
                zdt,
                source,
                record.getLoggerName(),
                record.getLevel().getName(),
                message,
                throwable,Thread.currentThread().getName());
    }
}
