# Invoker

这是一个多任务执行器，注册在其中的所有任务都会在各自的独立线程中去执行，当然执行器内部使用了线程池管理线程的调用，你无需关心线程管理相关的问题。

## 方法

### ParallelInvoker.call()

你必须使用该方法来申明执行器要执行的第一个任务（且，该任务会被添加到第一个任务组中），在它之后可以挂载then，和and。

### .then()

此方法用来注册一个新的任务，且开启一个新的任务组，该任务组只有在前置任务组执行完毕后才会执行。

### .and()

此方法用来注册一个新的任务，到当前任务组中。

### .continued()

此方法用来标记一个**无需等待**的任务组，被标记的任务组不会阻塞线程。即，被标记任务组被启动后，系统会继续执行后续任务组，如果没有后续任务组，则会执行主线程。特别的，该组任务有能力触发reject，如果reject触发时主程序已经进入“正常结束预备状态”或“结束状态”，则catched不会被调用。如果执行器再次开始新一轮执行，而此时上一次执行中的continued任务组触发了reject，则catched会被执行。所以我建议start()方法最好不要多次执行，除非你清楚这样做时系统会如何工作。

### .catched()

此方法用来注册一个异常消费者，当执行器中被注册的任何一个任务执行了reject()方法时，该异常消费者将被调用。

### .normalEnd()

此方法用来注册一个正常结束消费者，当执行器执行完所有任务，且没有任务调用reject()时，正常结束消费者将被调用。值得注意的是，当有任务组被continued标记时，执行器执行结束之时，任务组不一定执行结束，如果直到此时reject()方法依然没有被调用，则正常结束消费者将被调用。且此后被continued的任务组再reject，异常消费者也不会再被调用。

### .start()

此方法用来启动执行器。如果执行器处于“执行中状态”，start方法不会再次启动执行器。

### .reject()

此方法用来退出执行器。

当reject发生时，系统请求进入捕获状态，请求会因为不满足进入捕获状态条件而失败，失败时系统不会调用异常消费者。当系统成功进入捕获状态时，系统打开当前线程锁，但还未打开主线程锁（如果系统start时发现被注册了异常消费者，则会额外添加此主线程锁），此时系统会执行异常消费者，直到异常消费者执行完毕才打开主线程锁。当系统进入捕获状态时，后续任务组不会被启动。

- reject保证的是：
    
    当reject的时候，主线程停止等待；当reject的时候，还未启动的串行任务（线程）将不会被启动；
    
    当reject的时候，catched立即执行；只有第一个到达的reject会执行catched，后续将被拒绝（跳过）；
    
    当reject的时候，主线程会等待直到catched回调执行完毕

- reject不能的是：

    打断当前线程；

    打断并行线程；
    
    打断并行线程的创建（reject不能打断创建一组并行任务，即，一旦开始创建一组并行任务，它们将被成功创建）；

**综上，reject不能打断线程，如果你想让线程提前结束，需要自己使用return语句，但你也仅能打断当前线程**

## 其他

java提供了AtomicBoolean类，我考虑过使用这个类，但当intoRunningStatus方法被并发的调用时，可能出现A线程通过了if判断，B线程也通过了if判断，此时会导致A,B两个线程都能使执行器启动，导致线程不安全。所以（其他“into”方法同理）需要使用synchronized关键字，将进入某种状态时的判断和写入操作改写为原子性的。call方法（及和它类似的then，and等接口）也不能使用AtomicBoolean，当A线程通过了call方法中的if语句，B线程获得了CPU执行，从而开始运行invoke函数，此时A线程再次获得CPU执行，从而修改了invoker chain，这就会导致线程不安全问题。综上，使用synchronized关键字是比较合理的做法。

## 示例

```java

public class Main {
    public static void main(String[] args) {
        new ParallelInvoker.call(invoker -> { // call方法启动执行器
            print("任务组1 任务1");
        }).then(invoker -> {
            try {
                Thread.sleep(800);
                print("任务组2 任务1");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).and(invoker -> {
            print("任务组2 任务2");
        }).continued().then(invoker -> {
            print("任务组3 任务1");
        }).start();
    }

    private static void print(String i) {
        System.out.println(i);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

```