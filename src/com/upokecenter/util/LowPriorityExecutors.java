/*
Written in 2013 by Peter Occil.  Released to the public domain.
Public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
 */
package com.upokecenter.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LowPriorityExecutors {

	// Modified from DefaultThreadFactory in Executors.java,
	// a public domain file
	static class LowPriorityThreadFactory implements ThreadFactory {
		private static final AtomicInteger poolNumber = new AtomicInteger(1);
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;

		LowPriorityThreadFactory() {
			SecurityManager s = System.getSecurityManager();
			group = (s != null)? s.getThreadGroup() :
				Thread.currentThread().getThreadGroup();
			namePrefix = "pool-" +
					poolNumber.getAndIncrement() +
					"-thread-";
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r,
					namePrefix + threadNumber.getAndIncrement(),
					0);
			if (t.isDaemon()) {
				t.setDaemon(false);
			}
			if (t.getPriority() != 4) {
				t.setPriority(4);
			}
			return t;
		}
	}

	public static ExecutorService newFixedThreadPool(int nThreads) {
		return new ThreadPoolExecutor(nThreads, nThreads,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(),
				new LowPriorityThreadFactory());
	}


	public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
		return new ScheduledThreadPoolExecutor(corePoolSize,
				new LowPriorityThreadFactory());
	}
}
