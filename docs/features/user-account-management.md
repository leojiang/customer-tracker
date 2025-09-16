# Feature Description & Implementation Plan

## 🎯 **Feature: User Account Management with Enable/Disable Functionality**

### **Feature Overview**
Transform the current "User Approvals" page into a comprehensive "User Management" system that allows admin users to not only approve/reject user registrations but also enable/disable existing user accounts. Disabled users will be prevented from logging in and will receive appropriate error messages.

---

## 📋 **Detailed Requirements**

### **1. Page Renaming & Scope Expansion**
- **Current**: "User Approvals" page (`/dashboard/admin/user-approvals`)
- **New**: "User Management" page (`/dashboard/admin/user-management`)
- **Scope**: Expand from approval-only to full user lifecycle management

### **2. New User Status: DISABLED**
- **Current Statuses**: `PENDING`, `APPROVED`, `REJECTED`
- **New Status**: `DISABLED` (separate from approval status)
- **Purpose**: Allow admins to temporarily disable approved users without rejecting them

### **3. Enhanced User Management Actions**
- **Current Actions**: Approve, Reject, Reset to Pending
- **New Actions**: Enable Account, Disable Account
- **Target Users**: Only `APPROVED` users can be enabled/disabled

### **4. Login Prevention for Disabled Users**
- **Current**: Only `PENDING` and `REJECTED` users are blocked from login
- **New**: `DISABLED` users are also blocked from login
- **Error Message**: Clear indication that account is disabled by admin

---

## 🏗️ **Implementation Plan**

### **Phase 1: Backend Changes**

#### **1.1 Database Schema Updates**
```sql
-- Add new column to sales table
ALTER TABLE sales ADD COLUMN is_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE sales ADD COLUMN disabled_at TIMESTAMP NULL;
ALTER TABLE sales ADD COLUMN disabled_by_phone VARCHAR(20) NULL;
ALTER TABLE sales ADD COLUMN disabled_reason TEXT NULL;
```

#### **1.2 Model Updates**
- **File**: `backend/src/main/java/com/example/customers/model/Sales.java`
- **Changes**:
  - Add `isEnabled` boolean field
  - Add `disabledAt`, `disabledByPhone`, `disabledReason` fields
  - Add helper methods: `isDisabled()`, `enable()`, `disable()`

#### **1.3 Service Layer Updates**
- **File**: `backend/src/main/java/com/example/customers/service/UserApprovalService.java`
- **New Methods**:
  - `enableUser(String userPhone, String adminPhone, String reason)`
  - `disableUser(String userPhone, String adminPhone, String reason)`
  - `bulkEnable(List<String> userPhones, String adminPhone, String reason)`
  - `bulkDisable(List<String> userPhones, String adminPhone, String reason)`

#### **1.4 Authentication Service Updates**
- **File**: `backend/src/main/java/com/example/customers/service/AuthService.java`
- **Changes**:
  - Update `login()` method to check `isEnabled` status
  - Return appropriate error message for disabled accounts

#### **1.5 Controller Updates**
- **File**: `backend/src/main/java/com/example/customers/controller/UserApprovalController.java`
- **New Endpoints**:
  - `POST /api/admin/users/{phone}/enable`
  - `POST /api/admin/users/{phone}/disable`
  - `POST /api/admin/users/bulk-enable`
  - `POST /api/admin/users/bulk-disable`

#### **1.6 History Tracking**
- **File**: `backend/src/main/java/com/example/customers/model/UserApprovalHistory.java`
- **Changes**:
  - Add new `ApprovalAction` enum values: `ENABLED`, `DISABLED`
  - Update history tracking for enable/disable actions

### **Phase 2: Frontend Changes**

#### **2.1 Page Renaming & Routing**
- **Current**: `frontend/src/app/dashboard/admin/user-approvals/page.tsx`
- **New**: `frontend/src/app/dashboard/admin/user-management/page.tsx`
- **Update**: Navigation menu and routing configuration

#### **2.2 UI Component Updates**
- **File**: `frontend/src/components/ui/ApprovalModal.tsx`
- **Changes**:
  - Rename to `UserManagementModal.tsx`
  - Add support for `enable` and `disable` action types
  - Update modal configurations and styling

#### **2.3 Type Definitions**
- **File**: `frontend/src/types/auth.ts`
- **Changes**:
  - Add `isEnabled` field to `UserApprovalDto`
  - Add `disabledAt`, `disabledByPhone`, `disabledReason` fields
  - Update API response types

#### **2.4 API Integration**
- **File**: `frontend/src/lib/api.ts`
- **New Methods**:
  - `enableUser(userPhone: string, reason?: string)`
  - `disableUser(userPhone: string, reason?: string)`
  - `bulkEnable(userPhones: string[], reason?: string)`
  - `bulkDisable(userPhones: string[], reason?: string)`

