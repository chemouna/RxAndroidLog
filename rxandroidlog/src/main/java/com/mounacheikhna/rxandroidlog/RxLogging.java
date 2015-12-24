package com.mounacheikhna.rxandroidlog;

import com.mounacheikhna.rxandroidlog.Logger.LogLevel;

/**
 * Created by cheikhnamouna on 12/14/15.
 *
 * (and it can be even better if it is done like timber where user can plug in any tree)
 *
 */
public final class RxLogging {

  private static final String TAG = "RxLogging";

  private RxLogging() {}


  /**
   * Returns a builder for Log.
   * @return builder
   */
  public static <T> Parameters.Builder<T> logger() {
    return Parameters.<T> builder().source();
  }

  static void log(Logger logger, String s, LogLevel onNextLogLevel) {
    log(logger, s, onNextLogLevel, null);
  }

  //TODO: later make it accept a debugtree or something similar like timber
  static void log(Logger logger, String msg, LogLevel LogLevel, Throwable t) {

    if (t == null) {
      if (LogLevel == LogLevel.INFO)
        logger.info(msg);
      else if (LogLevel == LogLevel.DEBUG)
        logger.debug(msg);
      else if (LogLevel == LogLevel.WARN)
        logger.warn(msg);
      else if (LogLevel == LogLevel.ERROR)
        logger.error(msg);
    } else {
      if (LogLevel == LogLevel.INFO)
        logger.info(msg, t);
      else if (LogLevel == LogLevel.DEBUG)
        logger.debug(msg, t);
      else if (LogLevel == LogLevel.WARN)
        logger.warn(msg, t);
      else if (LogLevel == LogLevel.ERROR)
        logger.error(msg, t);
    }
  }

  static void addDelimited(StringBuilder b, String s) {
    if (s.length() > 0) {
      delimiter(b);
      b.append(s);
    }
  }

  static void delimiter(StringBuilder s) {
    if (s.length() > 0)
      s.append(", ");
  }
}
