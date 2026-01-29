# Session Management Feature

## 🎯 Feature Overview

Implement session-based authentication and data storage for enhanced security and privacy. All user data (authentication tokens, preferences, and filters) is stored in `sessionStorage` and automatically cleared when the browser window or tab is closed.

---

## 📋 Requirements

### **Core Behavior**
- User stays logged in as long as the browser window/tab is open
- All data clears automatically when window/tab is closed
- No persistent storage of authentication tokens or user preferences
- Fresh session starts every time the user opens the application

### **Data Stored in SessionStorage**

#### **Authentication Data**
- `auth_token` - JWT authentication token
- `user_data` - User information (phone, name, role)

#### **User Preferences**
- `language` - Language preference (en/zh-CN)

#### **Customer List Filters**
- `customerListFilters` - Search terms, status filters, certificate type filters, date ranges, pagination state

#### **Admin Dashboard Filters**
- `adminDashboardFilters` - Main dashboard filters (year, month, view options, certificate type selections)
- `admin_dashboard_overview` - Dashboard overview data cache
- `admin_dashboard_status_distribution` - Status distribution chart data
- `admin_dashboard_trends` - Trends chart data
- `admin_dashboard_certificate_trends` - Certificate type trends data
- `admin_dashboard_leaderboard` - Leaderboard data cache
- `admin_dashboard_certificate_type_selections` - Certificate type selection state
- `admin_dashboard_state` - Additional dashboard state data

---

## 🏗️ Implementation

### **Frontend Changes**

#### **Files Modified**

1. **`src/contexts/AuthContext.tsx`**
   - Changed `localStorage.getItem('auth_token')` → `sessionStorage.getItem('auth_token')`
   - Changed `localStorage.setItem('auth_token')` → `sessionStorage.setItem('auth_token')`
   - Updated login/register functions to use sessionStorage
   - Updated logout function to clear all sessionStorage items

2. **`src/contexts/LanguageContext.tsx`**
   - Changed `localStorage.getItem('language')` → `sessionStorage.getItem('language')`
   - Changed `localStorage.setItem('language')` → `sessionStorage.setItem('language')`

3. **`src/components/customers/CustomerList.tsx`**
   - Changed `localStorage.getItem('customerListFilters')` → `sessionStorage.getItem('customerListFilters')`
   - Changed `localStorage.setItem('customerListFilters')` → `sessionStorage.setItem('customerListFilters')`

4. **`src/app/dashboard/admin/page.tsx`**
   - Changed `localStorage.getItem('adminDashboardFilters')` → `sessionStorage.getItem('adminDashboardFilters')`
   - Changed `localStorage.setItem('adminDashboardFilters')` → `sessionStorage.setItem('adminDashboardFilters')`

5. **`src/lib/api.ts`**
   - Changed `localStorage.getItem('auth_token')` → `sessionStorage.getItem('auth_token')` (2 occurrences)
   - Updated `logout()` to clear `sessionStorage.removeItem('auth_token')`

### **No Backend Changes Required**
- Backend authentication logic remains unchanged
- JWT token validation works the same way
- No database schema changes needed

---

## 🔄 User Experience

### **Session Lifecycle**

#### **Login Flow**
1. User enters credentials
2. Backend validates and returns JWT token
3. Token stored in `sessionStorage`
4. User data stored in `sessionStorage`
5. User redirected to dashboard

#### **Active Session**
1. User navigates within application
2. All preferences and filters persist
3. Page refresh maintains session
4. Multiple tabs have independent sessions

#### **Session End**
1. User closes browser window/tab
2. **OR** user clicks logout button
3. All `sessionStorage` data cleared
4. Next visit requires fresh login

---

## 🔒 Security Benefits

### **Advantages of sessionStorage**

✅ **Automatic Cleanup**
- No persistent tokens on user's device
- Data cleared when window closes (private by default)
- Reduces risk of token theft from shared devices

✅ **Session Isolation**
- Each tab/window has independent session
- Closing one tab doesn't affect others
- Better security for multi-tab workflows

