# Customer Call-Through and Status Tracking System

A comprehensive **Customer Relationship Management (CRM) system** designed specifically for sales teams to manage customer interactions, track status transitions, and maintain complete audit trails. This production-ready MVP features intelligent status management, role-based access control, and a professional Material Design interface.

![Technology Stack](https://img.shields.io/badge/Java-17-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-green) ![Next.js](https://img.shields.io/badge/Next.js-14-black) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue) ![TypeScript](https://img.shields.io/badge/TypeScript-5.4-blue)

## 🌟 Key Features

### ✅ **Customer Management**
- Complete CRUD operations with validation
- Advanced search and pagination (name, phone, company, business requirements)
- Soft delete with restore functionality
- Phone number uniqueness enforcement (including deleted records)

### ✅ **Intelligent Status Tracking**
- **7-Status Workflow**: Customer Called → Replied → Order Placed → [Order Cancelled] → Product Delivered → Business Done / Lost
- **Smart Business Rules**: Prevents invalid status transitions
- **Complete Audit Trail**: Every status change tracked with timestamps, reasons, and user information
- **Dynamic UI**: Frontend shows only valid transition options in real-time

### ✅ **Authentication & Authorization**
- JWT-based authentication with role-based access control
- **Admin Role**: Full access to all customers and system features
- **Sales Role**: Access limited to customers they created
- Secure password hashing with BCrypt
- Token validation and refresh capabilities

### ✅ **Professional Material Design Interface**
- Responsive design optimized for mobile, tablet, and desktop
- Loading states, skeleton screens, and error handling
- Clean, intuitive user experience with semantic status badges
- Real-time status transition validation

### ✅ **API Documentation**
- Complete **Swagger/OpenAPI 3** documentation
- Interactive API testing interface
- Comprehensive request/response schemas
- Available at: `http://localhost:8080/swagger-ui/index.html`

## 🏗️ Architecture

**Backend**: Spring Boot 3.3.2 (Java 17)
- **Database**: PostgreSQL with Flyway migrations
- **Security**: Spring Security + JWT authentication
- **Documentation**: OpenAPI 3 + Swagger UI
- **Testing**: JUnit 5 with comprehensive test suite (72+ tests)
- **Code Quality**: Google Java Style + Spotless + Checkstyle

**Frontend**: Next.js 14 (TypeScript)
- **Styling**: Tailwind CSS with Material Design system
- **Icons**: Lucide React for consistent iconography
- **Date Handling**: date-fns for formatting and manipulation
- **Build**: Next.js optimized builds with automatic code splitting

**Database**: PostgreSQL 15
- **Migrations**: Flyway versioned schema management
- **Indexing**: Optimized for search and pagination performance
- **Constraints**: Proper foreign key relationships and data integrity

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- PostgreSQL 15+
- Maven 3.9+

### 1. Clone the Repository
```bash
git clone <repository-url>
cd customer-tracker
```

### 2. Database Setup
```bash
# Start PostgreSQL (using Docker/Podman)
./start-database.sh
```

### 3. Start All Services
```bash
# Start backend, frontend, and database
./start-all.sh
```

### 4. Access the Application
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui/index.html
- **Database**: localhost:5432 (database: `customers`)

## 📋 API Endpoints

### Customer Management
- `GET /api/customers` - Search & list customers with pagination
- `GET /api/customers/{id}` - Get customer by ID
- `POST /api/customers` - Create new customer
- `PATCH /api/customers/{id}` - Update customer information
- `DELETE /api/customers/{id}` - Soft delete customer
- `POST /api/customers/{id}/restore` - Restore soft-deleted customer

### Status Management
- `POST /api/customers/{id}/status-transition` - Change customer status
- `GET /api/customers/{id}/status-history` - Get status change history
- `GET /api/customers/{id}/valid-transitions` - Get valid status transitions
- `GET /api/customers/{id}/can-transition-to/{status}` - Validate specific transition

### Authentication
- `POST /api/auth/login` - Login with phone and password
- `POST /api/auth/register` - Register new sales user
- `POST /api/auth/validate` - Validate JWT token

### System
- `GET /api/health` - Comprehensive system health check
- `GET /api/customers/statistics` - Customer statistics
- `GET /api/customers/recent` - Recently updated customers

## 🧪 Testing

**Backend Testing**: 72+ comprehensive tests
```bash
cd backend
mvn test
```

**Frontend Testing**: (Setup ready for Jest/React Testing Library)
```bash
cd frontend
npm test
```

## 🔧 Development Scripts

### Individual Services
```bash
# Start database only
./start-database.sh

# Start backend only
./start-backend.sh

# Start frontend only
./start-frontend.sh

# Stop all services
./stop-all.sh
```

### Code Quality
```bash
# Backend code formatting
cd backend && mvn spotless:apply

# Backend style checking
cd backend && mvn checkstyle:check

# Frontend linting
cd frontend && npm run lint
```

## 📊 Database Schema

### Core Tables
- **customers**: Customer information with soft delete support
- **sales**: Sales user authentication and role management
- **status_history**: Complete audit trail of status changes

### Key Features
- **UUID Primary Keys**: Better performance and security
- **Soft Delete Pattern**: Data preservation with `deleted_at` timestamps
- **Audit Timestamps**: Automatic `created_at` and `updated_at` tracking
- **Optimized Indexes**: Performance-tuned for common query patterns

## 🌍 Production Deployment

The application is production-ready with:
- ✅ **Security**: JWT authentication, input validation, SQL injection prevention
- ✅ **Performance**: Database indexing, query optimization, connection pooling
- ✅ **Monitoring**: Health checks, structured logging, error handling
- ✅ **Scalability**: Stateless design, database connection pooling, CDN-ready assets

### Environment Variables
```bash
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/customers
DB_USERNAME=postgres
DB_PASSWORD=postgres

# JWT Configuration
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000

# Application Configuration
SERVER_PORT=8080
FRONTEND_PORT=3000
```

## 🛣️ Roadmap

### Phase 2: Enhanced Features (Next 2-4 weeks)
- [ ] CSV Import/Export UI completion
- [ ] Advanced filtering and search capabilities
- [ ] Dashboard analytics with visual metrics
- [ ] Bulk operations for multiple customers

### Phase 3: Enterprise Features (2-3 months)
- [ ] Multi-tenant support with organization-based isolation
- [ ] Email/SMS notification system
- [ ] CRM system integration APIs
- [ ] Advanced reporting and custom dashboards

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙋‍♂️ Support

For support, please open an issue in this repository or contact the development team.

---

**Built with ❤️ for sales teams who need better customer relationship management**