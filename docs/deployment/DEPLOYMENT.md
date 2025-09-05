# Deployment Guide

> **Complete guide for deploying the Customer Tracker CRM to production environments**

## 🚀 **Deployment Options**

### **🐳 Docker Deployment (Recommended)**
```bash
# Build and deploy with Docker Compose
docker-compose -f docker-compose.prod.yml up -d
```

### **☁️ Cloud Deployment**
- **Frontend**: Vercel, Netlify, or AWS S3 + CloudFront
- **Backend**: AWS ECS, Google Cloud Run, or DigitalOcean App Platform
- **Database**: AWS RDS, Google Cloud SQL, or managed PostgreSQL

### **🖥️ Traditional Server Deployment**
- **Frontend**: Nginx + static files
- **Backend**: SystemD service with JAR file
- **Database**: PostgreSQL instance with proper configuration

## 🔧 **Production Configuration**

### **🌐 Frontend Production Build**
```bash
cd frontend

# Install dependencies
npm ci --production

# Build for production
npm run build

# Verify build
npm run start
```

#### **Environment Variables**
```bash
# frontend/.env.production
NEXT_PUBLIC_API_URL=https://api.yourdomain.com/api
NEXT_PUBLIC_ENVIRONMENT=production
```

#### **Static Export (Optional)**
```bash
# For static hosting (S3, CDN)
npm run build && npm run export
```

### **⚙️ Backend Production Build**
```bash
cd backend

# Clean and package
mvn clean package -Dmaven.test.skip=true

# Build executable JAR
ls target/customers-*.jar
```

#### **Production Configuration**
```properties
# application-prod.properties
server.port=8080
server.servlet.context-path=/

# Database
spring.datasource.url=jdbc:postgresql://prod-db-host:5432/customers
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# Security
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000

# CORS
cors.allowed-origins=https://yourdomain.com,https://app.yourdomain.com

# Logging
logging.level.com.example.customers=INFO
logging.file.name=logs/application.log
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
```

## 🐳 **Docker Deployment**

### **📦 Docker Compose Setup**
```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  database:
    image: postgres:15
    environment:
      POSTGRES_DB: customers
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backups:/backups
    ports:
      - "5432:5432"
    restart: unless-stopped
    
  backend:
    build: 
      context: ./backend
      dockerfile: Dockerfile.prod
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
    ports:
      - "8080:8080"
    depends_on:
      - database
    restart: unless-stopped
    volumes:
      - ./logs:/app/logs
      
  frontend:
    build:
      context: ./frontend  
      dockerfile: Dockerfile.prod
    environment:
      - NEXT_PUBLIC_API_URL=http://backend:8080/api
    ports:
      - "3000:3000"
    depends_on:
      - backend
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/ssl
    depends_on:
      - frontend
      - backend
    restart: unless-stopped

volumes:
  postgres_data:
```

### **🔨 Dockerfile Examples**

#### **Backend Dockerfile**
```dockerfile
# backend/Dockerfile.prod
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy JAR file
COPY target/customers-*.jar app.jar

# Create logs directory
RUN mkdir -p logs

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run application
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
```

#### **Frontend Dockerfile**
```dockerfile
# frontend/Dockerfile.prod
FROM node:18-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build

# Production image
FROM node:18-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000
ENV PORT 3000

CMD ["node", "server.js"]
```

### **🌐 Nginx Configuration**
```nginx
# nginx/nginx.conf
upstream backend {
    server backend:8080;
}

upstream frontend {
    server frontend:3000;
}

server {
    listen 80;
    server_name yourdomain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl;
    server_name yourdomain.com;
    
    ssl_certificate /etc/ssl/certs/yourdomain.com.crt;
    ssl_certificate_key /etc/ssl/private/yourdomain.com.key;
    
    # Frontend
    location / {
        proxy_pass http://frontend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Backend API
    location /api/ {
        proxy_pass http://backend/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # CORS headers
        add_header Access-Control-Allow-Origin "https://yourdomain.com" always;
        add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS" always;
        add_header Access-Control-Allow-Headers "Authorization, Content-Type" always;
    }
}
```

## ☁️ **Cloud Deployment**

### **🌍 AWS Deployment**

#### **Architecture**
```
Internet -> CloudFront (CDN) -> ALB (Load Balancer)
                                  ├── ECS (Backend)
                                  └── S3 + CloudFront (Frontend)
                                      └── RDS PostgreSQL
```

