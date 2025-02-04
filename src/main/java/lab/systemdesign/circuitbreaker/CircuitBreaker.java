package lab.systemdesign.circuitbreaker;

import java.util.Objects;
import java.util.concurrent.Callable;

public class CircuitBreaker {
    private final CircuitBreakerConfig config;
    private final Clock clock;

    private State state = State.CLOSED;
    private int consecutiveFailures = 0;
    private int halfOpenSuccesses = 0;
    private long openedAtMillis = -1;

    public CircuitBreaker(CircuitBreakerConfig config, Clock clock) {
        this.config = Objects.requireNonNull(config, "config");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized <T> T execute(Callable<T> operation) throws Exception {
        transitionToHalfOpenIfReady();
        if (state == State.OPEN) {
            throw new CircuitBreakerOpenException("Circuit breaker is open");
        }

        try {
            T result = operation.call();
            onSuccess();
            return result;
        } catch (Exception ex) {
            onFailure();
            throw ex;
        }
    }

    public synchronized State state() {
        transitionToHalfOpenIfReady();
        return state;
    }

    private void onSuccess() {
        if (state == State.HALF_OPEN) {
            halfOpenSuccesses++;
            if (halfOpenSuccesses >= config.successThreshold()) {
                state = State.CLOSED;
                consecutiveFailures = 0;
                halfOpenSuccesses = 0;
                openedAtMillis = -1;
            }
            return;
        }

        consecutiveFailures = 0;
    }

    private void onFailure() {
        if (state == State.HALF_OPEN) {
            open();
            return;
        }

        consecutiveFailures++;
        if (consecutiveFailures >= config.failureThreshold()) {
            open();
        }
    }

    private void open() {
        state = State.OPEN;
        openedAtMillis = clock.millis();
        consecutiveFailures = 0;
        halfOpenSuccesses = 0;
    }

    private void transitionToHalfOpenIfReady() {
        if (state != State.OPEN) {
            return;
        }
        long elapsed = clock.millis() - openedAtMillis;
        if (elapsed >= config.openStateWait().toMillis()) {
            state = State.HALF_OPEN;
            halfOpenSuccesses = 0;
        }
    }

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}
