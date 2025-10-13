package com.ash.projects.redisclone.service;

import com.ash.projects.redisclone.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final Map<String, User> users = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void loadUsers() {
        try {
            ClassPathResource resource = new ClassPathResource("users.json");
            InputStream inputStream = resource.getInputStream();

            var jsonNode = objectMapper.readTree(inputStream);
            var usersArray = jsonNode.get("users");

            for (var userNode : usersArray) {
                User user = objectMapper.treeToValue(userNode, User.class);
                users.put(user.getUserid(), user);
            }

            logger.info("Loaded {} users from configuration", users.size());
        } catch (Exception e) {
            logger.error("Error loading users from users.json", e);

            // Create default admin user if file can't be loaded
            User defaultAdmin = new User();
            defaultAdmin.setUserid("admin");
            defaultAdmin.setName("Administrator");
            defaultAdmin.setPassword("admin");
            defaultAdmin.setRoles(Arrays.asList("ADMIN", "USER"));
            users.put("admin", defaultAdmin);

            logger.warn("Created default admin user");
        }
    }

    /**
     * Authenticate user with userid and password
     */
    public User authenticate(String userid, String password) {
        User user = users.get(userid);

        if (user != null && user.getPassword().equals(password)) {
            logger.info("User authenticated: {}", userid);
            return user;
        }

        logger.warn("Authentication failed for user: {}", userid);
        return null;
    }

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(User user, String role) {
        return user != null && user.hasRole(role);
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin(User user) {
        return user != null && user.isAdmin();
    }

    /**
     * Get all users (admin only)
     */
    public Collection<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    /**
     * Get user by userid
     */
    public User getUser(String userid) {
        return users.get(userid);
    }
}