#### **ECS Task Definition** 
```json
{
  "family": "customer-tracker-backend",
  "taskRoleArn": "arn:aws:iam::ACCOUNT:role/ecsTaskRole",
  "executionRoleArn": "arn:aws:iam::ACCOUNT:role/ecsTaskExecutionRole",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "containerDefinitions": [
    {
      "name": "customer-tracker-backend",
      "image": "your-ecr-repo/customer-tracker-backend:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        }
      ],
      "secrets": [
        {
          "name": "DB_PASSWORD",
          "valueFrom": "arn:aws:ssm:region:account:parameter/customer-tracker/db-password"
        },
        {
          "name": "JWT_SECRET", 
          "valueFrom": "arn:aws:ssm:region:account:parameter/customer-tracker/jwt-secret"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/customer-tracker",
          "awslogs-region": "us-west-2",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

#### **Frontend S3 + CloudFront**
```bash
# Build and deploy frontend
npm run build
aws s3 sync .next/static s3://yourdomain-frontend/static/
aws cloudfront create-invalidation --distribution-id DISTRIBUTION_ID --paths "/*"
```

### **🔷 Google Cloud Deployment**

#### **Cloud Run Service**
```yaml
# cloudbuild.yaml
steps:
  - name: 'gcr.io/cloud-builders/docker'
    args: ['build', '-t', 'gcr.io/$PROJECT_ID/customer-tracker-backend', './backend']
  - name: 'gcr.io/cloud-builders/docker'
    args: ['push', 'gcr.io/$PROJECT_ID/customer-tracker-backend']
    
  - name: 'gcr.io/cloud-builders/gcloud'
    args:
      - 'run'
      - 'deploy'
      - 'customer-tracker-backend'
      - '--image=gcr.io/$PROJECT_ID/customer-tracker-backend'
      - '--platform=managed'
      - '--region=us-central1'
      - '--allow-unauthenticated'
```

#### **Cloud SQL Configuration**
```bash
# Create Cloud SQL instance
gcloud sql instances create customer-tracker-db \
  --database-version=POSTGRES_15 \
  --tier=db-n1-standard-1 \
  --region=us-central1

# Create database
gcloud sql databases create customers --instance=customer-tracker-db
```

### **🌊 DigitalOcean App Platform**
```yaml
# .do/app.yaml
name: customer-tracker
services:
  - name: backend
    source_dir: /backend
    github:
      repo: leojiang/customer-tracker
      branch: main
    run_command: java -jar target/customers-*.jar
    environment_slug: java
    instance_count: 2
    instance_size_slug: basic-xxs
    envs:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: DB_PASSWORD
        value: ${DATABASE_PASSWORD}
        type: SECRET
        
  - name: frontend
    source_dir: /frontend  
    github:
      repo: leojiang/customer-tracker
      branch: main
    run_command: npm start
    environment_slug: node-js
    instance_count: 1
    instance_size_slug: basic-xxs

databases:
  - name: customer-tracker-db
    engine: PG
    version: "15"
    size_slug: db-s-1vcpu-1gb
```

## 🔐 **Security Configuration**

### **🛡️ Production Security**

#### **Environment Variables**
```bash
# Critical secrets (never commit these!)
export JWT_SECRET="your-super-secure-secret-key-here"
export DB_PASSWORD="your-secure-database-password"
export DB_USERNAME="app_user"

# Optional configurations
export CORS_ALLOWED_ORIGINS="https://yourdomain.com,https://app.yourdomain.com"
export RATE_LIMIT_REQUESTS_PER_MINUTE="100"
```

#### **Database Security**
```sql
-- Create application-specific user
CREATE USER app_user WITH PASSWORD 'secure_random_password';

-- Grant minimum required permissions
GRANT CONNECT ON DATABASE customers TO app_user;
GRANT USAGE ON SCHEMA public TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO app_user;

-- Revoke dangerous permissions
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE CREATE ON SCHEMA public FROM app_user;
```

#### **SSL/TLS Configuration**
```nginx
# Strong SSL configuration
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
ssl_prefer_server_ciphers off;
ssl_session_cache shared:SSL:10m;
ssl_session_timeout 10m;

# Security headers
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "DENY" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';" always;
```

## 📊 **Monitoring & Observability**

### **📈 Application Monitoring**

#### **Health Checks**
```bash
# Backend health
curl https://api.yourdomain.com/actuator/health

# Frontend health  
curl https://yourdomain.com/api/health

