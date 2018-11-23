import net.noboard.invoker.parallel.ParallelInvoker;

import java.util.Random;

public class Main {
    public static void main(String[] args) {
        // 你可以通过注释下面的部分代码来探索它的功能，
        // 一些重要的说明请到代码中寻找，注释写的很清楚
        ParallelInvoker parallelInvoker = new ParallelInvoker();
        parallelInvoker.call(invoker -> { // call方法启动执行器
            //获取线程数
            ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
            while(threadGroup.getParent() != null){
                threadGroup = threadGroup.getParent();
            }
            int totalThread = threadGroup.activeCount();
            System.out.println(totalThread);
        });

        int i = 0;
        for ( ; i < Math.random() * 1000; i++) {
            parallelInvoker.and(invoker -> {
                System.out.println("go i");
                //获取线程数
                ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
                while(threadGroup.getParent() != null){
                    threadGroup = threadGroup.getParent();
                }
                int totalThread = threadGroup.activeCount();
                System.out.println(totalThread);
            });
        }

        parallelInvoker.then(invoker -> {
            try {
                Thread.sleep(1000000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("then");
        });

        int t = 0;
        for ( ; t < Math.random() * 10; t++) {
            parallelInvoker.and(invoker -> {
                System.out.println("go t");
                //获取线程数
                ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
                while(threadGroup.getParent() != null){
                    threadGroup = threadGroup.getParent();
                }
                int totalThread = threadGroup.activeCount();
                System.out.println(totalThread);
            });
        }

        parallelInvoker.then(invoker -> { // then将前后断开，只有前面的回调全部执行完，且没有发生catched（没有调用reject），才会执行then后的函数，此处调用了reject，所以"then 开始"不会被打印
            System.out.println("then 开始");

            System.out.println("then 结束");
        }).continued().normalEnd(invoker -> {
            System.out.println("normalEnd");
        }).catched(e -> { // continued标识可让主线程不等待被标记点的执行
            System.out.println(e.getMessage());
        }).start();

        System.out.println("main continue");
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("main end " + i + " " + t);
    }
}
