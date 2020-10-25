package net.noboard.invoker;

public class DataWrapper<T> {

    private T data;

    public static<T> DataWrapper<T> empty() {
        return new DataWrapper<T>();
    }

    private DataWrapper() {

    }

    public void set(T data) {
        this.data = data;
    }

    public T get() {
        return this.data;
    }
}
