# SSL Protocol Error - Troubleshooting Guide

## Problem Description

**Error:** `(failed)net::ERR_SSL_PROTOCOL_ERROR`

**When it occurs:** When attempting to login to the application

**Root Cause:** The frontend is trying to connect to the backend via HTTPS (`https://47.109.72.216:8080/api`), but the backend server on port 8080 is configured to serve **HTTP only**, not HTTPS.

### Current Configuration

**Backend (`application.yml`):**
- Port: 8080
- SSL: **NOT configured** (serving HTTP only)

**Frontend (`.env.production`):**
- API URL: `https://47.109.72.216:8080/api` ❌ (HTTPS request to HTTP server)

**The mismatch:**
```
Frontend: HTTPS request → https://47.109.72.216:8080/api
Backend:  HTTP server  → http://47.109.72.216:8080 (no SSL)
Result:   ERR_SSL_PROTOCOL_ERROR
```

---

## Solution Options

### Option 1: Nginx Reverse Proxy (Recommended)

Use nginx as an SSL termination proxy. Nginx handles HTTPS on port 443 and proxies requests to the backend via HTTP on port 8080.

**Architecture:**
```
Browser ──HTTPS──> nginx:443 ──HTTP──> backend:8080
         (SSL here)           (no SSL needed)
```

**Pros:**
- ✅ Simple backend configuration (no SSL needed)
- ✅ Centralized SSL certificate management
- ✅ Industry-standard approach
- ✅ Easy to scale and maintain
- ✅ Can add load balancing later
- ✅ Better performance with nginx caching

**Cons:**
- ⚠️ Requires nginx configuration changes
- ⚠️ Need to rebuild frontend

#### Implementation Steps

**Step 1: Update Nginx Configuration**

SSH to your server and update the nginx SSL configuration:

```bash
ssh root@47.109.72.216
```

Edit the SSL configuration file:

```bash
sudo nano /etc/nginx/conf.d/customer-tracker-ssl.conf
```

Add this location block inside the HTTPS server block (after line 109):

```nginx
# HTTPS Server
server {
    listen 443 ssl http2;
    server_name 47.109.72.216;

    # ... existing SSL configuration ...

    # API Proxy - Pass requests to backend
    location /api {
        proxy_pass http://localhost:8080/api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $server_name;

        # CORS headers (if needed)
        add_header Access-Control-Allow-Origin * always;
        add_header Access-Control-Allow-Methods "GET, POST, PUT, PATCH, DELETE, OPTIONS" always;
        add_header Access-Control-Allow-Headers "*" always;

        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # ... rest of the configuration ...
}
```

Save and exit: `Ctrl+X`, then `Y`, then `Enter`

**Step 2: Test and Reload Nginx**

```bash
# Test nginx configuration
sudo nginx -t

# Expected output:
# nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
# nginx: configuration file /etc/nginx/nginx.conf test is successful

# Reload nginx
sudo systemctl reload nginx

# Verify nginx is running
sudo systemctl status nginx
```

**Step 3: Update Frontend Environment Variable**

On your **local machine**, update the frontend configuration:

```bash
cd frontend

# Edit .env.production
nano .env.production
```

Change:
```bash
# From:
NEXT_PUBLIC_API_URL=https://47.109.72.216:8080/api

# To:
NEXT_PUBLIC_API_URL=https://47.109.72.216/api
```

**Step 4: Rebuild Frontend**

```bash
# Build with HTTPS API URL (no port specified, uses default 443)
NEXT_PUBLIC_API_URL=https://47.109.72.216/api npm run build

# Verify build
ls -la out/
```

**Step 5: Deploy Frontend**

```bash
# Upload to server
scp -r out/* root@47.109.72.216:/var/www/customer-tracker-frontend/out/

# Verify on server
ssh root@47.109.72.216 "ls -la /var/www/customer-tracker-frontend/out/"
```

**Step 6: Verify the Fix**

Test in your browser:

1. Open: `https://47.109.72.216`
2. Accept the security warning (self-signed certificate)
3. Try to login
4. Open DevTools (F12) → Network tab
5. Verify API requests go to: `https://47.109.72.216/api/...`
6. Verify requests succeed (no ERR_SSL_PROTOCOL_ERROR)

**Verification from command line:**

```bash
# Test HTTPS frontend
curl -I https://47.109.72.216

# Test API proxy (should return 401 Unauthorized or API response)
curl -I https://47.109.72.216/api/auth/login

# Both should work without SSL errors
```

