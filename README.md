# lock-free-thread-safe-cache

A lock-free cache implementation in java.

Uses
- ConcurrentHashMap for thread-safe key-value storage
- AtomicReference for lock-free value updates, with CAS operation implementation.

Requirements:
- Java 25

Run local tests:
```bash
mvn test
```

Refer to LockFreeCacheTest for implementation details and test cases.

Note: This implementation is for learning purposes and may not be production ready.
This wont work as a distributed cache.