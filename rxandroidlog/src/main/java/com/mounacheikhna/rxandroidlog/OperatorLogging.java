package com.mounacheikhna.rxandroidlog;

import com.mounacheikhna.rxandroidlog.OperatorLogging.Parameters.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import rx.Notification;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.internal.operators.OperatorDoOnSubscribe;
import rx.internal.operators.OperatorDoOnUnsubscribe;

import static com.mounacheikhna.rxandroidlog.RxLogging.log;

/**
 * Created by cheikhnamouna on 12/14/15.
 */
public final class OperatorLogging<T> implements Observable.Operator<T, T> {

  private final Parameters<T> parameters;

  public OperatorLogging(Parameters<T> parameters) {
    this.parameters = parameters;
  }

  @Override
  public Subscriber<? super T> call(Subscriber<? super T> child) {
    // create the subject and an observable from the subject that
    // materializes the notifications from the subject
    PublishSubjectSingleSubscriber<T> subject = PublishSubjectSingleSubscriber.create();

    // create the logging observable
    Observable<Message<T>> observable = createObservableFromSubject(subject);

    // apply all the logging stream transformations
    for (Func1<Observable<Message<T>>, Observable<Message<T>>> transformation : parameters
        .getTransformations()) {
      observable = transformation.call(observable);
    }

    Action0 unsubscriptionLogger = createUnsubscriptionAction(parameters);
    Action0 subscriptionLogger = createSubscriptionAction(parameters);
    observable = observable
        // add subscription action
        .lift(new OperatorDoOnSubscribe<Message<T>>(subscriptionLogger))
        // add unsubscription action
        .lift(new OperatorDoOnUnsubscribe<Message<T>>(unsubscriptionLogger));

    // create parent subscriber
    Subscriber<T> parent = createParentSubscriber(subject, child);

    // create subscriber for the logging observable
    Subscriber<Message<T>> logSubscriber = createErrorLoggingSubscriber(parameters.getLogger());

    // ensure logSubscriber is unsubscribed when child is unsubscribed
    child.add(logSubscriber);

    // subscribe to the logging observable
    observable.unsafeSubscribe(logSubscriber);

    return parent;
  }

  private static <T> Subscriber<Message<T>> createErrorLoggingSubscriber(final Logger logger) {
    return new Subscriber<Message<T>>() {

      @Override
      public void onCompleted() {
        // ignore
      }

      @Override
      public void onError(Throwable e) {
        logger.error("the logging transformations through an exception: " + e.getMessage(),
            e);
      }

      @Override
      public void onNext(Message<T> t) {
        // ignore
      }
    };
  }

  private static <T> Observable<Message<T>> createObservableFromSubject(
      PublishSubjectSingleSubscriber<T> subject) {
    return subject.materialize().map(n -> new Message<T>(n, ""));
  }

  private static <T> Action0 createUnsubscriptionAction(final Parameters<T> p) {
    return () -> {
      // log unsubscription if requested
      if (p.getUnsubscribedMessage() != null)
        log(p.getLogger(), p.getUnsubscribedMessage(), p.getUnsubscribedLogLevel(), null);
    };
  }

  private static <T> Action0 createSubscriptionAction(final Parameters<T> p) {
    return () -> {
      // log subscription if requested
      if (p.getSubscribedMessage() != null)
        log(p.getLogger(), p.getSubscribedMessage(), p.getSubscribedLogLevel(), null);
    };
  }

