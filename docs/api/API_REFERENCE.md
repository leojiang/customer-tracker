# API Reference

> **Comprehensive REST API documentation for the Customer Tracker CRM system**

## 🌐 **Base Information**

- **Base URL**: `http://localhost:8080/api`
- **Production URL**: `https://your-domain.com/api`
- **Documentation**: `http://localhost:8080/swagger-ui.html`
- **API Version**: v1
- **Content-Type**: `application/json`
- **Authentication**: Bearer JWT Token

## 🔐 **Authentication**

### **🔑 Login**
```http
POST /auth/login
Content-Type: application/json

{
  "phone": "18980994001",
  "password": "123456"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "phone": "18980994001", 
  "role": "ADMIN",
  "error": null
}
```

### **📝 Register**
```http
POST /auth/register
Content-Type: application/json

{
  "phone": "13912345678",
  "password": "securepassword"
}
```

**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "phone": "13912345678",
  "role": "SALES", 
  "error": null
}
```

### **✅ Token Validation**
```http
POST /auth/validate
Content-Type: application/json

{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Headers for Authenticated Requests:**
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json
```

---

## 👥 **Customer Management API**

### **📋 List Customers**
```http
GET /customers?q=john&status=CUSTOMER_CALLED&page=1&limit=10
Authorization: Bearer {token}
```

**Query Parameters:**
- `q` (string): Unified search across name, phone, company, business requirements
- `phone` (string): Specific phone number search (partial match)
- `status` (CustomerStatus): Filter by current status
- `company` (string): Filter by company name  
- `salesPhone` (string): Filter by sales person (Admin only)
- `includeDeleted` (boolean): Include soft-deleted customers (default: false)
- `page` (integer): Page number, 1-based (default: 1)
- `limit` (integer): Items per page (default: 10, max: 100)

**Response (200 OK):**
```json
{
  "items": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "John Smith",
      "phone": "+1234567890", 
      "company": "Tech Corp",
      "businessRequirements": "Need CRM solution",
      "businessType": "Technology",
      "age": 35,
      "education": "Bachelor's Degree",
      "gender": "Male",
      "location": "New York",
      "currentStatus": "CUSTOMER_CALLED",
      "salesPhone": "18980994001",
      "createdAt": "2025-09-05T10:30:00Z",
      "updatedAt": "2025-09-05T10:30:00Z",
      "deleted": false
    }
  ],
  "total": 1,
  "page": 1, 
  "limit": 10,
  "totalPages": 1
}
```

### **👤 Get Customer by ID**
```http
GET /customers/{id}
Authorization: Bearer {token}
```

**Response (200 OK):** Returns single customer object (same structure as list items)

**Error (404 Not Found):**
```json
{
  "error": "Customer not found",
  "message": "Customer with id 123e4567-e89b-12d3-a456-426614174000 not found",
  "timestamp": "2025-09-05T10:30:00Z"
}
```

### **➕ Create Customer**
```http
POST /customers
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Jane Doe",
  "phone": "+1987654321",
  "company": "Business Solutions Inc",
  "businessRequirements": "Looking for inventory management",
  "businessType": "Retail",
  "age": 35,
  "education": "MBA", 
  "gender": "Female",
  "location": "California"
}
```

**Response (201 Created):** Returns created customer with generated ID and timestamps

### **✏️ Update Customer**
```http
PUT /customers/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Jane Smith",
  "phone": "+1987654321",
  "company": "Updated Business Inc",
  "businessRequirements": "Expanded requirements",
  "businessType": "Technology",
  "age": 36,
  "education": "MBA",
  "gender": "Female", 
  "location": "San Francisco"
}
```

**Response (200 OK):** Returns updated customer object

### **🗑️ Soft Delete Customer**
```http
DELETE /customers/{id}
Authorization: Bearer {token}
```

**Response (204 No Content)**

### **🔄 Restore Customer**
```http
POST /customers/{id}/restore
Authorization: Bearer {token}
```

**Response (200 OK):** Returns restored customer object

---

## 🔄 **Status Management API**

### **📊 Transition Customer Status**
```http
POST /customers/{id}/status-transition
Authorization: Bearer {token}
Content-Type: application/json

