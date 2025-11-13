# AshRedis GUI Features Documentation

**Version:** 3.0  
**Copyright:** All Rights Reserved 2025-2030, Ashutosh Sinha  
**Author:** ajsinha@gmail.com  
**Project:** Abhikarta LLM Platform - AshRedis GUI

---

## Legal Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

By using this software, you agree to use it in compliance with all applicable laws and accept that the software is provided without warranty.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Authentication System](#authentication-system)
4. [Dashboard](#dashboard)
5. [Navigation System](#navigation-system)
6. [Region Management](#region-management)
7. [Entry Management](#entry-management)
8. [Search Functionality](#search-functionality)
9. [Statistics & Monitoring](#statistics--monitoring)
10. [Enhanced Table Features](#enhanced-table-features)
11. [Auto-Refresh System](#auto-refresh-system)
12. [Live TTL Countdown](#live-ttl-countdown)
13. [User Interface Components](#user-interface-components)
14. [Responsive Design](#responsive-design)
15. [Security Features](#security-features)
16. [User Experience Enhancements](#user-experience-enhancements)
17. [Theme & Styling](#theme--styling)
18. [Browser Compatibility](#browser-compatibility)
19. [Accessibility Features](#accessibility-features)
20. [Troubleshooting](#troubleshooting)

---

## Overview

### Introduction

AshRedis GUI is a modern, feature-rich web interface for managing the AshRedis cache system. Built with Spring Boot, Thymeleaf, Bootstrap 5, and vanilla JavaScript, it provides an intuitive and powerful interface for administrators and users to interact with cache data.

### Key Highlights

✨ **Modern UI/UX** - Clean, intuitive interface built with Bootstrap 5  
🔐 **Role-Based Access** - Admin and user permissions  
📊 **Real-Time Monitoring** - Live TTL countdowns and auto-refresh  
🔍 **Advanced Search** - Multiple search modes with pattern matching  
📋 **Enhanced Tables** - Sorting, filtering, and pagination  
🎨 **Responsive Design** - Works seamlessly on all devices  
⚡ **High Performance** - Optimized JavaScript and efficient rendering  
🛡️ **Security First** - CSRF protection, session management  
♿ **Accessible** - WCAG compliant interface  
🎯 **User-Friendly** - Intuitive workflows and helpful tooltips  

### Technology Stack

**Frontend:**
- HTML5 with Thymeleaf templating
- Bootstrap 5.3.2 (CSS framework)
- jQuery 3.7.1 (DOM manipulation)
- Vanilla JavaScript (custom functionality)
- Bootstrap Icons

**Backend:**
- Spring Boot (MVC framework)
- Spring Security (authentication)
- Thymeleaf (server-side rendering)
- Session-based state management

---

## Architecture

### Application Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                     AshRedis Web GUI                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                   Presentation Layer                       │ │
│  │  ┌──────────────────────────────────────────────────────┐ │ │
│  │  │  Thymeleaf Templates                                 │ │ │
│  │  │  - base.html (layout)                                │ │ │
│  │  │  - login.html                                        │ │ │
│  │  │  - index.html (dashboard)                            │ │ │
│  │  │  - regions.html                                      │ │ │
│  │  │  - region-detail.html                                │ │ │
│  │  │  - entry-detail.html                                 │ │ │
│  │  │  - create-region.html                                │ │ │
│  │  │  - create-entry.html                                 │ │ │
│  │  │  - edit-entry.html                                   │ │ │
│  │  │  - search.html                                       │ │ │
│  │  │  - stats.html                                        │ │ │
│  │  │  - error.html                                        │ │ │
│  │  └──────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────┘ │
│                             ↓                                    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                   JavaScript Layer                         │ │
│  │  ┌──────────────────────────────────────────────────────┐ │ │
│  │  │  Enhanced Table Utilities (table-utils.js)          │ │ │
│  │  │  - Sorting                                           │ │ │
│  │  │  - Filtering                                         │ │ │
│  │  │  - Pagination                                        │ │ │
│  │  └──────────────────────────────────────────────────────┘ │ │
│  │  ┌──────────────────────────────────────────────────────┐ │ │
│  │  │  Page-Specific JavaScript                           │ │ │
│  │  │  - Auto-refresh system                              │ │ │
│  │  │  - Live TTL countdown                               │ │ │
│  │  │  - JSON auto-formatting                             │ │ │
│  │  │  - Form validation                                  │ │ │
│  │  │  - Clipboard operations                             │ │ │
│  │  └──────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────┘ │
│                             ↓                                    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                   Controller Layer                         │ │
│  │  - WebController (Spring MVC)                             │ │
│  │  - Session Management                                     │ │
│  │  - Request Routing                                        │ │
│  │  - Model Population                                       │ │
│  └────────────────────────────────────────────────────────────┘ │
│                             ↓                                    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                   Service Layer                            │ │
│  │  - CacheService                                           │ │
│  │  - UserService                                            │ │
│  │  - PubSubService                                          │ │
│  │  - ReplicationService                                     │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Page Flow Diagram

```
Login Page
    │
    ├──► Authentication
    │
    ↓
Dashboard (index.html)
    │
    ├──► Regions List (regions.html)
    │      │
    │      ├──► Create Region (create-region.html)
    │      │
    │      └──► Region Detail (region-detail.html)
    │             │
    │             ├──► Create Entry (create-entry.html)
    │             │
    │             └──► Entry Detail (entry-detail.html)
    │                    │
    │                    └──► Edit Entry (edit-entry.html)
    │
    ├──► Search (search.html)
    │      │
    │      └──► Search Results → Entry Detail
    │
    └──► Statistics (stats.html)
```

---

## Authentication System

### Login Page

**File:** `login.html`

**Features:**
- Clean, modern login interface
- Gradient background design
- Error message display
- CSRF token protection
- Session management

**Layout:**

```
┌────────────────────────────────┐
│      ⚡ AshRedis              │
│  High Performance Cache        │
├────────────────────────────────┤
│                                │
│  [Error Alert if any]          │
│                                │
│  User ID:  [____________]      │
│                                │
│  Password: [____________]      │
│                                │
│  [      Login      ]           │
│                                │
│  Default: admin / admin        │
└────────────────────────────────┘
```

**Usage:**

1. Navigate to `http://localhost:8080/login`
2. Enter credentials
   - User ID: admin
   - Password: admin (default)
3. Click "Login" button
4. Redirects to Dashboard on success

**Security Features:**
- Password field masking
- CSRF protection via Spring Security
- Session timeout handling
- Secure cookie management
- Failed login tracking

**Error Handling:**
- Invalid credentials message
- Session expiration notification
- Network error handling

---

## Dashboard

### Overview

**File:** `index.html`

The Dashboard is the main landing page after login, providing quick access to all major features and system status overview.

**Layout Structure:**

```
┌─────────────────────────────────────────────────────────────────┐
│  Navigation Bar                                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Dashboard                                                       │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  Instance    │  │   Total      │  │    Quick     │          │
│  │   Status     │  │   Regions    │  │   Actions    │          │
│  │              │  │              │  │              │          │
│  │  PRIMARY     │  │      5       │  │  [Search]    │          │
│  │              │  │              │  │  [Browse]    │          │
│  │              │  │  [View All]  │  │  [Stats]     │          │
│  │              │  │  [+ Create]  │  │              │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                  │
│  Available Regions                           [View All]         │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  region0                           Click to browse →   │    │
│  │  users                             Click to browse →   │    │
│  │  sessions                          Click to browse →   │    │
│  │  products                          Click to browse →   │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                  │
│  System Information                                             │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  Version:        1.0.0                                 │    │
│  │  Instance Mode:  PRIMARY                               │    │
│  │  Network Server: Port 6379 (Active)                    │    │
│  │  Database:       SQL (SQLite)                          │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Dashboard Components

#### 1. Status Cards

**Instance Status Card:**
- Color-coded: Green (PRIMARY) / Yellow (SECONDARY)
- Shows current instance role
- Explains write/read-only capability
- Real-time status indication

**Total Regions Card:**
- Displays region count
- Quick access to "View All Regions"
- "Create Region" button (admin only)
- Direct navigation to regions page

**Quick Actions Card:**
- Search entries link
- Browse regions link
- View statistics link
- One-click access to common tasks

#### 2. Available Regions Section

**Features:**
- Lists all available regions
- Click to browse keys in each region
- Shows "DEFAULT" badge for region0
- Empty state with helpful message
- "Get Started" guide for new users

**Display Format:**
```
Region Name [Badge if default]
Click to browse keys
```

#### 3. System Information Panel

**Displays:**
- Software version
- Instance mode (Primary/Secondary)
- Network server status and port
- Database type and connection info
- Repository type (SQL/RocksDB)

### User Interactions

**Navigation:**
- Click region name → View region details
- Click "View All" → Regions list page
- Click "Create Region" → Region creation form
- Click Quick Actions → Respective pages

**Permissions:**
- All users: View dashboard, browse regions
- Admin only: Create regions button visible
- Read-only on Secondary instances

---

## Navigation System

### Global Navigation Bar

**File:** `base.html`

The navigation bar is present on all pages except login and error pages.

**Structure:**

```
┌─────────────────────────────────────────────────────────────────┐
│ ⚡ AshRedis [PRIMARY]                                    [User]▼│
│                                                                  │
│  Dashboard | Regions | Search | Statistics                      │
└─────────────────────────────────────────────────────────────────┘
```

**Components:**

1. **Brand Section:**
   - AshRedis logo/name
   - Status badge (PRIMARY/SECONDARY)
   - Color-coded: Green for PRIMARY, Yellow for SECONDARY

2. **Main Menu:**
   - Dashboard - Home page
   - Regions - Region management
   - Search - Search functionality
   - Statistics - System statistics

3. **User Menu (Dropdown):**
   - User name display
   - Admin badge (if applicable)
   - Logout option

### Navigation Features

**Responsive Behavior:**
- Collapses to hamburger menu on mobile
- Touch-friendly on tablets
- Full menu on desktop

**Active State:**
- Current page highlighted
- Breadcrumb indication
- Visual feedback on hover

**Session Management:**
- User info always visible
- Role badge display
- Quick logout access

### Footer

**Content:**
```
AshRedis | High Performance Cache
© 2025-2030 Ashutosh Sinha. All Rights Reserved.
Patent Pending. Proprietary and Confidential.
```

**Features:**
- Consistent across all pages
- Copyright notice
- Legal information
- Professional appearance

---

## Region Management

### Regions List Page

**File:** `regions.html`

Displays all cache regions with management capabilities.

**Layout:**

```
┌─────────────────────────────────────────────────────────────────┐
│  Regions              [Auto-Refresh Toggle] [30s] [+ Create]    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ Show [10▼] entries                          Search: [____] │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ Region Name    │Total Keys│Memory│Database│Actions        │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ region0 [DEF]  │   1,234  │  500 │   734  │[View][Delete]│ │
│  │ users          │     856  │  300 │   556  │[View][Delete]│ │
│  │ sessions       │   2,341  │1,000 │ 1,341  │[View][Delete]│ │
│  │ products       │     423  │  200 │   223  │[View][Delete]│ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ Showing 1 to 4 of 4 entries    [Prev][1][2][3][Next]      │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Table Features

**Columns:**
1. **Region Name** - Name with DEFAULT badge for region0
2. **Total Keys** - Combined memory + database count
3. **Memory Keys** - Keys currently in memory
4. **Database Keys** - Keys persisted to disk
5. **Actions** - View and Delete buttons

**Enhanced Table Functionality:**
- **Sortable Columns** - Click headers to sort
- **Search/Filter** - Real-time table filtering
- **Pagination** - Configurable page size (5, 10, 50, 100, All)
- **Row Count** - "Showing X to Y of Z entries"
- **Keyboard Navigation** - Tab through controls

**Sorting:**
- Click column header to sort
- ▲ Ascending / ▼ Descending indicator
- ⇅ Default (unsorted) indicator
- Multi-level sort support
- Numeric and alphabetic sorting

**Filtering:**
- Type in search box for instant filtering
- Searches across all columns
- Case-insensitive matching
- Maintains pagination during search

**Pagination Controls:**
- Previous/Next navigation
- Direct page number access
- Ellipsis for many pages (... 5 6 7 ...)
- Disabled state for boundary pages

### Auto-Refresh System

**iOS-Style Toggle Switch:**

```
┌─────────────────────────────┐
│ 🔄 Auto-Refresh [○──────]  │  ← OFF
└─────────────────────────────┘

┌─────────────────────────────┐
│ 🔄 Auto-Refresh [──────●]  │  ← ON
│ 🕐 15s                      │  ← Countdown
└─────────────────────────────┘
```

**Features:**
- iOS-style toggle switch (green when ON)
- 30-second countdown timer
- Color-coded countdown badge:
  - Gray: 30-11 seconds
  - Yellow: 10-6 seconds
  - Red: 5-0 seconds
- Persistent state via localStorage
- Automatic page reload at zero

**Behavior:**
- Toggle ON: Starts countdown
- Toggle OFF: Stops countdown
- State persists across page reloads
- Visual feedback with color changes

### Create Region

**File:** `create-region.html`

**Layout:**

```
┌─────────────────────────────────────────────────────────────────┐
│  Create New Region                          [Back to Regions]   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  🗂️ Region Details                                              │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                                                             │ │
│  │  Region Name * [__________________________]                │ │
│  │                                                             │ │
│  │  ☐ Add Initial Entry (Optional)                           │ │
│  │                                                             │ │
│  │  [Conditional Entry Section - Hidden by Default]           │ │
│  │                                                             │ │
│  │  Key:   [__________________________]                       │ │
│  │                                                             │ │
│  │  Value: [__________________________ ]                      │ │
│  │         [                           ]                      │ │
│  │         [                           ]                      │ │
│  │                                                             │ │
│  │  TTL:   [_____] seconds                                    │ │
│  │                                                             │ │
│  │  Quick: [1min][5min][1hr][1day][1week][Never]             │ │
│  │                                                             │ │
│  │                              [Cancel] [Create Region]      │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  Help Sidebar:                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ 💡 About Regions                                           │ │
│  │ What are Regions?                                          │ │
│  │ Regions are logical partitions...                          │ │
│  │                                                             │ │
│  │ Region Naming Examples                                     │ │
│  │ • users - User profile data                                │ │
│  │ • sessions - Session information                           │ │
│  │ • products - Product catalog                               │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Create Region Features

**Region Name Input:**
- Required field with validation
- Pattern: `[a-zA-Z0-9_-]+`
- Real-time validation feedback
- Naming convention examples
- Prevents duplicate names

**Optional Initial Entry:**
- Toggle switch to show/hide entry form
- JavaScript validation
- Auto-required when checked
- Helpful for quick setup

**Entry Form (When Enabled):**

1. **Key Input:**
   - Pattern suggestions
   - Naming convention examples
   - Validation messages

2. **Value Textarea:**
   - Large text area (10 rows)
   - Monospace font
   - Auto-format JSON on blur
   - Syntax highlighting support

3. **TTL Configuration:**
   - Number input with validation
   - Unit display (seconds)
   - Quick-select buttons
   - Zero for no expiration

4. **Quick TTL Buttons:**
   - 1 minute (60s)
   - 5 minutes (300s)
   - 1 hour (3600s)
   - 1 day (86400s)
   - 1 week (604800s)
   - Never (0s)

**Help Sidebars:**

**About Regions Panel:**
- Explains region concept
- Use cases and benefits
- Naming conventions
- Organization tips

**Initial Entry Tips Panel:**
- Key naming patterns
- Value format examples
- JSON formatting guide
- Best practices

**Important Notes Panel:**
- Uniqueness requirements
- Default region behavior
- Optional entry notice
- Post-creation options

### Delete Region

**Confirmation Modal:**
```
┌────────────────────────────────────┐
│ ⚠️ Confirm Delete             [×] │
├────────────────────────────────────┤
│                                    │
│  ⚠️ Warning: Cannot be undone!    │
│                                    │
│  Delete region "users"?            │
│                                    │
│  This will delete ALL keys!        │
│                                    │
│  Region: users                     │
│                                    │
│         [Cancel] [Delete]          │
└────────────────────────────────────┘
```

**Features:**
- Two-step confirmation
- Cannot delete region0 (default)
- Admin permission required
- Primary instance only
- Shows region name
- Warns about data loss
- Cancel escape route

### Region Detail Page

**File:** `region-detail.html`

Displays all keys within a specific region.

**Layout:**

```
┌─────────────────────────────────────────────────────────────────┐
│  Region: users                    [Auto-Refresh] [30s] [Back]   │
│  [1,234 keys]                                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Key Pattern Filter                                             │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ Pattern: [user:*_____________]               [Filter Keys] │ │
│  │ Use * for wildcard matching                                │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  Keys in Region                      [More keys available ⚠️]   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ Show [10▼] entries                          Search: [____] │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ Key                                      │ Actions          │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ user:1                                   │[View][Edit][Del]│ │
│  │ user:2                                   │[View][Edit][Del]│ │
│  │ user:3                                   │[View][Edit][Del]│ │
│  │ user:123:profile                         │[View][Edit][Del]│ │
│  │ user:123:settings                        │[View][Edit][Del]│ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ Showing 1 to 5 of 1,234 entries  [Prev][1][2][Next]       │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Pattern Filtering

**Features:**
- Glob pattern matching
- Wildcard support (`*`, `?`)
- Real-time filtering
- Pattern examples provided
- Shows filtered count

**Pattern Examples:**
```
user:*           → All keys starting with "user:"
*:profile        → All keys ending with ":profile"
user:?:settings  → Single character wildcard
*                → All keys (default)
```

**Filter Display:**
- Shows active pattern as badge
- Clear indication of filtered results
- "Filtered by pattern" label
- Easy pattern reset

### Large Dataset Handling

**Features:**
- Maximum display limit (25,000 keys default)
- Warning alert when limit exceeded
- Shows displayed vs total count
- Encourages pattern refinement
- Maintains performance

**Warning Message:**
```
⚠️ Too many keys to display!
Showing 25,000 of 50,000 keys matching your pattern.
Please use the filter above to narrow down your search.
```

**Benefits:**
- Prevents browser freeze
- Maintains responsiveness
- Encourages efficient querying
- Clear user feedback

### Auto-Refresh on Region Detail

**Features:**
- Same iOS-style toggle as regions list
- 30-second countdown
- Preserves pattern filter during refresh
- State persistence via localStorage
- Color-coded countdown

**Behavior:**
- Refreshes with current pattern applied
- Maintains scroll position
- Updates key count
- Preserves table settings

---

## Entry Management

### Entry Detail Page

**File:** `entry-detail.html`

Displays complete information about a cache entry.

**Layout:**

```
┌─────────────────────────────────────────────────────────────────┐
│  Entry Detail                    [Back to Region] [Delete Entry]│
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  📋 Key Information                                             │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                                                             │ │
│  │  Region:   [users] ───────────────► View all keys in region│ │
│  │                                                             │ │
│  │  Key:      user:123                                        │ │
│  │                                                             │ │
│  │  TTL:      ⏱️ 45 seconds remaining                         │ │
│  │            (Live countdown with color coding)               │ │
│  │                                                             │ │
│  │  Value:    ┌─────────────────────────────────┐ [Copy]     │ │
│  │            │ {                                │            │ │
│  │            │   "name": "John Doe",           │            │ │
│  │            │   "email": "john@example.com",  │            │ │
│  │            │   "age": 30                     │            │ │
│  │            │ }                                │            │ │
│  │            └─────────────────────────────────┘            │ │
│  │                                                             │ │
│  │                                    [Edit Entry]            │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Entry Detail Features

#### 1. Key Information Display

**Region Badge:**
- Color-coded badge
- Clickable link to region
- Shows region context

**Key Display:**
- Large, readable font
- Monospace for clarity
- Copy-friendly format

#### 2. Live TTL Countdown

**Features:**
- Real-time countdown
- Updates every second
- Color-coded by urgency:
  - Green: > 1 hour
  - Blue: 1 hour - 31 seconds
  - Yellow: 30-11 seconds
  - Orange: 10-6 seconds
  - Red (Pulsing): 5-0 seconds
- Shows format based on time:
  - Hours + Minutes + Seconds (> 1 hour)
  - Minutes + Seconds (1-60 minutes)
  - Seconds only (< 1 minute)

**TTL States:**
```
⏱️ 2h 15m 30s remaining     (Green)
⏱️ 45m 20s remaining        (Blue)
⏱️ 25 seconds remaining     (Yellow)
⏱️ 5 seconds remaining      (Red, Pulsing)
🚫 EXPIRED                  (Red)
∞ No expiration             (Green)
❌ Key does not exist       (Red)
```

**Expiration Handling:**
- Shows "EXPIRED" when TTL reaches 0
- Pulsing animation in final 10 seconds
- Alert prompt after expiration
- Option to refresh or return to region

**JavaScript Implementation:**
```javascript
// Countdown updates every second
// Calculates elapsed time from start
// Color changes based on thresholds
// Format changes based on duration
// Alerts on expiration
```

#### 3. Value Display

**Features:**
- Scrollable container (max 400px)
- Monospace font for code
- Syntax-friendly formatting
- Copy button overlay
- Light background for readability

**Copy to Clipboard:**
- One-click copy
- Success alert
- Works with large values
- No formatting changes

#### 4. Action Buttons

**Edit Entry (Admin + Primary):**
- Opens edit form
- Pre-filled with current values
- Maintains context

**Delete Entry (Admin + Primary):**
- Opens confirmation modal
- Two-step protection
- Shows key details
- Cannot be undone warning

**Back to Region:**
- Returns to region detail page
- Maintains context
- Preserves filters

### Create Entry Page

**File:** `create-entry.html`

Form for creating new cache entries within a region.

**Layout:**

```
┌─────────────────────────────────────────────────────────────────┐
│  Create New Cache Entry                   [Back to Region]      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  🔍 Entry Details                                               │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                                                             │ │
│  │  Target Region:  [users] (read-only)                       │ │
│  │                                                             │ │
│  │  Key * :  [__________________________]                     │ │
│  │           e.g., user:123, session:abc                      │ │
│  │                                                             │ │
│  │  Value * :[__________________________]                     │ │
│  │          [__________________________]                      │ │
│  │          [__________________________]                      │ │
│  │          Enter value (text, JSON, etc.)                    │ │
│  │                                                             │ │
│  │  TTL:     [_____] seconds                                  │ │
│  │           Set to 0 for no expiration                       │ │
│  │                                                             │ │
│  │  Quick TTL: [1min][5min][1hr][1day][1week][Never]         │ │
│  │                                                             │ │
│  │                              [Cancel] [Create Entry]       │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  Help Sidebar:                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ 💡 Tips & Examples                                         │ │
│  │                                                             │ │
│  │ Key Naming Conventions                                     │ │
│  │ • user:123                                                 │ │
│  │ • session:abc-def                                          │ │
│  │ • cache:product:456                                        │ │
│  │                                                             │ │
│  │ Value Formats                                              │ │
│  │ Simple String:  Hello World                                │ │
│  │ JSON Object:    {"name":"John","age":30}                   │ │
│  │ JSON Array:     ["item1","item2"]                          │ │
│  │                                                             │ │
│  │ TTL Examples                                               │ │
│  │ • 60 = 1 minute                                            │ │
│  │ • 3600 = 1 hour                                            │ │
│  │ • 0 = No expiration                                        │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Create Entry Features

#### 1. Target Region Display
- Shows destination region
- Read-only field
- Hidden form field
- Context clarity

#### 2. Key Input
- Required field
- Pattern validation
- Naming suggestions
- Duplicate warning
- Auto-focus on load

**Validation:**
- Non-empty check
- Special character handling
- Length limits
- Real-time feedback

#### 3. Value Textarea
- Large input area (10 rows)
- Monospace font
- Auto-format JSON
- Syntax highlighting support
- Placeholder examples

**JSON Auto-Formatting:**
```javascript
// On blur event:
// 1. Detect JSON (starts with { or [)
// 2. Try to parse
// 3. Format with 2-space indent
// 4. Update textarea
// 5. Ignore if invalid JSON
```

#### 4. TTL Configuration
- Number input (min: 0)
- Unit display (seconds)
- Default: 0 (no expiration)
- Validation messages

#### 5. Quick TTL Buttons
- One-click presets
- Common durations
- Visual feedback
- Sets input value

**Button Grid:**
```
[1 min] [5 min] [1 hour] [1 day] [1 week] [Never]
  60      300     3600    86400   604800     0
```

#### 6. Help Sidebar

**Tips & Examples Panel:**
- Key naming patterns
- Value format examples
- TTL conversion table
- Best practices

**Important Notes Panel:**
- Key uniqueness requirement
- Overwrite warning
- Expiration behavior
- Serialization info

### Edit Entry Page

**File:** `edit-entry.html`

Form for editing existing cache entries.

**Layout:**

```
┌─────────────────────────────────────────────────────────────────┐
│  Edit Cache Entry                          [Back to Entry]      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ✏️ Edit Entry Details                                          │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                                                             │ │
│  │  Region:  [users] (read-only)                              │ │
│  │                                                             │ │
│  │  Key:     [user:123] (read-only)                           │ │
│  │           Keys cannot be changed                           │ │
│  │                                                             │ │
│  │  Current TTL Status:                                       │ │
│  │  ┌─────────────────────────────────────────────────────┐  │ │
│  │  │ Current Status: ⏱️ 45 seconds remaining             │  │ │
│  │  └─────────────────────────────────────────────────────┘  │ │
│  │                                                             │ │
│  │  Value * :[__________________________]                     │ │
│  │          [__________________________]                      │ │
│  │          [__________________________]                      │ │
│  │          (Current value pre-filled)                        │ │
│  │                                                             │ │
│  │  New TTL: [_____] seconds                                  │ │
│  │           Set to 0 for no expiration                       │ │
│  │                                                             │ │
│  │  Quick: [1min][5min][15min][1hr][1day][1wk][Never]        │ │
│  │                                                             │ │
│  │                              [Cancel] [Save Changes]       │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  Help Sidebar:                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ 💡 Editing Tips                                            │ │
│  │ • Auto-formats JSON on blur                                │ │
│  │ • Use Ctrl+Z to undo changes                               │ │
│  │ • Original value preserved until save                      │ │
│  │                                                             │ │
│  │ ⚠️ Important Notes                                         │ │
│  │ • Keys cannot be changed                                   │ │
│  │ • Changes are immediate                                    │ │
│  │ • Value overwrites completely                              │ │
│  │ • TTL resets from save time                                │ │
│  │                                                             │ │
│  │ 🗑️ Delete Entry                                            │ │
│  │ Need to remove this entry?                                 │ │
│  │ [Go Back to Delete]                                        │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Edit Entry Features

#### 1. Read-Only Fields
- Region (immutable)
- Key (immutable)
- Explanation message
- Hidden form fields

#### 2. Current TTL Status
- Info alert box
- Shows current TTL
- Three states:
  - Active: "X seconds remaining"
  - Expired: "Entry expired"
  - Persistent: "No expiration"

#### 3. Value Editing
- Pre-filled with current value
- Full editing capability
- Auto-format JSON
- Large textarea (12 rows)
- Monospace font
- Auto-focus on load

**JSON Auto-Format:**
- Triggers on blur
- Pretty-prints JSON
- 2-space indentation
- Preserves on error

#### 4. TTL Update
- Number input
- Pre-filled with current TTL (if active)
- Zero for no expiration
- Quick select buttons
- Additional 15-minute option

**TTL Reset Behavior:**
- Countdown starts from save time
- Overwrites previous TTL
- Can extend or shorten
- Can make persistent (0)

#### 5. Change Detection
- Tracks original values
- Warns on page leave
- Prevents accidental loss
- Disabled on form submit

**Unsaved Changes Warning:**
```javascript
// Compares current vs original
// Shows browser dialog on leave
// "You have unsaved changes"
// Prevents data loss
```

#### 6. Help Sidebar

**Editing Tips:**
- JSON auto-format feature
- Undo keyboard shortcut
- Zoom suggestion
- Original preservation

**TTL Table:**
- Common durations
- Second conversions
- Quick reference
- Visual clarity

**Important Notes:**
- Key immutability
- Immediate effect
- Complete overwrite
- TTL reset behavior

**Delete Entry Link:**
- Alternative action
- Dangerous operation
- Redirects to detail page
- Two-step protection

---

## Search Functionality

### Search Page

**File:** `search.html`

Advanced search interface for finding cache entries across regions.

**Layout:**

```
┌─────────────────────────────────────────────────────────────────┐
│  Search Cache Entries                                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Search Form                                                    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                                                             │ │
│  │  Region:      [All Regions ▼]                              │ │
│  │               • All Regions                                │ │
│  │               • region0                                    │ │
│  │               • users                                      │ │
│  │               • sessions                                   │ │
│  │                                                             │ │
│  │  Search Key:  [__________________________]                 │ │
│  │               Enter key to search...                       │ │
│  │                                                             │ │
│  │  Search Type: [Exact Match ▼]                              │ │
│  │               • Exact Match                                │ │
│  │               • Contains                                   │ │
│  │               • Regex Pattern                              │ │
│  │                                                             │ │
│  │               [Search] [Clear]                             │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  Search Results                            [5 results found]    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ Show [10▼] entries                          Search: [____] │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ Region  │ Key        │ Value Preview  │ TTL   │ Actions   │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ users   │ user:1     │ {"name":"...  │ ∞     │ [View]    │ │
│  │ users   │ user:123   │ {"name":"...  │ 45s   │ [View]    │ │
│  │ sessions│ session:a  │ "active"      │ 300s  │ [View]    │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ Showing 1 to 3 of 5 entries      [Prev][1][2][Next]       │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Search Form Features

#### 1. Region Selector
- Dropdown menu
- "All Regions" option
- Individual region selection
- Pre-populated from system
- Retains selection after search

**Options:**
```
All Regions  ← Search across all regions
region0      ← Default region
users        ← Custom region
sessions     ← Custom region
products     ← Custom region
```

#### 2. Search Key Input
- Text input field
- Required for search
- Placeholder text
- Retains value after search
- Clear on form reset

#### 3. Search Type Selector

**Exact Match:**
- Finds keys matching exactly
- Case-sensitive
- Fast and efficient
- Default option

**Contains:**
- Finds keys containing the search term
- Case-insensitive
- Substring matching
- Broad search

**Regex Pattern:**
- Uses regular expressions
- Advanced pattern matching
- Full regex support
- Power user feature

**Examples:**
```
Exact:    "user:123"      → Only "user:123"
Contains: "user"          → user:1, user:123, myuser
Regex:    "user:[0-9]+"   → user:1, user:123, user:999
```

#### 4. Action Buttons

**Search Button:**
- Primary action
- Submits form
- Triggers search
- Shows loading state

**Clear Button:**
- Resets form
- Clears results
- Returns to initial state
- Clears URL parameters

### Search Results

#### 1. Results Header
- Total count badge
- "X results found"
- Color-coded (blue)
- Clear visibility

#### 2. Results Table

**Columns:**
1. **Region** - Badge with region name
2. **Key** - Strong emphasis
3. **Value Preview** - Truncated (code format)
4. **TTL** - Badge with status
5. **Actions** - View button

**TTL Display:**
```
∞ No expiry     (Green badge)
45s             (Blue badge)
❌ Expired      (Red badge)
```

#### 3. Empty Results State
- Info alert box
- Helpful message
- Search suggestions
- Try different terms

**Message:**
```
ℹ️ No matching entries found.
Try a different search term or pattern.
```

#### 4. Enhanced Table Features
- All standard table features
- Sorting by columns
- In-table search/filter
- Pagination
- Row count display

#### 5. Result Actions
- View link for each result
- Opens entry detail page
- Preserves search context
- Back button returns to search

### Search Workflow

**Step-by-Step:**

1. **Select Region** (optional)
   - Choose specific region or "All Regions"

2. **Enter Search Term**
   - Type the key pattern to find

3. **Choose Search Type**
   - Exact, Contains, or Regex

4. **Click Search**
   - Form submits
   - Results display below
   - Form values retained

5. **Review Results**
   - Sort, filter, or paginate
   - Click "View" to see details

6. **Clear Search** (optional)
   - Reset form
   - Start new search

### Advanced Search Patterns

**Regex Examples:**

```
^user:           → Keys starting with "user:"
:profile$        → Keys ending with ":profile"
user:[0-9]{3}    → user: followed by 3 digits
session:.*:temp  → session: with :temp at end
```

**Use Cases:**

1. **Find all user profiles:**
   - Region: users
   - Search: user:
   - Type: Contains

2. **Find specific session:**
   - Region: sessions
   - Search: session:abc-123
   - Type: Exact Match

3. **Find temporary data:**
   - Region: All Regions
   - Search: :temp$
   - Type: Regex

4. **Find numbered keys:**
   - Region: cache
   - Search: item:[0-9]+
   - Type: Regex

---

## Statistics & Monitoring

### Statistics Page

**File:** `stats.html`

Displays system-wide statistics and monitoring information.

**Layout:**

```
┌─────────────────────────────────────────────────────────────────┐
│  System Statistics                                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────┐  ┌──────────────────────┐            │
│  │ System Information   │  │ Regions              │            │
│  ├──────────────────────┤  ├──────────────────────┤            │
│  │ Version:    1.0.0    │  │ Total: 4             │            │
│  │ Uptime:     86400s   │  │                      │            │
│  │ Mode:       PRIMARY  │  │ Region Names:        │            │
│  │ Database:   SQL      │  │ [region0] [users]    │            │
│  │             SQLite   │  │ [sessions] [cache]   │            │
│  └──────────────────────┘  └──────────────────────┘            │
│                                                                  │
│  Pub/Sub Statistics                                             │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ Channel Subscriptions:    5                                │ │
│  │ Region Subscriptions:     3                                │ │
│  │ Key Subscriptions:       12                                │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Statistics Components

#### 1. System Information Card

**Displays:**
- **Version** - Software version number
- **Uptime** - Seconds since start
- **Instance Mode** - PRIMARY or SECONDARY badge
- **Database Type** - SQL or RocksDB
- **Database Info** - Specific database (SQLite, PostgreSQL, etc.)

**Formatting:**
- Clean key-value pairs
- Badge for instance mode
- Color-coded status
- Read-only display

#### 2. Regions Card

**Displays:**
- **Total Regions** - Count of all regions
- **Region Names** - Badges for each region
- **Visual Layout** - Grid of badges

**Features:**
- Badge per region
- Primary color
- Wrapped layout
- Clickable (optional)

#### 3. Pub/Sub Statistics Card

**Displays:**
- **Channel Subscriptions** - Active channel subscribers
- **Region Subscriptions** - Active region watchers
- **Key Subscriptions** - Active key watchers

**Purpose:**
- Monitor active subscriptions
- Track pub/sub usage
- System health indicator

**Conditional Display:**
- Only shows if pub/sub enabled
- Empty state if not configured
- Clear labels

### Future Enhancements

**Potential Additions:**
- Memory usage graphs
- Request rate charts
- Cache hit/miss ratio
- Historical data trends
- Export capabilities
- Real-time updates
- Alert thresholds

---

## Enhanced Table Features

### Table Utilities

**File:** `table-utils.js`

JavaScript library providing advanced table functionality.

### Features Overview

```
┌────────────────────────────────────────────────────────────────┐
│ Show [10 ▼] entries                          Search: [_______] │
├────────────────────────────────────────────────────────────────┤
│ Column 1 ⇅  │ Column 2 ⇅  │ Column 3 ⇅  │ Actions          │ │
├────────────────────────────────────────────────────────────────┤
│ Data row 1                                                     │
│ Data row 2                                                     │
│ Data row 3                                                     │
├────────────────────────────────────────────────────────────────┤
│ Showing 1 to 10 of 50 entries    [Prev][1][2][3][4][5][Next]  │
└────────────────────────────────────────────────────────────────┘
```

### 1. Sorting

**Features:**
- Click column headers to sort
- Toggle ascending/descending
- Visual indicators:
  - ⇅ Unsorted
  - ▲ Ascending
  - ▼ Descending
- Multi-type support:
  - Numeric sorting
  - Alphabetic sorting
  - Date sorting

**Implementation:**
```javascript
// Click handler on headers
// Sorts filteredRows array
// Updates sort indicators
// Re-renders table
```

**Excluded Columns:**
- Add `class="no-sort"` to header
- Typically for action buttons
- Manual override available

**Behavior:**
- First click: Ascending
- Second click: Descending
- Third click: Back to ascending
- Visual feedback immediate

### 2. Search/Filter

**Features:**
- Real-time filtering
- Searches all columns
- Case-insensitive
- Instant results
- No page reload

**Search Box:**
- Positioned in top-right
- Text input field
- Placeholder: "Search in table..."
- Clear button (browser default)

**Behavior:**
```javascript
// Input event listener
// Filters rows on each keystroke
// Updates visible rows
// Resets to page 1
// Updates pagination
```

**Implementation:**
```javascript
filter(searchTerm) {
    const term = searchTerm.toLowerCase();
    this.filteredRows = this.allRows.filter(row => {
        return row.textContent.toLowerCase().includes(term);
    });
    this.currentPage = 1;
    this.render();
}
```

### 3. Pagination

**Features:**
- Configurable page size
- Multiple preset sizes
- Direct page navigation
- Previous/Next buttons
- Smart page button display

**Page Size Selector:**
```
Show [10 ▼] entries
Options: 5, 10, 50, 100, All
```

**Pagination Controls:**
```
[Prev] [1] [2] ... [10] [11] [12] ... [50] [Next]
        ↑Current page highlighted
```

**Smart Display:**
- Shows 5 page numbers max
- Current page centered
- Ellipsis for gaps
- First and last always visible

**Info Display:**
```
Showing 11 to 20 of 150 entries
```

**Features:**
- Shows range and total
- Updates on filter
- Clear and concise
- Left-aligned

### 4. Configuration Options

**Constructor:**
```javascript
new EnhancedTable('tableId', {
    rowsPerPage: 10,              // Default page size
    pageSizes: [5, 10, 50, 'All'], // Size options
    searchable: true,              // Enable search
    sortable: true                 // Enable sorting
});
```

**Initialization:**
```javascript
document.addEventListener('DOMContentLoaded', function() {
    new EnhancedTable('myTable', {
        rowsPerPage: 25,
        searchable: true,
        sortable: true
    });
});
```

### 5. No Results State

**Display:**
```
┌────────────────────────────────────────────────────────────────┐
│          No matching records found                             │
└────────────────────────────────────────────────────────────────┘
```

**Features:**
- Centered message
- Gray text color
- Full table width
- Helpful feedback

### 6. Performance Optimizations

**Techniques:**
- Virtual rows (filters array)
- DOM minimization
- Event delegation
- Efficient rendering
- Debounced search (optional)

**Benefits:**
- Smooth with 10,000+ rows
- No lag during typing
- Fast sort operations
- Responsive pagination

### Usage Examples

**Basic Table:**
```html
<table id="myTable" class="table">
    <thead>
        <tr>
            <th>Name</th>
            <th>Email</th>
            <th class="no-sort">Actions</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>John Doe</td>
            <td>john@example.com</td>
            <td><button>Edit</button></td>
        </tr>
    </tbody>
</table>

<script>
new EnhancedTable('myTable', {
    rowsPerPage: 10
});
</script>
```

**Advanced Configuration:**
```javascript
new EnhancedTable('complexTable', {
    rowsPerPage: 25,
    pageSizes: [10, 25, 50, 100, 'All'],
    searchable: true,
    sortable: true
});
```

---

## Auto-Refresh System

### Overview

The auto-refresh system provides real-time updates for region and key listings without manual page reloads.

### iOS-Style Toggle Switch

**Visual Design:**

```
OFF State:
┌──────────────────────────┐
│ 🔄 Auto-Refresh  ○────── │
└──────────────────────────┘

ON State:
┌──────────────────────────┐
│ 🔄 Auto-Refresh  ──────● │
│ 🕐 15s                   │
└──────────────────────────┘
```

**CSS Implementation:**
```css
.toggle-switch {
    width: 56px;
    height: 32px;
    position: relative;
}

.toggle-slider {
    border-radius: 32px;
    background-color: #ccc;
    transition: 0.3s;
}

input:checked + .toggle-slider {
    background-color: #34c759; /* iOS green */
}

.toggle-slider:before {
    /* White circle that slides */
    border-radius: 50%;
    transition: 0.3s;
}
```

### Countdown Timer

**Visual States:**

```
30-11 seconds:  [🕐 25s]  Gray badge
10-6 seconds:   [🕐 8s]   Yellow badge
5-0 seconds:    [🕐 3s]   Red badge
```

**Behavior:**
- Updates every second
- Color changes at thresholds
- Smooth transitions
- Visual urgency indicator

**JavaScript Implementation:**
```javascript
const REFRESH_INTERVAL = 30; // seconds
let remainingSeconds = 30;

function updateCountdownDisplay() {
    document.getElementById('countdownValue').textContent = remainingSeconds;
    
    // Color coding
    if (remainingSeconds <= 5) {
        badge.classList.add('bg-danger');
    } else if (remainingSeconds <= 10) {
        badge.classList.add('bg-warning');
    } else {
        badge.classList.add('bg-secondary');
    }
}
```

### State Persistence

**LocalStorage:**
```javascript
// Save state
localStorage.setItem('regionsAutoRefresh', 'true');

// Load state on page load
const savedState = localStorage.getItem('regionsAutoRefresh');
if (savedState === 'true') {
    enableAutoRefresh();
}
```

**Benefits:**
- Remembers user preference
- Persists across sessions
- Per-page configuration
- No server storage needed

### Refresh Behavior

**On Refresh:**
1. Page reloads
2. Countdown resets to 30
3. Toggle state preserved
4. Current filters maintained (region detail)
5. Scroll position reset

**Pattern Preservation:**
```javascript
// Region detail page
if (currentPattern && currentPattern !== '*') {
    window.location.href = window.location.pathname + 
                          '?pattern=' + encodeURIComponent(currentPattern);
} else {
    window.location.reload();
}
```

### Manual Controls

**Toggle Switch:**
- Click to enable/disable
- Instant visual feedback
- Starts/stops countdown
- Saves preference

**Manual Refresh:**
- Browser refresh button
- F5 key
- Ctrl+R / Cmd+R
- Stops auto-refresh

### Implementation Pages

**Regions List:**
- Storage key: `'regionsAutoRefresh'`
- Full page reload
- No parameters

**Region Detail:**
- Storage key: `'regionDetailAutoRefresh'`
- Preserves pattern parameter
- Maintains filter state

### Cleanup

**On Page Unload:**
```javascript
window.addEventListener('beforeunload', function() {
    if (countdownInterval) {
        clearInterval(countdownInterval);
    }
    if (refreshInterval) {
        clearInterval(refreshInterval);
    }
});
```

**Prevents:**
- Memory leaks
- Background timers
- Resource waste

---

## Live TTL Countdown

### Overview

Real-time TTL (Time To Live) countdown on entry detail pages, providing visual feedback on entry expiration.

### Visual States

**Format Based on Duration:**

```
> 1 hour:
⏱️ 2h 15m 30s remaining

1 hour - 1 minute:
⏱️ 45m 20s remaining

< 1 minute:
⏱️ 35 seconds remaining

Final 10 seconds:
⏱️ 5 seconds remaining (Pulsing animation)

Expired:
🚫 EXPIRED
```

### Color Coding

**Timeline:**
```
Green (Success):    > 1 hour
Blue (Info):        1 hour - 31 seconds
Yellow (Warning):   30-11 seconds
Orange (Alert):     10-6 seconds
Red (Danger):       5-0 seconds (Pulsing)
Red (Expired):      0 seconds (Permanent)
```

**CSS Classes:**
```css
.bg-success    /* Green */
.bg-info       /* Blue */
.bg-warning    /* Yellow */
.bg-danger     /* Red */

.ttl-pulse {   /* Pulsing animation */
    animation: pulse 1s ease-in-out infinite;
}
```

### Implementation

**HTML Structure:**
```html
<span id="ttl-badge"
      class="badge bg-warning text-dark ttl-countdown"
      data-initial-ttl="45">
    ⏱️ 45 seconds remaining
</span>
```

**JavaScript Logic:**
```javascript
// Get initial TTL from data attribute
let remainingSeconds = parseInt(ttlBadge.getAttribute('data-initial-ttl'));
const startTime = Date.now();

function updateTTL() {
    // Calculate elapsed time
    const elapsed = Math.floor((Date.now() - startTime) / 1000);
    remainingSeconds = initialTTL - elapsed;
    
    // Handle expiration
    if (remainingSeconds <= 0) {
        ttlBadge.className = 'badge bg-danger ttl-countdown';
        ttlBadge.innerHTML = '🚫 EXPIRED';
        clearInterval(countdownInterval);
        showExpirationAlert();
        return;
    }
    
    // Format display
    formatTTLDisplay(remainingSeconds);
}

// Update every second
setInterval(updateTTL, 1000);
```

### Time Formatting

**Logic:**
```javascript
if (seconds > 3600) {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hours}h ${minutes}m ${secs}s`;
} else if (seconds > 60) {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}m ${secs}s`;
} else {
    return `${seconds} seconds`;
}
```

**Examples:**
```
7285 seconds → 2h 1m 25s
125 seconds  → 2m 5s
45 seconds   → 45 seconds
```

### Expiration Handling

**Alert Dialog:**
```javascript
setTimeout(function() {
    if (confirm('This entry has expired. Refresh page or return to region?')) {
        window.location.reload();
    }
}, 2000); // 2 seconds after expiration
```

**Features:**
- Delayed 2 seconds after expiration
- User choice: refresh or return
- Prevents immediate disruption
- Clear communication

### Special States

**No Expiration:**
```html
<span class="badge bg-success">
    <i class="bi bi-infinity"></i> No expiration
</span>
```

**Key Not Found:**
```html
<span class="badge bg-danger">
    <i class="bi bi-x-circle"></i> Key does not exist
</span>
```

**Already Expired:**
```html
<span class="badge bg-danger">
    <i class="bi bi-x-circle"></i> Entry expired
</span>
```

### Performance Considerations

**Optimization:**
- Single setInterval per page
- Efficient DOM updates
- Minimal reflows
- Clean on page unload

**Cleanup:**
```javascript
window.addEventListener('beforeunload', function() {
    clearInterval(countdownInterval);
});
```

### User Experience

**Benefits:**
- Real-time feedback
- Visual urgency indication
- No manual refresh needed
- Prevents stale data viewing
- Clear expiration notice

**Accessibility:**
- Color + text indication
- Clear icon usage
- Readable formatting
- Screen reader friendly

---

## User Interface Components

### Flash Messages

**Success Messages:**
```html
<div class="alert alert-success alert-dismissible">
    <span>Region created successfully!</span>
    <button class="btn-close" data-bs-dismiss="alert"></button>
</div>
```

**Error Messages:**
```html
<div class="alert alert-danger alert-dismissible">
    <span>Error: Failed to delete entry</span>
    <button class="btn-close" data-bs-dismiss="alert"></button>
</div>
```

**Features:**
- Auto-positioned at top of content
- Dismissible with close button
- Color-coded by type
- Bootstrap alert styling
- Icon support (optional)

**Types:**
- Success (green)
- Error (red)
- Warning (yellow)
- Info (blue)

### Badges

**Status Badges:**
```html
<!-- Primary/Secondary -->
<span class="badge bg-success">PRIMARY</span>
<span class="badge bg-warning">SECONDARY</span>

<!-- Admin Role -->
<span class="badge bg-primary">ADMIN</span>

<!-- Default Region -->
<span class="badge bg-secondary">DEFAULT</span>
```

**TTL Badges:**
```html
<span class="badge bg-success">∞ No expiry</span>
<span class="badge bg-info">45s</span>
<span class="badge bg-danger">Expired</span>
```

**Features:**
- Color-coded by meaning
- Small and unobtrusive
- Bootstrap styling
- Icon support

### Buttons

**Primary Actions:**
```html
<button class="btn btn-primary">
    <i class="bi bi-plus-circle"></i> Create Entry
</button>
```

**Secondary Actions:**
```html
<button class="btn btn-secondary">
    <i class="bi bi-arrow-left"></i> Back
</button>
```

**Danger Actions:**
```html
<button class="btn btn-danger">
    <i class="bi bi-trash"></i> Delete
</button>
```

**Button Sizes:**
- `btn-sm` - Small
- `btn` - Regular (default)
- `btn-lg` - Large

**Button Groups:**
```html
<div class="btn-group">
    <button class="btn btn-sm btn-primary">View</button>
    <button class="btn btn-sm btn-warning">Edit</button>
    <button class="btn btn-sm btn-danger">Delete</button>
</div>
```

### Modal Dialogs

**Delete Confirmation:**
```html
<div class="modal fade" id="deleteModal">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header bg-danger text-white">
                <h5>⚠️ Confirm Delete</h5>
                <button class="btn-close btn-close-white"></button>
            </div>
            <div class="modal-body">
                <div class="alert alert-danger">
                    <strong>Warning:</strong> Cannot be undone!
                </div>
                <p>Delete this entry?</p>
            </div>
            <div class="modal-footer">
                <button class="btn btn-secondary">Cancel</button>
                <button class="btn btn-danger">Delete</button>
            </div>
        </div>
    </div>
</div>
```

**Features:**
- Backdrop overlay
- Escape key closes
- Click outside closes
- Header color coding
- Warning alerts
- Two-step confirmation

### Cards

**Standard Card:**
```html
<div class="card">
    <div class="card-header bg-primary text-white">
        <h5>Card Title</h5>
    </div>
    <div class="card-body">
        <p>Card content...</p>
    </div>
</div>
```

**Info Card:**
```html
<div class="card">
    <div class="card-header">
        <h6>💡 Helpful Information</h6>
    </div>
    <div class="card-body">
        <p>Tip content...</p>
    </div>
</div>
```

**Features:**
- Bordered container
- Header and body sections
- Color-coded headers
- Flexible content
- Responsive sizing

### Forms

**Input Groups:**
```html
<div class="input-group">
    <span class="input-group-text">🔑</span>
    <input type="text" class="form-control" placeholder="Enter key">
</div>
```

**Text Areas:**
```html
<textarea class="form-control" rows="10"
          style="font-family: 'Courier New', monospace;">
</textarea>
```

**Select Dropdowns:**
```html
<select class="form-select">
    <option>Option 1</option>
    <option>Option 2</option>
</select>
```

**Features:**
- Bootstrap styling
- Icon prefixes
- Help text
- Validation states
- Monospace fonts for code

### Icons

**Bootstrap Icons:**
```html
<i class="bi bi-search"></i>      <!-- Search -->
<i class="bi bi-trash"></i>       <!-- Delete -->
<i class="bi bi-pencil"></i>      <!-- Edit -->
<i class="bi bi-eye"></i>         <!-- View -->
<i class="bi bi-plus-circle"></i> <!-- Add -->
<i class="bi bi-arrow-left"></i>  <!-- Back -->
<i class="bi bi-clock"></i>       <!-- Time -->
<i class="bi bi-infinity"></i>    <!-- Infinity -->
```

**Usage:**
- Inside buttons
- Before labels
- In alerts
- Status indicators
- Navigation items

### Tooltips (Future)

**HTML:**
```html
<button class="btn btn-primary" 
        data-bs-toggle="tooltip" 
        title="Click to create new entry">
    Create
</button>
```

**Initialization:**
```javascript
const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
[...tooltipTriggerList].map(el => new bootstrap.Tooltip(el));
```

---

## Responsive Design

### Breakpoints

**Bootstrap 5 Breakpoints:**
```
xs: < 576px   (Extra small - phones)
sm: ≥ 576px   (Small - phones landscape)
md: ≥ 768px   (Medium - tablets)
lg: ≥ 992px   (Large - desktops)
xl: ≥ 1200px  (Extra large - wide desktops)
xxl: ≥ 1400px (Extra extra large)
```

### Mobile Optimization

**Navigation:**
- Collapses to hamburger menu
- Touch-friendly tap targets
- Full-width on mobile
- Smooth animations

**Tables:**
- Horizontal scroll on small screens
- Preserved functionality
- Touch-friendly controls
- Clear scroll indicators

**Forms:**
- Full-width inputs on mobile
- Large touch targets
- Stacked buttons
- Optimal spacing

**Cards:**
- Stack vertically on mobile
- Full-width on small screens
- Maintained readability
- Touch-friendly buttons

### Tablet Optimization

**Layout:**
- 2-column cards where appropriate
- Preserved navigation
- Optimized form layouts
- Touch-friendly controls

### Desktop Optimization

**Layout:**
- Multi-column layouts
- Sidebar help panels
- Efficient space usage
- Mouse-optimized interactions

### Touch Support

**Features:**
- Large tap targets (min 44x44px)
- Touch-friendly buttons
- Swipe-friendly tables
- No hover-dependent functionality

### Viewport Configuration

**HTML Head:**
```html
<meta name="viewport" content="width=device-width, initial-scale=1.0">
```

**Prevents:**
- Pinch-zoom disabled
- Optimal text size
- Proper scaling
- Touch optimization

---

## Security Features

### Authentication

**Session-Based:**
- Login required for all pages
- Session timeout handling
- Secure cookie storage
- HTTPS recommended

**Login Flow:**
1. User enters credentials
2. Server validates against users.json
3. Session created on success
4. Session ID in cookie
5. Validated on each request

### Authorization

**Role-Based Access Control:**

**Admin Role:**
- Create regions
- Create entries
- Edit entries
- Delete entries
- Delete regions
- All read operations

**User Role:**
- View dashboard
- Browse regions
- View entries
- Search functionality
- View statistics

**Instance-Based:**
- Write operations: Primary only
- Read operations: All instances
- Visual indicators of capability

### CSRF Protection

**Spring Security:**
- Automatic CSRF token
- Included in all forms
- Validated on POST requests
- Prevents cross-site attacks

**Implementation:**
```html
<form th:action="@{/entry/create}" method="post">
    <!-- CSRF token auto-included by Thymeleaf -->
    <input type="hidden" th:name="${_csrf.parameterName}" 
           th:value="${_csrf.token}"/>
</form>
```

### XSS Prevention

**Thymeleaf Escaping:**
- Automatic HTML escaping
- `th:text` for text content
- `th:utext` for unescaped (avoided)
- Context-aware escaping

**Example:**
```html
<!-- Safe - escaped -->
<span th:text="${userInput}">text</span>

<!-- Dangerous - don't use -->
<span th:utext="${userInput}">html</span>
```

### SQL Injection Prevention

**Prepared Statements:**
- Spring JDBC templates
- Parameterized queries
- No string concatenation
- Type-safe operations

### Session Security

**Features:**
- Secure flag (HTTPS)
- HttpOnly flag
- SameSite attribute
- Timeout configuration
- Automatic logout on expiry

**Configuration:**
```properties
server.servlet.session.timeout=30m
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.same-site=strict
```

### Input Validation

**Client-Side:**
- HTML5 validation
- Pattern attributes
- Required fields
- JavaScript validation

**Server-Side:**
- Spring validation
- Type checking
- Business rule validation
- Error messages

### Permission Checks

**Template-Level:**
```html
<button th:if="${user.isAdmin() and isPrimary}">
    Delete
</button>
```

**Features:**
- Role-based rendering
- Instance-based rendering
- Prevents unauthorized access
- UI consistency

---

## User Experience Enhancements

### Quick Actions

**TTL Preset Buttons:**
- One-click common values
- Visual button feedback
- Instant field update
- Reduces typing errors

**Pattern Examples:**
- Clickable examples
- Auto-fills input
- Educational
- Time-saving

### JSON Auto-Formatting

**Feature:**
- Detects JSON on blur
- Pretty-prints automatically
- 2-space indentation
- Ignores invalid JSON

**Benefits:**
- Improved readability
- Syntax checking
- Professional appearance
- Error detection

**Implementation:**
```javascript
valueTextarea.addEventListener('blur', function() {
    const value = this.value.trim();
    if (value.startsWith('{') || value.startsWith('[')) {
        try {
            const formatted = JSON.stringify(JSON.parse(value), null, 2);
            this.value = formatted;
        } catch (e) {
            // Not valid JSON, ignore
        }
    }
});
```

### Copy to Clipboard

**Feature:**
- One-click copy button
- No selection needed
- Works with large values
- Success feedback

**Implementation:**
```javascript
function copyToClipboard() {
    navigator.clipboard.writeText(value).then(
        () => alert('Copied!'),
        (err) => console.error('Copy failed:', err)
    );
}
```

### Keyboard Shortcuts

**Supported:**
- Tab navigation
- Enter to submit
- Escape to close modals
- Ctrl+Z undo in forms

### Contextual Help

**Help Sidebars:**
- Always visible
- Relevant to current page
- Examples and tips
- Best practices

**Inline Help:**
- Form text below inputs
- Placeholder text
- Validation messages
- Clear instructions

### Visual Feedback

**Loading States:**
- Button disable on submit
- Spinner (optional)
- Prevents double-submit
- Clear state indication

**Hover States:**
- Button highlights
- Link underlines
- Row highlights in tables
- Clear interactivity

**Focus States:**
- Input borders
- Button outlines
- Keyboard navigation
- Accessibility compliance

### Error Prevention

**Confirmation Modals:**
- Dangerous operations
- Two-step process
- Clear warnings
- Easy cancellation

**Validation:**
- Real-time feedback
- Clear error messages
- Helpful suggestions
- Input requirements

**Unsaved Changes:**
- Browser dialog on leave
- Prevents data loss
- Clear warning
- User choice

---

## Theme & Styling

### Color Scheme

**Primary Colors:**
```css
Primary:   #0d6efd (Bootstrap blue)
Success:   #198754 (Green)
Warning:   #ffc107 (Yellow)
Danger:    #dc3545 (Red)
Info:      #0dcaf0 (Light blue)
```

**Secondary Colors:**
```css
Secondary: #6c757d (Gray)
Light:     #f8f9fa (Light gray)
Dark:      #212529 (Dark gray)
```

**Instance Status:**
```css
Primary:   #198754 (Green) - Active/Write
Secondary: #ffc107 (Yellow) - Standby/Read-only
```

### Typography

**Fonts:**
```css
Body:      -apple-system, BlinkMacSystemFont, "Segoe UI"
Headings:  Inherit from body
Code:      'Courier New', Courier, monospace
```

**Sizes:**
```css
h1: 2.5rem
h2: 2rem
h3: 1.75rem
h4: 1.5rem
h5: 1.25rem
h6: 1rem
body: 1rem (16px)
```

### Spacing

**Padding:**
```css
Container:  padding-top: 60px (navbar clearance)
Cards:      padding: 1.25rem
Forms:      margin-bottom: 1rem between fields
```

**Margins:**
```css
Sections:   margin-bottom: 2rem
Cards:      margin-bottom: 1.5rem
Elements:   margin-bottom: 1rem
```

### Borders & Shadows

**Cards:**
```css
border: 1px solid rgba(0,0,0,.125)
border-radius: 0.375rem
box-shadow: none (default)
```

**Modals:**
```css
box-shadow: 0 0.5rem 1rem rgba(0,0,0,.15)
```

**Login/Error Pages:**
```css
box-shadow: 0 10px 40px rgba(0,0,0,0.2)
border-radius: 10px
```

### Gradients

**Login Page:**
```css
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
```

**Navigation Header:**
```css
background: #343a40 (Dark gray - solid)
```

### Icons

**Usage:**
- Bootstrap Icons library
- Inline with text
- Consistent sizing
- Color matches text

**Common Icons:**
```
⚡ Lightning - Brand
🔍 Search
🗑️ Trash - Delete
✏️ Pencil - Edit
👁️ Eye - View
➕ Plus - Add
⬅️ Arrow - Back
```

### Animations

**Transitions:**
```css
All interactive elements: transition: all 0.3s ease
Buttons: 0.15s ease
Toggle switch: 0.3s ease
```

**Pulsing:**
```css
@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.7; }
}
```

**Used For:**
- Critical TTL countdown
- Loading indicators
- Attention drawing

---

## Browser Compatibility

### Supported Browsers

**Desktop:**
- Chrome 90+ ✓
- Firefox 88+ ✓
- Safari 14+ ✓
- Edge 90+ ✓
- Opera 76+ ✓

**Mobile:**
- iOS Safari 14+ ✓
- Chrome Android 90+ ✓
- Firefox Android 88+ ✓
- Samsung Internet 14+ ✓

### Required Features

**JavaScript:**
- ES6 (2015) syntax
- Arrow functions
- Template literals
- Classes
- Promises
- Async/await
- Fetch API
- LocalStorage

**CSS:**
- Flexbox
- Grid (optional)
- Custom properties (--var)
- Transitions
- Animations

**HTML:**
- HTML5 elements
- Form validation
- Data attributes
- Semantic markup

### Fallbacks

**JavaScript Disabled:**
- Basic form submission works
- No dynamic features
- Server-side validation
- Static content display

**Old Browsers:**
- Graceful degradation
- Core functionality preserved
- Reduced visual effects
- Basic styling maintained

### Testing Recommendations

**Browser Testing:**
- Test in Chrome (primary)
- Test in Firefox
- Test in Safari
- Test in mobile browsers

**Device Testing:**
- Desktop (1920x1080)
- Laptop (1366x768)
- Tablet (768x1024)
- Phone (375x667)

---

## Accessibility Features

### WCAG Compliance

**Target Level:** AA (minimum)

**Areas:**
- Color contrast ratios
- Keyboard navigation
- Screen reader support
- Focus indicators
- Alternative text

### Keyboard Navigation

**Supported:**
- Tab through interactive elements
- Enter to submit forms
- Escape to close modals
- Arrow keys in dropdowns
- Focus management

**Focus Indicators:**
- Visible outline on focus
- Color contrast compliant
- Clear visual feedback
- No focus traps

### Screen Readers

**Features:**
- Semantic HTML
- ARIA labels where needed
- Form labels properly associated
- Table headers defined
- Landmark regions

**Improvements Needed:**
- More ARIA landmarks
- Live region announcements
- Better table descriptions
- Form error announcements

### Color Contrast

**Tested Combinations:**
- Text on white: 4.5:1+ ✓
- Buttons: 3:1+ ✓
- Badges: Varies by type
- Links: 4.5:1+ ✓

**Improvements Needed:**
- Some badge contrasts
- Light text on yellow
- Better status indicators

### Alternative Text

**Images:**
- Icon fonts (decorative)
- No critical images
- Status conveyed with text
- No alt text needed currently

### Form Accessibility

**Features:**
- Label associations
- Required indicators
- Error messages
- Help text
- Fieldset grouping

**Improvements:**
- Error announcements
- Validation feedback
- Success confirmation
- Progress indicators

---

## Troubleshooting

### Common Issues

#### 1. Login Issues

**Problem:** Cannot log in with correct credentials

**Solutions:**
- Check users.json file exists
- Verify user credentials
- Check application logs
- Clear browser cookies
- Try different browser

#### 2. Auto-Refresh Not Working

**Problem:** Toggle doesn't start countdown

**Solutions:**
- Check JavaScript console
- Clear localStorage
- Disable browser extensions
- Check for script errors
- Refresh page

#### 3. TTL Countdown Not Updating

**Problem:** TTL shows but doesn't count down

**Solutions:**
- Check JavaScript console
- Verify TTL > 0
- Refresh entry detail page
- Check browser compatibility
- Disable extensions

#### 4. Table Not Sorting

**Problem:** Clicking headers doesn't sort

**Solutions:**
- Verify table-utils.js loaded
- Check JavaScript console
- Verify table has ID
- Check initialization code
- Refresh page

#### 5. JSON Not Auto-Formatting

**Problem:** JSON stays on one line

**Solutions:**
- Ensure valid JSON
- Click outside textarea (blur)
- Check JavaScript console
- Verify no syntax errors
- Manual formatting works

### Browser Console Errors

**Check Console:**
1. Press F12
2. Go to Console tab
3. Look for red errors
4. Note error messages
5. Check network tab for 404s

**Common Errors:**
```
Failed to load resource: 404
→ Missing file (CSS, JS, or resource)

Uncaught TypeError
→ JavaScript error, check code

CORS policy error
→ Cross-origin issue

Failed to fetch
→ Network or server issue
```

### Performance Issues

**Slow Page Load:**
- Check network speed
- Verify server response
- Check browser extensions
- Clear cache
- Reduce concurrent requests

**Slow Table Operations:**
- Reduce displayed rows
- Use pagination
- Filter data
- Check row count
- Upgrade browser

### Mobile Issues

**Touch Not Working:**
- Ensure touch target size
- Check for hover-only features
- Test on actual device
- Check viewport settings
- Verify touch events

**Layout Issues:**
- Check viewport meta tag
- Test different orientations
- Verify responsive classes
- Check breakpoints
- Test on multiple devices

### Session Timeouts

**Problem:** Logged out unexpectedly

**Solutions:**
- Check session timeout config
- Increase timeout value
- Save work frequently
- Check server logs
- Verify session cookies

---

## Appendix

### File Structure

```
templates/
├── base.html              (Layout template)
├── login.html             (Login page)
├── index.html             (Dashboard)
├── regions.html           (Regions list)
├── region-detail.html     (Region keys)
├── create-region.html     (Create region form)
├── entry-detail.html      (Entry detail)
├── create-entry.html      (Create entry form)
├── edit-entry.html        (Edit entry form)
├── search.html            (Search interface)
├── stats.html             (Statistics)
└── error.html             (Error page)

static/
├── js/
│   └── table-utils.js     (Enhanced tables)
└── webjars/
    ├── bootstrap/5.3.2/   (Bootstrap CSS/JS)
    └── jquery/3.7.1/      (jQuery)
```

### Key Technologies

**Frontend:**
- HTML5
- CSS3
- JavaScript (ES6+)
- Bootstrap 5.3.2
- jQuery 3.7.1
- Bootstrap Icons

**Templating:**
- Thymeleaf 3.x
- Spring Expression Language (SpEL)

**Backend Integration:**
- Spring MVC
- Spring Security
- Session management

### Future Enhancements

**Planned Features:**
- Dark mode toggle
- Custom themes
- Keyboard shortcut panel
- Bulk operations UI
- Import/Export UI
- Real-time updates (WebSocket)
- Advanced charts
- User preferences panel
- Multi-language support
- Accessibility improvements

**UI Improvements:**
- Loading spinners
- Progress bars
- Toast notifications
- Better error pages
- Confirmation improvements
- Drag-and-drop
- Inline editing
- Quick filters

**Mobile Enhancements:**
- Native app feel
- Offline capability
- Touch gestures
- Better mobile tables
- Mobile-first forms

### Best Practices

**Development:**
- Use semantic HTML
- Follow Bootstrap conventions
- Maintain consistent spacing
- Use Bootstrap utilities
- Minimize custom CSS
- Comment complex code
- Test across browsers

**Performance:**
- Minimize DOM manipulation
- Use event delegation
- Debounce expensive operations
- Lazy load when possible
- Optimize images
- Minimize HTTP requests

**Accessibility:**
- Use semantic elements
- Add ARIA when needed
- Test with keyboard only
- Test with screen reader
- Maintain color contrast
- Provide text alternatives

**Security:**
- Never trust client input
- Escape all output
- Use HTTPS in production
- Implement CSRF protection
- Validate on server
- Sanitize user content

---

## Conclusion

The AshRedis GUI provides a comprehensive, user-friendly interface for managing cache data. With features like live TTL countdowns, auto-refresh, enhanced tables, and responsive design, it offers a professional and efficient experience for both administrators and users.

The interface prioritizes usability, security, and performance while maintaining a clean, modern aesthetic. Continuous improvements and feature additions ensure the GUI remains a powerful tool for cache management.

---

## Support

**For Issues:**
- Check this documentation
- Review troubleshooting section
- Check browser console
- Review application logs
- Contact: ajsinha@gmail.com

**For Features:**
- Submit feature requests
- Provide detailed use cases
- Include mockups if possible
- Explain business value

---

**Version:** 3.0  
**Last Updated:** January 2025  
**Copyright:** All Rights Reserved 2025-2030, Ashutosh Sinha  
**Author:** ajsinha@gmail.com

**End of GUI Documentation**
