# Development Guide

> **Complete guide for setting up, developing, and maintaining the Customer Tracker CRM system**

## 🏗️ **Project Architecture**

### **Technology Stack**

#### **Backend**
- **Framework**: Spring Boot 3.3.2 with Java 17
- **Database**: PostgreSQL with Flyway migrations  
- **ORM**: JPA/Hibernate with custom query methods
- **Security**: JWT-based authentication with role-based access
- **Documentation**: OpenAPI 3.0 with Swagger UI
- **Code Quality**: Google Java Style with Spotless + Checkstyle

#### **Frontend**  
- **Framework**: Next.js 14 with TypeScript 5.4.5
- **UI System**: Tailwind CSS with Material Design 3
- **Charts**: Chart.js with react-chartjs-2
- **Icons**: Lucide React for consistent iconography
- **Code Quality**: ESLint + TypeScript strict mode

#### **Infrastructure**
- **Database**: PostgreSQL 15+ via Docker/Podman
- **Process Management**: Shell scripts for coordinated startup
- **Environment**: Development-optimized with hot reload

## 📁 **Project Structure**

```
customer-tracker/
├── 📱 frontend/                    # Next.js application
│   ├── src/
│   │   ├── app/                   # App Router pages
│   │   │   ├── auth/              # Authentication pages
│   │   │   ├── dashboard/         # Analytics dashboards
│   │   │   │   ├── admin/         # Admin dashboard
│   │   │   │   └── sales/         # Sales dashboard  
│   │   │   └── page.tsx           # Main customer management
│   │   ├── components/            # Reusable components
│   │   │   ├── auth/              # Authentication components
│   │   │   ├── customers/         # Customer management
│   │   │   ├── dashboard/         # Dashboard components
│   │   │   │   ├── charts/        # Chart components
│   │   │   │   ├── widgets/       # Dashboard widgets
│   │   │   │   └── layout/        # Dashboard layout
│   │   │   └── ui/                # Base UI components
│   │   ├── contexts/              # React contexts (Auth)
│   │   ├── lib/                   # Utilities and API client
│   │   └── types/                 # TypeScript definitions
│   ├── public/                    # Static assets
│   └── package.json              # Dependencies
├── 🔧 backend/                     # Spring Boot application
│   ├── src/main/java/com/example/customers/
│   │   ├── controller/            # REST API controllers
│   │   │   ├── CustomerController.java
│   │   │   ├── AuthController.java
│   │   │   └── AnalyticsController.java
│   │   ├── service/               # Business logic
│   │   │   ├── CustomerService.java
│   │   │   ├── AuthService.java
│   │   │   ├── AnalyticsService.java
│   │   │   └── StatusTransitionValidator.java
│   │   ├── repository/            # Data access layer
│   │   │   ├── CustomerRepository.java
│   │   │   ├── SalesRepository.java
│   │   │   ├── StatusHistoryRepository.java
│   │   │   └── CustomerSpecifications.java
│   │   ├── model/                 # JPA entities
│   │   │   ├── Customer.java
│   │   │   ├── Sales.java
│   │   │   ├── StatusHistory.java
│   │   │   └── enums/
│   │   ├── config/                # Spring configuration
│   │   └── security/              # Security configuration
│   ├── src/main/resources/
│   │   ├── db/migration/          # Flyway migration scripts
│   │   └── application.properties # Configuration
│   └── pom.xml                    # Maven dependencies
├── 📚 docs/                        # Documentation
├── 🚀 *.sh                        # Startup scripts
└── README.md                      # Project overview
```

## ⚡ **Quick Start**

### **1. Prerequisites Installation**

#### **macOS (Homebrew)**
```bash
# Install Java 17  
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc

# Install Node.js
brew install node@18

# Install PostgreSQL tools
brew install postgresql@15

# Install Docker/Podman
brew install podman
```

#### **Ubuntu/Debian**
```bash
# Install Java 17
sudo apt update
sudo apt install openjdk-17-jdk

# Install Node.js
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# Install PostgreSQL
sudo apt install postgresql-15

# Install Docker
sudo apt install docker.io
```

