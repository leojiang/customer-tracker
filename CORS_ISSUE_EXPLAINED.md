# CORS Issue Explained: Why `npm run dev` Works But Nginx Fails

## The Mystery Solved! 🔍

You noticed that:
- ✅ **npm run dev** on remote server → API calls work
- ❌ **nginx serving static files** → CORS errors

## The Root Cause

### Browser Origin Headers

When a browser makes a request, it sends an `Origin` header. The origin includes:
- Protocol (http/https)
- Hostname (domain or IP)
- **Port** (if not default)

| Access Method | Frontend URL | Origin Header Sent |
|---------------|-------------|-------------------|
| `npm run dev` | `http://47.109.72.216:3000` | `http://47.109.72.216:3000` |
| nginx (port 80) | `http://47.109.72.216` | `http://47.109.72.216` (no port!) |
| nginx (port 443) | `https://47.109.72.216` | `https://47.109.72.216` (no port!) |

### Spring CORS Pattern Matching

The old CORS configuration had:
```java
"http://47.109.72.216:*"
```

This pattern expects:
- ✅ `http://47.109.72.216:3000` (matches - has explicit port)
- ✅ `http://47.109.72.216:8080` (matches - has explicit port)
- ❌ `http://47.109.72.216` (does NOT match - no explicit port)

## The Fix

Updated `CorsConfig.java` to include all three variations:

```java
allowedOriginPatterns(
    "http://localhost:*",
    "http://127.0.0.1:*",
    "https://localhost:*",
    "https://127.0.0.1:*",
    "http://47.109.72.216:*",        // With explicit port
    "http://47.109.72.216",          // Without port (nginx on port 80)
    "http://47.109.72.216:80")       // Explicit port 80
```

## Why Nginx Doesn't Need CORS Headers

**Important:** Nginx does NOT need CORS headers because:

1. **Nginx serves static files** (HTML, CSS, JS)
2. **Browser loads these files** from the same origin as the page
3. **API calls go to the backend** at `http://47.109.72.216:8080/api`
4. **Only the backend needs CORS** - it receives cross-origin requests from the frontend

### Request Flow

```
┌─────────────────┐
│  Browser        │
│  Origin:        │
│  http://47...   │
└────────┬────────┘
         │
    ┌────┴────────────────────────┐
    │                             │
    ▼                             ▼
┌─────────┐                  ┌─────────┐
│  Nginx  │                  │ Backend │
│  :80    │                  │  :8080  │
│         │                  │         │
│ Serves  │  API Call ──────▶│ Needs   │
│ static  │◀─────────────────│ CORS    │
│ files   │   Response       │ Config  │
└─────────┘                  └─────────┘
```

## How to Deploy the Fix

### 1. Rebuild the Backend
```bash
cd backend
./mvnw clean package -DskipTests
```

### 2. Deploy to Remote Server
```bash
# Upload JAR
scp backend/target/customers-0.0.1-SNAPSHOT.jar root@47.109.72.216:/opt/customers-backend/

# SSH to server
ssh root@47.109.72.216

# Restart backend
pkill -f customers-backend.jar
nohup java -jar /opt/customers-backend/customers-backend.jar > /var/log/customers-backend.log 2>&1 &
```

### 3. Verify CORS is Working
```bash
# Test CORS from browser console
fetch('http://47.109.72.216:8080/api/health/simple')

# Check backend logs
tail -f /var/log/customers-backend.log

# Look for:
# DEBUG o.s.web.cors.DefaultCorsProcessor - Allow: 'http://47.109.72.216' origin is allowed
```

## Testing Checklist

- [ ] Backend rebuilt with new CORS config
- [ ] Backend deployed and restarted on server
- [ ] Frontend built with correct `NEXT_PUBLIC_API_URL`
- [ ] Nginx configured to serve static files
- [ ] Test nginx-served frontend in browser
- [ ] Check browser DevTools Network tab for CORS errors
- [ ] Verify API calls succeed

## Why the Two Methods Behave Differently

### Method 1: npm run dev on Server
```
Browser → http://47.109.72.216:3000 (Origin with port)
         → API: http://47.109.72.216:8080/api
         → CORS Pattern: http://47.109.72.216:* MATCHES! ✅
```

### Method 2: Nginx Static Files
```
Browser → http://47.109.72.216 (Origin without port)
         → API: http://47.109.72.216:8080/api
         → CORS Pattern: http://47.109.72.216:* DOESN'T MATCH ❌
```

### With Our Fix
```
Browser → http://47.109.72.216 (Origin without port)
         → API: http://47.109.72.216:8080/api
         → CORS Pattern: http://47.109.72.216 MATCHES! ✅
```

## Common Mistakes

### ❌ Mistake 1: Adding CORS headers to nginx
```nginx
# NOT NEEDED - Only backend needs CORS
add_header Access-Control-Allow-Origin "*";
```
**Why wrong:** CORS is for API requests, not static files. The backend handles CORS.

### ❌ Mistake 2: Using `*` for CORS with credentials
```java
// DANGEROUS - Don't do this with credentials
allowedOrigins("*")
allowCredentials(true)
```
**Why wrong:** Browsers will block this. Can't use wildcard with credentials.

### ❌ Mistake 3: Forgetting to rebuild backend
```bash
# Editing CorsConfig.java without rebuilding
# Old JAR still running with old CORS config
```
**Why wrong:** Changes are only in source code, not in deployed JAR.

### ❌ Mistake 4: Wrong environment variable when building
```bash
# Building for production with local API URL
NEXT_PUBLIC_API_URL=http://localhost:8080/api npm run build
```
**Why wrong:** Environment variables are baked in at build time!

## Additional Resources

- [Spring Security CORS Documentation](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)
- [MDN CORS Guide](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
- [Next.js Environment Variables](https://nextjs.org/docs/basic-features/environment-variables)

## Summary

The issue was that Spring Security's CORS pattern `http://47.109.72.216:*` didn't match the origin header `http://47.109.72.216` (without port) that browsers send when accessing nginx on port 80.

**The fix:** Add explicit entries for origins without ports to the allowed origins list.

**The result:** Both `npm run dev` and nginx-served static files will work correctly!
