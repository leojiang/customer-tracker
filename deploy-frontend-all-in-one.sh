#!/bin/bash
# All-in-one deployment: uploads files AND configures Nginx

set -e

# Configuration
ECS_USER="root"
ECS_HOST="47.109.72.216"
LOCAL_FRONTEND_PATH="./frontend"
NGINX_SITE_NAME="customer-tracker-frontend"

echo "🚀 All-in-One Deployment: Frontend + Nginx Configuration"

# Step 1: Build static export
echo ""
echo "📦 Step 1/4: Building static export..."
cd "$LOCAL_FRONTEND_PATH"

if [ ! -d "node_modules" ]; then
    echo "📥 Installing dependencies..."
    npm install
fi

echo "🔨 Building for production..."
NODE_ENV=production npm run build

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

# Step 2: Create deployment package
echo ""
echo "📁 Step 2/4: Creating deployment package..."
COPYFILE_DISABLE=1 tar -czf ../frontend-static.tar.gz out/ 2>/dev/null || tar -czf ../frontend-static.tar.gz out/

# Step 3: Upload to server
echo ""
echo "📤 Step 3/4: Uploading to server..."
scp ../frontend-static.tar.gz ${ECS_USER}@${ECS_HOST}:/tmp/

# Step 4: Deploy and configure Nginx
echo ""
echo "🔧 Step 4/4: Deploying and configuring Nginx..."
ssh ${ECS_USER}@${ECS_HOST} << 'ENDSSH'
    echo "📁 Deploying frontend files..."
    sudo mkdir -p /var/www/customer-tracker-frontend

    # Backup existing version
    if [ -d "/var/www/customer-tracker-frontend/out" ]; then
        sudo mv /var/www/customer-tracker-frontend/out /var/www/customer-tracker-frontend/out.backup.$(date +%Y%m%d_%H%M%S)
    fi

    # Extract files
    sudo tar -xzf /tmp/frontend-static.tar.gz -C /var/www/customer-tracker-frontend/
    sudo chmod -R 755 /var/www/customer-tracker-frontend


    # Test Nginx configuration
    echo "🧪 Testing Nginx configuration..."
    sudo nginx -t

    if [ $? -eq 0 ]; then
        echo "✅ Nginx configuration is valid!"
        echo "🔄 Reloading Nginx..."
        sudo systemctl reload nginx
        echo "✅ Nginx reloaded successfully!"
    else
        echo "❌ Nginx configuration test failed!"
        echo "📋 Configuration file:"
        sudo cat /etc/nginx/nginx.conf
        exit 1
    fi

    # Cleanup
    rm /tmp/frontend-static.tar.gz /tmp/nginx-static.conf

    echo ""
    echo "✅ Deployment completed!"
    echo "📊 Deployment summary:"
    echo "   - Frontend files: /var/www/customer-tracker-frontend/out"
    echo "   - Nginx config: /etc/nginx/sites-available/$NGINX_SITE_NAME"
ENDSSH

# Cleanup local tarball
rm ../frontend-static.tar.gz

echo ""
echo "🎉 All done!"
echo "🌐 Your site should now be live at: http://$ECS_HOST"
echo ""
echo "🔍 To verify:"
echo "   curl -I http://$ECS_HOST"
echo ""
echo "📝 To view logs:"
echo "   ssh root@$ECS_HOST 'sudo tail -f /var/log/nginx/access.log'"
