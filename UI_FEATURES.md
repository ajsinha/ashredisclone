# AshRedis Clone - Web UI Feature Guide

## Overview

The AshRedis Web UI provides a complete management interface for your cache with role-based access control. This guide covers all available features and how to use them.

## Access & Authentication

### Login
- URL: `http://localhost:8080/login`
- Default Credentials:
    - **Admin**: `admin` / `admin123` (Full access)
    - **User**: `user1` / `user123` (Read/Query only)
    - **Viewer**: `viewer` / `viewer123` (Read only)

### User Roles

| Role | Capabilities |
|------|--------------|
| **ADMIN** | Create, Read, Update, Delete regions and entries |
| **USER** | Read and query entries |
| **VIEWER** | Read-only access |

**Note:** Write operations (create, update, delete) are only available on PRIMARY instances.

## Main Features

### 1. Dashboard (`/`)

**Overview of your cache instance:**
- ✅ Instance status (PRIMARY/SECONDARY)
- ✅ Total regions count
- ✅ Quick action buttons
- ✅ Region list with direct links
- ✅ System information
- ✅ Quick create region (Admin only)

**Quick Actions Available:**
- Search Entries
- Browse Regions
- View Statistics
- Create Region (Admin)

---

### 2. Region Management (`/regions`)

**List and manage all regions:**

#### View Regions
- See all regions with statistics
- Shows: Region name, Total keys, Memory keys
- Default region badge indicator

#### Create Region (Admin Only)
1. Click **"+ Create Region"** button
2. Enter region name (alphanumeric, underscores, hyphens)
3. Click **"Create Region"**

**Region Naming Rules:**
- Use letters, numbers, underscores (_), and hyphens (-)
- No spaces or special characters
- Example: `users`, `sessions`, `product-cache`

#### Delete Region (Admin Only)
1. Click **"Delete"** button next to region
2. Confirm deletion (cannot be undone)
3. All keys in region will be deleted

**Note:** Default region (`region0`) cannot be deleted.

---

### 3. Region Detail (`/region/{name}`)

**Browse and manage keys within a region:**

#### Search Keys in Region
- Use the search box to find keys
- Supports wildcard patterns:
    - `*` = matches any characters
    - `user:*` = all keys starting with "user:"
    - `*cache*` = all keys containing "cache"
    - `session:123` = exact match

#### Create Entry (Admin Only)
1. Click **"+ Create New Entry"**
2. Fill in:
    - **Key**: Unique identifier within region
    - **Value**: Any text or JSON data
    - **TTL**: Expiration time in seconds (0 = never expires)
3. Click **"Create Entry"**

**TTL Quick Reference:**
- 60 = 1 minute
- 3600 = 1 hour
- 86400 = 1 day
- 604800 = 1 week

#### Delete Region (Admin Only)
- Click **"Delete Region"** button at top
- Confirm deletion

---

### 4. Entry Detail (`/entry/{region}/{key}`)

**View and manage individual cache entries:**

#### View Entry Information
- Region name
- Key name
- TTL (time remaining)
- Full value display
- Copy to clipboard button

#### Edit Entry (Admin Only)
1. Click **"✏️ Edit Value"**
2. Modify the value
3. Click **"Save Changes"**

#### Change TTL (Admin Only)
1. Click **"🕐 Change TTL"**
2. Enter new TTL in seconds (0 = no expiration)
3. Click **"Update TTL"**

#### Delete Entry (Admin Only)
1. Click **"🗑️ Delete Entry"**
2. Confirm deletion (cannot be undone)

---

### 5. Advanced Search (`/search`)

**Search across all regions or specific regions:**

#### Search Options

**1. Region Selection**
- **All Regions**: Search across entire cache
- **Specific Region**: Limit search to one region

**2. Search Types**
- **Exact Match**: Find keys that match exactly
    - Example: `user:123` finds only "user:123"

- **Contains**: Find keys containing the text
    - Example: `session` finds "session:abc", "user:session:123", etc.
    - Automatically adds wildcards

- **Pattern/Regex**: Use wildcards for flexible matching
    - Example: `user:*` finds all keys starting with "user:"
    - Example: `*cache*` finds all keys containing "cache"

#### How to Search

1. **Select Region**: Choose "All Regions" or specific region
2. **Select Search Type**: Exact, Contains, or Pattern
3. **Enter Search Key**: Type your search term
4. Click **"🔍 Search"**

#### Search Results

Results show:
- Region name
- Key name
- Value preview (first 50 characters)
- TTL information
- **"View Details →"** button to see full entry

**Results grouped by region** for easy navigation.

#### Search Examples

| Goal | Region | Type | Search Key |
|------|--------|------|------------|
| Find specific user | All Regions | Exact | `user:123` |
| All session keys | sessions | Pattern | `session:*` |
| Keys with "cache" | All Regions | Contains | `cache` |
| User keys in region | users | Pattern | `user:*` |
| Products by ID range | products | Pattern | `product:10*` |