{
  "toStatus": "REPLIED_TO_CUSTOMER",
  "reason": "Customer called back and confirmed interest"
}
```

**Response (200 OK):** Returns updated customer with new status

### **🎯 Get Valid Transitions**
```http
GET /customers/{id}/valid-transitions
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
["REPLIED_TO_CUSTOMER", "LOST"]
```

### **✅ Validate Specific Transition**
```http
GET /customers/{id}/can-transition-to/BUSINESS_DONE
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "valid": false,
  "reason": "Cannot transition from CUSTOMER_CALLED to BUSINESS_DONE. Valid transitions are: REPLIED_TO_CUSTOMER, LOST"
}
```

### **📋 Get Status History**
```http
GET /customers/{id}/status-history?page=1&limit=20
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "items": [
    {
      "id": "987fcdeb-51a2-43d7-af94-123456789012",
      "customerId": "123e4567-e89b-12d3-a456-426614174000",
      "fromStatus": "CUSTOMER_CALLED",
      "toStatus": "REPLIED_TO_CUSTOMER", 
      "reason": "Customer expressed interest",
      "changedAt": "2025-09-05T14:30:00Z"
    }
  ],
  "total": 5,
  "page": 1,
  "limit": 20,
  "totalPages": 1
}
```

---

## 📊 **Analytics API**

### **📈 Dashboard Overview**
```http
GET /analytics/dashboard/overview?days=30
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "totalCustomers": 1247,
  "newCustomersThisPeriod": 89,
  "activeCustomers": 892,
  "conversionRate": 23.40,
  "periodChange": {
    "totalCustomersChange": 12.5,
    "newCustomersChange": 15.2,
    "conversionRateChange": 2.1
  }
}
```

### **📊 Status Distribution**
```http
GET /analytics/customers/status-distribution
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "statusCounts": {
    "CUSTOMER_CALLED": 45,
    "REPLIED_TO_CUSTOMER": 32,
    "ORDER_PLACED": 28,
    "PRODUCT_DELIVERED": 15,
    "BUSINESS_DONE": 12,
    "ORDER_CANCELLED": 8,
    "LOST": 6
  },
  "totalCustomers": 146
}
```

### **📈 Customer Trends**
```http
GET /analytics/customers/trends?days=90&granularity=daily
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "dataPoints": [
    {
      "date": "2025-09-01",
      "newCustomers": 5,
      "totalCustomers": 1200,
      "conversionRate": 22.8
    },
    {
      "date": "2025-09-02", 
      "newCustomers": 8,
      "totalCustomers": 1208,
      "conversionRate": 23.1
    }
  ],
  "granularity": "daily",
  "totalDays": 90
}
```

### **🏆 Sales Leaderboard (Admin Only)**
```http
GET /analytics/sales/leaderboard?days=30&metric=conversions
Authorization: Bearer {admin_token}
```

**Response (200 OK):**
```json
{
  "rankings": [
    {
      "salesPhone": "18980994001",
      "totalCustomers": 45,
      "conversions": 12,
      "conversionRate": 26.67,
      "rank": 1
    },
    {
      "salesPhone": "13912345678", 
      "totalCustomers": 38,
      "conversions": 8,
      "conversionRate": 21.05,
      "rank": 2
    }
  ],
  "totalDays": 30,
  "metric": "conversions"
}
```

### **📊 Sales Performance**
```http
GET /analytics/sales/performance?days=30
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "salesPhone": "13912345678",
  "totalCustomers": 38,
  "newCustomers": 12,
  "conversions": 8,
  "conversionRate": 21.05,
  "statusBreakdown": {
    "CUSTOMER_CALLED": 15,
    "REPLIED_TO_CUSTOMER": 10,
    "ORDER_PLACED": 8,
    "PRODUCT_DELIVERED": 3,
    "BUSINESS_DONE": 8,
    "LOST": 4
  }
}
```

### **⚡ Real-Time Metrics**
```http
GET /analytics/realtime/metrics
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "activeCustomersToday": 23,
  "newCustomersToday": 7,
  "conversionsToday": 2,
  "lastUpdated": "14:30:25"
}
```

---

## 📈 **Statistics API**

### **📊 Customer Statistics**
```http
GET /customers/statistics?includeDeleted=false
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "totalCustomers": 1247,
  "recentlyUpdatedCount": 156,
  "statusCounts": {
    "CUSTOMER_CALLED": 245,
    "REPLIED_TO_CUSTOMER": 198,
    "ORDER_PLACED": 176,
    "PRODUCT_DELIVERED": 134,
    "BUSINESS_DONE": 298,
    "ORDER_CANCELLED": 98,
    "LOST": 98
  }
}
```

### **📅 Recently Updated Customers**
```http
GET /customers/recent?days=7&page=1&limit=20
Authorization: Bearer {token}
```

**Response (200 OK):** Returns paginated list of recently updated customers

---

## ❌ **Error Responses**

### **🔐 Authentication Errors**
```json
// 401 Unauthorized
{
  "error": "Unauthorized", 
  "message": "Invalid or missing authentication token",
  "timestamp": "2025-09-05T10:30:00Z",
  "path": "/api/customers"
}
```

### **🚫 Authorization Errors**
```json
// 403 Forbidden
{
  "error": "Forbidden",
  "message": "Access denied. Insufficient privileges for this operation", 
  "timestamp": "2025-09-05T10:30:00Z",
  "path": "/api/analytics/sales/leaderboard"
}
```

### **📝 Validation Errors**
```json
// 400 Bad Request
{
  "error": "Validation Failed",
  "message": "Invalid input data",
  "details": {
    "phone": "Phone number already exists",
    "name": "Name cannot be empty"
  },
  "timestamp": "2025-09-05T10:30:00Z",
  "path": "/api/customers"
}
```

### **🔍 Not Found Errors**
```json
// 404 Not Found  
{
  "error": "Customer not found",
  "message": "Customer with id 123e4567-e89b-12d3-a456-426614174000 not found",
  "timestamp": "2025-09-05T10:30:00Z",
  "path": "/api/customers/123e4567-e89b-12d3-a456-426614174000"
}
```

### **⚖️ Business Rule Violations**
```json
// 422 Unprocessable Entity
{
  "error": "Business Rule Violation",
  "message": "Cannot transition from CUSTOMER_CALLED to BUSINESS_DONE. Valid transitions are: REPLIED_TO_CUSTOMER, LOST",
  "timestamp": "2025-09-05T10:30:00Z",
  "path": "/api/customers/123e4567-e89b-12d3-a456-426614174000/status-transition"
}
```

---

## 📊 **Status Codes**

| Code | Meaning | When Used |
|------|---------|-----------|
| **200** | OK | Successful GET, PUT operations |
| **201** | Created | Successful POST operations |  
| **204** | No Content | Successful DELETE operations |
| **400** | Bad Request | Invalid request format or parameters |
| **401** | Unauthorized | Missing or invalid authentication |
| **403** | Forbidden | Valid auth but insufficient permissions |
| **404** | Not Found | Resource doesn't exist |
| **422** | Unprocessable Entity | Business rule violations |
| **500** | Internal Server Error | Server-side errors |

---

## 🔗 **API Client Examples**

### **JavaScript/TypeScript**
```typescript
class CustomerTrackerAPI {
  constructor(private baseURL: string, private token: string) {}
  
