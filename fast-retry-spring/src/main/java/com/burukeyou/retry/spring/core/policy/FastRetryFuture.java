package com.burukeyou.retry.spring.core.policy;

import com.burukeyou.retry.core.policy.FastResultPolicy;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

/**
 *  FastRetry Complete Future Extend， can config RetryPolicy
 * @author  caizhihao
 * @param <T>
 */
public class FastRetryFuture<T> extends CompletableFuture<T> {

    static Object NIL;
    static Field resultFiled;

    private FastResultPolicy<T> retryResultPolicy;

    private CompletableFuture<T> asyncFuture;

    static {
        try {
            NIL = CompletableFuture.class.getDeclaredField("NIL");
            resultFiled = FastRetryFuture.class.getSuperclass().getDeclaredField("result");
            resultFiled.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FastRetryFuture() {
        super();
    }

    public FastRetryFuture(CompletableFuture<T> completableFuture) {
        this.asyncFuture = completableFuture;
        completableFuture.whenComplete((value,ex) -> {
            if (ex != null){
                this.completeExceptionally(ex);
            }else {
                this.complete(value);
            }
        });
    }

    FastRetryFuture(Object value){
        try {
            resultFiled.set(this,value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Returns a new FastRetryFuture that is already completed with
     * the given value.
     *
     * @param data      the value
     * @param <T>       the type of the value
     * @return the completed CompletableFuture
     */
    public static <T> FastRetryFuture<T> completedFuture(T data) {
        return new FastRetryFuture<>(data == null ? NIL : data);
    }

    /**
     * set the RetryPolicy
     * @param retryPolicy
     * @return
     */
    public FastRetryFuture<T> retryWhen(FastResultPolicy<T> retryPolicy){
        this.retryResultPolicy = retryPolicy;
        return this;
    }

    /**
     * get the config retry policy
     */
    public FastResultPolicy<T> getRetryWhen() {
        return retryResultPolicy;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (asyncFuture != null){
            return asyncFuture.cancel(mayInterruptIfRunning);
        }
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        if (asyncFuture != null){
            return asyncFuture.isCancelled();
        }
        return super.isCancelled();
    }




}