---

### 6. Statistics (`/stats`)

**View system statistics:**

#### System Information
- Version
- Uptime
- Instance mode (PRIMARY/SECONDARY)

#### Region Statistics
- Total regions count
- List of all region names

#### Pub/Sub Statistics (if enabled)
- Channel subscriptions
- Region subscriptions
- Key subscriptions

---

## Common Workflows

### Creating a New Region with Data

1. Go to **Regions** page
2. Click **"+ Create Region"**
3. Enter region name (e.g., `users`)
4. Click into the new region
5. Click **"+ Create New Entry"**
6. Add your first entry

### Finding Data Across Regions

1. Go to **Search** page
2. Select **"All Regions"**
3. Select **"Contains"** search type
4. Enter part of the key name
5. Click **"Search"**
6. Browse results and click **"View Details"**

### Updating an Entry's Value

1. Navigate to the entry (via Region → Key)
2. Click **"✏️ Edit Value"**
3. Modify the value
4. Click **"Save Changes"**

### Setting Expiration on Keys

1. Navigate to the entry
2. Click **"🕐 Change TTL"**
3. Enter seconds (e.g., 3600 for 1 hour)
4. Click **"Update TTL"**

### Deleting Old Data

**Delete Single Entry:**
1. Navigate to entry
2. Click **"🗑️ Delete Entry"**
3. Confirm

**Delete Entire Region:**
1. Go to **Regions** page
2. Click **"Delete"** next to region
3. Confirm (deletes all keys)

---

## Keyboard Shortcuts & Tips

### General Tips
- Use browser back button to navigate
- Flash messages appear at top after actions
- Confirmation required for destructive actions
- All forms validate input before submission

### Search Tips
- Start with broad patterns, narrow down
- Use `*` at start for suffix matching: `*:cache`
- Use `*` at end for prefix matching: `user:*`
- Use `*` on both sides for contains: `*session*`

### Performance Tips
- Search in specific regions when possible
- Use exact match for fastest results
- Pattern searches may be slower with many keys
- Consider pagination for large result sets

---

## Access Control Summary

### PRIMARY Instance - Admin Role
✅ Create regions
✅ Delete regions
✅ Create entries
✅ Update entries
✅ Delete entries
✅ Change TTL
✅ Search and query
✅ View all data

### PRIMARY Instance - User/Viewer Role
❌ Cannot create regions
❌ Cannot delete regions
❌ Cannot create entries
❌ Cannot update entries
❌ Cannot delete entries
❌ Cannot change TTL
✅ Search and query
✅ View all data

### SECONDARY Instance - Any Role
❌ No write operations allowed
✅ Search and query
✅ View all data

---

## Troubleshooting

### "Cannot modify data on secondary instance"
**Solution:** Connect to the PRIMARY instance for write operations, or wait for failover.

### "Only admins can create/delete"
**Solution:** Login with admin credentials or request admin access.

### Region not appearing in list
**Solution:** Refresh the page. Regions appear after first entry is created.

### Search returns no results
**Try:**
- Change search type to "Contains"
- Select "All Regions"
- Try broader pattern (e.g., `*`)
- Check spelling of search term

### Cannot delete default region
**This is normal:** The default region `region0` is protected and cannot be deleted.

---

## Best Practices

### Organization
- Use meaningful region names (`users`, `sessions`, not `region1`)
- Group related data in same region
- Use consistent key naming (e.g., `user:123`, `user:456`)

### Keys
- Use colon `:` for hierarchy (`user:profile:123`)
- Include type prefix (`session:`, `cache:`)
- Keep keys descriptive but concise

### TTL
- Set appropriate expiration times
- Use TTL for temporary data (sessions, caches)
- Don't use TTL for permanent data
- Monitor expired entries in statistics

### Search
- Test search patterns before deleting
- Use specific regions when possible
- Save frequently used search patterns
- Document your key naming conventions

---

## Quick Reference Card

| Action | Location | Access Level |
|--------|----------|--------------|
| View dashboard | `/` | All |
| List regions | `/regions` | All |
| Create region | `/regions` → Create button | Admin on PRIMARY |
| View region keys | `/region/{name}` | All |
| Search all data | `/search` | All |
| Create entry | Region page → Create button | Admin on PRIMARY |
| View entry | Click key in region | All |
| Edit entry | Entry page → Edit button | Admin on PRIMARY |
| Delete entry | Entry page → Delete button | Admin on PRIMARY |
| Change TTL | Entry page → Change TTL button | Admin on PRIMARY |
| View stats | `/stats` | All |

---

## Support

For issues or questions:
1. Check application logs
2. Verify user permissions
3. Confirm instance is PRIMARY for writes
4. Review this guide for proper usage

**Remember:** All destructive operations require confirmation and cannot be undone!