package net.noboard.invoker;

import java.util.function.Consumer;

public interface Invoker {
    Invoker call(Consumer<Invoker> consumer);

    Invoker then(Consumer<Invoker> consumer);

    Invoker and(Consumer<Invoker> consumer);

    void reject(Exception e);

    Invoker catched(Consumer<Exception> consumer);

    Invoker normalEnd(Consumer<Invoker> consumer);

    void start();

    Invoker continued();
}
