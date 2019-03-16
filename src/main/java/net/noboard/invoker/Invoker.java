package net.noboard.invoker;


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
