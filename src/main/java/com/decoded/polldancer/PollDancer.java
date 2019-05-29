package com.decoded.polldancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;


/**
 * Poll dancer is a non blocking mechanism for callbacks. Thread A can triggerNow the PollDancer, that will ultimately
 * notify thread b by setting an internal atomic boolean upon which thread B is polling. When the boolean triggers, the
 * onPollMethod called in a non-blocking fashion.
 */
public class PollDancer {
  private static final Logger LOG = LoggerFactory.getLogger(PollDancer.class);
  private final ExecutorService executorService;
  private CountDownLatch triggerLatch;
  private AtomicBoolean trigger = new AtomicBoolean(false);
  private Runnable triggerCallback;
  private Thread pollDancerThread;
  private Supplier<Boolean> pollTrigger = null;
  private long pollIntervalMs = 100;
  // this is big but can be set to whatever is desired for a specific use case.
  private long maxTimeoutMs = 120000;

  // create for each callback you want to invoke from a triggerNow.
  public PollDancer(ExecutorService executorService, Runnable triggerCallback) {
    this.executorService = executorService;
    this.triggerCallback = triggerCallback;
  }

  private static void debugIf(Supplier<String> message) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(message.get());
    }
  }

  /**
   * The interval at which poll dancer will check for the focused object to be updated.
   * @param pollIntervalMs the interval in milliseconds
   * @return this PollDancer
   */
  public PollDancer setPollIntervalMs(long pollIntervalMs) {
    this.pollIntervalMs = pollIntervalMs;
    return this;
  }

  /**
   * Set the max timeout that poll dancer will wait before considering the operation to be "un-ending" and timeout.
   * @param maxTimeoutMs the max milliseconds to wait.
   * @return A {@link PollDancer}
   */
  public PollDancer setMaxTimeoutMs(long maxTimeoutMs) {
    this.maxTimeoutMs = maxTimeoutMs;
    return this;
  }

  /**
   * The Trigger Callback
   * @return PollDancer
   */
  public PollDancer setTriggerCallback() {
    this.triggerCallback = triggerCallback;
    return this;
  }

  /**
   * Adds a wrapped supplier and triggers the poll dancer when the supplier is called.
   *
   * @param triggerFun a triggerNow method that, when called will triggerNow poll dancer
   *
   * @return a Supplier that will return the result of calling the triggerNow method, and if the triggerNow method
   * returns true, the poll dancer is effectively triggered.
   */
  public PollDancer setPollTrigger(Supplier<Boolean> triggerFun) {
    if(pollTrigger != null) {
      LOG.warn("Overwriting previous poll trigger!");
    }
    if (!trigger.get()) {
      Supplier<Boolean> tf = () -> {
        boolean result = triggerFun.get();
        if (result) {
          triggerNow();
        }

        return result;
      };

      pollTrigger = tf;
    }
    return this;
  }

  /**
   * Trigger the poll dancer and stop the polling action.
   */
  public void triggerNow() {
    debugIf(() -> "trigger now!");
    if (triggerLatch != null && triggerLatch.getCount() > 0) {
      triggerLatch.countDown();
    }
    trigger.compareAndSet(false, true);
  }

  /**
   * Starts the polling action (the PollDance)
   * @return this PollDancer instance.
   */
  public PollDancer start() {
    debugIf(() -> "starting...");
    // each triggerNow function must happen (in no particular order) in order
    // for the countdown latch to be triggered to zero.

    executorService.submit(() -> {
      pollDancerThread = Thread.currentThread();
      if (pollTrigger != null) {
        // if already true, triggerNow function will not actually update the value
        debugIf(() -> "Polling");
        long start = System.currentTimeMillis();
        while (!trigger.compareAndSet(false, pollTrigger.get())) {
          if (pollIntervalMs > 2) {
            try {
              debugIf(() -> "Next poll in " + pollIntervalMs + " ms");
              Thread.sleep(pollIntervalMs);
            } catch (InterruptedException ex) {
              LOG.error("Error", ex);
              return;
            }
          }

          if (System.currentTimeMillis() - start > maxTimeoutMs) {
            throw new RuntimeException(new TimeoutException("Timed out"));
          }
        }
        debugIf(() -> "Polling complete running trigger");
      } else {

        debugIf(() -> "Waiting");
        triggerLatch = new CountDownLatch(1);
        try {
          triggerLatch.await(maxTimeoutMs, TimeUnit.MILLISECONDS);
          debugIf(() -> "Countdown complete!");
        } catch (InterruptedException ex) {
          throw new RuntimeException(new TimeoutException("Timed out"));
        }
      }

      triggerCallback.run();
      stop();
    });

    return this;
  }

  /**
   * Stop the poll dancer and clear the state.
   *
   * @return this poll dancer.
   */
  public PollDancer stop() {
    debugIf(() -> "Stopping");
    if (pollDancerThread != null) {
      pollDancerThread.interrupt();
      trigger.compareAndSet(true, false);
      pollTrigger = null;
      pollDancerThread = null;
      triggerLatch = null;
    }

    return this;
  }
}
