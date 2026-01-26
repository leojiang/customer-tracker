# Self-Signed SSL Certificate Implementation Guide

## Overview

This guide provides step-by-step instructions to implement HTTPS on your Customer Tracker system using a **self-signed SSL certificate**.

**⚠️ Important Notes:**
- Self-signed certificates will show **browser security warnings**
- This is **normal and expected** for testing/internal systems
- Only use this for **development, testing, or internal company systems**
- For public production, use Let's Encrypt or a commercial certificate

**Benefits:**
- ✅ Free
- ✅ Works immediately
- ✅ No domain name required (uses IP address)
- ✅ Full encryption between browser and server
- ✅ Good for testing HTTPS implementation

**Drawbacks:**
- ❌ Browser warnings ("Your connection is not private")
- ❌ Users must accept the security warning
- ❌ Not trusted by browsers automatically

## Prerequisites

- Server access: `root@47.109.72.216`
- SSH access to server
- Nginx already installed and configured
- Firewall access to port 443

## Implementation Steps

### Step 1: Generate Self-Signed Certificate on Server

SSH into your server and run:

```bash
ssh root@47.109.72.216

# Create certificate directory
sudo mkdir -p /etc/ssl/certs/customer-tracker

# Generate self-signed certificate (valid for 1 year)
sudo openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
  -keyout /etc/ssl/certs/customer-tracker/key.pem \
  -out /etc/ssl/certs/customer-tracker/cert.pem \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=CustomerTracker/OU=IT/CN=47.109.72.216"

# Set proper permissions
sudo chmod 600 /etc/ssl/certs/customer-tracker/key.pem
sudo chmod 644 /etc/ssl/certs/customer-tracker/cert.pem

# Verify certificate was created
sudo ls -la /etc/ssl/certs/customer-tracker/
```

**Expected output:**
```
total 8
-rw-r--r-- 1 root root 1281 Jan 26 11:00 cert.pem
-rw------- 1 root root 1704 Jan 26 11:00 key.pem
```

### Step 2: Update Nginx Configuration

Create a new nginx configuration file with SSL:

```bash
# Create new SSL config
sudo nano /etc/nginx/conf.d/customer-tracker-ssl.conf
```

**Paste this configuration:**

```nginx
# HTTP Server - Redirect all traffic to HTTPS
server {
    listen 80;
    server_name 47.109.72.216;

    # Redirect all HTTP requests to HTTPS
    return 301 https://$server_name$request_uri;
}

# HTTPS Server
server {
    listen 443 ssl http2;
    server_name 47.109.72.216;

    # SSL Certificate Configuration
    ssl_certificate /etc/ssl/certs/customer-tracker/cert.pem;
    ssl_certificate_key /etc/ssl/certs/customer-tracker/key.pem;

    # SSL Protocol and Cipher Configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # SSL Session Configuration
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Root directory for static files
    root /var/www/customer-tracker-frontend/out;
    index index.html;

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml text/javascript application/json application/javascript application/xml+rss application/rss+xml font/truetype font/opentype application/vnd.ms-fontobject image/svg+xml;

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Next.js static files
    location /_next/static {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Next.js images
    location /_next/image {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Handle client-side routing
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Custom error pages
    error_page 404 /index.html;
}
```

**Save and exit:** `Ctrl+X`, then `Y`, then `Enter`

### Step 3: Test and Reload Nginx

```bash
# Test nginx configuration
sudo nginx -t

# Expected output:
# nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
# nginx: configuration file /etc/nginx/nginx.conf test is successful

# If test passes, reload nginx
sudo systemctl reload nginx

# OR if not using systemd:
sudo nginx -s reload

# Check nginx status
sudo systemctl status nginx
```

**Verify nginx is listening on port 443:**
```bash
sudo netstat -tlnp | grep :443
# Should show: tcp  0  0  0.0.0.0:443  0.0.0.0:*  LISTEN  12345/nginx
```

### Step 4: Update Firewall to Allow HTTPS

