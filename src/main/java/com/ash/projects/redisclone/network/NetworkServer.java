package com.ash.projects.redisclone.network;

import com.ash.projects.redisclone.service.CacheService;
import com.ash.projects.redisclone.service.RedisCommandService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
@ConditionalOnProperty(name = "network.server.enabled", havingValue = "true", matchIfMissing = true)
public class NetworkServer {

    private static final Logger logger = LoggerFactory.getLogger(NetworkServer.class);

    @Value("${network.server.port:6379}")
    private int port;

    @Value("${network.server.bind.address:0.0.0.0}")
    private String bindAddress;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private RedisCommandService commandService;

    private IoAcceptor acceptor;

    @PostConstruct
    public void start() throws IOException {
        acceptor = new NioSocketAcceptor();

        // Add filters
        acceptor.getFilterChain().addLast("logger", new LoggingFilter());
        acceptor.getFilterChain().addLast("codec",
                new ProtocolCodecFilter(new TextLineCodecFactory(StandardCharsets.UTF_8)));

        // Set handler
        acceptor.setHandler(new RedisProtocolHandler());

        // Configure
        acceptor.getSessionConfig().setReadBufferSize(2048);
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);

        // Bind
        acceptor.bind(new InetSocketAddress(bindAddress, port));

        logger.info("Network server started on {}:{}", bindAddress, port);
    }

    @PreDestroy
    public void stop() {
        if (acceptor != null) {
            acceptor.unbind();
            acceptor.dispose();
            logger.info("Network server stopped");
        }
    }

    private class RedisProtocolHandler extends IoHandlerAdapter {

        @Override
        public void sessionCreated(IoSession session) {
            logger.debug("Session created: {}", session.getRemoteAddress());
        }

        @Override
        public void sessionClosed(IoSession session) {
            logger.debug("Session closed: {}", session.getRemoteAddress());
        }

        @Override
        public void messageReceived(IoSession session, Object message) {
            String command = (String) message;
            logger.debug("Received command: {}", command);

            try {
                String response = processCommand(command);
                session.write(response);
            } catch (Exception e) {
                logger.error("Error processing command: {}", command, e);
                session.write("-ERR " + e.getMessage());
            }
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) {
            logger.error("Exception in session", cause);
            session.closeNow();
        }

        private String processCommand(String commandLine) {
            String[] parts = commandLine.trim().split("\\s+");
            if (parts.length == 0) {
                return "-ERR empty command";
            }

            String cmd = parts[0].toUpperCase();
            String region = null;
            int argStart = 1;

            // Check if region is specified
            if (parts.length > 1 && parts[1].startsWith("@")) {
                region = parts[1].substring(1);
                argStart = 2;
            }

            return switch (cmd) {
                case "PING" -> "+PONG";
                case "SET" -> handleSet(region, parts, argStart);
                case "GET" -> handleGet(region, parts, argStart);
                case "DEL" -> handleDel(region, parts, argStart);
                case "EXISTS" -> handleExists(region, parts, argStart);
                case "EXPIRE" -> handleExpire(region, parts, argStart);
                case "TTL" -> handleTtl(region, parts, argStart);
                case "PERSIST" -> handlePersist(region, parts, argStart);
                case "KEYS" -> handleKeys(region, parts, argStart);
                case "INCR" -> handleIncr(region, parts, argStart);
                case "DECR" -> handleDecr(region, parts, argStart);
                case "APPEND" -> handleAppend(region, parts, argStart);
                case "INFO" -> handleInfo();
                default -> "-ERR unknown command '" + cmd + "'";
            };
        }

        private String handleSet(String region, String[] parts, int start) {
            if (parts.length < start + 2) {
                return "-ERR wrong number of arguments for 'set' command";
            }

            String key = parts[start];
            String value = parts[start + 1];
            Long expiresAt = null;

            // Check for EX option
            if (parts.length > start + 2 && "EX".equalsIgnoreCase(parts[start + 2])) {
                if (parts.length > start + 3) {
                    long seconds = Long.parseLong(parts[start + 3]);
                    expiresAt = System.currentTimeMillis() + (seconds * 1000);
                }
            }

            cacheService.set(region, key, value, expiresAt);
            return "+OK";
        }

        private String handleGet(String region, String[] parts, int start) {
            if (parts.length < start + 1) {
                return "-ERR wrong number of arguments for 'get' command";
            }

            String key = parts[start];
            String value = cacheService.get(region, key);

            if (value == null) {
                return "$-1"; // Null bulk string
            }

            return "$" + value.length() + "\r\n" + value;
        }

        private String handleDel(String region, String[] parts, int start) {
            if (parts.length < start + 1) {
                return "-ERR wrong number of arguments for 'del' command";
            }

            String[] keys = Arrays.copyOfRange(parts, start, parts.length);
            long deleted = cacheService.del(region, keys);

            return ":" + deleted;
        }

        private String handleExists(String region, String[] parts, int start) {
            if (parts.length < start + 1) {
                return "-ERR wrong number of arguments for 'exists' command";
            }

            String[] keys = Arrays.copyOfRange(parts, start, parts.length);
            long count = cacheService.exists(region, keys);

            return ":" + count;
        }

        private String handleExpire(String region, String[] parts, int start) {
            if (parts.length < start + 2) {
                return "-ERR wrong number of arguments for 'expire' command";
            }

            String key = parts[start];
            long seconds = Long.parseLong(parts[start + 1]);

            boolean result = cacheService.expire(region, key, seconds);
            return ":" + (result ? 1 : 0);
        }

        private String handleTtl(String region, String[] parts, int start) {
            if (parts.length < start + 1) {
                return "-ERR wrong number of arguments for 'ttl' command";
            }

            String key = parts[start];
            long ttl = cacheService.ttl(region, key);

            return ":" + ttl;
        }

        private String handlePersist(String region, String[] parts, int start) {
            if (parts.length < start + 1) {
                return "-ERR wrong number of arguments for 'persist' command";
            }

            String key = parts[start];
            boolean result = cacheService.persist(region, key);

            return ":" + (result ? 1 : 0);
        }

        private String handleKeys(String region, String[] parts, int start) {
            if (parts.length < start + 1) {
                return "-ERR wrong number of arguments for 'keys' command";
            }

            String pattern = parts[start];
            var keys = cacheService.keys(region, pattern);

            StringBuilder response = new StringBuilder("*" + keys.size() + "\r\n");
            for (String key : keys) {
                response.append("$").append(key.length()).append("\r\n").append(key).append("\r\n");
            }

            return response.toString();
        }

        private String handleIncr(String region, String[] parts, int start) {
            if (parts.length < start + 1) {
                return "-ERR wrong number of arguments for 'incr' command";
            }

            String key = parts[start];
            long value = commandService.incr(region, key);

            return ":" + value;
        }

        private String handleDecr(String region, String[] parts, int start) {
            if (parts.length < start + 1) {
                return "-ERR wrong number of arguments for 'decr' command";
            }

            String key = parts[start];
            long value = commandService.decr(region, key);

            return ":" + value;
        }

        private String handleAppend(String region, String[] parts, int start) {
            if (parts.length < start + 2) {
                return "-ERR wrong number of arguments for 'append' command";
            }

            String key = parts[start];
            String value = parts[start + 1];
            long length = commandService.append(region, key, value);

            return ":" + length;
        }

        private String handleInfo() {
            var info = commandService.info(null);
            StringBuilder response = new StringBuilder();

            for (var entry : info.entrySet()) {
                response.append(entry.getKey()).append(":").append(entry.getValue()).append("\r\n");
            }

            return "$" + response.length() + "\r\n" + response;
        }
    }
}