#### **2.5 UI Enhancements**
- **Status Display**: Add visual indicators for enabled/disabled status
- **Action Buttons**: Add Enable/Disable buttons for approved users
- **Bulk Operations**: Support bulk enable/disable actions
- **Status Filtering**: Add filter for enabled/disabled users

### **Phase 3: Testing & Validation**

#### **3.1 Backend Tests**
- **File**: `backend/src/test/java/com/example/customers/service/UserApprovalServiceTest.java`
- **New Tests**:
  - `shouldEnableUserSuccessfully()`
  - `shouldDisableUserSuccessfully()`
  - `shouldPreventLoginForDisabledUser()`
  - `shouldHandleBulkEnableDisableOperations()`

#### **3.2 Frontend Tests**
- **Component Tests**: Test new enable/disable functionality
- **Integration Tests**: Test complete user management workflow
- **E2E Tests**: Test admin user management scenarios

### **Phase 4: Internationalization**

#### **4.1 Translation Updates**
- **File**: `frontend/src/contexts/LanguageContext.tsx`
- **New Keys**:
  - `userManagement.title`
  - `userManagement.enableUser`
  - `userManagement.disableUser`
  - `userManagement.accountDisabled`
  - `userManagement.enabledStatus`
  - `userManagement.disabledStatus`

---

## 🔄 **User Workflow**

### **Admin User Management Flow**
1. **Access**: Admin navigates to "User Management" page
2. **View**: See all users with their approval and enabled status
3. **Filter**: Filter by approval status (Pending, Approved, Rejected) and enabled status
4. **Actions**:
   - **Pending Users**: Approve/Reject/Reset
   - **Approved Users**: Enable/Disable/Reset
   - **Rejected Users**: Reset to Pending
5. **Bulk Operations**: Select multiple users for bulk actions
6. **History**: View complete action history for each user

### **Disabled User Login Flow**
1. **Attempt**: Disabled user tries to login
2. **Validation**: System checks `isEnabled` status
3. **Response**: Returns error message: "Your account has been disabled by an administrator"
4. **Action**: User must contact admin to re-enable account

---

## 📊 **Database Migration Strategy**

### **Migration Script**
```sql
-- Migration: Add user enable/disable functionality
-- File: backend/src/main/resources/db/migration/V20240915_001__AddUserEnableDisable.sql

-- Add new columns
ALTER TABLE sales ADD COLUMN is_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE sales ADD COLUMN disabled_at TIMESTAMP NULL;
ALTER TABLE sales ADD COLUMN disabled_by_phone VARCHAR(20) NULL;
ALTER TABLE sales ADD COLUMN disabled_reason TEXT NULL;

-- Update existing approved users to be enabled
UPDATE sales SET is_enabled = TRUE WHERE approval_status = 'APPROVED';

-- Update pending and rejected users to be disabled
UPDATE sales SET is_enabled = FALSE WHERE approval_status IN ('PENDING', 'REJECTED');

-- Add indexes for performance
CREATE INDEX idx_sales_enabled ON sales(is_enabled);
CREATE INDEX idx_sales_disabled_at ON sales(disabled_at);
```

---

## 🎨 **UI/UX Considerations**

### **Visual Design**
- **Status Badges**: 
  - Green: Approved & Enabled
  - Yellow: Approved & Disabled
  - Red: Rejected
  - Gray: Pending
- **Action Buttons**: Clear enable/disable buttons with appropriate icons
- **Confirmation Modals**: Require reason for disable actions
- **Bulk Selection**: Checkbox selection for multiple users

### **User Experience**
- **Clear Messaging**: Distinguish between approval status and enabled status
- **Confirmation Dialogs**: Prevent accidental disable actions
- **Audit Trail**: Show who disabled/enabled accounts and when
- **Responsive Design**: Ensure mobile-friendly interface

---

## 🚀 **Implementation Timeline**

### **Week 1: Backend Foundation**
- Database migration and model updates
- Service layer implementation
- Controller endpoints
- Basic testing

### **Week 2: Frontend Integration**
- Page renaming and routing
- UI component updates
- API integration
- Basic functionality testing

### **Week 3: Enhancement & Polish**
- Bulk operations
- Advanced filtering
- UI/UX improvements
- Comprehensive testing

### **Week 4: Testing & Deployment**
- End-to-end testing
- Performance optimization
- Documentation updates
- Production deployment

---

## 🔒 **Security Considerations**

### **Authorization**
- Only `ADMIN` role can enable/disable users
- Audit logging for all enable/disable actions
- Reason required for disable actions

### **Data Protection**
- Soft delete for disabled users (preserve data)
- Clear audit trail for compliance
- Secure API endpoints with proper validation

---

This comprehensive plan provides a clear roadmap for implementing the user account management feature while maintaining system integrity and providing a smooth user experience. The phased approach ensures minimal disruption to existing functionality while adding the requested capabilities.