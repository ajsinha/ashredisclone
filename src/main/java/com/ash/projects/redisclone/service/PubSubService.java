package com.ash.projects.redisclone.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

@Service
public class PubSubService {

    private static final Logger logger = LoggerFactory.getLogger(PubSubService.class);

    // Channel -> Subscribers
    private final Map<String, Set<Consumer<String>>> channelSubscribers = new ConcurrentHashMap<>();

    // Region -> Subscribers
    private final Map<String, Set<Consumer<ChangeEvent>>> regionSubscribers = new ConcurrentHashMap<>();

    // Key -> Subscribers (region-specific)
    private final Map<String, Map<String, Set<Consumer<ChangeEvent>>>> keySubscribers = new ConcurrentHashMap<>();

    /**
     * Publish a message to a channel
     */
    public long publish(String channel, String message) {
        Set<Consumer<String>> subscribers = channelSubscribers.get(channel);

        if (subscribers == null || subscribers.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Consumer<String> subscriber : subscribers) {
            try {
                subscriber.accept(message);
                count++;
            } catch (Exception e) {
                logger.error("Error notifying subscriber on channel: {}", channel, e);
            }
        }

        logger.debug("Published message to channel '{}' with {} subscribers", channel, count);
        return count;
    }

    /**
     * Subscribe to a channel
     */
    public void subscribe(String channel, Consumer<String> subscriber) {
        channelSubscribers.computeIfAbsent(channel, k -> new CopyOnWriteArraySet<>()).add(subscriber);
        logger.info("New subscriber added to channel: {}", channel);
    }

    /**
     * Unsubscribe from a channel
     */
    public void unsubscribe(String channel, Consumer<String> subscriber) {
        Set<Consumer<String>> subscribers = channelSubscribers.get(channel);
        if (subscribers != null) {
            subscribers.remove(subscriber);
            if (subscribers.isEmpty()) {
                channelSubscribers.remove(channel);
            }
        }
        logger.info("Subscriber removed from channel: {}", channel);
    }

    /**
     * Subscribe to all changes in a region
     */
    public void subscribeToRegion(String region, Consumer<ChangeEvent> subscriber) {
        regionSubscribers.computeIfAbsent(region, k -> new CopyOnWriteArraySet<>()).add(subscriber);
        logger.info("New subscriber added to region: {}", region);
    }

    /**
     * Unsubscribe from region changes
     */
    public void unsubscribeFromRegion(String region, Consumer<ChangeEvent> subscriber) {
        Set<Consumer<ChangeEvent>> subscribers = regionSubscribers.get(region);
        if (subscribers != null) {
            subscribers.remove(subscriber);
            if (subscribers.isEmpty()) {
                regionSubscribers.remove(region);
            }
        }
        logger.info("Subscriber removed from region: {}", region);
    }

    /**
     * Subscribe to changes for a specific key in a region
     */
    public void subscribeToKey(String region, String key, Consumer<ChangeEvent> subscriber) {
        keySubscribers.computeIfAbsent(region, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, k -> new CopyOnWriteArraySet<>())
                .add(subscriber);
        logger.info("New subscriber added to key: {}@{}", key, region);
    }

    /**
     * Unsubscribe from key changes
     */
    public void unsubscribeFromKey(String region, String key, Consumer<ChangeEvent> subscriber) {
        Map<String, Set<Consumer<ChangeEvent>>> regionKeys = keySubscribers.get(region);
        if (regionKeys != null) {
            Set<Consumer<ChangeEvent>> subscribers = regionKeys.get(key);
            if (subscribers != null) {
                subscribers.remove(subscriber);
                if (subscribers.isEmpty()) {
                    regionKeys.remove(key);
                }
            }
            if (regionKeys.isEmpty()) {
                keySubscribers.remove(region);
            }
        }
        logger.info("Subscriber removed from key: {}@{}", key, region);
    }

    /**
     * Publish a change event (called by CacheService)
     */
    public void publishChange(String region, String key, String operation) {
        ChangeEvent event = new ChangeEvent(region, key, operation, System.currentTimeMillis());

        // Notify region subscribers
        Set<Consumer<ChangeEvent>> regionSubs = regionSubscribers.get(region);
        if (regionSubs != null) {
            for (Consumer<ChangeEvent> subscriber : regionSubs) {
                try {
                    subscriber.accept(event);
                } catch (Exception e) {
                    logger.error("Error notifying region subscriber", e);
                }
            }
        }

        // Notify key-specific subscribers
        Map<String, Set<Consumer<ChangeEvent>>> regionKeys = keySubscribers.get(region);
        if (regionKeys != null) {
            Set<Consumer<ChangeEvent>> keySubs = regionKeys.get(key);
            if (keySubs != null) {
                for (Consumer<ChangeEvent> subscriber : keySubs) {
                    try {
                        subscriber.accept(event);
                    } catch (Exception e) {
                        logger.error("Error notifying key subscriber", e);
                    }
                }
            }
        }
    }

    /**
     * Get subscriber counts for monitoring
     */
    public Map<String, Integer> getSubscriberCounts() {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("channels", channelSubscribers.size());
        counts.put("regions", regionSubscribers.size());
        counts.put("keys", keySubscribers.values().stream()
                .mapToInt(Map::size)
                .sum());
        return counts;
    }

    /**
     * Change event model
     */
    public static class ChangeEvent {
        private final String region;
        private final String key;
        private final String operation;
        private final long timestamp;

        public ChangeEvent(String region, String key, String operation, long timestamp) {
            this.region = region;
            this.key = key;
            this.operation = operation;
            this.timestamp = timestamp;
        }

        public String getRegion() { return region; }
        public String getKey() { return key; }
        public String getOperation() { return operation; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("ChangeEvent{region='%s', key='%s', op='%s', time=%d}",
                    region, key, operation, timestamp);
        }
    }
}