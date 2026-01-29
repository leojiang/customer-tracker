# Database Backup Scripts - Quick Start Guide

## Prerequisites

### 1. Install MySQL Client

**macOS:**
```bash
brew install mysql-client
```

**Ubuntu/Debian:**
```bash
sudo apt-get install mysql-client
```

**Verify installation:**
```bash
which mysqldump
which mysql
```

### 2. Ensure MySQL Server is Running

**macOS:**
```bash
brew services start mysql
```

**Linux:**
```bash
sudo systemctl start mysql
```

**Test connection:**
```bash
mysql -u root -p
# Enter password (default: 123456)
```

---

## Basic Usage

### Create a Backup
```bash
cd backend/scripts
./backup-db.sh
```

### Create a Named Backup
```bash
./backup-db.sh before_migration
```

### Interactive Menu (Recommended)
```bash
./db-manager.sh
```

### Restore from Backup
```bash
./restore-db.sh customers_20240129_092000.sql.gz
```

---

## Files Created

```
backend/scripts/
├── backup-db.sh        # Create backups
├── restore-db.sh       # Restore backups
├── db-manager.sh       # Interactive menu
├── README.md           # Detailed documentation
└── QUICKSTART.md       # This file

backend/backups/        # Auto-created
├── customers_*.sql.gz  # Backup files
├── backup.log         # Backup logs
└── restore.log        # Restore logs
```

---

## Common Commands

### List all backups:
```bash
ls -lh backups/*.sql.gz
```

### Check backup size:
```bash
du -h backups/*.sql.gz
```

### View backup contents:
```bash
gunzip -c backups/customers_20240129.sql.gz | head -50
```

### Delete old backups:
```bash
# Remove backups older than 30 days
find backups/ -name "*.sql.gz" -mtime +30 -delete
```

---

## Troubleshooting

**Problem:** "mysqldump command not found"
**Solution:** Install MySQL client (see Prerequisites above)

**Problem:** "Cannot connect to MySQL database"
**Solutions:**
1. Start MySQL server
2. Check credentials in application.yml
3. Verify database exists:
   ```bash
   mysql -u root -p
   SHOW DATABASES;
   ```

**Problem:** "Permission denied"
**Solution:**
```bash
chmod +x scripts/*.sh
```

---

## Best Practices

1. **Daily Backups:**
   ```bash
   # Add to crontab
   crontab -e
   # Add: 0 2 * * * cd /path/to/backend/scripts && ./backup-db.sh
   ```

2. **Before Major Changes:**
   ```bash
   ./backup-db.sh before_migration
   ```

3. **Protect Backup Files:**
   ```bash
   chmod 700 backups/
   chmod 600 backups/*.sql.gz
   ```

4. **Offsite Storage:**
   ```bash
   scp backups/*.sql.gz user@remote:/backup/location/
   ```

---

## Need More Help?

See `README.md` for detailed documentation.
