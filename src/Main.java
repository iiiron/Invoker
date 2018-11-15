import net.noboard.invoker.parallel.ParallelInvoker;

public class Main {

    public static void main(String[] args) {
        new ParallelInvoker().call(invoker -> {
            System.out.println("1");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            invoker.reject(new Exception("e1"));
            System.out.println("2");
        }).continued().then(invoker -> {
            System.out.println("3");
            invoker.reject(new Exception("e2"));
            System.out.println("4");
        }).then(invoker -> {
            System.out.println("then 开始");
//            invoker.reject(new Exception());
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            System.out.println("then 结束");
        }).continued().catched(e -> {
            System.out.println(e.getMessage());
        }).start();
        System.out.println("main continue");
    }
}
