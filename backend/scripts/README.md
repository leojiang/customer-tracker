# Database Backup and Restore Scripts

This directory contains scripts for backing up and restoring the Customer Tracker MySQL database.

## 📁 Scripts Overview

### 1. `backup-db.sh`
Creates a compressed backup of the customers database.

**Usage:**
```bash
./backup-db.sh [optional_backup_name]
```

**Examples:**
```bash
# Auto-generated backup name (with timestamp)
./backup-db.sh

# Custom backup name
./backup-db.sh customers_before_migration
./backup-db.sh backup_2024_01_15
```

**Features:**
- ✅ Creates timestamped backups automatically
- ✅ Compresses backups using gzip
- ✅ Keeps only the 10 most recent backups (auto-cleanup)
- ✅ Detailed logging to `backups/backup.log`
- ✅ Pre-flight checks (MySQL client, connection)
- ✅ Displays file size and recent backups after completion

**Output:**
- Backup file: `backups/customers_YYYYMMDD_HHMMSS.sql.gz`
- Log file: `backups/backup.log`

---

### 2. `restore-db.sh`
Restores the database from a backup file.

**Usage:**
```bash
./restore-db.sh <backup_file>
```

**Examples:**
```bash
# Restore from a specific file
./restore-db.sh customers_20240115_143022.sql.gz

# Restore using just the filename (script searches in backups/ directory)
./restore-db.sh customers_20240115_143022.sql.gz
```

**Features:**
- ✅ Creates pre-restore backup automatically
- ✅ Drops and recreates database for clean restore
- ✅ Supports both .sql and .sql.gz files
- ✅ Confirmation prompt before restore
- ✅ Detailed logging to `backups/restore.log`
- ✅ Post-restore verification
- ✅ Lists available backups if file not found

**Safety Features:**
- ⚠️  Requires explicit confirmation before proceeding
- 📦 Creates automatic backup before restore
- ✅ Verifies restore success

---

### 3. `db-manager.sh`
Interactive menu-driven tool for all database operations.

**Usage:**
```bash
./db-manager.sh
```

**Features:**
- 📋 **Option 1:** Backup Database
- 📥 **Option 2:** Restore Database
- 📊 **Option 3:** List All Backups
- 🗑️  **Option 4:** Delete Backup
- 🔍 **Option 5:** View Backup Info (with preview)
- 🚪 **Option 0:** Exit

**Benefits:**
- User-friendly interactive interface
- Color-coded output for better readability
- No need to remember commands or filenames
- Quick access to all operations

---

## 🚀 Quick Start

### First Time Setup

1. **Ensure MySQL client is installed:**
   ```bash
   # macOS
   brew install mysql-client

   # Ubuntu/Debian
   sudo apt-get install mysql-client

   # Verify installation
   which mysqldump
   which mysql
   ```

2. **Make scripts executable** (if not already):
   ```bash
   chmod +x scripts/*.sh
   ```

3. **Create your first backup:**
   ```bash
   cd backend/scripts
   ./backup-db.sh
   ```

### Daily Operations

**Quick Backup:**
```bash
cd backend/scripts
./backup-db.sh
```

**Interactive Management:**
```bash
cd backend/scripts
./db-manager.sh
```

**Restore from Backup:**
```bash
cd backend/scripts
./restore-db.sh customers_20240115_143022.sql.gz
```

---

## 📂 Directory Structure

```
backend/
├── scripts/
│   ├── backup-db.sh          # Backup script
│   ├── restore-db.sh         # Restore script
│   ├── db-manager.sh         # Interactive menu
│   └── README.md             # This file
└── backups/                  # Created automatically
    ├── customers_20240115_143022.sql.gz
    ├── customers_20240116_090315.sql.gz
    ├── backup.log            # Backup log file
    └── restore.log           # Restore log file
```

---

## ⚙️ Configuration

Database credentials are configured in `backup-db.sh` and `restore-db.sh`:

```bash
DB_NAME="customers"
DB_USER="root"
DB_PASS="123456"
DB_HOST="localhost"
DB_PORT="3306"
```

**⚠️ Important:** These should match the configuration in `src/main/resources/application.yml`.

---

## 💡 Use Cases

