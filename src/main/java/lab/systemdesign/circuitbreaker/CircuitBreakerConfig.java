package lab.systemdesign.circuitbreaker;

import java.time.Duration;

public record CircuitBreakerConfig(
        int failureThreshold,
        int successThreshold,
        Duration openStateWait
) {
    public CircuitBreakerConfig {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("failureThreshold must be positive");
        }
        if (successThreshold <= 0) {
            throw new IllegalArgumentException("successThreshold must be positive");
        }
        if (openStateWait.isNegative() || openStateWait.isZero()) {
            throw new IllegalArgumentException("openStateWait must be positive");
        }
    }
}
