package com.mounacheikhna.rxandroidlog;


/**
 * Created by cheikhnamouna on 12/24/15.
 */
public interface Logger {

  enum LogLevel {
    INFO, WARN, DEBUG, ERROR
  }

  void info(String msg);

  void debug(String msg);

  void warn(String msg);

  void error(String msg);

  void info(String msg, Throwable t);

  void debug(String msg, Throwable t);

  void warn(String msg, Throwable t);

  void error(String msg, Throwable t);

}