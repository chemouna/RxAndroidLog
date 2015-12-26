package com.mounacheikhna.rxandroidlog;

import com.mounacheikhna.rxandroidlog.Logger.LogLevel;
import com.mounacheikhna.rxandroidlog.OperatorLogging.Builder;
import com.mounacheikhna.rxandroidlog.OperatorLogging.Parameters;

/**
 * Created by cheikhnamouna on 12/14/15.
 *
 */
public final class RxLogging {

  private static final String TAG = "RxLogging";

  private RxLogging() {}


  /**
   * Returns a builder for Log.
   * @return builder
   */
  public static <T> Builder<T> logger() {
    return Parameters.<T> builder().source();
  }

  static void log(Logger logger, String s, LogLevel onNextLogLevel) {
    log(logger, s, onNextLogLevel, null);
  }

  //TODO: later make it accept a debugtree or something similar like timber
  static void log(Logger logger, String msg, LogLevel LogLevel, Throwable t) {

    if (t == null) {
      if (LogLevel == Logger.LogLevel.INFO)
        logger.info(msg);
      else if (LogLevel == Logger.LogLevel.DEBUG)
        logger.debug(msg);
      else if (LogLevel == Logger.LogLevel.WARN)
        logger.warn(msg);
      else if (LogLevel == Logger.LogLevel.ERROR)
        logger.error(msg);
    } else {
      if (LogLevel == Logger.LogLevel.INFO)
        logger.info(msg, t);
      else if (LogLevel == Logger.LogLevel.DEBUG)
        logger.debug(msg, t);
      else if (LogLevel == Logger.LogLevel.WARN)
        logger.warn(msg, t);
      else if (LogLevel == Logger.LogLevel.ERROR)
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