### **2. Clone and Setup**
```bash
git clone git@github.com:leojiang/customer-tracker.git
cd customer-tracker

# Make scripts executable
chmod +x scripts/*.sh

# Install frontend dependencies
cd frontend && npm install && cd ..
```

### **3. Environment Configuration**

#### **Frontend (.env.local)**
```bash
cat > frontend/.env.local << EOF
NEXT_PUBLIC_API_URL=http://localhost:8080/api
EOF
```

#### **Backend (application.properties)**
The backend is pre-configured for local development with PostgreSQL.

### **4. Start Application**
```bash
# Start all services (database + backend + frontend)
./scripts/start-all.sh

# Or start individually
./scripts/start-database.sh   # PostgreSQL container
./scripts/start-backend.sh    # Spring Boot API  
./scripts/start-frontend.sh   # Next.js UI

# Stop all services
./scripts/stop-all.sh
```

### **5. Verify Installation**
- **Frontend**: http://localhost:3000 (Customer Tracker UI)
- **Backend**: http://localhost:8080/actuator/health (Health check)
- **API Docs**: http://localhost:8080/swagger-ui.html (Interactive API)
- **Database**: localhost:5432 (customers database)

## 🔧 **Development Workflow**

### **Backend Development**

#### **Hot Reload Development**
```bash
cd backend
mvn spring-boot:run

# The application will restart automatically when code changes
```

#### **Database Management**
```bash
# Run migrations manually
mvn flyway:migrate

# Reset database (careful!)
mvn flyway:clean && mvn flyway:migrate

# Check migration status
mvn flyway:info
```

#### **Testing**
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CustomerServiceTest

# Generate test coverage report
mvn jacoco:report
```

#### **Code Quality**
```bash
# Format code (Google Java Style)
mvn spotless:apply

# Check code style
mvn spotless:check

# Run checkstyle
mvn checkstyle:check

# Full quality check
mvn clean verify
```

### **Frontend Development**

#### **Development Server**
```bash
cd frontend
npm run dev

# Application will hot-reload on code changes
```

#### **Build and Quality**
```bash
# Production build
npm run build

# Type checking
npm run type-check

# Lint code
npm run lint

# Auto-fix linting issues
npm run lint:fix

# Full quality check
npm run build && npm run lint && npm run type-check
```

#### **Component Development**
```bash
# Create new component
mkdir src/components/[category]
touch src/components/[category]/ComponentName.tsx

# Follow naming conventions:
# - PascalCase for component files
# - camelCase for utility files
# - kebab-case for page routes
```

### **Database Development**

#### **Migration Management**
```bash
# Create new migration
touch backend/src/main/resources/db/migration/V[N]__description.sql

# Migration naming convention:
# V1__initial_schema.sql
# V2__add_customer_fields.sql  
# V3__create_analytics_tables.sql
```

#### **Sample Migration**
```sql
-- V7__add_customer_notes.sql
ALTER TABLE customers ADD COLUMN notes TEXT;
CREATE INDEX idx_customers_notes ON customers USING gin(to_tsvector('english', notes));

-- Always include rollback information in comments
-- ROLLBACK: DROP INDEX idx_customers_notes; ALTER TABLE customers DROP COLUMN notes;
```

## 🔍 **Testing Strategy**

### **Backend Testing**

#### **Unit Tests**
```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock private CustomerRepository customerRepository;
    @InjectMocks private CustomerService customerService;
    
    @Test
    void shouldCreateCustomerSuccessfully() {
        // Test implementation
    }
}
```

#### **Integration Tests**  
```java
@SpringBootTest
@Testcontainers
class CustomerControllerIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test")
            .withUsername("test") 
            .withPassword("test");
}
```

### **Frontend Testing**

#### **Component Tests**
```typescript
import { render, screen } from '@testing-library/react';
import CustomerList from '@/components/customers/CustomerList';

