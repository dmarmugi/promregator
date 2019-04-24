package org.cloudfoundry.promregator.cache;


import java.time.Duration;

import org.junit.Assert;
import org.junit.Test;

import reactor.core.publisher.Mono;

public class AutoRefreshingCacheMapTest {

	@Test
	public void testPutAndGet() {
		AutoRefreshingCacheMap<String, String> subject = new AutoRefreshingCacheMap<>("test", null, Duration.ofSeconds(1), Duration.ofSeconds(1), key -> {
			Assert.fail("should not be reached");
			return null;
		});
		
		subject.put("test", "value");
		String value = subject.get("test");
		
		Assert.assertEquals("value", value);
	}
	
	@Test
	public void testAutoLoad() {
		AutoRefreshingCacheMap<String, String> subject = new AutoRefreshingCacheMap<>("test", null, Duration.ofSeconds(1), Duration.ofSeconds(1), key -> {
			char[] source = key.toCharArray();
			char[] target = new char[source.length];
			for (int i = 0;i<source.length;i++) {
				target[source.length-i-1] = source[i];
			}
			return new String(target);
		});
		
		String value = subject.get("autoload");
		Assert.assertEquals("daolotua", value);
	}
	
	@Test
	public void testExpiry() throws InterruptedException {
		AutoRefreshingCacheMap<String, String> subject = new AutoRefreshingCacheMap<>("test", null, Duration.ofMillis(100), Duration.ofSeconds(10), key -> {
			char[] source = key.toCharArray();
			char[] target = new char[source.length];
			for (int i = 0;i<source.length;i++) {
				target[source.length-i-1] = source[i];
			}
			return new String(target);
		});
		
		String value = subject.get("autoload");
		Assert.assertEquals("daolotua", value);
		
		// we have to wait until the thread has been called to clean up
		Thread.sleep(1000);
		
		boolean exists = subject.containsKey("autoload");
		Assert.assertFalse(exists);
	}
	
	private static class Counter {
		private int counter;
		
		public void increase() {
			this.counter++;
		}
		public int getCounter() {
			return this.counter;
		}
	}
	
	@Test
	public void testAutoRefresh() throws InterruptedException {
		final Counter counter = new Counter();
		
		AutoRefreshingCacheMap<String, String> subject = new AutoRefreshingCacheMap<>("test", null, Duration.ofSeconds(10), Duration.ofMillis(500), key -> {
			counter.increase();
			return "refreshed";
		});
		
		subject.put("key", "initial");
		String value = subject.get("key");
		Assert.assertEquals("initial", value);
		
		// we have to wait until the thread had a chance to refresh
		Thread.sleep(1000);
		
		boolean exists = subject.containsKey("key");
		Assert.assertTrue(exists);
		
		Assert.assertEquals("refreshed", subject.get("key"));
		Assert.assertEquals(1, counter.getCounter());
	}
	
	private static class MassOperationTestThread extends Thread {
		private AutoRefreshingCacheMap<String, String> subject;
		private int threadNumber;
		
		private boolean terminatedProperly = false;

		public MassOperationTestThread(AutoRefreshingCacheMap<String, String> subject, int threadNumber) {
			this.subject = subject;
			this.threadNumber = threadNumber;
		}

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			try {
				final String deleteKey = "Delete-"+this.threadNumber;
				subject.put(deleteKey, "entry-to-be-deleted");
				
				String value = subject.get("global");
				Assert.assertEquals("labolg", value);

				Thread.sleep(1500);
				
				// check that a cleanup has taken place
				value = subject.get(deleteKey);
				Assert.assertNotEquals("entry-to-be-deleted", value);
				
				// verify that "global" wasn't touched
				value = subject.get("global");
				Assert.assertEquals("labolg", value);
			} catch (InterruptedException e) {
				Assert.fail("Thread was interrupted");
			}
			
			this.terminatedProperly = true;
		}

