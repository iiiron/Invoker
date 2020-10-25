package net.noboard.invoker.parallel;

import net.noboard.invoker.Caller;
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

    private List<Caller> chain = new ArrayList<>();

    private CountDownLatch currentCountDownLatch;

    public void addConsumer(Caller caller) {
        chain.add(caller);
    }

    public boolean isWait() {
        return isWait;
    }

    public void setWait(boolean wait) {
        isWait = wait;
    }

    public List<Caller> getChain() {
        return chain;
    }

    public void setChain(List<Caller> chain) {
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
