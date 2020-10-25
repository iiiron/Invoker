package net.noboard.invoker;

@FunctionalInterface
public interface Caller {
    void accept() throws Exception;
}
