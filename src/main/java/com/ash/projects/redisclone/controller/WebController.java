package com.ash.projects.redisclone.controller;

import com.ash.projects.redisclone.model.User;
import com.ash.projects.redisclone.repository.CacheRepository;
import com.ash.projects.redisclone.service.*;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

/**
 * Web Controller for AshRedis Clone
 * Handles all web-based interactions for the cache management system
 *
 * Copyright (c) 2025 AshRedis Clone
 * Contact: ajsinha@gmail.com
 */
@Controller
public class WebController {

    private static final Logger logger = LoggerFactory.getLogger(WebController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private RedisCommandService commandService;

    @Autowired(required = false)
    private ReplicationService replicationService;

    @Autowired(required = false)
    private PubSubService pubSubService;

    @Autowired
    private CacheRepository cacheRepository;

    @Value("${cache.rocksdb.base.path:./data/rocksdb}")
    private String rocksDbBasePath;

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Get repository information
        String repositoryType = cacheRepository.getImplementationType();
        String repositoryInfo;

        if (cacheRepository.isRocksDbImplementation()) {
            repositoryInfo = "RocksDB (" + rocksDbBasePath + ")";
        } else {
            // SQL implementation
            repositoryInfo = "SQL Database";
        }

        model.addAttribute("user", user);
        model.addAttribute("regions", cacheService.getAllRegions());
        model.addAttribute("isPrimary", replicationService == null || replicationService.isPrimary());
        model.addAttribute("repositoryType", repositoryType);
        model.addAttribute("repositoryInfo", repositoryInfo);

        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String userid,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        User user = userService.authenticate(userid, password);

        if (user != null) {
            session.setAttribute("user", user);
            return "redirect:/";
        }

        redirectAttributes.addFlashAttribute("error", "Invalid credentials");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/regions")
    public String regions(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Set<String> regions = cacheService.getAllRegions();
        List<Map<String, Object>> regionStats = new ArrayList<>();

        for (String region : regions) {
            regionStats.add(cacheService.getRegionStats(region));
        }

        model.addAttribute("user", user);
        model.addAttribute("regions", regionStats);
        model.addAttribute("isPrimary", replicationService == null || replicationService.isPrimary());

        return "regions";
    }

    /**
     * Display the create region page
     * NEW ENDPOINT - Shows dedicated region creation form with optional entry
     *
     * @param model Spring MVC model
     * @param session HTTP session
     * @return view name
     */
    @GetMapping("/region/create")
    public String showCreateRegionPage(Model model, HttpSession session) {
        // Check authentication
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Only admins can create regions
        if (!userService.isAdmin(user)) {
            model.addAttribute("error", "Only administrators can create regions");
            return "redirect:/regions";
        }

        // Check if primary instance
        boolean isPrimary = replicationService == null || replicationService.isPrimary();
        if (!isPrimary) {
            model.addAttribute("error", "Region creation is only allowed on PRIMARY instance");
            return "redirect:/regions";
        }

        // Add model attributes for base template
        model.addAttribute("user", user);
        model.addAttribute("isPrimary", isPrimary);

        logger.info("User {} accessing create region page", user.getUserid());

        return "create-region";
    }

    /**
     * Handle region creation with optional initial entry
     * NEW ENDPOINT - Supports creating a region and optionally adding the first entry
     *
     * @param regionName Name of the region to create (required)
     * @param key Key for initial entry (optional)
     * @param value Value for initial entry (optional)
     * @param ttl Time-to-live for initial entry in seconds (optional, default 0)
     * @param session HTTP session
     * @param redirectAttributes Spring redirect attributes for flash messages
     * @return redirect path
     */
    @PostMapping("/region/create-with-entry")
    public String createRegionWithEntry(
            @RequestParam("regionName") String regionName,
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "value", required = false) String value,
            @RequestParam(value = "ttl", required = false, defaultValue = "0") int ttl,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Check authentication
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Only admins can create regions
        if (!userService.isAdmin(user)) {
            redirectAttributes.addFlashAttribute("error",
                    "Only administrators can create regions");
            return "redirect:/regions";
        }

        // Check if primary instance
        if (replicationService != null && !replicationService.isPrimary()) {
            redirectAttributes.addFlashAttribute("error",
                    "Region creation is only allowed on PRIMARY instance");
            return "redirect:/regions";
        }

        try {
            // Validate region name
            if (regionName == null || regionName.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "Region name cannot be empty");
                return "redirect:/region/create";
            }

            // Validate region name pattern (alphanumeric, underscore, hyphen)
            if (!regionName.matches("[a-zA-Z0-9_-]+")) {
                redirectAttributes.addFlashAttribute("error",
                        "Region name can only contain alphanumeric characters, underscores, and hyphens");
                return "redirect:/region/create";
            }

            // Log region creation
            logger.info("User {} creating region: {}", user.getUserid(), regionName);

            // Create region and check if initial entry is provided
            boolean entryCreated = false;

            if (key != null && !key.trim().isEmpty() &&
                    value != null && !value.trim().isEmpty()) {

                logger.info("Creating initial entry in region {}: key={}, ttl={}",
                        regionName, key, ttl);

                // Set the cache entry with or without TTL
                if (ttl > 0) {
                    Long expiresAt = System.currentTimeMillis() + (ttl * 1000L);
                    cacheService.set(regionName, key, value, expiresAt);
                    logger.info("Entry created with TTL: {} seconds", ttl);
                } else {
                    cacheService.set(regionName, key, value, null);
                    logger.info("Entry created with no expiration");
                }

                entryCreated = true;
            } else {
                // Create region with a dummy key and then delete it
                String dummyKey = "__region_init__";
                cacheService.set(regionName, dummyKey, "initialized", null);
                cacheService.del(regionName, dummyKey);
                logger.info("Region created without initial entry");
            }

            // Set success message
            if (entryCreated) {
                redirectAttributes.addFlashAttribute("success",
                        String.format("Region '%s' created successfully with initial entry '%s'",
                                regionName, key));
            } else {
                redirectAttributes.addFlashAttribute("success",
                        String.format("Region '%s' created successfully", regionName));
            }

            // Log the successful operation
            logger.info("Region '{}' created successfully by user {}",
                    regionName, user.getUserid());

            // Redirect to the new region's detail page
            return "redirect:/region/" + regionName;

        } catch (IllegalArgumentException e) {
            // Handle validation errors
            logger.warn("Validation error creating region: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Validation error: " + e.getMessage());
            return "redirect:/region/create";

        } catch (Exception e) {
            // Handle other errors
            logger.error("Error creating region '{}': {}", regionName, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Error creating region: " + e.getMessage());
            return "redirect:/region/create";
        }
    }

