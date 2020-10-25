package net.noboard.invoker.parallel;

import net.noboard.invoker.Caller;
import net.noboard.invoker.CallerWithoutException;
import net.noboard.invoker.Invoker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 并行执行器
 * @author wanxm
 */
public class ParallelInvoker implements Invoker {

    private List<ParallelInvokerChain> chain;

    private ParallelInvokerChain current;

    private Consumer<Exception> catchConsumer;

    private CallerWithoutException normalEnd;

    private CountDownLatch currentChainThreadLock;

    private CountDownLatch mainThreadLock;

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private final Executor executor;

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
    private boolean isInNormalEndPrepareStatus = false;

    public ParallelInvoker () {
        this.executor = executorService;
    }

    public ParallelInvoker (Executor executor) {
        this.executor = executor;
    }

    @Override
    public synchronized Invoker call(Caller consumer) {
        if (!isInRunningStatus) {
            chain = new ArrayList<>();
            current = new ParallelInvokerChain();
            current.addConsumer(consumer);
            chain.add(current);
        }
        return this;
    }

    @Override
    public synchronized Invoker then(Caller consumer) {
        if (!isInRunningStatus) {
            current = new ParallelInvokerChain();
            current.addConsumer(consumer);
            chain.add(current);
        }
        return this;
    }

    @Override
    public synchronized Invoker and(Caller consumer) {
        if (!isInRunningStatus) {
            current.addConsumer(consumer);
        }
        return this;
    }

    @Override
    public synchronized Invoker abnormal(Consumer<Exception> consumer) {
        if (!isInRunningStatus) {
            catchConsumer = consumer;
        }
        return this;
    }

    @Override
    public synchronized Invoker normalEnd(CallerWithoutException consumer) {
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
                    normalEnd.accept();
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
                normalEnd.accept();
            }
        }

        this.intoEndingStatus();
    }

    private void reject(Exception e) {
        if (this.intoCatchStatus()) {
            while (currentChainThreadLock.getCount() > 0) {
                currentChainThreadLock.countDown();
            }
            if (this.catchConsumer != null) {
                this.catchConsumer.accept(e);
                mainThreadLock.countDown();
            }
        }
    }

    /**
     * 请求进入捕获状态，返回成功或者失败
     *
     * 如果不在捕获状态，且在运行中状态，且不在正常结束状态，则可以进入捕获状态（返回true），且将状态置为捕获状态；
     * 如果已经在捕获状态，则不可以再次进入捕获状态（返回false）。
     *
     * @return
     */
    private synchronized boolean intoCatchStatus() {
        if (!this.isInCatchStatus && this.isInRunningStatus && !this.isInNormalEndPrepareStatus) {
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
            this.isInNormalEndPrepareStatus = false;
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
            this.isInNormalEndPrepareStatus = true;
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
                List<Caller> list = parallel.getChain();
                parallel.setCurrentCountDownLatch(new CountDownLatch(list.size()));
                currentChainThreadLock = parallel.getCurrentCountDownLatch();
                for (Caller consumer : list) {
                    executor.execute(() -> {
                        try {
                            consumer.accept();
                        } catch (Exception e) {
                            reject(e);
                        } finally {
                            parallel.getCurrentCountDownLatch().countDown();
                        }
                    });
                }
                if (parallel.isWait()) {
                    parallel.getCurrentCountDownLatch().await();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