  async getCustomers(params: CustomerSearchParams): Promise<CustomerPageResponse> {
    const url = new URL('/customers', this.baseURL);
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined) {
        url.searchParams.set(key, String(value));
      }
    });
    
    const response = await fetch(url.toString(), {
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${await response.text()}`);
    }
    
    return response.json();
  }
  
  async createCustomer(customer: CreateCustomerRequest): Promise<Customer> {
    const response = await fetch(`${this.baseURL}/customers`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(customer)
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || `HTTP ${response.status}`);
    }
    
    return response.json();
  }
}

// Usage
const api = new CustomerTrackerAPI('http://localhost:8080/api', token);
const customers = await api.getCustomers({ q: 'john', status: 'CUSTOMER_CALLED' });
```

### **Python**
```python
import requests
from typing import Optional, Dict, Any

class CustomerTrackerAPI:
    def __init__(self, base_url: str, token: str):
        self.base_url = base_url
        self.headers = {
            'Authorization': f'Bearer {token}',
            'Content-Type': 'application/json'
        }
    
    def get_customers(self, **params) -> Dict[str, Any]:
        """Get customers with optional search parameters"""
        response = requests.get(
            f'{self.base_url}/customers',
            headers=self.headers,
            params=params
        )
        response.raise_for_status()
        return response.json()
    
    def create_customer(self, customer_data: Dict[str, Any]) -> Dict[str, Any]:
        """Create a new customer"""
        response = requests.post(
            f'{self.base_url}/customers',
            headers=self.headers,
            json=customer_data
        )
        response.raise_for_status()
        return response.json()

# Usage
api = CustomerTrackerAPI('http://localhost:8080/api', token)
customers = api.get_customers(q='john', status='CUSTOMER_CALLED')
```

### **cURL Examples**
```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone": "18980994001", "password": "123456"}'

# Get customers
curl -H "Authorization: Bearer YOUR_TOKEN" \
  "http://localhost:8080/api/customers?q=john&page=1&limit=10"

# Create customer
curl -X POST http://localhost:8080/api/customers \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "phone": "+1234567890",
    "company": "Example Corp"
  }'

# Update customer status
curl -X POST http://localhost:8080/api/customers/CUSTOMER_ID/status-transition \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "toStatus": "REPLIED_TO_CUSTOMER",
    "reason": "Customer responded positively"
  }'
```

---

## 🔍 **Data Types & Enums**

### **CustomerStatus Enum**
```typescript
enum CustomerStatus {
  CUSTOMER_CALLED = 'CUSTOMER_CALLED',
  REPLIED_TO_CUSTOMER = 'REPLIED_TO_CUSTOMER',
  ORDER_PLACED = 'ORDER_PLACED',
  ORDER_CANCELLED = 'ORDER_CANCELLED', 
  PRODUCT_DELIVERED = 'PRODUCT_DELIVERED',
  BUSINESS_DONE = 'BUSINESS_DONE',
  LOST = 'LOST'
}
```

### **SalesRole Enum**
```typescript
enum SalesRole {
  ADMIN = 'ADMIN',
  SALES = 'SALES'
}
```

### **Customer Interface**
```typescript
interface Customer {
  id: string;                    // UUID
  name: string;                  // Required
  phone: string;                 // Required, globally unique
  company?: string;              // Optional
  businessRequirements?: string; // Optional
  businessType?: string;         // Optional
  age?: number;                  // Optional
  education?: string;            // Optional
  gender?: string;               // Optional
  location?: string;             // Optional
  currentStatus: CustomerStatus; // Required
  salesPhone: string;            // Sales person assignment
  createdAt: string;             // ISO 8601 datetime
  updatedAt: string;             // ISO 8601 datetime
  deleted: boolean;              // Soft delete status
}
```

---

## 🚀 **Rate Limits & Performance**

### **Rate Limiting**
| Endpoint Category | Limit | Window |
|------------------|-------|---------|
| Authentication | 5 requests | 1 minute |
| Customer Operations | 100 requests | 1 minute |
| Analytics | 50 requests | 1 minute |
| Search | 200 requests | 1 minute |

### **Response Times (SLA)**
- **Customer CRUD**: < 200ms
- **Search Operations**: < 500ms
- **Analytics Queries**: < 1000ms
- **Dashboard Load**: < 2000ms

### **Pagination Limits**
- **Default Page Size**: 10 items
- **Maximum Page Size**: 100 items
- **Maximum Total Results**: 10,000 items

---

## 🧪 **Testing the API**

### **Postman Collection**
```json
{
  "info": {
    "name": "Customer Tracker API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "auth": {
    "type": "bearer",
    "bearer": [
      {
        "key": "token", 
        "value": "{{jwt_token}}",
        "type": "string"
      }
    ]
  }
}
```

### **Integration Testing**
```bash
# Health check
curl http://localhost:8080/actuator/health

# API availability  
curl http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone": "test", "password": "test"}' \
  -w "%{http_code}\n"
```

---

## 📚 **Additional Resources**

### **🔗 Interactive Documentation**
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

### **🐛 Common Issues**
- **CORS Errors**: Ensure frontend URL is in CORS configuration
- **Token Expiry**: Implement token refresh in client applications  
- **Rate Limits**: Implement exponential backoff for retries
- **Large Datasets**: Use pagination for performance

### **🎯 Best Practices**
- **Always use HTTPS** in production
- **Implement retry logic** with exponential backoff
- **Cache responses** when appropriate
- **Handle errors gracefully** with user-friendly messages
- **Use proper HTTP methods** (GET for reads, POST for creates, etc.)

---

**For live API testing, use the interactive Swagger UI at http://localhost:8080/swagger-ui.html** 🚀