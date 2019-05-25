# Poll Dancer

<p sytle="text-align: center;">

![alt text](./docs/img/poll_dancer.png)   

</p>


Poll Dancer is a Java Widget that uses Non-Blocking behavior to hook up to callbacks that 
cannot be called on the same thread as the caller. 

It achieves this through polling an atomic object on a thread and can make the result of the poll available to its caller (on the caller thread)
in an "ansync" way.
