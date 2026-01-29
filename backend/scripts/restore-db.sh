#!/bin/bash

################################################################################
# Database Restore Script for Customer Tracker
# Usage: ./restore-db.sh <backup_file>
#   backup_file: Path to the backup file (.sql or .sql.gz)
################################################################################

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Database configuration
DB_NAME="customers_restore"
DB_USER="root"
DB_PASS="123456"
DB_HOST="localhost"
DB_PORT="3306"

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${SCRIPT_DIR}/../backups"

# Log file
LOG_FILE="${BACKUP_DIR}/restore.log"
mkdir -p "${BACKUP_DIR}"

################################################################################
# Helper Functions
################################################################################

log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "${LOG_FILE}"
}

log_success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "${LOG_FILE}"
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
    echo "  Customer Tracker Database Restore"
    echo "=========================================="
    echo ""
}

################################################################################
# Pre-flight Checks
################################################################################

check_mysql_client() {
    if ! command -v mysql &> /dev/null; then
        log_error "mysql command not found. Please install MySQL client."
        log_error "  macOS: brew install mysql-client"
        log_error "  Ubuntu/Debian: sudo apt-get install mysql-client"
        exit 1
    fi
}

check_backup_file() {
    if [ -z "${BACKUP_FILE}" ]; then
        log_error "No backup file specified!"
        log_error "Usage: $0 <backup_file>"
        log_error ""
        log_error "Available backups in ${BACKUP_DIR}:"
        ls -lh "${BACKUP_DIR}"/*.sql.gz 2>/dev/null | awk '{print "  " $9 " (" $5 ")"}' || log "  No backups found"
        exit 1
    fi

    # Check if file exists
    if [ ! -f "${BACKUP_FILE}" ]; then
        # Try to find it in the backup directory
        if [ -f "${BACKUP_DIR}/${BACKUP_FILE}" ]; then
            BACKUP_FILE="${BACKUP_DIR}/${BACKUP_FILE}"
        else
            log_error "Backup file not found: ${BACKUP_FILE}"
            log_error ""
            log_error "Available backups in ${BACKUP_DIR}:"
            ls -lh "${BACKUP_DIR}"/*.sql.gz 2>/dev/null | awk '{print "  " $9 " (" $5 ")"}' || log "  No backups found"
            exit 1
        fi
    fi

    log_success "Backup file found: ${BACKUP_FILE}"
}

check_mysql_connection() {
    log "Checking MySQL connection..."
    if ! mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" -e "SELECT 1;" &> /dev/null; then
        log_error "Cannot connect to MySQL server"
        log_error "  Host: ${DB_HOST}:${DB_PORT}"
        log_error "  User: ${DB_USER}"
        log_error "  Please check your database configuration in application.yml"
        exit 1
    fi
    log_success "MySQL connection successful"
}

confirm_restore() {
    # Get file size
    FILE_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
    FILE_DATE=$(stat -f "%Sm" -t "%Y-%m-%d %H:%M:%S" "${BACKUP_FILE}" 2>/dev/null || stat -c "%y" "${BACKUP_FILE}" 2>/dev/null | cut -d'.' -f1)

    log_warning "⚠️  WARNING: This will REPLACE all data in database '${DB_NAME}'!"
    log "  Backup file: ${BACKUP_FILE}"
    log "  File size: ${FILE_SIZE}"
    log "  File date: ${FILE_DATE}"
    echo ""

    read -p "Are you sure you want to continue? (yes/no): " CONFIRM

    if [ "${CONFIRM}" != "yes" ]; then
        log "Restore cancelled by user."
        exit 0
    fi
}

################################################################################
# Restore Functions
################################################################################

create_backup_before_restore() {
    log "Creating backup before restore..."

    PRE_BACKUP="${BACKUP_DIR}/pre_restore_${DB_NAME}_$(date +%Y%m%d_%H%M%S).sql"

    if mysqldump -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        --quick \
        --lock-tables=false \
        "${DB_NAME}" > "${PRE_BACKUP}" 2>> "${LOG_FILE}"; then

        gzip "${PRE_BACKUP}"
        log_success "Pre-restore backup created: ${PRE_BACKUP}.gz"
    else
        log_warning "Failed to create pre-restore backup (continuing anyway)..."
    fi
}

drop_and_recreate_database() {
    log "Dropping and recreating database '${DB_NAME}'..."

    # Drop database
    mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" \
        -e "DROP DATABASE IF EXISTS ${DB_NAME};" 2>> "${LOG_FILE}"

    # Create database
    mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" \
        -e "CREATE DATABASE ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>> "${LOG_FILE}"

    log_success "Database recreated successfully"
}

perform_restore() {
    log "Starting database restore from ${BACKUP_FILE}..."

    # Check if file is gzipped
    if [[ "${BACKUP_FILE}" == *.gz ]]; then
        # Restore from gzipped backup
        if gunzip -c "${BACKUP_FILE}" | mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" 2>> "${LOG_FILE}"; then
            log_success "Database restored successfully from gzipped backup!"
        else
            log_error "Restore failed! Check ${LOG_FILE} for details."
            exit 1
        fi
    else
        # Restore from plain SQL file
        if mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" < "${BACKUP_FILE}" 2>> "${LOG_FILE}"; then
            log_success "Database restored successfully!"
        else
            log_error "Restore failed! Check ${LOG_FILE} for details."
            exit 1
        fi
    fi
}

verify_restore() {
    log "Verifying restore..."

    # Check if database has tables
    TABLE_COUNT=$(mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" \
        -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '${DB_NAME}';" \
        -s -N 2>> "${LOG_FILE}")

    if [ "${TABLE_COUNT}" -gt 0 ]; then
        log_success "Database verification successful! Found ${TABLE_COUNT} table(s)."
    else
        log_warning "Database appears to be empty (0 tables found)."
    fi
}

################################################################################
# Main Execution
################################################################################

main() {
    # Check if backup file argument is provided
    BACKUP_FILE="$1"

    print_header

    # Pre-flight checks
    check_mysql_client
    check_backup_file
    check_mysql_connection
    confirm_restore

    # Optional: Create backup before restore
    if mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" \
        -e "USE ${DB_NAME};" &> /dev/null; then
        create_backup_before_restore
    fi

    # Perform restore
    drop_and_recreate_database
    perform_restore

    # Verify restore
    verify_restore

    echo ""
    log_success "Database restore process completed successfully!"
    echo ""
}

# Run main function
main "$@"
