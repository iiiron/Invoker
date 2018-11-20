package net.noboard.invoker.parallel;

import net.noboard.invoker.Invoker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * 并行执行器
 *
 * reject保证的是：
 * 当reject的时候，主线程停止等待；当reject的时候，还未启动的串行任务（线程）将不会被启动；
 * 当reject的时候，catched立即执行；只有第一个到达的reject会执行catched，后续将被拒绝（跳过）；
 * 当reject的时候，主线程会等待直到catched回调执行完毕
 *
 * reject不能的是：
 * 打断当前线程；
 * 打断并行线程；
 * 打断并行线程的创建（reject不能打断创建一组并行任务，即，一旦开始创建一组并行任务，它们将被成功创建）；
 * ## 综上，reject不能打断线程，如果你想让线程提前结束，需要自己使用return语句，但你也仅能打断当前线程 ##
 *
 * continued用来标记一组并行任务，该组任务的执行不会阻塞主线程，但该组任务有能力触发reject。
 * 如果reject触发时主程序已经进入“正常结束预备状态”或“结束状态”，则catched不会被调用。
 * 注意：如果执行器再次开始新一轮执行，而此时上一次执行中的continued任务组触发了reject，则catched
 * 会被执行。
 *
 * 如果执行器处于“执行中状态”，start方法不会再次启动执行器
 *
 * ## 说明
 *
 * java提供了AtomicBoolean类，我考虑过使用这个类，但当intoRunningStatus方法被并发
 * 的调用时，可能出现A线程通过了if判断，B线程也通过了if判断，此时会导致A,B两个线程
 * 都能使执行器启动，导致线程不安全。所以（其他“into”方法同理）需要使用synchronized
 * 关键字，将进入某种状态时的判断和写入操作改写为原子性的。call方法（及和它类似的then，
 * and等接口）也不能使用AtomicBoolean，当A线程通过了call方法中的if语句，B线程获得了CPU
 * 执行，从而开始运行invoke函数，此时A线程再次获得CPU执行，从而修改了invoker chain，这
 * 就会导致线程不安全问题。综上，使用synchronized关键字是比较合理的做法。
 *
 * @author wanxm
 */
public class ParallelInvoker implements Invoker {

    private List<ParallelInvokerChain> chain;

    private ParallelInvokerChain current;

    private Consumer<Exception> catchConsumer;

    private Consumer<Invoker> normalEnd;

    private CountDownLatch currentChainThreadLock;

    private CountDownLatch mainThreadLock;

    /**
     * 是否处在捕获状态
     */
    private boolean isInCatchStatus = false;

    /**
     * 是否处在执行中状态
     */
    private boolean isInRunningStatus = false;

    /**
     * 是否处在正常结束预备状态
     */
    private boolean isInNormalEndPreparStatus = false;

    @Override
    public synchronized Invoker call(Consumer<Invoker> consumer) {
        if (!isInRunningStatus) {
            chain = new ArrayList<>();
            current = new ParallelInvokerChain();
            current.addConsumer(consumer);
            chain.add(current);
        }
        return this;
    }

    @Override
    public synchronized Invoker then(Consumer<Invoker> consumer) {
        if (!isInRunningStatus) {
            current = new ParallelInvokerChain();
            current.addConsumer(consumer);
            chain.add(current);
        }
        return this;
    }

    @Override
    public synchronized Invoker and(Consumer<Invoker> consumer) {
        if (!isInRunningStatus) {
            current.addConsumer(consumer);
        }
        return this;
    }

    @Override
    public synchronized Invoker catched(Consumer<Exception> consumer) {
        if (!isInRunningStatus) {
            catchConsumer = consumer;
        }
        return this;
    }

    @Override
    public synchronized Invoker normalEnd(Consumer<Invoker> consumer) {
        if (!isInRunningStatus) {
            normalEnd = consumer;
        }
        return this;
    }

    @Override
    public synchronized Invoker continued() {
        if (!isInRunningStatus) {
            current.setWait(false);
        }
        return this;
    }

    @Override
    public void start() {
        if (!this.intoRunningStatus()) {
            return;
        }

        if (this.catchConsumer != null) {
            mainThreadLock = new CountDownLatch(1);
            this.invoke(this.chain);
            if (this.intoNormalEndPreparStatus()) {
                if (this.normalEnd != null) {
                    normalEnd.accept(this);
                }
            } else {
                try {
                    mainThreadLock.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            this.invoke(this.chain);
            if (this.intoNormalEndPreparStatus() && this.normalEnd != null) {
                normalEnd.accept(this);
            }
        }

        this.intoEndingStatus();
    }

    @Override
    public void reject(Exception e) {
        if (this.intoCatchStatus()) {
            this.onReject(e);
            if (this.catchConsumer != null) {
                this.catchConsumer.accept(e);
                mainThreadLock.countDown();
            }
        }
    }

    /**
     * 请求进入捕获状态，返回成功或者失败
     *
     * 如果不在捕获状态，且在运行中状态，则可以进入捕获状态（返回true），且将状态置为捕获状态；
     * 如果已经在捕获状态，则不可以再次进入捕获状态（返回false）。
     *
     * @return
     */
    private synchronized boolean intoCatchStatus() {
        if (!this.isInCatchStatus && this.isInRunningStatus && !this.isInNormalEndPreparStatus) {
            this.isInCatchStatus = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * 请求进入运行状态，返回成功或者失败
     *
     * @return
     */
    private synchronized boolean intoRunningStatus() {
        if (!this.isInRunningStatus) {
            this.isInRunningStatus = true;
            this.isInCatchStatus = false;
            this.isInNormalEndPreparStatus = false;
            return true;
        } else {
            return false;
        }
    }

    /**
     * 请求进入正常结束预备状态（正常结束前会尝试执行normalEnd回调，直到该回调执行完成才正真进入结束状态）
     *
     * 系统不处在捕获状态 && 系统处在运行状态 => 才进入正常结束预备状态
     */
    private synchronized boolean intoNormalEndPreparStatus() {
        if (!this.isInCatchStatus && this.isInRunningStatus) {
            this.isInNormalEndPreparStatus = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * 进入结束状态
     */
    private synchronized void intoEndingStatus() {
        this.isInRunningStatus = false;
    }

    private synchronized boolean isInRunningStatus() {
        return this.isInRunningStatus;
    }

    private synchronized boolean isInCatchStatus() {
        return this.isInCatchStatus;
    }

    private void invoke(List<ParallelInvokerChain> chain) {
        for (ParallelInvokerChain parallel : chain) {
            if (this.isInCatchStatus()) {
                return;
            }

            try {
                List<Consumer<Invoker>> list = parallel.getChain();
                parallel.setCurrentCountDownLatch(new CountDownLatch(list.size()));
                currentChainThreadLock = parallel.getCurrentCountDownLatch();
                for (Consumer<Invoker> consumer : list) {
                    new Thread(() -> {
                        try {
                            consumer.accept(this);
                        } catch (Exception e) {
                            e.printStackTrace();
                            reject(e);
                        } finally {
                            parallel.getCurrentCountDownLatch().countDown();
                        }
                    }).start();
                }
                if (parallel.isWait()) {
                    parallel.getCurrentCountDownLatch().await();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 打开主线程锁，让主线程继续执行
     *
     * 在调用catched consumer之前调用此方法。因为e会被传递给catched consumer，
     * 在catched consumer之前调用此方法可以保证它所接收的e和reject时的e是一致的。
     * @param e
     */
    private void onReject(Exception e) {
        while (currentChainThreadLock.getCount() > 0) {
            currentChainThreadLock.countDown();
        }
    }
}
