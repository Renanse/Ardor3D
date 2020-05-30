/**
 * Originally from: https://stackoverflow.com/questions/807223/how-do-i-implement-task-prioritization-using-an-executorservice-in-java-5/42831172#42831172
 */

package com.ardor3d.extension.terrain.util;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PriorityExecutors {

  public static ExecutorService newFixedThreadPool(final int nThreads) {
    return newFixedThreadPool(nThreads, Executors.defaultThreadFactory());
  }

  public static ExecutorService newFixedThreadPool(final int nThreads, final ThreadFactory factory) {
    return new PriorityExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, factory);
  }

  private static class PriorityExecutor extends ThreadPoolExecutor {
    private static final int DEFAULT_PRIORITY = 0;
    private static AtomicLong instanceCounter = new AtomicLong();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public PriorityExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime,
      final TimeUnit unit, final ThreadFactory factory) {
      super(corePoolSize, maximumPoolSize, keepAliveTime, unit,
          (BlockingQueue) new PriorityBlockingQueue<>(100,
              ComparableTask.comparatorByPriorityAndSequentialOrder()),
          factory);
    }

    @Override
    public void execute(final Runnable command) {
      // If this is ugly then delegator pattern needed
      if (command instanceof ComparableTask) {
        super.execute(command);
      } else {
        super.execute(newComparableRunnableFor(command));
      }
    }

    private Runnable newComparableRunnableFor(final Runnable runnable) {
      return new ComparableRunnable(ensurePriorityRunnable(runnable));
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(final Callable<T> callable) {
      return new ComparableFutureTask<>(ensurePriorityCallable(callable));
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value) {
      return new ComparableFutureTask<>(ensurePriorityRunnable(runnable), value);
    }

    private <T> PriorityCallable<T> ensurePriorityCallable(final Callable<T> callable) {
      return callable instanceof PriorityCallable ? (PriorityCallable<T>) callable
          : PriorityCallable.of(callable, DEFAULT_PRIORITY);
    }

    private PriorityRunnable ensurePriorityRunnable(final Runnable runnable) {
      return runnable instanceof PriorityRunnable ? (PriorityRunnable) runnable
          : PriorityRunnable.of(runnable, DEFAULT_PRIORITY);
    }

    private class ComparableFutureTask<T> extends FutureTask<T> implements ComparableTask {
      private final Long sequentialOrder = instanceCounter.getAndIncrement();
      private final HasPriority hasPriority;

      public ComparableFutureTask(final PriorityCallable<T> priorityCallable) {
        super(priorityCallable);
        this.hasPriority = priorityCallable;
      }

      public ComparableFutureTask(final PriorityRunnable priorityRunnable, final T result) {
        super(priorityRunnable, result);
        this.hasPriority = priorityRunnable;
      }

      @Override
      public long getInstanceCount() { return sequentialOrder; }

      @Override
      public int getPriority() { return hasPriority.getPriority(); }
    }

    private static class ComparableRunnable implements Runnable, ComparableTask {
      private final Long instanceCount = instanceCounter.getAndIncrement();
      private final HasPriority hasPriority;
      private final Runnable runnable;

      public ComparableRunnable(final PriorityRunnable priorityRunnable) {
        runnable = priorityRunnable;
        hasPriority = priorityRunnable;
      }

      @Override
      public void run() {
        runnable.run();
      }

      @Override
      public int getPriority() { return hasPriority.getPriority(); }

      @Override
      public long getInstanceCount() { return instanceCount; }
    }

    private interface ComparableTask extends Runnable {
      int getPriority();

      long getInstanceCount();

      static Comparator<ComparableTask> comparatorByPriorityAndSequentialOrder() {
        return (o1, o2) -> {
          final int priorityResult = o2.getPriority() - o1.getPriority();
          return priorityResult != 0 ? priorityResult : (int) (o1.getInstanceCount() - o2.getInstanceCount());
        };
      }

    }

  }

  private interface HasPriority {
    int getPriority();
  }

  public interface PriorityCallable<V> extends Callable<V>, HasPriority {

    static <V> PriorityCallable<V> of(final Callable<V> callable, final int priority) {
      return new PriorityCallable<>() {
        @Override
        public V call() throws Exception {
          return callable.call();
        }

        @Override
        public int getPriority() { return priority; }
      };
    }
  }

  public interface PriorityRunnable extends Runnable, HasPriority {

    static PriorityRunnable of(final Runnable runnable, final int priority) {
      return new PriorityRunnable() {
        @Override
        public void run() {
          runnable.run();
        }

        @Override
        public int getPriority() { return priority; }
      };
    }
  }

}
