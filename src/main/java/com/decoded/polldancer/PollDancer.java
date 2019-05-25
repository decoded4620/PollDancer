package com.decoded.polldancer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.Runnable;
import java.lang.Thread;
import java.lang.InterruptedException;

/**
 * Poll dancer is a non blocking mechanism for callbacks. Thread A can trigger the PollDancer, that will ultimately
 * notify thread b by setting an internal atomic boolean upon which thread B is polling. When the boolean triggers, the onPollMethod
 * called in a non-blocking fashion.
 */
public class PollDancer {
  private AtomicBoolean trigger = new AtomicBoolean(false);
  private final ExecutorService executorService;
  private Runnable onPoll;
  private Thread pollDancerThread;
  
  // create for each callback you want to invoke from a trigger.
  public PollDancer(ExecutorService executorService, Runnable onPoll) {
    this.executorService = executorService;
    this.onPoll = onPoll;
  }

  public void trigger() {
    trigger.set(true);
  }
  public void start() {
    executorService.submit(() -> {
      pollDancerThread = Thread.currentThread();
      while(!trigger.get()) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException ex) {
          return;
        }
      }

      onPoll.run();
    });
  }

  public void stop() {
    if(pollDancerThread != null) {
      pollDancerThread.interrupt();
      pollDancerThread = null;
    }
  }
}
