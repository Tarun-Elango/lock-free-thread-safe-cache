package com.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class LockFreeCacheTest {
    private LockFreeCache<String, String> cache;

    @BeforeEach
    void setUp() {
        // setup cache that will be used for all tests
        cache = new LockFreeCacheImpl<>();
    }

    // simple tests

    @Test
    void put_and_get() {
        cache.put("name", "John");
        assertEquals(Optional.of("John"), cache.get("name"));
    }   

    @Test
    void get_nonexistent_key() {
        // this will check if the cache returns an empty optional when the key is not found
        assertEquals(Optional.empty(), cache.get("name"));
    }

    @Test
    void put_overwrite_existing_value() {
        cache.put("name", "John");
        cache.put("name", "Jane");
        assertEquals(Optional.of("Jane"), cache.get("name"));
    }

    @Test
    void remove_deletes_entry() {
        cache.put("name", "John");
        cache.remove("name");
        assertEquals(Optional.empty(), cache.get("name"));
    }

    @Test
    void remove_nonexistent_key_not_throw(){
        assertDoesNotThrow(() -> cache.remove("name"));//
        // check if remove throws exception when key is not found
        // it should not throw any exception
    }

    @Test
    void size_reflects_entries(){
        assertEquals(0, cache.size());
        cache.put("a","1");
        cache.put("b","2");
        assertEquals(2, cache.size());
        cache.remove("a");
        assertEquals(1, cache.size());
    }

    @Test
    void clear_empties_cache(){
        cache.put("a","1");
        cache.put("b","2");
        assertEquals(2, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
    }

    // Multiple write: 10 threads - no key overlap, all writes must be visible
    // CountDownLatch(n) - lets thread wait until n operations are completed
    @Test
    void concurrent_write_no_overlap() {
        int threadCount = 10;
        int writePerThread = 1000;

        // create latches, which will be used to synchronize the threads, all threads will wait for the start gate to open
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);// tells main thread that all threads have finished
        
        // create a thread pool with 10 threads
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>(); // store futures for each thread
        
        // for each index, create a thread, which will write to the cache
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            futures.add(pool.submit(()->{
                try{
                    startGate.await();// each thread waits till gate opens startGate.countdown()
                   // System.out.println("Thread " + threadId + " started");
                    for (int j = 0; j < writePerThread; j++){
                        // each write operation will have a unique key, no overlap
                        String key = "thread-" + threadId + "-key-" + j;
                        cache.put(key, "value");
                    }
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }finally {
                    // decrement the count of the latch, so that the main thread can proceed
                    doneLatch.countDown();
                }
            }));
        }
        
       //System.out.println("All threads started, releasing gate...");
       // release all threads
       startGate.countDown();
       try {
        doneLatch.await();// main thread waits till all threads finish
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
       pool.shutdown();
    }
    

    // 10 threads all write to the same key - last write wins
    @Test
    void concurrent_write_same_key() throws Exception {
        int threadCount = 10;
        int writePerThread = 500;
     
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < writePerThread; j++) {
                        cache.put("key", "thread-" + threadId + "-value-" + j);
                    }
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }finally {
                    doneLatch.countDown();
                }
            }));
        }
        
        startGate.countDown();
        doneLatch.await();
        pool.shutdown();

        for (Future<?> future : futures){
            future.get();// this will throw exception if the task threw exception
        }

        // we know key exists, non null, must start with "thread-"  and value should be the last write
        Optional<String> value = cache.get("key");
        assertTrue(value.isPresent(), "Shared key should exist");
        assertTrue(value.get().startsWith("thread-"), "value should start with 'thread-'");
        //System.out.println("Final value: " + value.get());
    }

    // reader - writer contention, 5 readers, 5 writers simultaneously, no read errors, all writes successful 
    @Test
    void reader_writer_contention() throws Exception {
        // prefill
        for (int t = 0; t < 100; t++) {
            cache.put("key-" + t, "value-" + t);
        }
        
        int writerCount = 5;
        int readerCount = 5;
        int totalThreads = writerCount + readerCount;
        
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);
        AtomicInteger readSuccess = new AtomicInteger(0);// thread safe counter for tracking successful reads
        
        ExecutorService pool = Executors.newFixedThreadPool(totalThreads);
        List<Future<?>> futures = new ArrayList<>();

        // writers
        for (int i = 0; i < writerCount; i++) {
            final int threadId = i;
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < 200; j++) {
                        cache.put("key-" + (j%100), "write-" + threadId + "-" + j);// add to existing key
                        cache.put("new-key-" + threadId + "-" +j, "value");// add new key
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        // readers
        for (int t = 0; t < readerCount; t++) {
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j <500; j++){
                        Optional<String> value = cache.get("key-" + (j%100));
                        if (value.isPresent()) {
                            assertFalse(value.isEmpty(), "Value should not be empty");
                            readSuccess.incrementAndGet();// this will increment the counter if the value is present
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }));
        }
        
        startGate.countDown();
        doneLatch.await();
        pool.shutdown();


        for (Future<?> future : futures) {
            future.get();
        }

        assertTrue(readSuccess.get() > 0, "At least one read should be successful");
        System.out.println("Read success: " + readSuccess.get());// 5 threads * 500 iterations = 2500
    }

}
