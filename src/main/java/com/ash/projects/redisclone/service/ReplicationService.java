package com.ash.projects.redisclone.service;

import com.ash.projects.redisclone.model.DataType;
import com.ash.projects.redisclone.model.ReplicationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(name = "kafka.replication.enabled", havingValue = "true")
public class ReplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ReplicationService.class);

    @Value("${kafka.replication.topic:ashredis-replication}")
    private String replicationTopic;

    @Value("${kafka.health.topic:ashredis-health}")
    private String healthTopic;

    @Value("${cluster.instance.id}")
    private String instanceId;

    @Value("${cluster.mode:primary}")
    private String initialMode;

    @Value("${cluster.enabled:false}")
    private boolean clusterEnabled;

    @Value("${cluster.health.check.interval.ms:5000}")
    private long healthCheckInterval;

    @Value("${cluster.failover.timeout.ms:15000}")
    private long failoverTimeout;

    @Value("${cluster.auto.promote.on.no.primary:true}")
    private boolean autoPromoteOnNoPrimary;

    @Value("${cluster.startup.wait.for.primary.ms:10000}")
    private long startupWaitForPrimary;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private CacheService cacheService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicBoolean isPrimary = new AtomicBoolean(false);
    private final AtomicLong lastPrimaryHeartbeat = new AtomicLong(0);
    private final ConcurrentHashMap<String, Long> instanceHeartbeats = new ConcurrentHashMap<>();

    private String currentPrimaryId = null;

    @PostConstruct
    public void initialize() {
        // If clustering is disabled, always be primary
        if (!clusterEnabled) {
            isPrimary.set(true);
            logger.info("Clustering disabled - Running as PRIMARY");
            return;
        }

        isPrimary.set("primary".equalsIgnoreCase(initialMode));
        logger.info("Replication service initialized - Instance ID: {}, Initial Mode: {}",
                instanceId, isPrimary.get() ? "PRIMARY" : "SECONDARY");

        // If started as secondary, check if primary exists
        if (!isPrimary.get() && autoPromoteOnNoPrimary) {
            scheduleStartupPrimaryCheck();
        }
    }

    /**
     * Check for primary existence during startup
     */
    private void scheduleStartupPrimaryCheck() {
        new Thread(() -> {
            try {
                logger.info("Waiting {} ms to detect primary instance...", startupWaitForPrimary);
                Thread.sleep(startupWaitForPrimary);

                // Check if we've seen a primary
                long timeSinceLastPrimary = System.currentTimeMillis() - lastPrimaryHeartbeat.get();

                if (lastPrimaryHeartbeat.get() == 0 || timeSinceLastPrimary > failoverTimeout) {
                    logger.warn("No primary detected after {} ms. Auto-promoting to PRIMARY.",
                            startupWaitForPrimary);
                    tryBecomePrimary();
                } else {
                    logger.info("Primary detected. Remaining as SECONDARY.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Startup primary check interrupted", e);
            }
        }, "startup-primary-check").start();
    }

    /**
     * Check if this instance is primary
     */
    public boolean isPrimary() {
        return isPrimary.get();
    }

    /**
     * Replicate SET operation
     */
    public void replicateSet(String region, String key, Object value, DataType dataType, Long expiresAt) {
        if (!isPrimary.get()) {
            logger.warn("Cannot replicate - not primary instance");
            return;
        }

        try {
            ReplicationEvent event = new ReplicationEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setEventType("SET");
            event.setRegion(region);
            event.setKey(key);
            event.setValue(value);
            event.setDataType(dataType);
            event.setExpiresAt(expiresAt);

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(replicationTopic, key, json);

            logger.debug("Replicated SET: region={}, key={}", region, key);
        } catch (Exception e) {
            logger.error("Error replicating SET operation", e);
        }
    }

    /**
     * Replicate DELETE operation
     */
    public void replicateDelete(String region, String key) {
        if (!isPrimary.get()) {
            return;
        }

        try {
            ReplicationEvent event = new ReplicationEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setEventType("DELETE");
            event.setRegion(region);
            event.setKey(key);

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(replicationTopic, key, json);

            logger.debug("Replicated DELETE: region={}, key={}", region, key);
        } catch (Exception e) {
            logger.error("Error replicating DELETE operation", e);
        }
    }

    /**
     * Replicate EXPIRE operation
     */
    public void replicateExpire(String region, String key, long seconds) {
        if (!isPrimary.get()) {
            return;
        }

        try {
            ReplicationEvent event = new ReplicationEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setEventType("EXPIRE");
            event.setRegion(region);
            event.setKey(key);
            event.setValue(seconds);

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(replicationTopic, key, json);

            logger.debug("Replicated EXPIRE: region={}, key={}, seconds={}", region, key, seconds);
        } catch (Exception e) {
            logger.error("Error replicating EXPIRE operation", e);
        }
    }

    /**
     * Listen for replication events (secondary instances)
     */
    @KafkaListener(topics = "${kafka.replication.topic:ashredis-replication}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void handleReplicationEvent(String message) {
        if (isPrimary.get()) {
            // Primary doesn't process its own replication events
            return;
        }

        try {
            ReplicationEvent event = objectMapper.readValue(message, ReplicationEvent.class);

            switch (event.getEventType()) {
                case "SET" -> {
                    cacheService.set(event.getRegion(), event.getKey(),
                            (String) event.getValue(), event.getExpiresAt());
                    logger.debug("Applied replicated SET: region={}, key={}",
                            event.getRegion(), event.getKey());
                }
                case "DELETE" -> {
                    cacheService.del(event.getRegion(), event.getKey());
                    logger.debug("Applied replicated DELETE: region={}, key={}",
                            event.getRegion(), event.getKey());
                }
                case "EXPIRE" -> {
                    long seconds = ((Number) event.getValue()).longValue();
                    cacheService.expire(event.getRegion(), event.getKey(), seconds);
                    logger.debug("Applied replicated EXPIRE: region={}, key={}",
                            event.getRegion(), event.getKey());
                }
                default -> logger.warn("Unknown replication event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            logger.error("Error processing replication event", e);
        }
    }

    /**
     * Send heartbeat (all instances)
     */
    @Scheduled(fixedDelayString = "${cluster.health.check.interval.ms:5000}")
    public void sendHeartbeat() {
        try {
            String heartbeat = String.format("{\"instanceId\":\"%s\",\"isPrimary\":%b,\"timestamp\":%d}",
                    instanceId, isPrimary.get(), System.currentTimeMillis());

            kafkaTemplate.send(healthTopic, instanceId, heartbeat);

            if (isPrimary.get()) {
                lastPrimaryHeartbeat.set(System.currentTimeMillis());
            }

            logger.trace("Sent heartbeat: {}", heartbeat);
        } catch (Exception e) {
            logger.error("Error sending heartbeat", e);
        }
    }

    /**
     * Listen for heartbeats and handle failover
     */
    @KafkaListener(topics = "${kafka.health.topic:ashredis-health}",
            groupId = "${spring.kafka.consumer.group-id}-health")
    public void handleHeartbeat(String message) {
        try {
            var node = objectMapper.readTree(message);
            String nodeId = node.get("instanceId").asText();
            boolean nodeIsPrimary = node.get("isPrimary").asBoolean();
            long timestamp = node.get("timestamp").asLong();

            instanceHeartbeats.put(nodeId, timestamp);

            if (nodeIsPrimary) {
                currentPrimaryId = nodeId;
                lastPrimaryHeartbeat.set(timestamp);
            }

            // Check for failover condition
            checkFailover();

        } catch (Exception e) {
            logger.error("Error processing heartbeat", e);
        }
    }

    /**
     * Check if failover is needed
     */
    private void checkFailover() {
        long now = System.currentTimeMillis();
        long timeSinceLastPrimary = now - lastPrimaryHeartbeat.get();

        if (!isPrimary.get() && timeSinceLastPrimary > failoverTimeout) {
            logger.warn("Primary heartbeat timeout detected. Time since last: {}ms", timeSinceLastPrimary);

            // Attempt to become primary
            tryBecomePrimary();
        }
    }

    /**
     * Attempt to promote this instance to primary
     */
    private synchronized void tryBecomePrimary() {
        if (isPrimary.get()) {
            return;
        }

        logger.info("Attempting to become primary instance...");

        // Simple election: instance with lexicographically smallest ID becomes primary
        String smallestId = instanceHeartbeats.keySet().stream()
                .min(String::compareTo)
                .orElse(instanceId);

        if (instanceId.equals(smallestId)) {
            isPrimary.set(true);
            currentPrimaryId = instanceId;
            logger.info("This instance is now PRIMARY: {}", instanceId);

            // Send immediate heartbeat as new primary
            sendHeartbeat();
        } else {
            logger.info("Instance {} was elected as primary", smallestId);
        }
    }

    /**
     * Cleanup old heartbeats
     */
    @Scheduled(fixedRate = 30000)
    public void cleanupOldHeartbeats() {
        long cutoff = System.currentTimeMillis() - (failoverTimeout * 2);
        instanceHeartbeats.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Replication service shutting down");
    }
}