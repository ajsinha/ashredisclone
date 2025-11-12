package com.ash.projects.redisclone.controller;

import com.ash.projects.redisclone.model.User;
import com.ash.projects.redisclone.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class WebController {

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

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("regions", cacheService.getAllRegions());
        model.addAttribute("isPrimary", replicationService == null || replicationService.isPrimary());

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

    @PostMapping("/region/create")
    public String createRegion(@RequestParam String regionName,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        if (!userService.isAdmin(user)) {
            redirectAttributes.addFlashAttribute("error", "Only admins can create regions");
            return "redirect:/regions";
        }

        if (replicationService != null && !replicationService.isPrimary()) {
            redirectAttributes.addFlashAttribute("error", "Cannot create region on secondary instance");
            return "redirect:/regions";
        }

        // Validate region name
        if (regionName == null || regionName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Region name cannot be empty");
            return "redirect:/regions";
        }

        // Create region by setting a dummy key (region will be created automatically)
        String dummyKey = "__region_init__";
        cacheService.set(regionName, dummyKey, "initialized", null);
        // Optionally delete the dummy key
        cacheService.del(regionName, dummyKey);

        redirectAttributes.addFlashAttribute("success", "Region '" + regionName + "' created successfully");
        return "redirect:/region/" + regionName;
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
    public String showCreateEntryPage(@RequestParam String region,
                                      HttpSession session,
                                      Model model,
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
            redirectAttributes.addFlashAttribute("error", "Cannot modify data on secondary instance");
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
            redirectAttributes.addFlashAttribute("error", "Cannot modify data on secondary instance");
            return "redirect:/region/" + region;
        }

        Long expiresAt = ttl != null && ttl > 0 ? System.currentTimeMillis() + (ttl * 1000) : null;
        cacheService.set(region, key, value, expiresAt);

        redirectAttributes.addFlashAttribute("success", "Entry created successfully");
        return "redirect:/entry/" + region + "/" + key;
    }

    @GetMapping("/entry/edit")
    public String showEditEntryPage(@RequestParam String region,
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

        model.addAttribute("user", user);
        model.addAttribute("info", info);
        model.addAttribute("isPrimary", replicationService == null || replicationService.isPrimary());

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

    // Inner class for search results
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

        public String getValuePreview() {
            if (value == null) return "null";
            return value.length() > 50 ? value.substring(0, 50) + "..." : value;
        }
    }
}