```bash
# If using firewalld (CentOS/RHEL):
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --reload

# Verify
sudo firewall-cmd --list-all

# OR if using ufw (Ubuntu/Debian):
sudo ufw allow 443/tcp
sudo ufw allow 80/tcp
sudo ufw reload

# Verify
sudo ufw status
```

### Step 5: Backend CORS Configuration Updates

Update the backend to accept HTTPS origins. This needs to be done on your **local machine**, then rebuilt and deployed.

**File to modify:** `backend/src/main/java/com/example/customers/config/CorsConfig.java`

**Find this section (around line 24-31):**
```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOriginPatterns(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://localhost:*",
            "https://127.0.0.1:*",
            "http://47.109.72.216:*",
            "http://47.109.72.216",
            "http://47.109.72.216:80")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false)
        .exposedHeaders("*")
        .maxAge(3600);
```

**Add these three lines:**
```java
        .allowedOriginPatterns(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://localhost:*",
            "https://127.0.0.1:*",
            "http://47.109.72.216:*",
            "http://47.109.72.216",
            "http://47.109.72.216:80",
            "https://47.109.72.216:*",      // ADD THIS
            "https://47.109.72.216",        // ADD THIS
            "https://47.109.72.216:443")    // ADD THIS
```

**Also update the `corsConfigurationSource()` method (around line 57-67):**
```java
CorsConfiguration configuration = new CorsConfiguration();
configuration.setAllowedOriginPatterns(
    Arrays.asList(
        "http://localhost:*",
        "http://127.0.0.1:*",
        "https://localhost:*",
        "https://127.0.0.1:*",
        "http://47.109.72.216:*",
        "http://47.109.72.216",
        "http://47.109.72.216:80",
        "https://47.109.72.216:*",      // ADD THIS
        "https://47.109.72.216",        // ADD THIS
        "https://47.109.72.216:443"));  // ADD THIS
```

### Step 6: Rebuild and Deploy Backend

**On your local machine:**

```bash
# Navigate to backend directory
cd backend

# Build the backend
./mvnw clean package -DskipTests

# Upload to server
scp target/customers-0.0.1-SNAPSHOT.jar root@47.109.72.216:/opt/customers-backend/

# SSH to server
ssh root@47.109.72.216

# Restart backend service
sudo systemctl restart customer-tracker-backend
# OR if running manually:
pkill -f customers-backend.jar
nohup java -jar /opt/customers-backend/customers-backend.jar > /var/log/customers-backend.log 2>&1 &

# Verify backend is running
sudo systemctl status customer-tracker-backend
# OR
ps aux | grep customers-backend.jar
```

### Step 7: Rebuild and Deploy Frontend

**On your local machine:**

```bash
# Navigate to frontend directory
cd frontend

# Build frontend with HTTPS API URL
NEXT_PUBLIC_API_URL=https://47.109.72.216:8080/api npm run build

# Verify build completed successfully
ls -la out/

# Upload to server
scp -r out/* root@47.109.72.216:/var/www/customer-tracker-frontend/out/

# SSH to server to verify
ssh root@47.109.72.216
ls -la /var/www/customer-tracker-frontend/out/
```

### Step 8: Verify HTTPS is Working

```bash
# Test HTTP to HTTPS redirect
curl -I http://47.109.72.216

# Expected output should include:
# HTTP/1.1 301 Moved Permanently
# Location: https://47.109.72.216/

# Test HTTPS access
curl -I https://47.109.72.216

# Expected output should include:
# HTTP/2 200
# strict-transport-security: max-age=31536000; includeSubDomains

# Test SSL certificate
openssl s_client -connect 47.109.72.216:443 -servername 47.109.72.216
```

## Browser Testing

### First Time Access - Accepting Security Warning

1. **Open browser and go to:** `https://47.109.72.216`

2. **You will see a security warning** (this is NORMAL for self-signed certificates):
   - Chrome: "Your connection is not private"
   - Firefox: "Warning: Potential Security Risk"
   - Safari: "This connection is not private"

3. **Click "Advanced" or "Show More"**

4. **Click "Proceed to 47.109.72.216 (unsafe)" or "Accept the Risk"**

