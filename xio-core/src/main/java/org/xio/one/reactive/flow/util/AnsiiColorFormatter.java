package org.xio.one.reactive.flow.util;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class AnsiiColorFormatter extends SimpleLogFormatter {

  private boolean ansiColor;
  private HashMap<Level, AnsiColor> colors;
  private AnsiColor loggerColor;

  public AnsiiColorFormatter() {
    LogManager manager = LogManager.getLogManager();
    String color = manager.getProperty(AnsiiColorFormatter.class.getCanonicalName() + ".ansiColor");
    if ("true".equals(color)) {
      ansiColor = true;
    }
    colors = new HashMap<>();
    colors.put(Level.INFO, AnsiColor.BOLD_INTENSE_GREEN);
    colors.put(Level.WARNING, AnsiColor.BOLD_INTENSE_YELLOW);
    colors.put(Level.SEVERE, AnsiColor.BOLD_INTENSE_RED);
    loggerColor = AnsiColor.BOLD_INTENSE_BLUE;
    String infoColor = manager.getProperty(this.getClass().getCanonicalName()+".infoColor");
    if (infoColor != null) {
      try {
        colors.put(Level.INFO, AnsiColor.valueOf(infoColor));
      }catch (IllegalArgumentException iae) {
        colors.put(Level.INFO, AnsiColor.BOLD_INTENSE_GREEN);
      }
    }
    String colorProp = manager.getProperty(this.getClass().getCanonicalName()+".warnColor");
    if (colorProp != null) {
      try {
        colors.put(Level.WARNING, AnsiColor.valueOf(colorProp));
      }catch (IllegalArgumentException iae) {
        colors.put(Level.WARNING, AnsiColor.BOLD_INTENSE_YELLOW);
      }
    }
    colorProp = manager.getProperty(this.getClass().getCanonicalName()+".severeColor");
    if (colorProp != null) {
      try {
        colors.put(Level.SEVERE, AnsiColor.valueOf(colorProp));
      }catch (IllegalArgumentException iae) {
        colors.put(Level.SEVERE, AnsiColor.BOLD_INTENSE_RED);
      }
    }

    colorProp = manager.getProperty(this.getClass().getCanonicalName()+".loggerColor");
    if (colorProp != null) {
      try {
        loggerColor = AnsiColor.valueOf(colorProp);
      }catch (IllegalArgumentException iae) {
        loggerColor = AnsiColor.BOLD_INTENSE_BLUE;
      }
    }

  }

  public AnsiColor getLoggerColor() {
    return loggerColor;
  }

  protected boolean color() {
    return ansiColor;
  }

  public void noAnsi(){
    ansiColor = false;
  }

  protected AnsiColor getColor(Level level) {
    AnsiColor result = colors.get(level);
    if (result == null) {
      result = AnsiColor.NOTHING;
    }
    return result;
  }

  protected AnsiColor getReset() {
    return AnsiColor.RESET;
  }

  @Override
  public String format(LogRecord record) {
     return getColor(record.getLevel()) + super.format(record);
  }

  @Override
  public String formatMessage(LogRecord record) {
    return getColor(record.getLevel())  + super.formatMessage(record);
  }
}
