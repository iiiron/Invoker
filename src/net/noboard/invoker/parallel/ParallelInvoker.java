package net.noboard.invoker.parallel;

import net.noboard.invoker.Invoker;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * 并行执行器
 *
 * reject保证的是：
 * 当reject的时候，主线程停止等待；当reject的时候，还未启动的串行任务（线程）将不会被启动；
 * 当reject的时候，catched立即执行；只有第一个到达的reject会执行catched，后续将被拒绝（跳过）；
 *
 * reject不能的是：
 * 打断当前线程；打断并行线程；打断并行线程的创建（reject不能打断创建一组并行任务，即，
 * 一旦开始创建一组并行任务，它们将被成功创建）；
 *
 * continued用来标记一组并行任务，该组任务的执行不会阻塞主线程，但该组任务有能力触发reject。
 * 当被continued标记的任务组触发reject时，catched会被立即调用，正在阻塞主线程的任务会释放线程锁，让主线程继续
 * 执行。
 *
 * 当continued标记参与进来时，会出现主线程已经在执行后续代码的时候，catched被调用的情况，
 * 你要注意这种情况下给程序带来的副作用。可以保证的是，当没有continued标记任何任务时，主
 * 程序一定会在catched被调用后才向后执行。
 *
 * @author wanxm
 */
public class ParallelInvoker extends AbstractInvoker {

    private CountDownLatch currentCountDownLatch;

    @Override
    protected void invoke(List<ParallelInvokerChain> chain) {
        for (ParallelInvokerChain parallel : chain) {
            if (this.isInCatchStatus()) {
                return;
            }

            try {
                List<Consumer<Invoker>> list = parallel.getChain();
                parallel.setCurrentCountDownLatch(new CountDownLatch(list.size()));
                currentCountDownLatch = parallel.getCurrentCountDownLatch();
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
     * 当reject时，将主线程锁打开
     */
    @Override
    protected void onReject(Exception e) {
        while (currentCountDownLatch.getCount() > 0) {
            currentCountDownLatch.countDown();
        }
    }
}