5. **You should now see your application** with a 🔒 lock icon (possibly with a warning triangle)

### Verifying Features

Test these to ensure everything works:

```bash
# 1. Check page loads
✅ https://47.109.72.216

# 2. Check login works
✅ Login with your credentials

# 3. Check API calls work (open browser DevTools → Network tab)
✅ All requests should go to https://47.109.72.216:8080/api
✅ No mixed content warnings

# 4. Check all features
✅ Dashboard loads
✅ Customer list loads
✅ Forms work
✅ File uploads work
```

## Troubleshooting

### Issue 1: "Connection Refused" on port 443

**Symptom:** Browser can't connect to `https://47.109.72.216`

**Solution:**
```bash
# Check nginx is listening on 443
sudo netstat -tlnp | grep :443

# If not listening, reload nginx
sudo systemctl reload nginx

# Check firewall
sudo firewall-cmd --list-all
# Ensure https service is allowed

# Restart nginx if needed
sudo systemctl restart nginx
```

### Issue 2: "ERR_SSL_PROTOCOL_ERROR"

**Symptom:** Browser shows SSL protocol error

**Solution:**
```bash
# Verify certificate files exist
sudo ls -la /etc/ssl/certs/customer-tracker/

# Check nginx config syntax
sudo nginx -t

# Check nginx error log
sudo tail -50 /var/log/nginx/error.log

# Ensure certificate paths in nginx config are correct
```

### Issue 3: CORS Errors in Browser Console

**Symptom:** API calls blocked by CORS policy

**Solution:**
```bash
# Check backend logs for CORS errors
sudo tail -50 /var/log/customers-backend.log

# Look for lines like:
# DEBUG o.s.web.cors.DefaultCorsProcessor - Allow: 'https://47.109.72.216' origin is allowed
# If you see "Reject", CORS config needs update

# Verify backend was rebuilt with new CORS config
# Check commit hash/deployment time
```

### Issue 4: Mixed Content Warnings

**Symptom:** Browser console shows "Mixed Content" errors

**Solution:**
```bash
# Frontend was not built with HTTPS URL
# Rebuild with:
cd frontend
NEXT_PUBLIC_API_URL=https://47.109.72.216:8080/api npm run build

# Clear browser cache
# Chrome: Ctrl+Shift+Delete (Cmd+Shift+Delete on Mac)
# Select "Cached images and files"
# Click "Clear data"

# Rebuild and redeploy
```

### Issue 5: Redirect Loop

**Symptom:** Browser shows "Too many redirects"

**Solution:**
```bash
# Check nginx config
# Ensure only ONE server block listens on port 80 and does redirect
# Remove duplicate redirect rules

# Test config
sudo nginx -t

# Reload
sudo systemctl reload nginx
```

## Accepting Self-Signed Certificate Permanently

### For Chrome/Chromium

Users can add exception:

1. Open `chrome://settings/certificates`
2. Go to "Authorities" tab
3. Click "Import"
4. Upload `/etc/ssl/certs/customer-tracker/cert.pem` (download from server first)
5. Check "Trust this certificate for identifying websites"
6. Click OK

**Or download certificate:**
```bash
# On server, make certificate downloadable
sudo cp /etc/ssl/certs/customer-tracker/cert.pem /var/www/customer-tracker-frontend/out/

# Access: https://47.109.72.216/cert.pem
# Download and import into browser
```

### For Firefox

1. Open Firefox
2. Visit `https://47.109.72.216`
3. Click "Advanced" → "Accept Risk and Continue"
4. Firefox will remember this choice

## Monitoring and Maintenance

### Check Certificate Expiry

```bash
# On server
sudo openssl x509 -in /etc/ssl/certs/customer-tracker/cert.pem -noout -dates

# Output:
# notBefore=Jan 26 03:00:00 2026 GMT
# notAfter=Jan 26 03:00:00 2027 GMT

# Check expiry date
sudo openssl x509 -in /etc/ssl/certs/customer-tracker/cert.pem -noout -enddate
```

### Renew Certificate (After 1 Year)

