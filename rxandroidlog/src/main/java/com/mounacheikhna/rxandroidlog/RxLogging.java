package com.mounacheikhna.rxandroidlog;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import rx.Notification;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by cheikhnamouna on 12/14/15.
 *
 * (and it can be even better if it is done like timber where user can plug in any tree)
 *
 */
public final class RxLogging {

  private static final String TAG = "RxLogging";

  private RxLogging() {}

  public enum Level {
    INFO, WARN, DEBUG, ERROR
  }

  public static class Parameters<T> {

    private final Logger logger;
    private final String subscribedMessage;
    private final String unsubscribedMessage;
    private final Level subscribedLevel;
    private final Level unsubscribedLevel;
    private final List<Func1<Observable<Message<T>>, Observable<Message<T>>>> transformations;

    private Parameters(Logger logger, String subscribedMessage, String unsubscribedMessage,
        Level subscribedLevel, Level unsubscribedLevel,
        List<Func1<Observable<Message<T>>, Observable<Message<T>>>> transformations) {
      this.logger = logger;
      this.subscribedMessage = subscribedMessage;
      this.unsubscribedMessage = unsubscribedMessage;
      this.subscribedLevel = subscribedLevel;
      this.unsubscribedLevel = unsubscribedLevel;
      this.transformations = transformations;
    }

    public Logger getLogger() {
      return logger;
    }

    public Level getSubscribedLevel() {
      return subscribedLevel;
    }

    public String getSubscribedMessage() {
      return subscribedMessage;
    }

    public String getUnsubscribedMessage() {
      return unsubscribedMessage;
    }

    public Level getUnsubscribedLevel() {
      return unsubscribedLevel;
    }

    public List<Func1<Observable<Message<T>>, Observable<Message<T>>>> getTransformations() {
      return transformations;
    }

    public static <T> Builder<T> builder() {
      return new Builder<T>();
    }

    public static class Message<T> {
      private final Notification<T> value;
      private final String message;

      public Message(Notification<T> value, String message) {
        super();
        this.value = value;
        this.message = message;
      }

      public Notification<T> value() {
        return value;
      }

      public String message() {
        return message;
      }

      public Message<T> append(String s) {
        if (message.length() > 0)
          return new Message<T>(value, message + ", " + s);
        else
          return new Message<T>(value, message + s);
      }
    }

    public static class Builder<T> {

      private Logger logger;
      private String loggerName;
      private String onCompleteMessage = "onCompleted";
      private String subscribedMessage = "onSubscribe";
      private String unsubscribedMessage = "onUnsubscribe";
      private final boolean logOnNext = true;
      private final boolean logOnError = true;
      private String onErrorFormat = "";
      private String onNextFormat = "";
      private Level onNextLevel = Level.INFO;
      private Level onErrorLevel = Level.ERROR;
      private Level onCompletedLevel = Level.INFO;
      private Level subscribedLevel = Level.DEBUG;
      private Level unsubscribedLevel = Level.DEBUG;
      private Func1<? super T, ?> valueFunction = new Func1<T, T>() {
        @Override
        public T call(T t) {
          return t;
        }
      };
      private boolean logStackTrace = false;
      private boolean logMemory = false;

      private final List<Func1<Observable<Message<T>>, Observable<Message<T>>>> transformations = new ArrayList<Func1<Observable<Message<T>>, Observable<Message<T>>>>();

      public Logger getLogger() {
        if (logger != null)
          return logger;
        else if (loggerName != null)
          return new AndroidLogger(loggerName);
        else {
          return new AndroidLogger();
        }
      }

      private Builder() {
      }

      /**
       * Sets the SLF4J {@link Logger} to be used to do the logging.
       *
       * @param logger
       * @return
       */
      public Builder<T> logger(Logger logger) {
        this.logger = logger;
        return this;
      }

      /**
       * Sets the name of the {@link Logger} to used to do the logging.
       *
       * @param loggerName
       * @return
       */
      public Builder<T> name(String loggerName) {
        this.loggerName = loggerName;
        return this;
      }

      /**
       * Sets the cls to be used to create a {@link Logger} to do the
       * logging.
       *
       * @param cls
       * @return
       */
      public Builder<T> logger(Class<?> cls) {
        return name(cls.getName());
      }

      /**
       * The message to be logged on stream completion.
       *
       * @param onCompleteMessage
       * @return
       */
      public Builder<T> onCompleted(final String onCompleteMessage) {
        this.onCompleteMessage = onCompleteMessage;
        return this;
      }

      /**
       * The message to be logged when subscribed.
       *
       * @param subscribedMessage
       * @return
       */
      public Builder<T> subscribed(String subscribedMessage) {
        this.subscribedMessage = subscribedMessage;
        return this;
      }