# Database health
curl https://api.yourdomain.com/actuator/health/db
```

#### **Metrics Collection**
```yaml
# Prometheus configuration
scrape_configs:
  - job_name: 'customer-tracker-backend'
    static_configs:
      - targets: ['backend:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    
  - job_name: 'customer-tracker-db'
    static_configs:
      - targets: ['postgres-exporter:9187']
    scrape_interval: 30s
```

#### **Grafana Dashboard**
```json
{
  "dashboard": {
    "title": "Customer Tracker CRM",
    "panels": [
      {
        "title": "API Response Times",
        "type": "graph",
        "targets": [
          {
            "expr": "http_request_duration_seconds{job=\"customer-tracker-backend\"}"
          }
        ]
      },
      {
        "title": "Database Connections",
        "type": "graph", 
        "targets": [
          {
            "expr": "pg_stat_database_numbackends{datname=\"customers\"}"
          }
        ]
      }
    ]
  }
}
```

### **🚨 Alerting**

#### **Critical Alerts**
```yaml
# AlertManager rules
groups:
  - name: customer-tracker
    rules:
      - alert: HighResponseTime
        expr: http_request_duration_seconds{quantile="0.95"} > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High API response times detected"
          
      - alert: DatabaseConnectionHigh
        expr: pg_stat_database_numbackends > 50  
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Database connection count is high"
          
      - alert: ApplicationDown
        expr: up{job="customer-tracker-backend"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Customer Tracker application is down"
```

## 🔄 **CI/CD Pipeline**

### **🔧 GitHub Actions**
```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Java 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          
      - name: Test Backend
        run: |
          cd backend
          mvn clean test
          mvn spotless:check
          
      - name: Test Frontend  
        run: |
          cd frontend
          npm ci
          npm run lint
          npm run type-check
          npm run build

  deploy:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Deploy to Production
        env:
          DEPLOY_HOST: ${{ secrets.DEPLOY_HOST }}
          DEPLOY_USER: ${{ secrets.DEPLOY_USER }}
          DEPLOY_KEY: ${{ secrets.DEPLOY_KEY }}
        run: |
          # Deployment script
          ./scripts/deploy-prod.sh
```

### **📦 Deployment Script**
```bash
#!/bin/bash
# scripts/deploy-prod.sh

set -e

echo "🚀 Starting production deployment..."

# Build application
echo "🔨 Building application..."
cd backend && mvn clean package -Dmaven.test.skip=true
cd ../frontend && npm ci && npm run build

# Upload files to server
echo "📤 Uploading to production server..."
rsync -avz --delete backend/target/customers-*.jar $DEPLOY_USER@$DEPLOY_HOST:/app/
rsync -avz --delete frontend/.next/ $DEPLOY_USER@$DEPLOY_HOST:/app/frontend/

# Restart services
echo "🔄 Restarting services..."
ssh $DEPLOY_USER@$DEPLOY_HOST "sudo systemctl restart customer-tracker-backend"
ssh $DEPLOY_USER@$DEPLOY_HOST "sudo systemctl restart customer-tracker-frontend"

# Health check
echo "🩺 Running health checks..."
sleep 30
curl -f https://api.yourdomain.com/actuator/health
curl -f https://yourdomain.com/

echo "✅ Deployment completed successfully!"
```

## 🔒 **Backup & Recovery**

### **💾 Database Backup**
```bash
#!/bin/bash
# scripts/backup-database.sh

BACKUP_DIR="/backups/customer-tracker"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/customers_backup_$DATE.sql"

# Create backup directory
mkdir -p $BACKUP_DIR

# Create backup
pg_dump -h $DB_HOST -U $DB_USER -d customers \
  --format=custom \
  --compress=9 \
  --verbose \
  --file=$BACKUP_FILE

# Verify backup
pg_restore --list $BACKUP_FILE > /dev/null

# Clean old backups (keep last 30 days)
find $BACKUP_DIR -name "customers_backup_*.sql" -mtime +30 -delete

echo "✅ Backup completed: $BACKUP_FILE"
```

### **🔄 Recovery Procedures**
```bash
# Full database restore
pg_restore -h $DB_HOST -U $DB_USER -d customers_new \
  --verbose \
  --clean \
  --create \
  backup_file.sql

# Selective table restore
pg_restore -h $DB_HOST -U $DB_USER -d customers \
  --table=customers \
  --table=status_history \
  backup_file.sql
```

### **📊 Application Backup**
```bash
#!/bin/bash
# scripts/backup-application.sh

# Backup configuration files
tar -czf config_backup_$(date +%Y%m%d).tar.gz \
  backend/src/main/resources/application*.properties \
  frontend/.env.production \
  nginx/nginx.conf

# Backup logs
tar -czf logs_backup_$(date +%Y%m%d).tar.gz \
  logs/ \
  frontend_run.log \
  backend_run.log
```

## 📊 **Performance Tuning**

### **🚀 Application Performance**

#### **Backend Optimization**
```properties
# JVM tuning
JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=100

# Connection pool tuning
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.leak-detection-threshold=60000

# JPA optimization
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true
```

#### **Frontend Optimization**
```javascript
// next.config.js
module.exports = {
  experimental: {
    optimizeCss: true,
    optimizeImages: true,
  },
  compiler: {
    removeConsole: process.env.NODE_ENV === 'production',
  },
  images: {
    domains: ['yourdomain.com'],
    formats: ['image/webp', 'image/avif'],
  },
};
```

### **🗄️ Database Performance**
```sql
-- Production database optimization
ALTER SYSTEM SET shared_preload_libraries = 'pg_stat_statements';
ALTER SYSTEM SET max_connections = 200;
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET work_mem = '4MB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';
SELECT pg_reload_conf();

-- Enable query statistics
CREATE EXTENSION pg_stat_statements;

-- Optimize autovacuum
ALTER TABLE customers SET (autovacuum_vacuum_scale_factor = 0.1);
ALTER TABLE status_history SET (autovacuum_vacuum_scale_factor = 0.05);
```

## 🚨 **Troubleshooting Production Issues**

### **📊 Common Production Problems**

#### **High CPU Usage**
```bash
# Check application processes
top -p $(pgrep -f "customer-tracker")

# Check database queries
psql -d customers -c "SELECT query, state, query_start FROM pg_stat_activity WHERE state != 'idle';"

# Thread dump for Java application
kill -3 $(pgrep -f "customer-tracker")
```

#### **Memory Issues**
```bash
# Check memory usage
free -h
cat /proc/meminfo

# Java heap dump
jmap -dump:format=b,file=heap.hprof $(pgrep -f "customer-tracker")

# Analyze with Eclipse MAT or jhat
```

#### **Database Connection Issues**
```sql
-- Check connection limits
SHOW max_connections;
SELECT count(*) FROM pg_stat_activity;

-- Kill long-running queries  
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE query_start < now() - interval '10 minutes'
  AND state != 'idle';
```

### **🔧 Emergency Procedures**

#### **Application Recovery**
```bash
# Graceful restart
sudo systemctl reload customer-tracker-backend
sudo systemctl reload customer-tracker-frontend

# Force restart
sudo systemctl restart customer-tracker-backend
sudo systemctl restart customer-tracker-frontend

# Check status
sudo systemctl status customer-tracker-*
```

#### **Database Recovery**
```bash
# Check database status
sudo systemctl status postgresql

# Restart database
sudo systemctl restart postgresql

# Check connections
psql -h localhost -U app_user -d customers -c "SELECT version();"
```

## 📈 **Scaling Strategy**

### **🔼 Vertical Scaling**
- **CPU**: Increase instance CPU for compute-heavy analytics
- **Memory**: Add RAM for larger datasets and caching
- **Storage**: Scale database storage for data growth
- **Network**: Upgrade bandwidth for high-traffic periods

### **🔀 Horizontal Scaling**
- **Load Balancing**: Multiple backend instances behind load balancer
- **Database Replicas**: Read replicas for analytics queries
- **CDN**: Content delivery network for static assets
- **Microservices**: Split into smaller, focused services

### **📊 Capacity Planning**
```bash
# Monitor key metrics
# - API requests per minute
# - Database query execution time  
# - Memory usage patterns
# - Storage growth rate
# - User concurrency levels

# Scale triggers
# - Response time > 2 seconds
# - CPU usage > 80% sustained  
# - Memory usage > 85%
# - Database connections > 80% of max
# - Error rate > 1%
```

---

## 🎯 **Deployment Checklist**

### **📋 Pre-Deployment**
- [ ] All tests pass in CI/CD pipeline
- [ ] Database migrations tested on staging
- [ ] Environment variables configured
- [ ] SSL certificates installed and valid
- [ ] Monitoring and alerting configured
- [ ] Backup procedures tested
- [ ] Performance testing completed
- [ ] Security review completed

### **🚀 During Deployment**
- [ ] Maintenance page displayed to users
- [ ] Database backup created
- [ ] Application deployed with zero downtime
- [ ] Health checks passing
- [ ] Smoke tests completed
- [ ] Monitoring confirms stability

### **✅ Post-Deployment**  
- [ ] All features working as expected
- [ ] Performance metrics within acceptable ranges
- [ ] No error alerts triggered
- [ ] User acceptance testing completed
- [ ] Documentation updated
- [ ] Team notified of deployment completion

---

**🎉 Your Customer Tracker CRM is now ready for production deployment with enterprise-grade reliability and performance!**