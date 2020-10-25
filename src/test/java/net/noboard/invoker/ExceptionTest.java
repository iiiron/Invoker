package net.noboard.invoker;

import net.noboard.invoker.parallel.ParallelInvoker;
import org.junit.Test;

public class ExceptionTest {


    @Test
    public void exceptionTest() {
        DataWrapper<String> dw = DataWrapper.empty();
        DataWrapper<String> dw2 = DataWrapper.empty();
        new ParallelInvoker().call(() -> {
            dw.set("tag 1");
        }).then(() -> {
            throw new Exception("tag 2");
        }).and(() -> {
            System.out.println(dw.get());
        }).then(() -> {
            System.out.println("tag 3");
        }).abnormal(e -> {
            System.out.println(e.getMessage());
        }).normalEnd(() -> {
            System.out.println("tag 4");
        }).start();
    }

}