      /**
       * The message to be logged when unsubscribed.
       *
       * @param unsubscribedMessage
       * @return
       */
      public Builder<T> unsubscribed(String unsubscribedMessage) {
        this.unsubscribedMessage = unsubscribedMessage;
        return this;
      }

      /**
       * If and only if <tt>logOnNext</tt> is true requests that
       * <i>onNext</i> values are logged.
       *
       * @param logOnNext
       * @return
       */
      public Builder<T> onNext(final boolean logOnNext) {
        transformations.add(new Func1<Observable<Message<T>>, Observable<Message<T>>>() {

          @Override
          public Observable<Message<T>> call(Observable<Message<T>> observable) {
            return observable.filter(new Func1<Message<T>, Boolean>() {

              @Override
              public Boolean call(Message<T> m) {
                return m.value().isOnNext() == logOnNext;
              }
            });

          }
        });
        return this;
      }

      /**
       * If and only if <tt>logOnError</tt> is true requests that
       * <i>onError</i> notifications are logged.
       *
       * @param logOnError
       * @return
       */
      public Builder<T> onError(final boolean logOnError) {
        transformations.add(new Func1<Observable<Message<T>>, Observable<Message<T>>>() {

          @Override
          public Observable<Message<T>> call(Observable<Message<T>> observable) {
            return observable.filter(new Func1<Message<T>, Boolean>() {

              @Override
              public Boolean call(Message<T> m) {
                return m.value().isOnError() == logOnError;
              }
            });

          }
        });
        return this;
      }

      /**
       * Requests that errors are to be prefixed with value of
       * <tt>onErrorPrefix</tt>.
       *
       * @param onErrorPrefix
       * @return
       */
      public Builder<T> onErrorPrefix(String onErrorPrefix) {
        this.onErrorFormat = onErrorPrefix + "%s";
        return this;
      }

      /**
       * Requests that errors are to be formatted using
       * <tt>onErrorFormat</tt> (as in
       * {@link String#format(String, Object...)}.
       *
       * @param onErrorFormat
       * @return
       */
      public Builder<T> onErrorFormat(String onErrorFormat) {
        this.onErrorFormat = onErrorFormat;
        return this;
      }

      public Builder<T> onNextPrefix(String onNextPrefix) {
        this.onNextFormat = onNextPrefix + "%s";
        return this;
      }

      public Builder<T> onNextFormat(String onNextFormat) {
        this.onNextFormat = onNextFormat;
        return this;
      }

      public Builder<T> onNext(Level onNextLevel) {
        this.onNextLevel = onNextLevel;
        return this;
      }

      public Builder<T> onError(Level onErrorLevel) {
        this.onErrorLevel = onErrorLevel;
        return this;
      }

      public Builder<T> onCompleted(Level onCompletedLevel) {
        this.onCompletedLevel = onCompletedLevel;
        return this;
      }

      public Builder<T> subscribed(Level subscribedLevel) {
        this.subscribedLevel = subscribedLevel;
        return this;
      }

      public Builder<T> prefix(String prefix) {
        onNextPrefix(prefix);
        return onErrorPrefix(prefix);
      }

      public Builder<T> unsubscribed(Level unsubscribedLevel) {
        this.unsubscribedLevel = unsubscribedLevel;
        return this;
      }

      public Builder<T> showCount(final String label) {
        return showCount(label, null);
      }

      public Builder<T> showCount(AtomicLong count) {
        return showCount("count", count);
      }

      public Builder<T> showCount(final String label, final AtomicLong count) {
        transformations.add(new Func1<Observable<Message<T>>, Observable<Message<T>>>() {

          @Override
          public Observable<Message<T>> call(Observable<Message<T>> observable) {
            return observable.map(new Func1<Message<T>, Message<T>>() {
              final AtomicLong c = count == null ? new AtomicLong(0) : count;

              @Override
              public Message<T> call(Message<T> m) {
                long val;
                if (m.value().isOnNext())
                  val = c.incrementAndGet();
                else
                  val = c.get();

                return m.append(label + "=" + val);
              }
            });
          }
        });
        return this;
      }

      public Builder<T> showCount() {
        return showCount("count");
      }

      public Builder<T> showRateSince(final String label, final long sinceMs) {
        return showRateSince(label, sinceMs, null);
      }

