# Poll Dancer


<img src="./docs/img/poll_dancer.png" width="33%">




Poll Dancer is a Java Widget that uses Non-Blocking behavior to hook up to callbacks that 
cannot be called on the same thread as the caller. 

It achieves this through polling an atomic object on a thread and can make the result of the poll available to its caller (on the caller thread)
in an "ansync" way.

## Use Case 1
Use poll dancer to detect play application existence during the startup phase of a Play Server off of the main thread.

--- Main Thread --------------->>>
 \                          /
  \-- Poll Dancer Thread --/

In this case Poll dancer will poll the trigger closure every 100 ms, and when Play.current() doesn't throw
it will trigger poll dancer.
```java
// A Play poll dancer that is triggered when Play Current is available.
new PollDancer(executorService, this::onAppStartup, 100).setPollTrigger(() -> {
  try {
    Play.current();
    return true;
  } catch (Throwable ex) {
    debugIf(() -> ex.getMessage() + ", still waiting for play to start");
    return false;
  }
}).start();

protected void onAppStartup() {
 // when play is actually started this will be called within 100ms accuracy (based on the polling)
}
```

## Use Case 2
External object events can trigger poll dancer (so poll dancer can also be used as a Countdown latch that calls your trigger closure).

```java
ZoolServiceHub zoolServiceHub;

microServicesHub.setHostsLoadedCallback(
          new PollDancer(executorService, this::onZoolHostsLoaded, 20).start()::triggerNow);
          
private void onZoolHostsLoaded() {
   // zool service hosts loaded
   microServiceHub.printKnownHosts();
}
```
