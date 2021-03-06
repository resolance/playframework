/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.concurrent;
import play.Play;
import play.libs.F;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Utilities for creating {@link java.util.concurrent.CompletionStage}.
 */
public interface Futures {

    /**
     * Creates a CompletionStage that returns either the input stage, or a futures.
     *
     * Note that futures is not the same as cancellation.  Even in case of futures,
     * the given completion stage will still complete, even though that completed value
     * is not returned.
     *
     * @param stage the input completion stage that may time out.
     * @param amount The amount (expressed with the corresponding unit).
     * @param unit The time Unit.
     * @param <A> the completion's result type.
     * @return either the completed future, or a completion stage that failed with futures.
     */
    <A> CompletionStage<A> timeout(CompletionStage<A> stage, long amount, TimeUnit unit);

    /**
     * An alias for futures(stage, delay, unit) that uses a java.time.Duration.
     *
     * @param stage the input completion stage that may time out.
     * @param duration The duration after which there is a timeout.
     * @param <A> the completion stage that should be wrapped with a future.
     * @return the completion stage, or a completion stage that failed with futures.
     */
    <A> CompletionStage<A> timeout(CompletionStage<A> stage, Duration duration);

    /**
     * Create a CompletionStage which, after a delay, will be redeemed with the result of a
     * given supplier. The supplier will be called after the delay.
     *
     * @param stage the input completion stage that is delayed.
     * @param amount The time to wait.
     * @param unit The units to use for the amount.
     * @param <A> the type of the completion's result.
     * @return the delayed CompletionStage wrapping supplier.
     */
    <A> CompletionStage<A> delayed(CompletionStage<A> stage, long amount, TimeUnit unit);

    /**
     * Create a CompletionStage which, after a delay, will be redeemed with the result of a
     * given supplier. The supplier will be called after the delay.
     *
     * @param stage the input completion stage that is delayed.
     * @param duration to wait.
     * @param <A> the type of the completion's result.
     * @return the delayed CompletionStage wrapping supplier.
     */
    <A> CompletionStage<A> delayed(CompletionStage<A> stage, Duration duration);

    /**
     * Combine the given CompletionStages into a single CompletionStage for the list of results.
     *
     * The sequencing operations are performed in the default ExecutionContext.
     *
     * @param promises The CompletionStages to combine
     * @param <A> the type of the completion's result.
     * @return A single CompletionStage whose methods act on the list of redeemed CompletionStages
     */
    public static <A> CompletionStage<List<A>> sequence(Iterable<? extends CompletionStage<A>> promises) {
        CompletableFuture<List<A>> result = CompletableFuture.completedFuture(new ArrayList<>());
        for (CompletionStage<A> promise: promises) {
            result = result.thenCombine(promise, (list, a) -> {
                list.add(a);
                return list;
            });
        }
        return result;
    }

    /**
     * Combine the given CompletionStages into a single CompletionStage for the list of results.
     *
     * The sequencing operations are performed in the default ExecutionContext.
     * @param promises The CompletionStages to combine
     * @param <A> the type of the completion's result.
     * @return A single CompletionStage whose methods act on the list of redeemed CompletionStage
     */
    public static <A> CompletionStage<List<A>> sequence(CompletionStage<A>... promises) {
        return sequence(Arrays.asList(promises));
    }

    /**
     * Create a CompletionStage that is redeemed after a timeout.  This method
     * is useful for returning fallback values on futures.
     *
     * The underlying implementation uses TimerTask, which has a
     * resolution in milliseconds.
     *
     * @deprecated Use injected {@code play.libs.concurrent.Futures.timeout}, since 2.6.0
     * @param value The result value to use to complete the CompletionStage.
     * @param amount The amount (expressed with the corresponding unit).
     * @param unit The time unit, i.e. java.util.concurrent.TimeUnit.MILLISECONDS
     * @param <A> the type of the completion's result.
     * @return the CompletionStage wrapping the result value
     */
    @Deprecated
    public static <A> CompletionStage<A> timeout(A value, long amount, TimeUnit unit) {
        final Futures timeout = Play.application().injector().instanceOf(Futures.class);
        CompletableFuture<A> future = CompletableFuture.completedFuture(value);
        return timeout.timeout(future, amount, unit);
    }

    /**
     * Creates a CompletionStage timer that throws a PromiseTimeoutException after
     * a given timeout.
     *
     * The returned CompletionStage is usually combined with other CompletionStage,
     * i.e. {@code completionStage.applyToEither(Futures.timeout(delay, unit), Function.identity()) }
     *
     * The underlying implementation uses TimerTask, which has a
     * resolution in milliseconds.
     *
     * A previous implementation used {@code CompletionStage<Void>} which made
     * it unsuitable for composition.  Cast with {@code Futures.<Void>timeout} if
     * necessary.
     *
     * @deprecated Use injected {@code play.libs.concurrent.Futures.timeout}, since 2.6.0
     * @param delay The delay (expressed with the corresponding unit).
     * @param unit The time Unit.
     * @param <A> the type of the completion's result.
     * @return a CompletionStage that failed exceptionally
     */
    @Deprecated
    public static <A> CompletionStage<A> timeout(final long delay, final TimeUnit unit) {
        requireNonNull(unit, "Null unit");
        final Futures timeout = Play.application().injector().instanceOf(Futures.class);
        String msg = "Timeout in promise after " + delay + " " + unit.toString();
        final CompletableFuture<A> future = new CompletableFuture<>();
        final F.PromiseTimeoutException ex = new F.PromiseTimeoutException(msg);
        future.completeExceptionally(ex);
        return timeout.timeout(future, delay, unit);
    }

    /**
     * Create a CompletionStage which, after a delay, will be redeemed with the result of a
     * given supplier. The supplier will be called after the delay.
     *
     * @deprecated Use injected {@code play.libs.concurrent.Futures.delayed}, since 2.6.0
     * @param supplier The supplier to call to fulfill the CompletionStage.
     * @param delay The time to wait.
     * @param unit The units to use for the delay.
     * @param executor The executor to run the supplier in.
     * @param <A> the type of the completion's result.
     * @return the delayed CompletionStage wrapping supplier.
     */
    @Deprecated
    public static <A> CompletionStage<A> delayed(Supplier<A> supplier, long delay, TimeUnit unit, Executor executor) {
        final Futures timeout = Play.application().injector().instanceOf(Futures.class);

        return timeout.delayed(supplyAsync(supplier, executor), delay, unit);
    }

}