```bash
# Generate new certificate
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /etc/ssl/certs/customer-tracker/key.pem \
  -out /etc/ssl/certs/customer-tracker/cert.pem \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=CustomerTracker/OU=IT/CN=47.109.72.216"

# Reload nginx
sudo systemctl reload nginx

# Verify
curl -I https://47.109.72.216
```

### Set Up Certificate Expiry Reminder

Add a cron job to alert 30 days before expiry:

```bash
# Edit crontab
sudo crontab -e

# Add this line:
0 9 * * * openssl x509 -in /etc/ssl/certs/customer-tracker/cert.pem -checkend 2592000 -noout && echo "SSL certificate expires in 30 days" || true

# This will check daily at 9 AM and alert if certificate expires within 30 days
```

## Performance Verification

### Check if HTTP/2 is Enabled

```bash
# Test with curl
curl -I https://47.109.72.216

# Look for:
# HTTP/2 200

# OR use nmap
nmap --script http2 -p 443 47.109.72.216
```

### Benchmark HTTP vs HTTPS

```bash
# HTTP
ab -n 1000 -c 10 http://47.109.72.216/

# HTTPS
ab -n 1000 -c 10 https://47.109.72.216/

# HTTPS should be only slightly slower due to TLS handshake
# HTTP/2 helps offset this for modern browsers
```

## Summary of Changes

### Files Modified

1. **Server Configuration:**
   - `/etc/nginx/conf.d/customer-tracker-ssl.conf` (NEW)
   - Firewall rules (UPDATED)

2. **Server Files Created:**
   - `/etc/ssl/certs/customer-tracker/cert.pem` (NEW)
   - `/etc/ssl/certs/customer-tracker/key.pem` (NEW)

3. **Backend Code:**
   - `backend/src/main/java/com/example/customers/config/CorsConfig.java` (MODIFIED)

4. **Frontend Build:**
   - Rebuilt with `NEXT_PUBLIC_API_URL=https://47.109.72.216:8080/api`

### URLs Changed

| Before | After |
|--------|-------|
| `http://47.109.72.216` | `https://47.109.72.216` (redirects to HTTPS) |
| `http://47.109.72.216:8080/api` | `https://47.109.72.216:8080/api` |

### What Users Will Experience

1. **First visit:** Browser warning about untrusted certificate
2. **After accepting:** Normal HTTPS connection with 🔒 (possibly with warning)
3. **All data:** Encrypted between browser and server
4. **Performance:** Minimal impact (HTTP/2 offsets TLS overhead)

## Rollback Procedure

If you need to revert to HTTP:

```bash
# Remove SSL nginx config
sudo rm /etc/nginx/conf.d/customer-tracker-ssl.conf

# Restore original HTTP config (if you backed it up)
sudo cp /etc/nginx/sites-enabled/customer-tracker-frontend.backup \
         /etc/nginx/sites-enabled/customer-tracker-frontend

# Reload nginx
sudo systemctl reload nginx

# Rebuild frontend with HTTP URL
cd frontend
NEXT_PUBLIC_API_URL=http://47.109.72.216:8080/api npm run build

# Deploy
scp -r out/* root@47.109.72.216:/var/www/customer-tracker-frontend/out/
```

## Next Steps

1. ✅ Implement this guide
2. ✅ Test all functionality
3. ✅ Document for users how to accept certificate
4. ⚠️ **Plan to migrate to Let's Encrypt** when you get a domain name
5. ⚠️ **Set certificate expiry reminder**

## Production Recommendation

**Self-signed certificates are NOT recommended for production systems with external users.**

**For production, plan to:**
1. Purchase a domain name (~$10/year)
2. Use Let's Encrypt (Free, trusted)
3. Follow `HTTPS_SECURITY_PLAN.md` Phase 1

---

**Questions?**
- If you encounter issues, check the troubleshooting section
- For Let's Encrypt migration, see the main HTTPS plan
- Browser warnings are NORMAL for self-signed certificates

**Document Version:** 1.0
**Last Updated:** 2026-01-26
**Certificate Validity:** 365 days from creation