      public Builder<T> showRateSince(final String label, final long sinceMs,
          final AtomicLong count) {
        transformations.add(new Func1<Observable<Message<T>>, Observable<Message<T>>>() {

          @Override
          public Observable<Message<T>> call(Observable<Message<T>> observable) {
            return observable.map(new Func1<Message<T>, Message<T>>() {
              final AtomicLong c = count == null ? new AtomicLong(0) : count;
              volatile long lastTime = 0;
              volatile long lastNum = 0;
              volatile double rate = 0;

              @Override
              public Message<T> call(Message<T> m) {
                long t = System.currentTimeMillis();
                long num;
                if (m.value().isOnNext()) {
                  num = c.incrementAndGet();
                } else
                  num = c.get();
                long diffMs = t - lastTime;
                if (diffMs >= sinceMs) {
                  rate = ((num - lastNum) * 1000.0 / diffMs);
                  lastTime = t;
                  lastNum = num;
                }
                return m.append(label + "=" + rate);
              }
            });
          }
        });
        return this;
      }

      public Builder<T> showRateSinceStart(final String label) {
        return showRateSinceStart(label, null);
      }

      public Builder<T> showRateSinceStart(final String label, final AtomicLong count) {
        transformations.add(new Func1<Observable<Message<T>>, Observable<Message<T>>>() {

          @Override
          public Observable<Message<T>> call(Observable<Message<T>> observable) {
            return observable.map(new Func1<Message<T>, Message<T>>() {
              final AtomicLong c = count == null ? new AtomicLong(0) : count;
              volatile long startTime = 0;
              volatile double rate = 0;

              @Override
              public Message<T> call(Message<T> m) {
                long t = System.currentTimeMillis();
                if (startTime == 0)
                  startTime = t;
                long num;
                if (m.value().isOnNext())
                  num = c.incrementAndGet();
                else
                  num = c.get();

                long diffMs = t - startTime;
                if (diffMs > 0) {
                  rate = num * 1000.0 / diffMs;
                }
                return m.append(label + "=" + rate);
              }
            });
          }
        });
        return this;
      }

      public Builder<T> every(final int every) {
        return every(every, (AtomicLong) null);
      }

      public Builder<T> every(long duration, TimeUnit unit) {
        if (duration > 0) {
          final long durationMs = unit.toMillis(duration);
          transformations
              .add(new Func1<Observable<Message<T>>, Observable<Message<T>>>() {

                @Override
                public Observable<Message<T>> call(Observable<Message<T>> observable) {
                  return observable.filter(new Func1<Message<T>, Boolean>() {
                    long lastTime = 0;

                    @Override
                    public Boolean call(Message<T> t) {
                      long now = System.currentTimeMillis();
                      if (now - lastTime > durationMs) {
                        lastTime = now;
                        return true;
                      } else
                        return false;
                    }
                  });
                }
              });
        }
        return this;
      }

      public Builder<T> every(final int every, final AtomicLong count) {
        if (every > 1) {
          transformations
              .add(new Func1<Observable<Message<T>>, Observable<Message<T>>>() {

                @Override
                public Observable<Message<T>> call(Observable<Message<T>> observable) {
                  return observable.filter(new Func1<Message<T>, Boolean>() {
                    final AtomicLong c = count == null ? new AtomicLong(0)
                        : count;

                    @Override
                    public Boolean call(Message<T> t) {
                      if (t.value().isOnNext())
                        return c.incrementAndGet() % every == 0;
                      else
                        return true;
                    }
                  });
                }
              });
        }
        return this;
      }

      public Builder<T> showValue(boolean logValue) {
        if (logValue)
          return showValue();
        else
          return excludeValue();
      }

      public Builder<T> showValue() {
        if (onNextFormat.length() == 0)
          onNextFormat = "%s";
        return this;
      }

      public Builder<T> value(Func1<? super T, ?> function) {
        this.valueFunction = function;
        return this;
      }

      public Builder<T> excludeValue() {
        onNextFormat = "";
        return this;
      }

      public Builder<T> showStackTrace() {
        this.logStackTrace = true;
        return this;
      }

      public Builder<T> when(final Func1<? super T, Boolean> when) {
        transformations.add(new Func1<Observable<Message<T>>, Observable<Message<T>>>() {

          @Override
          public Observable<Message<T>> call(Observable<Message<T>> observable) {
            return observable.filter(new Func1<Message<T>, Boolean>() {
              @Override
              public Boolean call(Message<T> t) {
                if (t.value().isOnNext())
                  return when.call(t.value().getValue());
                else
                  return true;
              }
            });
          }
        });
        return this;
      }

      public Builder<T> start(final long start) {
        transformations.add(new Func1<Observable<Message<T>>, Observable<Message<T>>>() {

          @Override
          public Observable<Message<T>> call(Observable<Message<T>> observable) {
            return observable.filter(new Func1<Message<T>, Boolean>() {
              AtomicLong count = new AtomicLong(0);

              @Override
              public Boolean call(Message<T> t) {
                if (t.value().isOnNext())
                  return start <= count.incrementAndGet();
                else
                  return true;
              }
            });
          }
        });
        return this;
      }