---

### Option 2: Direct Backend SSL (Alternative)

Configure the Spring Boot backend to serve HTTPS directly on port 8080 (or a dedicated SSL port like 8443).

**Architecture:**
```
Browser ──HTTPS──> backend:8080 (with SSL)
```

**Pros:**
- ✅ Direct connection to backend (no proxy)
- ✅ Simpler nginx configuration

**Cons:**
- ❌ More complex backend configuration
- ❌ Need to manage Java keystore
- ❌ Harder to maintain SSL certificates
- ❌ Cannot easily add load balancing later
- ❌ Mixed HTTP/HTTPS serving requires two ports

#### Implementation Steps

**Step 1: Generate Java Keystore from SSL Certificates**

On the **server**, convert your SSL certificates to Java KeyStore (JKS) or PKCS12 format:

```bash
ssh root@47.109.72.216

# Create directory for keystore
sudo mkdir -p /etc/ssl/certs/customer-tracker/keystore

# If you have .pem files (from self-signed guide):
sudo openssl pkcs12 -export \
  -in /etc/ssl/certs/customer-tracker/cert.pem \
  -inkey /etc/ssl/certs/customer-tracker/key.pem \
  -out /etc/ssl/certs/customer-tracker/keystore/keystore.p12 \
  -name tomcat \
  -password pass:changeit

# Set permissions
sudo chmod 640 /etc/ssl/certs/customer-tracker/keystore/keystore.p12
sudo chown root:root /etc/ssl/certs/customer-tracker/keystore/keystore.p12

# Verify keystore
sudo keytool -list -v -keystore /etc/ssl/certs/customer-tracker/keystore/keystore.p12 \
  -storetype PKCS12 -storepass changeit
```

**Step 2: Update Backend Configuration**

On your **local machine**, update the backend configuration:

```bash
cd backend
nano src/main/resources/application.yml
```

Add SSL configuration:

```yaml
server:
  port: 8080
  ssl:
    enabled: true
    key-store: /etc/ssl/certs/customer-tracker/keystore/keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: tomcat
```

**Alternative: Use separate HTTP and HTTPS ports**

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: /etc/ssl/certs/customer-tracker/keystore/keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: tomcat

# HTTP redirect server (if needed, requires additional configuration)
```

**Step 3: Add HTTP to HTTPS Redirect (Optional)**

Create a new configuration class to redirect HTTP to HTTPS:

```bash
nano src/main/java/com/example/customers/config/HttpToHttpsRedirectConfig.java
```

```java
package com.example.customers.config;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpToHttpsRedirectConfig {

  @Bean
  public ServletWebServerFactory servletContainer() {
    TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
      @Override
      protected void postProcessContext(Context context) {
        SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setUserConstraint("CONFIDENTIAL");
        SecurityCollection collection = new SecurityCollection();
        collection.addPattern("/*");
        securityConstraint.addCollection(collection);
        context.addConstraint(securityConstraint);
      }
    };

    tomcat.addAdditionalTomcatConnectors(redirectConnector());
    return tomcat;
  }

  private Connector redirectConnector() {
    Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
    connector.setScheme("http");
    connector.setPort(8080);
    connector.setSecure(false);
    connector.setRedirectPort(8443);
    return connector;
  }
}
```

**Step 4: Rebuild Backend**

```bash
cd backend

# Build with new configuration
./mvnw clean package -DskipTests

