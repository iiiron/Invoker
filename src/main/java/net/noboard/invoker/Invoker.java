package net.noboard.invoker;

import java.util.function.Consumer;

public interface Invoker {
    Invoker call(Caller caller);

    Invoker then(Caller caller);

    Invoker and(Caller caller);

    Invoker abnormal(Consumer<Exception> consumer);

    Invoker normalEnd(CallerWithoutException consumer);

    void start();

    Invoker continued();
}
