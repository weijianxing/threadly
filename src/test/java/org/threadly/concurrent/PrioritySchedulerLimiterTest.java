package org.threadly.concurrent;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Test;
import org.threadly.concurrent.SimpleSchedulerInterfaceTest.PrioritySchedulerFactory;
import org.threadly.test.concurrent.TestRunnable;

@SuppressWarnings("javadoc")
public class PrioritySchedulerLimiterTest {
  @Test
  public void constructorFail() {
    try {
      new PrioritySchedulerLimiter(null, 100);
      fail("Exception should have thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }
    try {
      new PrioritySchedulerLimiter(new PriorityScheduledExecutor(1, 1, 100), 0);
      fail("Exception should have thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
  
  @Test
  public void getDefaultPriorityTest() {
    PriorityScheduledExecutor executor = new PriorityScheduledExecutor(1, 1, 10, TaskPriority.Low, 100);
    assertTrue(new PrioritySchedulerLimiter(executor, 1).getDefaultPriority() == executor.getDefaultPriority());
    
    executor = new PriorityScheduledExecutor(1, 1, 10, TaskPriority.High, 100);
    assertTrue(new PrioritySchedulerLimiter(executor, 1).getDefaultPriority() == executor.getDefaultPriority());
  }
  
  @Test
  public void consumeAvailableTest() {
    int testQty = 10;
    PriorityScheduledExecutor executor = new PriorityScheduledExecutor(1, 1, 10, TaskPriority.High, 100);
    PrioritySchedulerLimiter psl = new PrioritySchedulerLimiter(executor, testQty);
    
    boolean flip1 = true;
    boolean flip2 = true;
    List<TestRunnable> runnables = new ArrayList<TestRunnable>(testQty);
    for (int i = 0; i < testQty; i++) {
      
      if (flip1) {
        TestRunnable tr = new TestRunnable();
        runnables.add(tr);
        if (flip2) {
          psl.waitingTasks.add(psl.new PriorityRunnableWrapper(tr, TaskPriority.High, 
                                                               psl.new FutureFuture<Object>()));
          flip2 = false;
        } else {
          psl.waitingTasks.add(psl.new PriorityRunnableWrapper(tr, TaskPriority.High, null));
          flip2 = true;
        }
        flip1 = false;
      } else {
        psl.waitingTasks.add(psl.new PriorityCallableWrapper<Object>(new Callable<Object>() {
          @Override
          public Object call() throws Exception {
            return new Object();
          }
        }, TaskPriority.High, psl.new FutureFuture<Object>()));
        flip1 = true;
      }
    }
    
    psl.consumeAvailable();
    
    // should be fully consumed
    assertEquals(psl.waitingTasks.size(), 0);
    
    Iterator<TestRunnable> it = runnables.iterator();
    while (it.hasNext()) {
      it.next().blockTillFinished();  // throws exception if it does not finish
    }
  }
  
  @Test
  public void executeTest() {
    SchedulerLimiterFactory sf = new SchedulerLimiterFactory();
    
    try {
      SimpleSchedulerInterfaceTest.executeTest(sf);
    } finally {
      sf.shutdown();
    }
  }
  
  @Test
  public void submitRunnableTest() {
    SchedulerLimiterFactory sf = new SchedulerLimiterFactory();
    
    try {

      int runnableCount = 10;
      
      SimpleSchedulerInterface scheduler = sf.make(runnableCount, false);
      
      List<TestRunnable> runnables = new ArrayList<TestRunnable>(runnableCount);
      List<Future<?>> futures = new ArrayList<Future<?>>(runnableCount);
      for (int i = 0; i < runnableCount; i++) {
        TestRunnable tr = new TestRunnable();
        Future<?> future = scheduler.submit(tr);
        assertNotNull(future);
        runnables.add(tr);
        futures.add(future);
      }
      
      // verify execution
      Iterator<TestRunnable> it = runnables.iterator();
      while (it.hasNext()) {
        TestRunnable tr = it.next();
        tr.blockTillFinished();
        
        assertEquals(tr.getRunCount(), 1);
      }
      
      Iterator<Future<?>> futureIt = futures.iterator();
      while (futureIt.hasNext()) {
        Future<?> f = futureIt.next();
        try {
          f.get();
        } catch (InterruptedException e) {
          fail();
        } catch (ExecutionException e) {
          fail();
        }
        assertTrue(f.isDone());
      }
    } finally {
      sf.shutdown();
    }
  }
  
  @Test
  public void submitCallableTest() throws InterruptedException, ExecutionException {
    SchedulerLimiterFactory sf = new SchedulerLimiterFactory();
    
    try {
      SimpleSchedulerInterfaceTest.submitCallableTest(sf);
    } finally {
      sf.shutdown();
    }
  }
  
  @Test (expected = IllegalArgumentException.class)
  public void executeTestFail() {
    SchedulerLimiterFactory sf = new SchedulerLimiterFactory();
    
    try {
      SimpleSchedulerInterfaceTest.executeFail(sf);
    } finally {
      sf.shutdown();
    }
  }
  
  @Test (expected = IllegalArgumentException.class)
  public void submitRunnableFail() {
    SchedulerLimiterFactory sf = new SchedulerLimiterFactory();
    
    try {
      SimpleSchedulerInterfaceTest.submitRunnableFail(sf);
    } finally {
      sf.shutdown();
    }
  }
  
  @Test (expected = IllegalArgumentException.class)
  public void submitCallableFail() {
    SchedulerLimiterFactory sf = new SchedulerLimiterFactory();
    
    try {
      SimpleSchedulerInterfaceTest.submitCallableFail(sf);
    } finally {
      sf.shutdown();
    }
  }
  
  @Test
  public void scheduleExecutionTest() {
    SchedulerLimiterFactory sf = new SchedulerLimiterFactory();
    
    try {
      SimpleSchedulerInterfaceTest.scheduleTest(sf);
    } finally {
      sf.shutdown();
    }
  }
  
  @Test
  public void scheduleExecutionFail() {
    SchedulerLimiterFactory sf = new SchedulerLimiterFactory();
    
    try {
      SimpleSchedulerInterfaceTest.scheduleExecutionFail(sf);
    } finally {
      sf.shutdown();
    }
  }
  
  @Test
  public void submitScheduledRunnableTest() {
    SchedulerLimiterFactory sf = new SchedulerLimiterFactory();
    // we can't defer to the interface implementation for this check
    try {
      int runnableCount = 10;
      int scheduleDelay = 50;
      
      SimpleSchedulerInterface scheduler = sf.make(runnableCount, true);
      
      List<TestRunnable> runnables = new ArrayList<TestRunnable>(runnableCount);
      List<Future<?>> futures = new ArrayList<Future<?>>(runnableCount);
      for (int i = 0; i < runnableCount; i++) {
        TestRunnable tr = new TestRunnable();
        Future<?> future = scheduler.submitScheduled(tr, scheduleDelay);
        assertNotNull(future);
        runnables.add(tr);
        futures.add(future);
      }
      
      // verify execution and execution times
      Iterator<TestRunnable> it = runnables.iterator();
      while (it.hasNext()) {
        TestRunnable tr = it.next();
        long executionDelay = tr.getDelayTillFirstRun();
        assertTrue(executionDelay >= scheduleDelay);
        // should be very timely with a core pool size that matches runnable count
        assertTrue(executionDelay <= (scheduleDelay + 2000));  
        assertEquals(tr.getRunCount(), 1);
      }
      
      Iterator<Future<?>> futureIt = futures.iterator();
      while (futureIt.hasNext()) {
        Future<?> f = futureIt.next();
        try {
          f.get();
        } catch (InterruptedException e) {
          fail();
        } catch (ExecutionException e) {
          fail();
        }
        assertTrue(f.isDone());
      }
    } finally {
      sf.shutdown();
    }
  }
  
  @Test
  public void submitScheduledCallableTest() throws InterruptedException, ExecutionException {
    SchedulerLimiterFactory sf = new SchedulerLimiterFactory();
    
    try {
      SimpleSchedulerInterfaceTest.submitScheduledCallableTest(sf);
    } finally {
      sf.shutdown();
    }
  }
  
  @Test
  public void submitScheduledRunnableFail() {
    SchedulerLimiterFactory sf = new SchedulerLimiterFactory();
    
    try {
      SimpleSchedulerInterfaceTest.submitScheduledRunnableFail(sf);
    } finally {
      sf.shutdown();
    }
  }
  
  @Test
  public void submitScheduledCallableFail() {
    SchedulerLimiterFactory sf = new SchedulerLimiterFactory();
    
    try {
      SimpleSchedulerInterfaceTest.submitScheduledCallableFail(sf);
    } finally {
      sf.shutdown();
    }
  }
  
  @Test
  public void recurringExecutionTest() {
    SchedulerLimiterFactory sf = new SchedulerLimiterFactory();
    
    try {
      SimpleSchedulerInterfaceTest.recurringExecutionTest(sf);
    } finally {
      sf.shutdown();
    }
  }
  
  @Test
  public void recurringExecutionFail() {
    SchedulerLimiterFactory sf = new SchedulerLimiterFactory();
    
    try {
      SimpleSchedulerInterfaceTest.recurringExecutionFail(sf);
    } finally {
      sf.shutdown();
    }
  }

  private class SchedulerLimiterFactory implements PrioritySchedulerFactory {
    private final List<PriorityScheduledExecutor> executors;
    
    private SchedulerLimiterFactory() {
      executors = new LinkedList<PriorityScheduledExecutor>();
    }
    
    @Override
    public PrioritySchedulerLimiter make(int poolSize, boolean prestartIfAvailable) {
      PriorityScheduledExecutor executor = new PriorityScheduledExecutor(poolSize, poolSize, 
                                                                         1000 * 10);
      if (prestartIfAvailable) {
        executor.prestartAllCoreThreads();
      }
      executors.add(executor);
      return new PrioritySchedulerLimiter(executor, poolSize);
    }
    
    @Override
    public void shutdown() {
      Iterator<PriorityScheduledExecutor> it = executors.iterator();
      while (it.hasNext()) {
        it.next().shutdown();
        it.remove();
      }
    }
  }
}
