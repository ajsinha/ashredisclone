package com.ash.projects.redisclone.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Transaction support for MULTI, EXEC, and DISCARD commands
 */
@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private CacheService cacheService;

    @Autowired
    private RedisCommandService commandService;

    // Session ID -> Transaction Context
    private final Map<String, TransactionContext> activeTransactions = new ConcurrentHashMap<>();

    /**
     * MULTI - Start a transaction
     */
    public String multi(String sessionId) {
        if (activeTransactions.containsKey(sessionId)) {
            return "-ERR MULTI calls can not be nested";
        }

        TransactionContext context = new TransactionContext();
        context.setSessionId(sessionId);
        context.setStartTime(System.currentTimeMillis());

        activeTransactions.put(sessionId, context);

        logger.debug("Transaction started for session: {}", sessionId);
        return "+OK";
    }

    /**
     * Add a command to the transaction queue
     */
    public String queueCommand(String sessionId, Supplier<Object> command, String commandDesc) {
        TransactionContext context = activeTransactions.get(sessionId);

        if (context == null) {
            // Not in transaction mode, execute immediately
            try {
                Object result = command.get();
                return formatResult(result);
            } catch (Exception e) {
                return "-ERR " + e.getMessage();
            }
        }

        // In transaction mode, queue the command
        context.addCommand(command, commandDesc);
        return "+QUEUED";
    }

    /**
     * EXEC - Execute all queued commands
     */
    public List<String> exec(String sessionId) {
        TransactionContext context = activeTransactions.remove(sessionId);

        if (context == null) {
            return List.of("-ERR EXEC without MULTI");
        }

        List<String> results = new ArrayList<>();

        logger.debug("Executing transaction with {} commands for session: {}",
                context.getCommands().size(), sessionId);

        for (QueuedCommand queuedCommand : context.getCommands()) {
            try {
                Object result = queuedCommand.getCommand().get();
                results.add(formatResult(result));
            } catch (Exception e) {
                logger.error("Error executing command in transaction: {}",
                        queuedCommand.getDescription(), e);
                results.add("-ERR " + e.getMessage());
            }
        }

        logger.debug("Transaction completed for session: {} with {} results",
                sessionId, results.size());

        return results;
    }

    /**
     * DISCARD - Discard all queued commands
     */
    public String discard(String sessionId) {
        TransactionContext context = activeTransactions.remove(sessionId);

        if (context == null) {
            return "-ERR DISCARD without MULTI";
        }

        logger.debug("Transaction discarded for session: {} with {} queued commands",
                sessionId, context.getCommands().size());

        return "+OK";
    }

    /**
     * Check if session is in transaction mode
     */
    public boolean isInTransaction(String sessionId) {
        return activeTransactions.containsKey(sessionId);
    }

    /**
     * Get number of queued commands for a session
     */
    public int getQueuedCommandCount(String sessionId) {
        TransactionContext context = activeTransactions.get(sessionId);
        return context != null ? context.getCommands().size() : 0;
    }

    /**
     * Clear transaction for session (e.g., on disconnect)
     */
    public void clearTransaction(String sessionId) {
        TransactionContext removed = activeTransactions.remove(sessionId);
        if (removed != null) {
            logger.debug("Cleared transaction for disconnected session: {}", sessionId);
        }
    }

    /**
     * Get all active transaction sessions
     */
    public Set<String> getActiveTransactionSessions() {
        return new HashSet<>(activeTransactions.keySet());
    }

    private String formatResult(Object result) {
        if (result == null) {
            return "$-1";
        } else if (result instanceof String) {
            String str = (String) result;
            return "+" + str;
        } else if (result instanceof Integer || result instanceof Long) {
            return ":" + result;
        } else if (result instanceof Boolean) {
            return ":" + ((Boolean) result ? 1 : 0);
        } else if (result instanceof Collection) {
            Collection<?> collection = (Collection<?>) result;
            return "*" + collection.size();
        } else {
            return "+" + result.toString();
        }
    }

    // Transaction context
    private static class TransactionContext {
        private String sessionId;
        private long startTime;
        private final List<QueuedCommand> commands = new ArrayList<>();

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public List<QueuedCommand> getCommands() {
            return commands;
        }

        public void addCommand(Supplier<Object> command, String description) {
            commands.add(new QueuedCommand(command, description));
        }
    }

    // Queued command
    private static class QueuedCommand {
        private final Supplier<Object> command;
        private final String description;
        private final long queuedAt;

        public QueuedCommand(Supplier<Object> command, String description) {
            this.command = command;
            this.description = description;
            this.queuedAt = System.currentTimeMillis();
        }

        public Supplier<Object> getCommand() {
            return command;
        }

        public String getDescription() {
            return description;
        }

        public long getQueuedAt() {
            return queuedAt;
        }
    }
}