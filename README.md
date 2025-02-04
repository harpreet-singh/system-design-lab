# Classic Distributed Systems Patterns in Java

This project includes working Java implementations of:

- A distributed token-bucket rate limiter backed by a shared state store
- A classic circuit breaker with closed, open, and half-open states

## Project Layout

- `src/main/java/lab/systemdesign/ratelimiter`: distributed rate limiter implementation
- `src/main/java/lab/systemdesign/circuitbreaker`: circuit breaker implementation
- `src/test/java`: JUnit tests that exercise the behavior

## Run

```bash
mvn test
```

## Notes

The rate limiter uses a `DistributedStateStore` abstraction so the algorithm can be backed by Redis, DynamoDB, Hazelcast, or another shared store in a real deployment. The included in-memory implementation is enough to demonstrate correctness and node coordination in tests.

The circuit breaker implementation is intentionally small and explicit so the state transitions are easy to follow and extend.
