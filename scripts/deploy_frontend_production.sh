#!/bin/bash

# Remove current build
rm -rf /root/customer-tracker/frontend/out/*


cd /root/customer-tracker/frontend
npm run build

if [ $? -eq 0 ]; then
    echo "Build succeeded!"
    echo "Backup current deployed version..."
    mkdir -p /var/www/customer-tracker-frontend/out.backup.$(date +%Y%m%d_%H%M%S)
    mv -T /var/www/customer-tracker/out/* /var/www/customer-tracker-frontend/out.backup.$(date +%Y%m%d_%H%M%S)/
    echo "copy new version to nginx folder..."
    mv -T /root/customer-tracker/frontend/out/* /var/www/customer-tracker-frontend/out/
    echo "🔄 Reloading Nginx..."
    sudo systemctl reload nginx
    echo "✅ Nginx reloaded successfully!"
else
    echo "�~]~L Build frontend package failed!"
    exit 1
fi