describe('CustomerList', () => {
  test('renders customer list correctly', () => {
    render(<CustomerList />);
    expect(screen.getByText('Customer Management')).toBeInTheDocument();
  });
});
```

#### **E2E Tests**
```typescript
// cypress/e2e/customer-flow.cy.ts
describe('Customer Management Flow', () => {
  it('should create and update customer', () => {
    cy.visit('/');
    cy.get('[data-testid=add-customer]').click();
    // Test implementation
  });
});
```

## 🚀 **Deployment**

### **Development Environment**
```bash
# All services on localhost
Frontend:  http://localhost:3000
Backend:   http://localhost:8080  
Database:  localhost:5432
```

### **Production Build**
```bash
# Build frontend
cd frontend && npm run build

# Build backend
cd backend && mvn clean package

# Generated artifacts:
# - frontend/.next/ (Static files)
# - backend/target/customers-*.jar (Executable JAR)
```

### **Environment Variables**

#### **Frontend Production**
```bash
NEXT_PUBLIC_API_URL=https://api.yourcompany.com/api
```

#### **Backend Production**
```bash
spring.datasource.url=jdbc:postgresql://prod-db:5432/customers
spring.datasource.username=app_user
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=true
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000
```

## 🔧 **Troubleshooting**

### **Common Issues**

#### **Port Conflicts**
```bash
# Check what's using ports
lsof -i :3000  # Frontend
lsof -i :8080  # Backend  
lsof -i :5432  # Database

# Kill processes
pkill -f "next dev"
pkill -f "spring-boot"
pkill -f "postgres"
```

#### **Database Connection Issues**
```bash
# Check PostgreSQL container status
podman ps | grep postgres

# Restart database
./stop-all.sh
./start-database.sh

# Check database logs  
podman logs [container-id]
```

#### **Build Failures**
```bash
# Clear caches
cd frontend && npm run clean && npm install
cd backend && mvn clean

# Check for TypeScript errors
cd frontend && npm run type-check

# Check for Java compilation errors
cd backend && mvn compile
```

### **Log Files**
```bash
# Application logs
tail -f frontend_run.log  # Frontend output
tail -f backend_run.log   # Backend output

# Database logs
podman logs -f [postgres-container-id]

# Spring Boot application logs
cat backend/target/logs/spring.log
```

### **Performance Debugging**

#### **Database Performance**
```sql
-- Check slow queries
SELECT query, mean_exec_time, calls 
FROM pg_stat_statements 
ORDER BY mean_exec_time DESC LIMIT 10;

-- Check active connections
SELECT count(*) FROM pg_stat_activity;

-- Check table sizes
SELECT schemaname,tablename,pg_size_pretty(pg_total_relation_size(tablename::text))
FROM pg_tables WHERE schemaname='public';
```

#### **Application Performance**
```bash
# JVM performance monitoring
jps -l | grep customers
jstack [pid]  # Thread dump
jmap -dump:format=b,file=heap.hprof [pid]  # Heap dump

# Frontend performance  
cd frontend && npm run build && npm run analyze
```

## 📋 **Development Checklist**

### **Before Committing**
- [ ] All tests pass (`mvn test` and `npm test`)
- [ ] Code style checks pass (`mvn spotless:check` and `npm run lint`)
- [ ] TypeScript compilation succeeds (`npm run type-check`)
- [ ] No console errors in browser
- [ ] Manual testing of changed functionality
- [ ] Documentation updated if needed

### **Before Deploying**
- [ ] Production build succeeds
- [ ] Database migrations tested
- [ ] Environment variables configured
- [ ] Performance testing completed
- [ ] Security review completed
- [ ] Monitoring and logging configured

## 🤝 **Contributing Guidelines**

### **Code Style**
- **Backend**: Google Java Style (100 char line length, 2-space indentation)
- **Frontend**: ESLint + Prettier (2-space indentation, semicolons, single quotes)
- **Commits**: Conventional Commits format (`feat:`, `fix:`, `docs:`, etc.)

### **Branch Strategy**
```bash
# Feature development
git checkout -b feature/dashboard-enhancements
# Work and commit
git push origin feature/dashboard-enhancements
# Create pull request