✅ **Privacy Protection**
- No long-term tracking of user preferences
- Language preference resets each session
- Filter selections not persisted across sessions

✅ **Compliance Ready**
- Easier to comply with privacy regulations (GDPR, etc.)
- No persistent user data without explicit consent
- Automatic data retention policy (session-only)

---

## 📊 Behavior Comparison

| Scenario | localStorage (Before) | sessionStorage (After) |
|----------|----------------------|------------------------|
| **Close browser window** | ✅ Stay logged in | ❌ Logged out |
| **Close tab** | ✅ Stay logged in | ❌ Logged out |
| **Open new tab** | ✅ Logged in | ❌ Need to login |
| **Refresh page** | ✅ Stay logged in | ✅ Stay logged in |
| **Navigate within app** | ✅ Stay logged in | ✅ Stay logged in |
| **Restart browser** | ✅ Stay logged in | ❌ Need to login |
| **Language preference** | ✅ Remembered | ❌ Reset to default |
| **Search filters** | ✅ Remembered | ❌ Reset to default |

---

## 🧪 Testing

### **Test Cases**

#### **Basic Session Tests**
- [ ] Login to application
- [ ] Verify `auth_token` exists in sessionStorage
- [ ] Refresh page → Should stay logged in
- [ ] Close tab and reopen → Should show login screen
- [ ] Verify sessionStorage is empty after close

#### **Data Persistence Tests**
- [ ] Login and set language to English
- [ ] Apply customer list filters
- [ ] Navigate to admin dashboard and set filters
- [ ] Refresh page → All settings should persist
- [ ] Close tab → All settings should be cleared
- [ ] Reopen and login → Should have fresh state

#### **Multi-Tab Tests**
- [ ] Login in Tab 1
- [ ] Open Tab 2 → Tab 2 should NOT be logged in
- [ ] Login in Tab 2 with different language
- [ ] Each tab should maintain independent state
- [ ] Close Tab 1 → Tab 2 should remain logged in

#### **Logout Tests**
- [ ] Login and apply filters
- [ ] Click logout button
- [ ] Verify all sessionStorage cleared
- [ ] Verify redirect to login page
- [ ] Verify no data persists after logout

---

## 📝 Code Examples

### **Reading from SessionStorage**
```typescript
const token = sessionStorage.getItem('auth_token');
const filters = JSON.parse(sessionStorage.getItem('customerListFilters') || '{}');
```

### **Writing to SessionStorage**
```typescript
sessionStorage.setItem('auth_token', token);
sessionStorage.setItem('customerListFilters', JSON.stringify(filters));
```

### **Clearing SessionStorage**
```typescript
sessionStorage.removeItem('auth_token');
sessionStorage.clear(); // Clear all items
```

---

## 🚀 Future Enhancements

### **Potential Improvements**

1. **"Remember Me" Option**
   - Add checkbox to allow users to opt-in to persistent storage
   - Use `localStorage` only when explicitly requested
   - Default to `sessionStorage` for privacy

2. **Session Timeout Warning**
   - Show warning before session expires
   - Allow user to extend session with activity
   - Auto-logout after period of inactivity (30 minutes)

3. **Cross-Tab Sync**
   - Use `BroadcastChannel` API to sync changes across tabs
   - Logout in one tab → logout all tabs
   - Language change in one tab → update all tabs

4. **Session Analytics**
   - Track session duration
   - Monitor most-used features per session
   - Analyze user engagement patterns

---

## 📚 References

- [MDN: sessionStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage)
- [MDN: localStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage)
- [OWASP: Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)

---

## ✅ Implementation Status

**Status**: ✅ Completed

**Implemented**: January 29, 2026

**Commit**: "Migrate all user data from localStorage to sessionStorage"

**Files Changed**: 5
- `src/contexts/AuthContext.tsx`
- `src/contexts/LanguageContext.tsx`
- `src/components/customers/CustomerList.tsx`
- `src/app/dashboard/admin/page.tsx`
- `src/lib/api.ts`

---

This session management implementation provides a secure, privacy-first approach to user data storage while maintaining a smooth user experience within each session.
