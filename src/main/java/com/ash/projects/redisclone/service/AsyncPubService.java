package com.ash.projects.redisclone.service;

import com.ash.projects.redisclone.model.CacheEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class AsyncPubService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncPubService.class);

    @Autowired
    private CacheService cacheService;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${activemq.broker.url:tcp://localhost:61616}")
    private String activemqBrokerUrl;

    @Value("${activemq.enabled:false}")
    private boolean activemqEnabled;

    @Value("${kafka.async.enabled:false}")
    private boolean kafkaAsyncEnabled;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Active MQ connections
    private final Map<String, javax.jms.Connection> activemqConnections = new ConcurrentHashMap<>();

    // Track active publications
    private final Map<String, PublicationStatus> activePublications = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        logger.info("AsyncPub Service initialized - Kafka: {}, ActiveMQ: {}",
                kafkaAsyncEnabled, activemqEnabled);

        if (activemqEnabled) {
            initializeActiveMQConnection("default", activemqBrokerUrl);
        }
    }

    private void initializeActiveMQConnection(String connectionName, String brokerUrl) {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            javax.jms.Connection connection = factory.createConnection();
            connection.start();
            activemqConnections.put(connectionName, connection);
            logger.info("ActiveMQ connection '{}' initialized: {}", connectionName, brokerUrl);
        } catch (JMSException e) {
            logger.error("Failed to initialize ActiveMQ connection: {}", connectionName, e);
        }
    }

    /**
     * PUBTODEST command
     * Publishes cache entries to a destination
     *
     * @param region Region to search in
     * @param keyPattern Key pattern (can be single key, list, or regex)
     * @param destination Destination in format: file://, kafka://, or activemq://
     * @return UUID tracking the publication
     */
    public String pubToDest(String region, String keyPattern, String destination) {
        String uuid = UUID.randomUUID().toString();

        PublicationStatus status = new PublicationStatus();
        status.setUuid(uuid);
        status.setRegion(region);
        status.setKeyPattern(keyPattern);
        status.setDestination(destination);
        status.setStartTime(System.currentTimeMillis());
        status.setStatus("INITIATED");

        activePublications.put(uuid, status);

        // Execute asynchronously
        CompletableFuture.runAsync(() -> executePublication(uuid, region, keyPattern, destination));

        logger.info("Publication initiated: uuid={}, region={}, pattern={}, dest={}",
                uuid, region, keyPattern, destination);

        return uuid;
    }

    @Async
    protected void executePublication(String uuid, String region, String keyPattern, String destination) {
        PublicationStatus status = activePublications.get(uuid);

        try {
            status.setStatus("IN_PROGRESS");

            // Find matching keys
            Set<String> matchingKeys = findMatchingKeys(region, keyPattern);
            status.setTotalKeys(matchingKeys.size());

            logger.info("Found {} matching keys for uuid={}", matchingKeys.size(), uuid);

            // Parse destination
            DestinationInfo destInfo = parseDestination(destination);

            // Send BEGIN marker
            publishMessage(destInfo, String.format("--begin uuid: %s--", uuid));

            // Publish each entry
            int published = 0;
            for (String key : matchingKeys) {
                try {
                    String value = cacheService.get(region, key);
                    if (value != null) {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("region", region);
                        entry.put("key", key);
                        entry.put("value", value);
                        entry.put("timestamp", System.currentTimeMillis());

                        String json = objectMapper.writeValueAsString(entry);
                        publishMessage(destInfo, json);

                        published++;
                        status.setPublishedKeys(published);
                    }
                } catch (Exception e) {
                    logger.error("Error publishing key: {}", key, e);
                    status.incrementErrors();
                }
            }

            // Send END marker
            publishMessage(destInfo, String.format("--end uuid: %s--", uuid));

            status.setStatus("COMPLETED");
            status.setEndTime(System.currentTimeMillis());

            logger.info("Publication completed: uuid={}, published={}/{}",
                    uuid, published, matchingKeys.size());

        } catch (Exception e) {
            logger.error("Error executing publication: uuid={}", uuid, e);
            status.setStatus("FAILED");
            status.setError(e.getMessage());
        } finally {
            status.setEndTime(System.currentTimeMillis());
        }
    }

    private Set<String> findMatchingKeys(String region, String keyPattern) {
        Set<String> result = new HashSet<>();

        // Check if it's a regex pattern or glob pattern
        if (keyPattern.contains("*") || keyPattern.contains("?")) {
            // Glob pattern
            result = cacheService.keys(region, keyPattern);
        } else if (keyPattern.startsWith("regex:")) {
            // Regex pattern
            String regex = keyPattern.substring(6);
            Pattern pattern = Pattern.compile(regex);

            Set<String> allKeys = cacheService.keys(region, "*");
            for (String key : allKeys) {
                if (pattern.matcher(key).matches()) {
                    result.add(key);
                }
            }
        } else if (keyPattern.contains(",")) {
            // Comma-separated list of keys
            String[] keys = keyPattern.split(",");
            for (String key : keys) {
                if (cacheService.exists(region, key.trim()) > 0) {
                    result.add(key.trim());
                }
            }
        } else {
            // Single key
            if (cacheService.exists(region, keyPattern) > 0) {
                result.add(keyPattern);
            }
        }

        return result;
    }

    private DestinationInfo parseDestination(String destination) {
        DestinationInfo info = new DestinationInfo();

        if (destination.startsWith("file://")) {
            info.type = DestinationType.FILE;
            info.path = destination.substring(7);
        } else if (destination.startsWith("kafka://")) {
            info.type = DestinationType.KAFKA;
            // Format: kafka://<connection>/topic/<topic>
            String remainder = destination.substring(8);
            String[] parts = remainder.split("/");
            if (parts.length >= 3 && "topic".equals(parts[1])) {
                info.connectionName = parts[0];
                info.topicName = parts[2];
            }
        } else if (destination.startsWith("activemq://")) {
            // Format: activemq://<connection>/queue/<queue> or activemq://<connection>/topic/<topic>
            String remainder = destination.substring(11);
            String[] parts = remainder.split("/");
            if (parts.length >= 3) {
                info.connectionName = parts[0];
                info.destinationType = parts[1]; // queue or topic
                info.destinationName = parts[2];

                if ("queue".equals(info.destinationType)) {
                    info.type = DestinationType.ACTIVEMQ_QUEUE;
                } else {
                    info.type = DestinationType.ACTIVEMQ_TOPIC;
                }
            }
        }

        return info;
    }

    private void publishMessage(DestinationInfo destInfo, String message) throws Exception {
        switch (destInfo.type) {
            case FILE -> publishToFile(destInfo.path, message);
            case KAFKA -> publishToKafka(destInfo.topicName, message);
            case ACTIVEMQ_QUEUE -> publishToActiveMQQueue(destInfo.connectionName,
                    destInfo.destinationName, message);
            case ACTIVEMQ_TOPIC -> publishToActiveMQTopic(destInfo.connectionName,
                    destInfo.destinationName, message);
        }
    }

    private void publishToFile(String filePath, String message) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(message);
            writer.newLine();
        }
    }

    private void publishToKafka(String topic, String message) {
        if (kafkaTemplate != null && kafkaAsyncEnabled) {
            kafkaTemplate.send(topic, message);
        } else {
            logger.warn("Kafka not available for publishing");
        }
    }

    private void publishToActiveMQQueue(String connectionName, String queueName, String message)
            throws JMSException {
        publishToActiveMQ(connectionName, queueName, message, false);
    }

    private void publishToActiveMQTopic(String connectionName, String topicName, String message)
            throws JMSException {
        publishToActiveMQ(connectionName, topicName, message, true);
    }

    private void publishToActiveMQ(String connectionName, String destinationName,
                                   String message, boolean isTopic) throws JMSException {
        javax.jms.Connection connection = activemqConnections.get(connectionName);
        if (connection == null) {
            throw new JMSException("ActiveMQ connection not found: " + connectionName);
        }

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        javax.jms.Destination destination;

        if (isTopic) {
            destination = session.createTopic(destinationName);
        } else {
            destination = session.createQueue(destinationName);
        }

        MessageProducer producer = session.createProducer(destination);
        javax.jms.TextMessage textMessage = session.createTextMessage(message);
        producer.send(textMessage);

        producer.close();
        session.close();
    }

    /**
     * Get publication status
     */
    public PublicationStatus getPublicationStatus(String uuid) {
        return activePublications.get(uuid);
    }

    /**
     * Get all active publications
     */
    public Collection<PublicationStatus> getAllPublications() {
        return new ArrayList<>(activePublications.values());
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down AsyncPub Service");

        // Close ActiveMQ connections
        for (javax.jms.Connection connection : activemqConnections.values()) {
            try {
                connection.close();
            } catch (JMSException e) {
                logger.error("Error closing ActiveMQ connection", e);
            }
        }
    }

    // Helper classes
    private enum DestinationType {
        FILE, KAFKA, ACTIVEMQ_QUEUE, ACTIVEMQ_TOPIC
    }

    private static class DestinationInfo {
        DestinationType type;
        String path;
        String connectionName;
        String topicName;
        String destinationType;
        String destinationName;
    }

    public static class PublicationStatus {
        private String uuid;
        private String region;
        private String keyPattern;
        private String destination;
        private String status;
        private int totalKeys;
        private int publishedKeys;
        private int errors;
        private long startTime;
        private Long endTime;
        private String error;

        // Getters and setters
        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }

        public String getKeyPattern() { return keyPattern; }
        public void setKeyPattern(String keyPattern) { this.keyPattern = keyPattern; }

        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getTotalKeys() { return totalKeys; }
        public void setTotalKeys(int totalKeys) { this.totalKeys = totalKeys; }

        public int getPublishedKeys() { return publishedKeys; }
        public void setPublishedKeys(int publishedKeys) { this.publishedKeys = publishedKeys; }

        public int getErrors() { return errors; }
        public void setErrors(int errors) { this.errors = errors; }
        public void incrementErrors() { this.errors++; }

        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }

        public Long getEndTime() { return endTime; }
        public void setEndTime(Long endTime) { this.endTime = endTime; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public long getDurationMs() {
            if (endTime != null) {
                return endTime - startTime;
            }
            return System.currentTimeMillis() - startTime;
        }
    }
}