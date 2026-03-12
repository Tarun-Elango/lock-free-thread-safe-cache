package com.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    

}
