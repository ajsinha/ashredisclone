package com.ash.projects.redisclone.client;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Java client for AshRedis Clone
 * Usage:
 * AshRedisClient client = new AshRedisClient("localhost", 6379);
 * client.connect();
 * client.set("mykey", "myvalue");
 * String value = client.get("mykey");
 * client.close();
 */
public class AshRedisClient implements AutoCloseable {

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String defaultRegion;

    private final Map<String, Consumer<String>> channelSubscribers = new ConcurrentHashMap<>();
    private Thread subscriptionThread;
    private volatile boolean subscriptionActive = false;

    public AshRedisClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.defaultRegion = null;
    }

    public AshRedisClient(String host, int port, String defaultRegion) {
        this.host = host;
        this.port = port;
        this.defaultRegion = defaultRegion;
    }

    /**
     * Connect to the server
     */
    public void connect() throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Set default region for all operations
     */
    public void useRegion(String region) {
        this.defaultRegion = region;
    }

    /**
     * PING command
     */
    public String ping() throws IOException {
        return sendCommand("PING");
    }

    /**
     * SET command
     */
    public String set(String key, String value) throws IOException {
        return setInRegion(defaultRegion, key, value, null);
    }

    public String setInRegion(String region, String key, String value) throws IOException {
        return setInRegion(region, key, value, null);
    }

    public String setInRegion(String region, String key, String value, Long ttlSeconds) throws IOException {
        StringBuilder cmd = new StringBuilder("SET");
        if (region != null) {
            cmd.append(" @").append(region);
        }
        cmd.append(" ").append(key).append(" ").append(value);

        if (ttlSeconds != null && ttlSeconds > 0) {
            cmd.append(" EX ").append(ttlSeconds);
        }

        return sendCommand(cmd.toString());
    }

    /**
     * GET command
     */
    public String get(String key) throws IOException {
        return getInRegion(defaultRegion, key);
    }

    public String getInRegion(String region, String key) throws IOException {
        StringBuilder cmd = new StringBuilder("GET");
        if (region != null) {
            cmd.append(" @").append(region);
        }
        cmd.append(" ").append(key);

        return sendCommand(cmd.toString());
    }

    /**
     * DEL command
     */
    public long del(String... keys) throws IOException {
        return delInRegion(defaultRegion, keys);
    }

    public long delInRegion(String region, String... keys) throws IOException {
        StringBuilder cmd = new StringBuilder("DEL");
        if (region != null) {
            cmd.append(" @").append(region);
        }
        for (String key : keys) {
            cmd.append(" ").append(key);
        }

        String response = sendCommand(cmd.toString());
        return parseInteger(response);
    }

    /**
     * EXISTS command
     */
    public long exists(String... keys) throws IOException {
        return existsInRegion(defaultRegion, keys);
    }

    public long existsInRegion(String region, String... keys) throws IOException {
        StringBuilder cmd = new StringBuilder("EXISTS");
        if (region != null) {
            cmd.append(" @").append(region);
        }
        for (String key : keys) {
            cmd.append(" ").append(key);
        }

        String response = sendCommand(cmd.toString());
        return parseInteger(response);
    }

    /**
     * EXPIRE command
     */
    public boolean expire(String key, long seconds) throws IOException {
        return expireInRegion(defaultRegion, key, seconds);
    }

    public boolean expireInRegion(String region, String key, long seconds) throws IOException {
        StringBuilder cmd = new StringBuilder("EXPIRE");
        if (region != null) {
            cmd.append(" @").append(region);
        }
        cmd.append(" ").append(key).append(" ").append(seconds);

        String response = sendCommand(cmd.toString());
        return parseInteger(response) == 1;
    }

    /**
     * TTL command
     */
    public long ttl(String key) throws IOException {
        return ttlInRegion(defaultRegion, key);
    }

    public long ttlInRegion(String region, String key) throws IOException {
        StringBuilder cmd = new StringBuilder("TTL");
        if (region != null) {
            cmd.append(" @").append(region);
        }
        cmd.append(" ").append(key);

        String response = sendCommand(cmd.toString());
        return parseInteger(response);
    }

    /**
     * PERSIST command
     */
    public boolean persist(String key) throws IOException {
        return persistInRegion(defaultRegion, key);
    }

    public boolean persistInRegion(String region, String key) throws IOException {
        StringBuilder cmd = new StringBuilder("PERSIST");
        if (region != null) {
            cmd.append(" @").append(region);
        }
        cmd.append(" ").append(key);

        String response = sendCommand(cmd.toString());
        return parseInteger(response) == 1;
    }

    /**
     * KEYS command
     */
    public Set<String> keys(String pattern) throws IOException {
        return keysInRegion(defaultRegion, pattern);
    }

    public Set<String> keysInRegion(String region, String pattern) throws IOException {
        StringBuilder cmd = new StringBuilder("KEYS");
        if (region != null) {
            cmd.append(" @").append(region);
        }
        cmd.append(" ").append(pattern);

        String response = sendCommand(cmd.toString());
        return parseArray(response);
    }

    /**
     * INCR command
     */
    public long incr(String key) throws IOException {
        return incrInRegion(defaultRegion, key);
    }

    public long incrInRegion(String region, String key) throws IOException {
        StringBuilder cmd = new StringBuilder("INCR");
        if (region != null) {
            cmd.append(" @").append(region);
        }
        cmd.append(" ").append(key);

        String response = sendCommand(cmd.toString());
        return parseInteger(response);
    }

    /**
     * DECR command
     */
    public long decr(String key) throws IOException {
        return decrInRegion(defaultRegion, key);
    }

    public long decrInRegion(String region, String key) throws IOException {
        StringBuilder cmd = new StringBuilder("DECR");
        if (region != null) {
            cmd.append(" @").append(region);
        }
        cmd.append(" ").append(key);

        String response = sendCommand(cmd.toString());
        return parseInteger(response);
    }

    /**
     * APPEND command
     */
    public long append(String key, String value) throws IOException {
        return appendInRegion(defaultRegion, key, value);
    }

    public long appendInRegion(String region, String key, String value) throws IOException {
        StringBuilder cmd = new StringBuilder("APPEND");
        if (region != null) {
            cmd.append(" @").append(region);
        }
        cmd.append(" ").append(key).append(" ").append(value);

        String response = sendCommand(cmd.toString());
        return parseInteger(response);
    }

    /**
     * Subscribe to a channel
     */
    public void subscribe(String channel, Consumer<String> callback) {
        channelSubscribers.put(channel, callback);

        if (!subscriptionActive) {
            startSubscriptionThread();
        }
    }

    /**
     * Unsubscribe from a channel
     */
    public void unsubscribe(String channel) {
        channelSubscribers.remove(channel);
    }

    private void startSubscriptionThread() {
        subscriptionActive = true;
        subscriptionThread = new Thread(() -> {
            try {
                while (subscriptionActive && isConnected()) {
                    String message = reader.readLine();
                    if (message != null) {
                        processSubscriptionMessage(message);
                    }
                }
            } catch (IOException e) {
                if (subscriptionActive) {
                    e.printStackTrace();
                }
            }
        });
        subscriptionThread.setDaemon(true);
        subscriptionThread.start();
    }

    private void processSubscriptionMessage(String message) {
        // Parse and dispatch to appropriate callback
        // Format expected: CHANNEL:channel_name:message
        if (message.startsWith("CHANNEL:")) {
            String[] parts = message.split(":", 3);
            if (parts.length == 3) {
                String channel = parts[1];
                String content = parts[2];

                Consumer<String> callback = channelSubscribers.get(channel);
                if (callback != null) {
                    callback.accept(content);
                }
            }
        }
    }

    /**
     * Send command and get response
     */
    private synchronized String sendCommand(String command) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected to server");
        }

        writer.println(command);
        String response = reader.readLine();

        if (response == null) {
            throw new IOException("Connection closed by server");
        }

        return parseResponse(response);
    }

    private String parseResponse(String response) throws IOException {
        if (response.startsWith("+")) {
            return response.substring(1);
        } else if (response.startsWith("-")) {
            throw new IOException("Error: " + response.substring(1));
        } else if (response.startsWith(":")) {
            return response.substring(1);
        } else if (response.startsWith("$")) {
            int length = Integer.parseInt(response.substring(1));
            if (length == -1) {
                return null;
            }
            return reader.readLine();
        } else if (response.startsWith("*")) {
            return response; // Return array response as is for further parsing
        }

        return response;
    }

    private long parseInteger(String response) {
        try {
            return Long.parseLong(response);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Set<String> parseArray(String response) throws IOException {
        Set<String> result = new HashSet<>();

        if (response.startsWith("*")) {
            int count = Integer.parseInt(response.substring(1).split("\r\n")[0]);

            for (int i = 0; i < count; i++) {
                String lengthLine = reader.readLine();
                if (lengthLine.startsWith("$")) {
                    int length = Integer.parseInt(lengthLine.substring(1));
                    if (length > 0) {
                        result.add(reader.readLine());
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void close() throws IOException {
        subscriptionActive = false;

        if (subscriptionThread != null) {
            subscriptionThread.interrupt();
        }

        if (reader != null) {
            reader.close();
        }

        if (writer != null) {
            writer.close();
        }

        if (socket != null) {
            socket.close();
        }
    }

    /**
     * Example usage
     */
    public static void main(String[] args) {
        try (AshRedisClient client = new AshRedisClient("localhost", 6379)) {
            client.connect();

            System.out.println("PING: " + client.ping());

            // Set and get a value
            client.set("mykey", "Hello, AshRedis!");
            System.out.println("GET mykey: " + client.get("mykey"));

            // Use a specific region
            client.setInRegion("users", "user:1", "John Doe");
            System.out.println("GET user:1 from users: " + client.getInRegion("users", "user:1"));

            // Increment counter
            client.set("counter", "0");
            long value = client.incr("counter");
            System.out.println("Counter after INCR: " + value);

            // Check existence
            long existsCount = client.exists("mykey", "counter");
            System.out.println("Keys exist: " + existsCount);

            // Set expiration
            client.expire("mykey", 60);
            long ttl = client.ttl("mykey");
            System.out.println("TTL for mykey: " + ttl + " seconds");

            // Pattern matching
            Set<String> keys = client.keys("*");
            System.out.println("All keys: " + keys);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}