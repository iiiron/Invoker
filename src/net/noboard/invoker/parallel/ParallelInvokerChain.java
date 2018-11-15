package net.noboard.invoker.parallel;

import net.noboard.invoker.Invoker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * 并行执行链
 */
public class ParallelInvokerChain {
    /**
     * 主线程是否等待标志位
     */
    private boolean isWait = true;

    private List<Consumer<Invoker>> chain = new ArrayList<>();

    private CountDownLatch currentCountDownLatch;

    public void addConsumer(Consumer<Invoker> consumer) {
        chain.add(consumer);
    }

    public boolean isWait() {
        return isWait;
    }

    public void setWait(boolean wait) {
        isWait = wait;
    }

    public List<Consumer<Invoker>> getChain() {
        return chain;
    }

    public void setChain(List<Consumer<Invoker>> chain) {
        this.chain = chain;
    }

    public CountDownLatch getCurrentCountDownLatch() {
        return currentCountDownLatch;
    }

    public void setCurrentCountDownLatch(CountDownLatch currentCountDownLatch) {
        this.currentCountDownLatch = currentCountDownLatch;
    }

    @Override
    public String toString() {
        return "ParallelInvokerChain{" +
                "isWait=" + isWait +
                ", chain=" + chain +
                ", currentCountDownLatch=" + currentCountDownLatch +
                '}';
    }
}
