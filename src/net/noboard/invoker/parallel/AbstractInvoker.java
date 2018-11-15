package net.noboard.invoker.parallel;

import net.noboard.invoker.Invoker;
import net.noboard.invoker.parallel.ParallelInvokerChain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public abstract class AbstractInvoker implements Invoker {

    private List<ParallelInvokerChain> chain;

    private ParallelInvokerChain current;

    private Consumer<Exception> catchConsumer;

    private Consumer<Invoker> normalEnd;

    /**
     * 是否已经处理catch事件
     */
    private boolean isCatched = false;

    /**
     * 是否正在执行中
     */
    private boolean isRunning = false;

    /**
     * 主线程继续的条件是：invoke执行结束；catchConsumer执行结束。
     */
    private CountDownLatch mainThreadCountDown;

    @Override
    public Invoker call(Consumer<Invoker> consumer) {
        chain = new ArrayList<>();
        current = new ParallelInvokerChain();
        current.addConsumer(consumer);
        chain.add(current);
        return this;
    }

    @Override
    public Invoker then(Consumer<Invoker> consumer) {
        current = new ParallelInvokerChain();
        current.addConsumer(consumer);
        chain.add(current);
        return this;
    }

    @Override
    public Invoker and(Consumer<Invoker> consumer) {
        current.addConsumer(consumer);
        return this;
    }

    @Override
    public Invoker catched(Consumer<Exception> consumer) {
        catchConsumer = consumer;
        return this;
    }

    @Override
    public Invoker normalEnd(Consumer<Invoker> consumer) {
        normalEnd = consumer;
        return this;
    }

    @Override
    public Invoker continued() {
        current.setWait(false);
        return this;
    }

    /**
     * 当invoke执行过程中，发生了reject，则invoke结束时需要等待执行catchConsumer的线程。
     */
    @Override
    public void start() {
        if (!this.intoRunning()) {
            return;
        }

        if (this.catchConsumer != null) {
            mainThreadCountDown = new CountDownLatch(1);
            this.invoke(this.chain);
            if (this.isCatched) {
                try {
                    mainThreadCountDown.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            this.invoke(this.chain);
        }

        if (!isCatched && normalEnd != null) {
            normalEnd.accept(this);
        }

        this.isRunning = false;
    }

    /**
     * reject不能打断任何线程的执行，它所能保证的只是：
     * catchConsumer立即执行；
     * 主线程一定等待catchConsumer的执行；
     * catchConsumer只执行一次。
     *
     * @param e
     */
    @Override
    public void reject(Exception e) {
        if (this.intoCatch()) {
            this.onReject(e);
            if (this.catchConsumer != null) {
                this.catchConsumer.accept(e);
            }
            mainThreadCountDown.countDown();
        }
    }

    /**
     * 请求进入捕获状态，返回成功或者失败
     *
     * 如果不在捕获状态，则可以进入捕获状态（返回true），且将状态置为捕获状态；
     * 如果已经在捕获状态，则不可以再次进入捕获状态（返回false）。
     *
     * @return
     */
    private synchronized boolean intoCatch() {
        if (!this.isCatched) {
            this.isCatched = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * 请求进入运行状态，返回成功或者失败
     * @return
     */
    private synchronized boolean intoRunning() {
        if (!this.isRunning) {
            this.isRunning = true;
            this.isCatched = false;
            return true;
        } else {
            return false;
        }
    }

    protected synchronized boolean isRunning() {
        return this.isRunning;
    }

    protected synchronized boolean isCatched() {
        return this.isCatched;
    }

    abstract protected void invoke(List<ParallelInvokerChain> chain);

    /**
     * 在调用catched consumer之前调用此方法。因为e会被传递给catched consumer，
     * 在catched consumer之前调用此方法可以保证它所接收的e和reject时的e是一致的。
     * @param e
     */
    abstract protected void onReject(Exception e);
}
