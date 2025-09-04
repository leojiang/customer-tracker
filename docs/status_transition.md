# Status Transition Logic - Implementation Documentation

## Overview
This document describes the status transition business rules implemented throughout the system.

## Status Transition Rules ✅ IMPLEMENTED

### From CUSTOMER_CALLED
- ✅ Can transition to: `REPLIED_TO_CUSTOMER`, `LOST`
- ❌ Cannot transition back to: `CUSTOMER_CALLED` (no self-transitions)

### From REPLIED_TO_CUSTOMER  
- ✅ Can transition to: `ORDER_PLACED`, `LOST`
- ❌ Cannot transition back to: `CUSTOMER_CALLED`, `REPLIED_TO_CUSTOMER`

### From ORDER_PLACED
- ✅ Can transition to: `PRODUCT_DELIVERED`, `ORDER_CANCELLED`
- ❌ Cannot transition back to: `CUSTOMER_CALLED`, `REPLIED_TO_CUSTOMER`, `ORDER_PLACED`

### From ORDER_CANCELLED
- ✅ Can transition to: `ORDER_PLACED` (re-order), `LOST`
- ❌ Cannot transition to: earlier stages

### From PRODUCT_DELIVERED
- ✅ Can transition to: `BUSINESS_DONE` only
- ❌ Cannot transition to: any other status

### From BUSINESS_DONE
- ❌ **Terminal State**: Cannot transition to any other status (business process complete)

### From LOST
- ✅ Can transition to: `CUSTOMER_CALLED` (restart the sales process)
- ❌ Cannot transition to: intermediate states directly

## Implementation Details

### Backend Validation
- **Class**: `StatusTransitionValidator.java`
- **Location**: `backend/src/main/java/com/example/customers/service/`
- **Method**: `isValidTransition(CustomerStatus fromStatus, CustomerStatus toStatus)`

### API Endpoints
- `GET /api/customers/{id}/valid-transitions` - Get all valid transitions for customer
- `GET /api/customers/{id}/can-transition-to/{status}` - Validate specific transition
- `POST /api/customers/{id}/status-transition` - Execute transition (with validation)

### Frontend Implementation
- **Dynamic UI**: Status transition dropdown only shows valid options
- **Real-time validation**: Invalid transitions are prevented at the UI level
- **Error handling**: Clear messages for invalid transition attempts

### Business Logic
- **No backward transitions**: Once a customer progresses, they cannot go back to earlier stages
- **Exception**: ORDER_CANCELLED can go back to ORDER_PLACED (re-order scenario)
- **Restart capability**: LOST customers can restart from CUSTOMER_CALLED
- **Terminal state**: BUSINESS_DONE is final, no further transitions allowed