    /**
     * UPDATED - Backward compatibility endpoint
     * Delegates to createRegionWithEntry for simple region creation
     * This maintains compatibility with the old modal-based approach
     */
    @PostMapping("/region/create")
    public String createRegion(@RequestParam String regionName,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        // Delegate to the new handler with no entry parameters
        return createRegionWithEntry(regionName, null, null, 0, session, redirectAttributes);
    }

    @PostMapping("/region/delete")
    public String deleteRegion(@RequestParam String regionName,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        if (!userService.isAdmin(user)) {
            redirectAttributes.addFlashAttribute("error", "Only admins can delete regions");
            return "redirect:/regions";
        }

        if (replicationService != null && !replicationService.isPrimary()) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete region on secondary instance");
            return "redirect:/regions";
        }

        // Prevent deletion of default region
        if (cacheService.getDefaultRegion().equals(regionName)) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete default region");
            return "redirect:/regions";
        }

        // Delete all keys in the region
        Set<String> keys = cacheService.keys(regionName, "*");
        if (!keys.isEmpty()) {
            cacheService.del(regionName, keys.toArray(new String[0]));
        }

        redirectAttributes.addFlashAttribute("success", "Region '" + regionName + "' deleted successfully");
        return "redirect:/regions";
    }

    @GetMapping("/region/{name}")
    public String regionDetail(@PathVariable String name,
                               @RequestParam(required = false, defaultValue = "*") String pattern,
                               HttpSession session,
                               Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Set<String> keys = cacheService.keys(name, pattern);

        model.addAttribute("user", user);
        model.addAttribute("region", name);
        model.addAttribute("pattern", pattern);
        model.addAttribute("keys", keys);
        model.addAttribute("isPrimary", replicationService == null || replicationService.isPrimary());

        return "region-detail";
    }

    @GetMapping("/entry/{region}/{key}")
    public String entryDetail(@PathVariable String region,
                              @PathVariable String key,
                              HttpSession session,
                              Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        String value = cacheService.get(region, key);
        long ttl = cacheService.ttl(region, key);

        model.addAttribute("user", user);
        model.addAttribute("region", region);
        model.addAttribute("key", key);
        model.addAttribute("value", value);
        model.addAttribute("ttl", ttl);
        model.addAttribute("isPrimary", replicationService == null || replicationService.isPrimary());

        return "entry-detail";
    }

    @GetMapping("/entry/create")
    public String createEntryPage(@RequestParam String region,
                                  HttpSession session,
                                  Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        if (!userService.isAdmin(user)) {
            model.addAttribute("error", "Only admins can create entries");
            return "redirect:/region/" + region;
        }

        if (replicationService != null && !replicationService.isPrimary()) {
            model.addAttribute("error", "Cannot create entries on secondary instance");
            return "redirect:/region/" + region;
        }

        model.addAttribute("user", user);
        model.addAttribute("region", region);
        model.addAttribute("isPrimary", replicationService == null || replicationService.isPrimary());

        return "create-entry";
    }

    @PostMapping("/entry/create")
    public String createEntry(@RequestParam String region,
                              @RequestParam String key,
                              @RequestParam String value,
                              @RequestParam(required = false) Long ttl,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        if (!userService.isAdmin(user)) {
            redirectAttributes.addFlashAttribute("error", "Only admins can create entries");
            return "redirect:/region/" + region;
        }

        if (replicationService != null && !replicationService.isPrimary()) {
            redirectAttributes.addFlashAttribute("error", "Cannot create entries on secondary instance");
            return "redirect:/region/" + region;
        }

        Long expiresAt = ttl != null && ttl > 0 ? System.currentTimeMillis() + (ttl * 1000) : null;
        cacheService.set(region, key, value, expiresAt);

        redirectAttributes.addFlashAttribute("success", "Entry created successfully");
        return "redirect:/entry/" + region + "/" + key;
    }

    @GetMapping("/entry/edit")
    public String editEntryPage(@RequestParam String region,
                                @RequestParam String key,
                                HttpSession session,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        if (!userService.isAdmin(user)) {
            redirectAttributes.addFlashAttribute("error", "Only admins can edit entries");
            return "redirect:/entry/" + region + "/" + key;
        }

        if (replicationService != null && !replicationService.isPrimary()) {
            redirectAttributes.addFlashAttribute("error", "Cannot modify data on secondary instance");
            return "redirect:/entry/" + region + "/" + key;
        }

        // Load the entry
        String value = cacheService.get(region, key);
        long ttl = cacheService.ttl(region, key);

        if (value == null && ttl == -2) {
            redirectAttributes.addFlashAttribute("error", "Entry not found");
            return "redirect:/region/" + region;
        }

        model.addAttribute("user", user);
        model.addAttribute("region", region);
        model.addAttribute("key", key);
        model.addAttribute("value", value);
        model.addAttribute("ttl", ttl);
        model.addAttribute("isPrimary", replicationService == null || replicationService.isPrimary());

        return "edit-entry";
    }

    @PostMapping("/entry/update")
    public String updateEntry(@RequestParam String region,
                              @RequestParam String key,
                              @RequestParam String value,
                              @RequestParam(required = false) Long ttl,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        if (!userService.isAdmin(user)) {
            redirectAttributes.addFlashAttribute("error", "Only admins can update entries");
            return "redirect:/entry/" + region + "/" + key;
        }

        if (replicationService != null && !replicationService.isPrimary()) {
            redirectAttributes.addFlashAttribute("error", "Cannot modify data on secondary instance");
            return "redirect:/entry/" + region + "/" + key;
        }

        Long expiresAt = ttl != null && ttl > 0 ? System.currentTimeMillis() + (ttl * 1000) : null;
        cacheService.set(region, key, value, expiresAt);

        redirectAttributes.addFlashAttribute("success", "Entry updated successfully");
        return "redirect:/entry/" + region + "/" + key;
    }

    @PostMapping("/entry/delete")
    public String deleteEntry(@RequestParam String region,
                              @RequestParam String key,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        if (!userService.isAdmin(user)) {
            redirectAttributes.addFlashAttribute("error", "Only admins can delete entries");
            return "redirect:/region/" + region;
        }

        if (replicationService != null && !replicationService.isPrimary()) {
            redirectAttributes.addFlashAttribute("error", "Cannot modify data on secondary instance");
            return "redirect:/region/" + region;
        }

        cacheService.del(region, key);

        redirectAttributes.addFlashAttribute("success", "Entry deleted successfully");
        return "redirect:/region/" + region;
    }

    @GetMapping("/stats")
    public String stats(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Map<String, Object> info = commandService.info(null);

        // Get repository information
        String repositoryType = cacheRepository.getImplementationType();
        String repositoryInfo;

        if (cacheRepository.isRocksDbImplementation()) {
            repositoryInfo = "RocksDB (" + rocksDbBasePath + ")";
        } else {
            // SQL implementation
            repositoryInfo = "SQL Database";
        }

        model.addAttribute("user", user);
        model.addAttribute("info", info);
        model.addAttribute("isPrimary", replicationService == null || replicationService.isPrimary());
        model.addAttribute("repositoryType", repositoryType);
        model.addAttribute("repositoryInfo", repositoryInfo);

        if (pubSubService != null) {
            model.addAttribute("pubsubStats", pubSubService.getSubscriberCounts());
        }

        return "stats";
    }

    @GetMapping("/search")
    public String search(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("regions", cacheService.getAllRegions());
        model.addAttribute("isPrimary", replicationService == null || replicationService.isPrimary());

        return "search";
    }

    @PostMapping("/search")
    public String searchEntries(@RequestParam(required = false) String region,
                                @RequestParam String searchKey,
                                @RequestParam(required = false, defaultValue = "exact") String searchType,
                                HttpSession session,
                                Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Map<String, List<SearchResult>> results = new HashMap<>();

        // Determine which regions to search
        Set<String> regionsToSearch = new HashSet<>();
        if (region != null && !region.trim().isEmpty() && !"all".equals(region)) {
            regionsToSearch.add(region);
        } else {
            regionsToSearch.addAll(cacheService.getAllRegions());
        }

        // Search in each region
        for (String r : regionsToSearch) {
            Set<String> matchingKeys;

            if ("regex".equals(searchType)) {
                // Use regex pattern
                matchingKeys = cacheService.keys(r, searchKey);
            } else if ("contains".equals(searchType)) {
                // Search for keys containing the text
                matchingKeys = cacheService.keys(r, "*" + searchKey + "*");
            } else {
                // Exact match
                long exists = cacheService.exists(r, searchKey);
                matchingKeys = exists > 0 ? Set.of(searchKey) : Set.of();
            }

            if (!matchingKeys.isEmpty()) {
                List<SearchResult> regionResults = new ArrayList<>();
                for (String key : matchingKeys) {
                    SearchResult result = new SearchResult();
                    result.setRegion(r);
                    result.setKey(key);
                    result.setValue(cacheService.get(r, key));
                    result.setTtl(cacheService.ttl(r, key));
                    regionResults.add(result);
                }
                results.put(r, regionResults);
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("regions", cacheService.getAllRegions());
        model.addAttribute("isPrimary", replicationService == null || replicationService.isPrimary());
        model.addAttribute("searchKey", searchKey);
        model.addAttribute("searchType", searchType);
        model.addAttribute("selectedRegion", region);
        model.addAttribute("results", results);
        model.addAttribute("totalResults", results.values().stream().mapToInt(List::size).sum());

        return "search";
    }

    /**
     * Inner class for search results
     * Represents a search result entry with region, key, value, and TTL information
     */
    public static class SearchResult {
        private String region;
        private String key;
        private String value;
        private long ttl;

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public long getTtl() { return ttl; }
        public void setTtl(long ttl) { this.ttl = ttl; }

        /**
         * Get a preview of the value (first 50 characters)
         * @return truncated value for display
         */
        public String getValuePreview() {
            if (value == null) return "null";
            return value.length() > 50 ? value.substring(0, 50) + "..." : value;
        }
    }
}