      public Builder<T> finish(final long finish) {
        transformations.add(new Func1<Observable<Message<T>>, Observable<Message<T>>>() {

          @Override
          public Observable<Message<T>> call(Observable<Message<T>> observable) {
            return observable.filter(new Func1<Message<T>, Boolean>() {
              AtomicLong count = new AtomicLong(0);

              @Override
              public Boolean call(Message<T> t) {
                if (t.value().isOnNext())
                  return finish >= count.incrementAndGet();
                else
                  return true;
              }
            });
          }
        });
        return this;
      }

      public Builder<T> to(
          final Func1<Observable<? super Message<T>>, Observable<Message<T>>> f) {
        transformations.add(new Func1<Observable<Message<T>>, Observable<Message<T>>>() {

          @Override
          public Observable<Message<T>> call(Observable<Message<T>> observable) {
            return f.call(observable);
          }
        });
        return this;
      }

      public Builder<T> showMemory() {
        logMemory = true;
        return this;
      }

      public OperatorLogging<T> log() {
        transformations.add(new Func1<Observable<Message<T>>, Observable<Message<T>>>() {

          @Override
          public Observable<Message<T>> call(Observable<Message<T>> observable) {
            return observable.doOnNext(log);
          }
        });
        return new OperatorLogging<T>(new Parameters<T>(getLogger(), subscribedMessage,
            unsubscribedMessage, subscribedLevel, unsubscribedLevel, transformations));
      }

      private Builder<T> source() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        String callingClassName = elements[4].getClassName();
        return name(callingClassName);
      }

      private final Action1<Message<T>> log = new Action1<Message<T>>() {

        @Override
        public void call(Message<T> m) {

          if (m.value().isOnCompleted() && onCompleteMessage != null) {
            StringBuilder s = new StringBuilder();
            addDelimited(s, onCompleteMessage);
            addDelimited(s, m.message());
            addMemory(s);
            RxLogging.log(getLogger(), s.toString(), onCompletedLevel, null);
          } else if (m.value().isOnError() && logOnError) {
            StringBuilder s = new StringBuilder();
            addDelimited(s,
                String.format(onErrorFormat, m.value().getThrowable().getMessage()));
            addDelimited(s, m.message());
            addMemory(s);
            RxLogging.log(getLogger(), s.toString(), onErrorLevel, m.value()
                .getThrowable());
          } else if (m.value().isOnNext() && logOnNext) {
            StringBuilder s = new StringBuilder();
            if (onNextFormat.length() > 0)
              s.append(String.format(onNextFormat,
                  String.valueOf(valueFunction.call(m.value().getValue()))));
            addDelimited(s, m.message());
            addMemory(s);
            addStackTrace(s);
            RxLogging.log(getLogger(), s.toString(), onNextLevel, null);
          }
        }

        private void addStackTrace(StringBuilder s) {
          if (logStackTrace) {
            for (StackTraceElement elem : Thread.currentThread().getStackTrace()) {
              s.append("\n    ");
              s.append(elem);
            }
          }
        }

        private void addMemory(StringBuilder s) {
          //if (logMemory)
            //TODO: uncomment & get memory usage
            //addDelimited(s, memoryUsage());
        }

      };

    }
  }

  /**
   * Returns a builder for Log.
   * @return builder
   */
  public static <T> Parameters.Builder<T> logger() {
    return Parameters.<T> builder().source();
  }

  static void log(Logger logger, String s, Level onNextLevel) {
    log(logger, s, onNextLevel, null);
  }

  //TODO: later make it accept a debugtree or something similar like timber
  static void log(Logger logger, String msg, Level level, Throwable t) {

    if (t == null) {
      if (level == Level.INFO)
        logger.info(msg);
      else if (level == Level.DEBUG)
        logger.debug(msg);
      else if (level == Level.WARN)
        logger.warn(msg);
      else if (level == Level.ERROR)
        logger.error(msg);
    } else {
      if (level == Level.INFO)
        logger.info(msg, t);
      else if (level == Level.DEBUG)
        logger.debug(msg, t);
      else if (level == Level.WARN)
        logger.warn(msg, t);
      else if (level == Level.ERROR)
        logger.error(msg, t);
    }
  }

  public interface Logger {

    void info(String msg);

    void debug(String msg);

    void warn(String msg);

    void error(String msg);

    void info(String msg, Throwable t);

    void debug(String msg, Throwable t);

    void warn(String msg, Throwable t);

    void error(String msg, Throwable t);
  }

  static class AndroidLogger implements Logger {

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

  private static void addDelimited(StringBuilder b, String s) {
    if (s.length() > 0) {
      delimiter(b);
      b.append(s);
    }
  }

  private static void delimiter(StringBuilder s) {
    if (s.length() > 0)
      s.append(", ");
  }
}
