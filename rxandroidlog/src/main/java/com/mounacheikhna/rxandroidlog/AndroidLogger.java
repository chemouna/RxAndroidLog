package com.mounacheikhna.rxandroidlog;

import android.util.Log;

/**
 * Created by cheikhnamouna on 12/24/15.
 */
class AndroidLogger implements Logger {

  private final String ST_TAG = "AndroidLogger";
  private final String tag;

  public AndroidLogger(String tag) {
    this.tag = tag;
  }

  public AndroidLogger() {
    tag = ST_TAG;
  }

  @Override public void info(String msg) {
    Log.i(tag, msg);
  }

  @Override public void debug(String msg) {
    Log.d(tag, msg);
  }

  @Override public void warn(String msg) {
    Log.d(tag, msg);
  }

  @Override public void error(String msg) {
    Log.e(tag, msg);
  }

  @Override public void info(String msg, Throwable t) {
    Log.i(tag, msg, t);
  }

  @Override public void debug(String msg, Throwable t) {
    Log.d(tag, msg, t);
  }

  @Override public void warn(String msg, Throwable t) {
    Log.w(tag, msg, t);
  }

  @Override public void error(String msg, Throwable t) {
    Log.e(tag, msg, t);
  }
}