		public boolean isTerminatedProperly() {
			return terminatedProperly;
		}
		
	}
	
	@Test
	public void testMassRun() throws InterruptedException {
		AutoRefreshingCacheMap<String, String> subject = new AutoRefreshingCacheMap<>("test", null, Duration.ofMillis(200), Duration.ofMillis(200), key -> {
			char[] source = key.toCharArray();
			char[] target = new char[source.length];
			for (int i = 0;i<source.length;i++) {
				target[source.length-i-1] = source[i];
			}
			return new String(target);
		});
		
		MassOperationTestThread[] threads = new MassOperationTestThread[10];
		for (int i = 0;i<threads.length;i++) {
			threads[i] = new MassOperationTestThread(subject, i);
			threads[i].start();
		}
		
		for (int i = 0;i<threads.length;i++) {
			threads[i].join(5000);
		}
		
		for (int i = 0;i<threads.length;i++) {
			Assert.assertFalse(threads[i].isAlive());
			Assert.assertTrue(threads[i].isTerminatedProperly()); // if this check fails, check on the console log!
		}
	}

	private int lockObjectStringWorksCalls = 0;
	
	private class LockObjectStringWorksThread extends Thread {
		private AutoRefreshingCacheMap<String, Mono<String>> subject;
		
		public LockObjectStringWorksThread(AutoRefreshingCacheMap<String, Mono<String>> subject) {
			super();
			this.subject = subject;
		}

		@Override
		public void run() {
			this.subject.get("abc");
		}
		
	}
	
	@Test
	public void testLockObjectStringWorks() throws InterruptedException {
		AutoRefreshingCacheMap<String, Mono<String>> subject = new AutoRefreshingCacheMap<>("test",null, Duration.ofSeconds(10), Duration.ofSeconds(10), key -> {
			lockObjectStringWorksCalls++;
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				Assert.fail("Interrupted");
			}
			return Mono.just(key+"*");
		});
		
		LockObjectStringWorksThread[] threads = new LockObjectStringWorksThread[5];
		for (int i = 0;i<threads.length;i++) {
			threads[i] = new LockObjectStringWorksThread(subject);
			threads[i].start();
		}
		
		for (int i = 0;i<threads.length;i++) {
			threads[i].join(5000);
		}
		
		for (int i = 0;i<threads.length;i++) {
			Assert.assertFalse(threads[i].isAlive());
		}
		
		// there shall only be one call to the loader function!
		Assert.assertEquals(1, this.lockObjectStringWorksCalls);
	}
	
	private class CompositeKey {
		private String a;
		private String b;
		public CompositeKey(String a, String b) {
			super();
			this.a = a;
			this.b = b;
		}
		/**
		 * @return the a
		 */
		public String getA() {
			return a;
		}
		/**
		 * @return the b
		 */
		public String getB() {
			return b;
		}
		
	}
	
	private class LockObjectCompositeWorksThread extends Thread {
		private AutoRefreshingCacheMap<CompositeKey, Mono<String>> subject;
		
		public LockObjectCompositeWorksThread(AutoRefreshingCacheMap<CompositeKey, Mono<String>> subject) {
			this.subject = subject;
		}

		@Override
		public void run() {
			this.subject.get(new CompositeKey("a1", "b1"));
		}
		
	}
	
	private int lockObjectCompositeWorksCalls = 0;
	
	@Test
	public void testLockObjectCompositeWorks() throws InterruptedException {
		AutoRefreshingCacheMap<CompositeKey, Mono<String>> subject = new AutoRefreshingCacheMap<>("test",null, Duration.ofSeconds(10), Duration.ofSeconds(10), key -> {
			lockObjectCompositeWorksCalls++;
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				Assert.fail("Interrupted");
			}
			return Mono.just(key.getA() + key.getB() +"*");
		});
		
		LockObjectCompositeWorksThread[] threads = new LockObjectCompositeWorksThread[5];
		for (int i = 0;i<threads.length;i++) {
			threads[i] = new LockObjectCompositeWorksThread(subject);
			threads[i].start();
		}
		
		for (int i = 0;i<threads.length;i++) {
			threads[i].join(5000);
		}
		
		for (int i = 0;i<threads.length;i++) {
			Assert.assertFalse(threads[i].isAlive());
		}
		
		// there shall only be one call to the loader function!
		Assert.assertEquals(1, this.lockObjectCompositeWorksCalls);
	}
	
}
