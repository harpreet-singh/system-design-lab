package lab.systemdesign.ratelimiter;

import java.util.function.Function;

public interface DistributedStateStore<K, V> {
    V compute(K key, Function<V, V> updateFunction);
}
