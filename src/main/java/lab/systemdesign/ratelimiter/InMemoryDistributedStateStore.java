package lab.systemdesign.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class InMemoryDistributedStateStore<K, V> implements DistributedStateStore<K, V> {
    private final ConcurrentHashMap<K, V> store = new ConcurrentHashMap<>();

    @Override
    public V compute(K key, Function<V, V> updateFunction) {
        return store.compute(key, (ignored, existing) -> updateFunction.apply(existing));
    }
}
