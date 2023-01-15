package io.collective;

import java.time.Clock;
import java.util.*;

public class SimpleAgedCache {
    /*
    size - tracks size of the cache
    keyToExpiry - maps key to expiry, useful when answering get calls
    expiryToKey - maps expiry to key sets, useful when clearing outdated keys from cache.
    cache - key value store
    sentinel - placeholder expiry epoch in case no clock is passed (cache does not behave like an aged cache)
     */
    Clock clock;
    HashMap<Object, Long> keyToExpiry = new HashMap<>();
    TreeMap<Long, HashSet<Object>> expiryToKey = new TreeMap<>();
    HashMap<Object, Object> cache = new HashMap<>();
    Long sentinel = Long.MAX_VALUE;

    public SimpleAgedCache(Clock clock) {
        this.clock = clock;
    }

    public SimpleAgedCache() {
        this.clock = null;
    }

    public void put(Object key, Object value, int retentionInMillis) {
        if(retentionInMillis < 0) return;

        Long expiryEpoch = this.clock==null?sentinel:(this.clock.millis()+retentionInMillis);
        cache.put(key, value);
        keyToExpiry.put(key, expiryEpoch);
        HashSet<Object> keySet = expiryToKey.getOrDefault(expiryEpoch, new HashSet<>());
        keySet.add(key);
        expiryToKey.put(expiryEpoch, keySet);
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    public int size() {
        updateCache();
        return cache.size();
    }

    public Object get(Object key) {
        if(this.clock==null || !this.cache.containsKey(key))
            return cache.getOrDefault(key, null);
        if(this.clock.millis()>keyToExpiry.get(key))
            return null;

        return this.cache.get(key);
    }

    private void updateCache() {
        if(this.clock==null)
            return;

        Long expiryEpoch = this.clock.millis();
        Collection<HashSet<Object>> expiredKeySets = expiryToKey.headMap(expiryEpoch).values();
        for(HashSet<Object> keySet: expiredKeySets){
            for(Object key: keySet){
                this.cache.remove(key);
                this.keyToExpiry.remove(key);
            }
        }
        expiryToKey.headMap(expiryEpoch).clear();
    }
}