  private static <T> Subscriber<T> createParentSubscriber(
      final PublishSubjectSingleSubscriber<T> subject, final Subscriber<? super T> child) {
    return new Subscriber<T>(child) {

      @Override
      public void onCompleted() {
        subject.onCompleted();
        child.onCompleted();
      }

      @Override
      public void onError(Throwable e) {
        subject.onError(e);
        child.onError(e);
      }

      @Override
      public void onNext(T t) {
        subject.onNext(t);
        child.onNext(t);
      }
    };
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
    private Logger.LogLevel onNextLogLevel = Logger.LogLevel.INFO;
    private Logger.LogLevel onErrorLogLevel = Logger.LogLevel.ERROR;
    private Logger.LogLevel onCompletedLogLevel = Logger.LogLevel.INFO;
    private Logger.LogLevel subscribedLogLevel = Logger.LogLevel.DEBUG;
    private Logger.LogLevel unsubscribedLogLevel = Logger.LogLevel.DEBUG;
    private Func1<? super T, ?> valueFunction = t -> t;
    private boolean logStackTrace = false;
    private boolean logMemory = false;

    private List<Func1<Observable<Message<T>>, Observable<Message<T>>>>
        transformations = new ArrayList<>();

    public Logger getLogger() {
      if (logger != null)
        return logger;
      else if (loggerName != null)
        return new AndroidLogger(loggerName);
      else {
        return new AndroidLogger();
      }
    }

    Builder() {
    }

    /**
     * Sets the {@link Logger} to be used to do the logging.
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

    public Builder<T> onNext(Logger.LogLevel onNextLogLevel) {
      this.onNextLogLevel = onNextLogLevel;
      return this;
    }

    public Builder<T> onError(Logger.LogLevel onErrorLogLevel) {
      this.onErrorLogLevel = onErrorLogLevel;
      return this;
    }

    public Builder<T> onCompleted(Logger.LogLevel onCompletedLogLevel) {
      this.onCompletedLogLevel = onCompletedLogLevel;
      return this;
    }

    public Builder<T> subscribed(Logger.LogLevel subscribedLogLevel) {
      this.subscribedLogLevel = subscribedLogLevel;
      return this;
    }

    public Builder<T> prefix(String prefix) {
      onNextPrefix(prefix);
      return onErrorPrefix(prefix);
    }

    public Builder<T> unsubscribed(Logger.LogLevel unsubscribedLogLevel) {
      this.unsubscribedLogLevel = unsubscribedLogLevel;
      return this;
    }

    Builder<T> transformations(List<Func1<Observable<Message<T>>, Observable<Message<T>>>> transformations) {
      this.transformations = transformations;
      return this;
    }

    public Builder<T> showCount(final String label) {
      return showCount(label, null);
    }

    public Builder<T> showCount(AtomicLong count) {
      return showCount("count", count);
    }

    /**
     * Use to show a count of onNext events.
     * @param label to describe the count.
     * @param count count of onNext events.
     * @return {@link Builder} for OperatorLogging.
     */
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

    /**
     * Show the rate at which events are emitted with onNext since a time specified.
     *
     * @param label to show with logs.
     * @param sinceMs time to calculate rate since.
     * @param count for number of onNext events.
     * @return {@link Builder}
     */
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
            .add(observable -> observable.filter(new Func1<Message<T>, Boolean>() {
              final AtomicLong c = count == null ? new AtomicLong(0)
                  : count;

              @Override
              public Boolean call(Message<T> t) {
                if (t.value().isOnNext())
                  return c.incrementAndGet() % every == 0;
                else
                  return true;
              }
            }));
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
      transformations.add(observable -> observable.filter((Func1<Message<T>, Boolean>) t -> {
        if (t.value().isOnNext())
          return when.call(t.value().getValue());
        else
          return true;
      }));
      return this;
    }

    public Builder<T> start(final long start) {
      transformations.add(observable -> observable.filter(new Func1<Message<T>, Boolean>() {
        AtomicLong count = new AtomicLong(0);

        @Override
        public Boolean call(Message<T> t) {
          if (t.value().isOnNext())
            return start <= count.incrementAndGet();
          else
            return true;
        }
      }));
      return this;
    }

    public Builder<T> finish(final long finish) {
      transformations.add(observable -> observable.filter(new Func1<Message<T>, Boolean>() {
        AtomicLong count = new AtomicLong(0);

        @Override
        public Boolean call(Message<T> t) {
          if (t.value().isOnNext())
            return finish >= count.incrementAndGet();
          else
            return true;
        }
      }));
      return this;
    }

