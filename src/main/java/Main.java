import net.noboard.invoker.parallel.ParallelInvoker;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    public static void main(String[] args) {
        AtomicBoolean atomicBoolean = new AtomicBoolean(true);
        ParallelInvoker parallelInvoker = new ParallelInvoker();
        parallelInvoker.call(() -> { // call方法启动执行器
            while (atomicBoolean.get()) {
                Main.printThreadNum();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });



        int i = 0;
        for ( ; i < Math.random() * 10000; i++) {
            parallelInvoker.and(() -> {
                exc(0);
            });
        }

        parallelInvoker.continued();
        parallelInvoker.start();

        System.out.println("main continue: " + i);
        try {
            Thread.sleep(600000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        atomicBoolean.set(false);
    }

    private static void printThreadNum() {
        //获取线程数
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        while(threadGroup.getParent() != null){
            threadGroup = threadGroup.getParent();
        }
        int totalThread = threadGroup.activeCount();
        System.out.println(totalThread);
    }

    private static void exc(int i) {
        ParallelInvoker parallelInvoker = new ParallelInvoker();
        parallelInvoker.call(() -> { // call方法启动执行器
            print("GO.call." + i);
        }).then(() -> {
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            print("GO.then." + i);
        }).and(() -> {
            print("GO.and1." + i);
        }).and(() -> {
            print("GO.and2." + i);
        }).continued().start();
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