# Bug fixes
git checkout -b fix/chart-alignment-issue
# Fix and commit  
git push origin fix/chart-alignment-issue
# Create pull request

# Documentation
git checkout -b docs/api-documentation
# Update docs and commit
git push origin docs/api-documentation  
# Create pull request
```

### **Pull Request Guidelines**
1. **Clear Title**: Describe the change in one line
2. **Detailed Description**: Explain what was changed and why
3. **Testing**: Include test results and verification steps
4. **Screenshots**: For UI changes, include before/after images
5. **Breaking Changes**: Clearly document any breaking changes
6. **Reviewer Assignment**: Tag appropriate team members

## 📊 **Performance Guidelines**

### **Backend Performance**
- Use JPA query methods instead of native SQL when possible
- Implement proper indexing for frequently queried columns
- Use `@Transactional(readOnly = true)` for read operations
- Implement caching for expensive operations
- Monitor database query performance with profiling

### **Frontend Performance**  
- Use React.memo() for expensive components
- Implement lazy loading for large lists
- Optimize image loading with Next.js Image component
- Use proper dependency arrays in useEffect
- Implement virtual scrolling for large datasets

### **Database Performance**
- Regular `VACUUM` and `ANALYZE` operations
- Monitor slow query log
- Use connection pooling (HikariCP)
- Implement read replicas for heavy read workloads
- Consider materialized views for complex analytics

## 🔒 **Security Guidelines**

### **Backend Security**
- Always use parameterized queries (JPA handles this)
- Validate all input at controller and service layers
- Implement proper CORS configuration
- Use strong JWT secrets in production
- Implement rate limiting for public endpoints

### **Frontend Security**
- Sanitize user input before display
- Use HTTPS in production
- Implement Content Security Policy (CSP)
- Validate data received from APIs
- Never expose sensitive data in client-side code

## 📈 **Monitoring & Observability**

### **Health Checks**
```bash
# Application health
curl http://localhost:8080/actuator/health

# Database health  
curl http://localhost:8080/actuator/health/db

# Custom metrics
curl http://localhost:8080/actuator/metrics
```

### **Logging**
```java
// Use SLF4J with structured logging
@Slf4j
@Service
public class CustomerService {
    public Customer createCustomer(Customer customer) {
        log.info("Creating customer: phone={}, name={}", 
                customer.getPhone(), customer.getName());
        // Implementation
    }
}
```

## 🛠️ **Advanced Development**

### **Custom Repository Methods**
```java
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    
    @Query("SELECT c FROM Customer c WHERE c.name ILIKE %:name%")
    List<Customer> findByNameContainingIgnoreCase(@Param("name") String name);
    
    @Modifying
    @Query("UPDATE Customer c SET c.deletedAt = :deletedAt WHERE c.id = :id")
    void softDeleteById(@Param("id") UUID id, @Param("deletedAt") ZonedDateTime deletedAt);
}
```

### **Custom React Hooks**
```typescript
// hooks/useCustomers.ts
export function useCustomers(searchParams: CustomerSearchParams) {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(false);
  
  // Implementation with proper dependency management
}
```

### **Error Handling Patterns**
```java
// Backend global exception handler
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        // Standardized error response
    }
}
```

```typescript
// Frontend error boundary
export function ErrorBoundary({ children }: { children: React.ReactNode }) {
  // React error boundary implementation
}
```

---

## 📞 **Support & Resources**

### **Getting Help**
- **📖 Documentation**: Check `docs/` folder for specific guides
- **🐛 Issues**: Report bugs on GitHub Issues
- **💬 Questions**: Use GitHub Discussions for help
- **🔍 API Reference**: http://localhost:8080/swagger-ui.html

### **Useful Resources**
- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **Next.js Docs**: https://nextjs.org/docs
- **Material Design**: https://m3.material.io/
- **PostgreSQL Docs**: https://www.postgresql.org/docs/

---

**Happy coding! 🎉**