    public Builder<T> to(
        final Func1<Observable<? super Message<T>>, Observable<Message<T>>> f) {
      transformations.add(observable -> f.call(observable));
      return this;
    }

    public Builder<T> showMemory() {
      logMemory = true;
      return this;
    }

    public OperatorLogging<T> log(String tag) {
      return new Builder<T>().name(tag).logger(getLogger())
              .subscribed(subscribedMessage)
              .unsubscribed(unsubscribedMessage)
              .subscribed(subscribedLogLevel)
              .unsubscribed(unsubscribedLogLevel)
              .transformations(transformations)
    }

    public OperatorLogging<T> log() {
      transformations.add(observable -> observable.doOnNext(log));
      return new OperatorLogging<>(new Parameters<>(getLogger(), subscribedMessage,
              unsubscribedMessage, subscribedLogLevel, unsubscribedLogLevel, transformations));
    }

    Builder<T> source() {
      StackTraceElement[] elements = Thread.currentThread().getStackTrace();
      String callingClassName = elements[4].getClassName();
      return name(callingClassName);
    }

    private final Action1<Message<T>> log = new Action1<Message<T>>() {

      @Override
      public void call(Message<T> m) {

        if (m.value().isOnCompleted() && onCompleteMessage != null) {
          StringBuilder s = new StringBuilder();
          RxLogging.addDelimited(s, onCompleteMessage);
          RxLogging.addDelimited(s, m.message());
          addMemory(s);
          RxLogging.log(getLogger(), s.toString(), onCompletedLogLevel, null);
        } else if (m.value().isOnError() && logOnError) {
          StringBuilder s = new StringBuilder();
          RxLogging.addDelimited(s,
              String.format(onErrorFormat, m.value().getThrowable().getMessage()));
          RxLogging.addDelimited(s, m.message());
          addMemory(s);
          RxLogging.log(getLogger(), s.toString(), onErrorLogLevel, m.value()
              .getThrowable());
        } else if (m.value().isOnNext() && logOnNext) {
          StringBuilder s = new StringBuilder();
          if (onNextFormat.length() > 0)
            s.append(String.format(onNextFormat,
                String.valueOf(valueFunction.call(m.value().getValue()))));
          RxLogging.addDelimited(s, m.message());
          addMemory(s);
          addStackTrace(s);
          RxLogging.log(getLogger(), s.toString(), onNextLogLevel, null);
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

  public static class Parameters<T> {

    private final Logger logger;
    private final String subscribedMessage;
    private final String unsubscribedMessage;
    private final Logger.LogLevel mSubscribedLogLevel;
    private final Logger.LogLevel mUnsubscribedLogLevel;
    private final List<Func1<Observable<Message<T>>, Observable<Message<T>>>> transformations;

    Parameters(Logger logger, String subscribedMessage, String unsubscribedMessage,
        Logger.LogLevel subscribedLogLevel, Logger.LogLevel unsubscribedLogLevel,
        List<Func1<Observable<Message<T>>, Observable<Message<T>>>> transformations) {
      this.logger = logger;
      this.subscribedMessage = subscribedMessage;
      this.unsubscribedMessage = unsubscribedMessage;
      this.mSubscribedLogLevel = subscribedLogLevel;
      this.mUnsubscribedLogLevel = unsubscribedLogLevel;
      this.transformations = transformations;
    }

    public Logger getLogger() {
      return logger;
    }

    public Logger.LogLevel getSubscribedLogLevel() {
      return mSubscribedLogLevel;
    }

    public String getSubscribedMessage() {
      return subscribedMessage;
    }

    public String getUnsubscribedMessage() {
      return unsubscribedMessage;
    }

    public Logger.LogLevel getUnsubscribedLogLevel() {
      return mUnsubscribedLogLevel;
    }

    public List<Func1<Observable<Message<T>>, Observable<Message<T>>>> getTransformations() {
      return transformations;
    }

    public static <T> Builder<T> builder() {
      return new Builder<>();
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
          return new Message<>(value, message + ", " + s);
        else
          return new Message<>(value, message + s);
      }
    }

  }

}