import net.noboard.invoker.parallel.ParallelInvoker;

public class Main {
    public static void main(String[] args) {
        // 你可以通过注释下面的部分代码来探索它的功能，
        // 一些重要的说明请到代码中寻找，注释写的很清楚
        new ParallelInvoker().call(invoker -> { // call方法启动执行器
            System.out.println("1");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            invoker.reject(new Exception("e1"));
            System.out.println("2");
        }).and(invoker -> {
            System.out.println("3");
            invoker.reject(new Exception("e2"));
            System.out.println("4");
        }).then(invoker -> { // then将前后断开，只有前面的回调全部执行完，且没有发生catched（没有调用reject），才会执行then后的函数，此处调用了reject，所以"then 开始"不会被打印
            System.out.println("then 开始");
            invoker.reject(new Exception());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("then 结束");
        }).continued().catched(e -> { // continued标识可让主线程不等待被标记点的执行
            System.out.println(e.getMessage());
        }).start();
        System.out.println("main continue");
    }
}
