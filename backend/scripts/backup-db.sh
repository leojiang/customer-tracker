#!/bin/bash

################################################################################
# Database Backup Script for Customer Tracker
# Usage: ./backup-db.sh [backup_name]
#   backup_name: Optional. If not provided, uses timestamp as backup name
################################################################################

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Database configuration
DB_NAME="customers"
DB_USER="root"
DB_PASS="123456"
DB_HOST="localhost"
DB_PORT="3306"

# Backup directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${SCRIPT_DIR}/../backups"
mkdir -p "${BACKUP_DIR}"

# Backup name (timestamp or provided argument)
BACKUP_NAME="${1:-customers_$(date +%Y%m%d_%H%M%S)}"
BACKUP_FILE="${BACKUP_DIR}/${BACKUP_NAME}.sql"

# Log file
LOG_FILE="${BACKUP_DIR}/backup.log"

################################################################################
# Helper Functions
################################################################################

log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "${LOG_FILE}"
}

log_success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%M:%S')]${NC} $1" | tee -a "${LOG_FILE}"
}

log_error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "${LOG_FILE}"
}

log_warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "${LOG_FILE}"
}

print_header() {
    echo ""
    echo "=========================================="
    echo "  Customer Tracker Database Backup"
    echo "=========================================="
    echo ""
}

################################################################################
# Pre-flight Checks
################################################################################

check_mysql_client() {
    if ! command -v mysqldump &> /dev/null; then
        log_error "mysqldump command not found. Please install MySQL client."
        log_error "  macOS: brew install mysql-client"
        log_error "  Ubuntu/Debian: sudo apt-get install mysql-client"
        exit 1
    fi
}

check_mysql_connection() {
    log "Checking MySQL connection..."
    if ! mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" -e "USE ${DB_NAME};" &> /dev/null; then
        log_error "Cannot connect to MySQL database '${DB_NAME}'"
        log_error "  Host: ${DB_HOST}:${DB_PORT}"
        log_error "  User: ${DB_USER}"
        log_error "  Please check your database configuration in application.yml"
        exit 1
    fi
    log_success "MySQL connection successful"
}

################################################################################
# Backup Functions
################################################################################

perform_backup() {
    log "Starting backup of database '${DB_NAME}'..."
    log "Backup file: ${BACKUP_FILE}"

    # Perform the backup
    if mysqldump -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        --quick \
        --lock-tables=false \
        "${DB_NAME}" > "${BACKUP_FILE}" 2>> "${LOG_FILE}"; then

        # Compress the backup
        gzip "${BACKUP_FILE}"
        BACKUP_FILE="${BACKUP_FILE}.gz"

        # Get file size
        FILE_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)

        log_success "Backup completed successfully!"
        log "  Backup file: ${BACKUP_FILE}"
        log "  File size: ${FILE_SIZE}"

        # List recent backups
        log ""
        log "Recent backups (showing last 5):"
        ls -lt "${BACKUP_DIR}"/*.sql.gz 2>/dev/null | head -5 | awk '{print "  " $9 " (" $5 ")"}' || log "  No previous backups found"

    else
        log_error "Backup failed! Check ${LOG_FILE} for details."
        exit 1
    fi
}

cleanup_old_backups() {
    log "Cleaning up old backups (keeping last 10)..."

    # Count backup files
    BACKUP_COUNT=$(ls -1 "${BACKUP_DIR}"/*.sql.gz 2>/dev/null | wc -l)

    if [ "${BACKUP_COUNT}" -gt 10 ]; then
        # Remove oldest backups, keeping only the 10 most recent
        ls -t "${BACKUP_DIR}"/*.sql.gz | tail -n +11 | xargs rm -f
        log_success "Removed $((BACKUP_COUNT - 10)) old backup(s)"
    else
        log "No cleanup needed (found ${BACKUP_COUNT} backup(s))"
    fi
}

################################################################################
# Main Execution
################################################################################

main() {
    print_header

    # Pre-flight checks
    check_mysql_client
    check_mysql_connection

    # Perform backup
    perform_backup

    # Cleanup old backups
    cleanup_old_backups

    echo ""
    log_success "Database backup process completed successfully!"
    echo ""
}

# Run main function
main "$@"
