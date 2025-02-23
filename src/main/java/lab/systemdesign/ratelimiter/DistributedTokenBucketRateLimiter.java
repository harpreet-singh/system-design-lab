package lab.systemdesign.ratelimiter;

import java.util.Objects;

public class DistributedTokenBucketRateLimiter {
    private final DistributedStateStore<String, BucketState> stateStore;
    private final int capacity;
    private final double refillTokensPerMillis;
    private final Clock clock;

    public DistributedTokenBucketRateLimiter(
            DistributedStateStore<String, BucketState> stateStore,
            int capacity,
            double refillTokensPerSecond,
            Clock clock
    ) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (refillTokensPerSecond <= 0) {
            throw new IllegalArgumentException("refillTokensPerSecond must be positive");
        }
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.capacity = capacity;
        this.refillTokensPerMillis = refillTokensPerSecond / 1000.0;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public RateLimitDecision allow(String key) {
        return allow(key, 1);
    }

    public RateLimitDecision allow(String key, int requestedTokens) {
        if (requestedTokens <= 0) {
            throw new IllegalArgumentException("requestedTokens must be positive");
        }

        long now = clock.millis();
        BucketState updated = stateStore.compute(key, ignored -> {
            BucketState current = ignored == null
                    ? new BucketState(capacity, now, true, capacity, 0).consume(requestedTokens, refillTokensPerMillis)
                    : ignored.refill(now, capacity, refillTokensPerMillis).consume(requestedTokens, refillTokensPerMillis);

            if (ignored == null && requestedTokens > capacity) {
                return new BucketState(capacity, now, false, capacity, retryAfterMillis(requestedTokens - capacity));
            }
            return current;
        });
        return new RateLimitDecision(updated.allowed(), updated.remainingTokens(), updated.retryAfterMillis());
    }

    private long retryAfterMillis(int missingTokens) {
        return (long) Math.ceil(missingTokens / refillTokensPerMillis);
    }

    public record BucketState(
            double availableTokens,
            long lastRefillAtMillis,
            boolean allowed,
            int remainingTokens,
            long retryAfterMillis
    ) {
        BucketState refill(long now, int capacity, double refillTokensPerMillis) {
            long elapsedMillis = Math.max(0, now - lastRefillAtMillis);
            double replenished = Math.min(capacity, availableTokens + (elapsedMillis * refillTokensPerMillis));
            return new BucketState(replenished, now, allowed, remainingTokens, retryAfterMillis);
        }

        BucketState consume(int requestedTokens, double refillTokensPerMillis) {
            if (requestedTokens <= availableTokens) {
                double tokensLeft = availableTokens - requestedTokens;
                return new BucketState(tokensLeft, lastRefillAtMillis, true, (int) Math.floor(tokensLeft), 0);
            }
            double missingTokens = requestedTokens - availableTokens;
            long retryAfter = (long) Math.ceil(missingTokens / refillTokensPerMillis);
            return new BucketState(availableTokens, lastRefillAtMillis, false, (int) Math.floor(availableTokens), retryAfter);
        }
    }
}