# Verify JAR was created
ls -lh target/*.jar
```

**Step 5: Deploy Backend**

```bash
# Upload to server
scp target/customers-0.0.1-SNAPSHOT.jar root@47.109.72.216:/opt/customers-backend/

# SSH to server
ssh root@47.109.72.216

# Restart backend service
sudo systemctl restart customer-tracker-backend

# OR if running manually:
pkill -f customers-backend.jar
nohup java -jar /opt/customers-backend/customers-backend.jar > /var/log/customers-backend.log 2>&1 &

# Check logs for SSL startup
sudo tail -50 /var/log/customers-backend.log

# Verify backend is running on HTTPS
curl -k -I https://localhost:8080/api
```

**Step 6: Update Frontend (if using different port)**

If you changed the backend port to 8443:

```bash
cd frontend
nano .env.production
```

```bash
NEXT_PUBLIC_API_URL=https://47.109.72.216:8443/api
```

Rebuild and redeploy:

```bash
NEXT_PUBLIC_API_URL=https://47.109.72.216:8443/api npm run build
scp -r out/* root@47.109.72.216:/var/www/customer-tracker-frontend/out/
```

**Step 7: Update Nginx (if applicable)**

If using nginx for frontend only, update the API proxy configuration to use HTTPS backend:

```nginx
location /api {
    proxy_pass https://localhost:8080/api;

    # SSL verification for backend
    proxy_ssl_verify off;
    proxy_ssl_trusted_certificate /etc/ssl/certs/customer-tracker/cert.pem;

    # Standard proxy headers
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Reload nginx:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

**Step 8: Verify the Fix**

Test in browser and command line:

```bash
# Test backend HTTPS directly
curl -k -I https://47.109.72.216:8080/api

# Test via nginx proxy
curl -I https://47.109.72.216/api
```

---

## Comparison Table

| Aspect | Option 1: Nginx Proxy | Option 2: Backend SSL |
|--------|----------------------|----------------------|
| **Complexity** | Low (nginx only) | High (backend + keystore) |
| **Backend Changes** | None | SSL config required |
| **Frontend URL** | `https://47.109.72.216/api` | `https://47.109.72.216:8080/api` |
| **SSL Cert Management** | Centralized in nginx | Distributed (backend) |
| **Performance** | Better (nginx caching) | Good |
| **Scalability** | Easy (load balancing) | Harder |
| **Maintenance** | Easy | Complex |
| **Industry Standard** | ✅ Yes | ❌ Less common |
| **Recommended** | ✅ Yes | ⚠️ Only if needed |

---

## Quick Decision Guide

### Choose Option 1 (Nginx Proxy) if:
- ✅ You want the standard, production-ready approach
- ✅ You prefer simpler configuration
- ✅ You might add load balancing later
- ✅ You want centralized SSL management

### Choose Option 2 (Backend SSL) if:
- You need direct HTTPS access to backend (no nginx)
- You're running backend without nginx
- You have specific requirements for backend SSL

---

## Current Recommendation

**Use Option 1 (Nginx Reverse Proxy)**

Reasons:
1. Your SSL guide (`SELFSIGNED_SSL_GUIDE.md`) already sets up nginx with SSL
2. No backend code changes needed
3. Industry-standard approach
4. Easier to maintain and scale

---

## Verification Checklist

After implementing either option, verify:

- [ ] Frontend loads without SSL errors
- [ ] Login works correctly
- [ ] API calls succeed in browser DevTools
- [ ] No ERR_SSL_PROTOCOL_ERROR in browser console
- [ ] All features work (dashboard, customer list, forms, etc.)
- [ ] HTTP requests redirect to HTTPS (if configured)
- [ ] Backend logs show no SSL errors
- [ ] nginx logs show no errors

---

## Rolling Back

### Rollback Option 1 (Nginx Proxy):

```bash
# On server - remove API proxy from nginx config
sudo nano /etc/nginx/conf.d/customer-tracker-ssl.conf
# Remove the location /api block

sudo nginx -t
sudo systemctl reload nginx

# On local - rebuild frontend with original URL
cd frontend
# .env.production
NEXT_PUBLIC_API_URL=http://47.109.72.216:8080/api

npm run build
scp -r out/* root@47.109.72.216:/var/www/customer-tracker-frontend/out/
```

### Rollback Option 2 (Backend SSL):

```bash
# Revert backend configuration
cd backend
git diff src/main/resources/application.yml
# Remove SSL configuration

./mvnw clean package -DskipTests
scp target/customers-0.0.1-SNAPSHOT.jar root@47.109.72.216:/opt/customers-backend/
ssh root@47.109.72.216 "sudo systemctl restart customer-tracker-backend"

# Rebuild frontend
cd frontend
# .env.production
NEXT_PUBLIC_API_URL=http://47.109.72.216:8080/api

npm run build
scp -r out/* root@47.109.72.216:/var/www/customer-tracker-frontend/out/
```

---

## Additional Resources

- Self-signed SSL guide: `docs/deployment/SELFSIGNED_SSL_GUIDE.md`
- Spring Boot SSL documentation: https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto-configure-ssl
- Nginx reverse proxy guide: https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/

---

**Document Version:** 1.0
**Created:** 2026-01-26
**Related Issue:** ERR_SSL_PROTOCOL_ERROR on login