### 1. Before Major Changes
```bash
# Create backup before schema changes
./backup-db.sh before_schema_change
```

### 2. Scheduled Backups
Add to crontab for automated daily backups:
```bash
# Open crontab
crontab -e

# Add daily backup at 2 AM
0 2 * * * cd /path/to/backend/scripts && ./backup-db.sh
```

### 3. Development/Testing
```bash
# Backup production data
./backup-db.sh prod_backup

# Restore to development environment
./restore-db.sh prod_backup_20240115.sql.gz
```

### 4. Data Migration
```bash
# Backup before migration
./backup-db.sh pre_migration

# Run migration...

# If issues occur, restore
./restore-db.sh pre_migration_20240115.sql.gz
```

### 5. Disaster Recovery
```bash
# List available backups
./db-manager.sh

# Choose and restore the most recent backup
# (Use interactive menu option 2)
```

---

## 🔍 Troubleshooting

### "mysqldump command not found"
**Solution:** Install MySQL client
```bash
# macOS
brew install mysql-client

# Ubuntu/Debian
sudo apt-get install mysql-client
```

### "Cannot connect to MySQL database"
**Possible causes:**
1. MySQL server not running
   ```bash
   # Start MySQL
   sudo systemctl start mysql  # Linux
   brew services start mysql    # macOS
   ```

2. Wrong credentials in script
   - Check `application.yml` for correct credentials
   - Update scripts with matching credentials

3. Database doesn't exist
   ```bash
   mysql -u root -p
   CREATE DATABASE customers;
   ```

### "Permission denied" when running scripts
**Solution:** Make scripts executable
```bash
chmod +x scripts/*.sh
```

### "No backups found"
**Solution:** Create a backup first
```bash
./backup-db.sh
```

---

## 📊 Backup Details

### What Gets Backed Up?

The `mysqldump` command includes:
- ✅ All tables and data
- ✅ Stored routines (procedures/functions)
- ✅ Triggers
- ✅ Events
- ✅ Database structure (schema)

### Backup Options Used:
```bash
--single-transaction      # InnoDB consistent backup without locking
--routines                # Include stored procedures
--triggers                # Include triggers
--events                  # Include events
--quick                   # Retrieve rows one at a time
--lock-tables=false       # Don't lock tables (with single-transaction)
```

### Compression:
- Backups are compressed using **gzip**
- Typical compression ratio: 10:1
- A 100 MB database becomes ~10 MB backup

---

## 🔐 Security Best Practices

1. **Protect Backup Files**
   ```bash
   # Restrict backup directory access
   chmod 700 backups/
   chmod 600 backups/*.sql.gz
   ```

2. **Store Credentials Securely**
   - Consider using environment variables
   - Don't commit backup files to version control
   - Add `backups/` to `.gitignore`

3. **Offsite Backups**
   ```bash
   # Copy to remote server
   scp backups/customers_*.sql.gz user@remote:/backups/

   # Or use rsync
   rsync -avz backups/ user@remote:/backups/
   ```

4. **Encrypt Sensitive Backups**
   ```bash
   # Encrypt backup
   gzip -c customers.sql | openssl enc -aes-256-cbc -e > customers.sql.gz.enc

   # Decrypt backup
   openssl enc -aes-256-cbc -d -in customers.sql.gz.enc | gunzip > customers.sql
   ```

---

## 📝 Maintenance

### Auto-Cleanup
The backup script automatically keeps only the **10 most recent backups**. To change this limit:

Edit `backup-db.sh`:
```bash
# Change this value
if [ "${BACKUP_COUNT}" -gt 10 ]; then
    # To keep more backups, change 10 to your desired number
fi
```

### Manual Cleanup
```bash
# Remove specific backup
rm backups/customers_20240115.sql.gz

# Remove backups older than 30 days
find backups/ -name "*.sql.gz" -mtime +30 -delete
```

---

## 📞 Support

For issues or questions:
1. Check the log files: `backups/backup.log` and `backups/restore.log`
2. Review the troubleshooting section above
3. Verify database credentials match `application.yml`
4. Ensure MySQL server is running

---

## 📄 License

These scripts are part of the Customer Tracker project.

---

**Last Updated:** 2024-01-28
**Version:** 1.0.0
