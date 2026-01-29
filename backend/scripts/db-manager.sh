#!/bin/bash

################################################################################
# Database Management Script for Customer Tracker
# Provides an interactive menu for backup and restore operations
################################################################################

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${SCRIPT_DIR}/../backups"
mkdir -p "${BACKUP_DIR}"

################################################################################
# Helper Functions
################################################################################

print_banner() {
    clear
    echo -e "${CYAN}${BOLD}"
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║     Customer Tracker Database Management Tool           ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

print_menu() {
    echo ""
    echo -e "${BOLD}Select an option:${NC}"
    echo ""
    echo -e "  ${GREEN}1${NC}) Backup Database"
    echo -e "  ${GREEN}2${NC}) Restore Database"
    echo -e "  ${GREEN}3${NC}) List Backups"
    echo -e "  ${GREEN}4${NC}) Delete Backup"
    echo -e "  ${GREEN}5${NC}) View Backup Info"
    echo ""
    echo -e "  ${YELLOW}0${NC}) Exit"
    echo ""
    echo -ne "${BOLD}Enter your choice [0-5]: ${NC}"
}

backup_database() {
    echo ""
    echo -e "${BOLD}═════════════════════════════════════════════════════${NC}"
    echo -e "${BOLD}  Backup Database${NC}"
    echo -e "${BOLD}═════════════════════════════════════════════════════${NC}"
    echo ""

    # Optional custom name
    read -p "Enter backup name (press Enter for auto-generated): " CUSTOM_NAME

    echo ""
    "${SCRIPT_DIR}/backup-db.sh" "${CUSTOM_NAME}"
}

restore_database() {
    echo ""
    echo -e "${BOLD}═════════════════════════════════════════════════════${NC}"
    echo -e "${BOLD}  Restore Database${NC}"
    echo -e "${BOLD}═════════════════════════════════════════════════════${NC}"
    echo ""

    # List available backups
    BACKUPS=($(ls -t "${BACKUP_DIR}"/*.sql.gz 2>/dev/null))

    if [ ${#BACKUPS[@]} -eq 0 ]; then
        echo -e "${RED}No backups found!${NC} Please create a backup first."
        return
    fi

    echo "Available backups:"
    echo ""
    for i in "${!BACKUPS[@]}"; do
        INDEX=$((i + 1))
        BACKUP_FILE="${BACKUPS[$i]}"
        FILE_NAME=$(basename "${BACKUP_FILE}")
        FILE_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
        FILE_DATE=$(stat -f "%Sm" -t "%Y-%m-%d %H:%M:%S" "${BACKUP_FILE}" 2>/dev/null || stat -c "%y" "${BACKUP_FILE}" 2>/dev/null | cut -d'.' -f1)
        echo -e "  ${CYAN}${INDEX})${NC} ${FILE_NAME}"
        echo -e "      Size: ${FILE_SIZE} | Date: ${FILE_DATE}"
        echo ""
    done

    echo ""
    read -p "Select backup to restore [1-${#BACKUPS[@]}]: " CHOICE

    if [[ "${CHOICE}" =~ ^[0-9]+$ ]] && [ "${CHOICE}" -ge 1 ] && [ "${CHOICE}" -le ${#BACKUPS[@]} ]; then
        SELECTED_BACKUP="${BACKUPS[$((CHOICE - 1))]}"
        echo ""
        echo -e "${YELLOW}Selected backup: $(basename "${SELECTED_BACKUP}")${NC}"
        echo ""
        "${SCRIPT_DIR}/restore-db.sh" "${SELECTED_BACKUP}"
    else
        echo -e "${RED}Invalid selection!${NC}"
    fi
}

list_backups() {
    echo ""
    echo -e "${BOLD}═════════════════════════════════════════════════════${NC}"
    echo -e "${BOLD}  Available Backups${NC}"
    echo -e "${BOLD}═════════════════════════════════════════════════════${NC}"
    echo ""

    BACKUPS=($(ls -t "${BACKUP_DIR}"/*.sql.gz 2>/dev/null))

    if [ ${#BACKUPS[@]} -eq 0 ]; then
        echo -e "${YELLOW}No backups found.${NC}"
        echo ""
        return
    fi

    printf "${BOLD}%-4s %-40s %-10s %-20s${NC}\n" "No." "Filename" "Size" "Date"
    echo "────────────────────────────────────────────────────────────────"

    for i in "${!BACKUPS[@]}"; do
        INDEX=$((i + 1))
        BACKUP_FILE="${BACKUPS[$i]}"
        FILE_NAME=$(basename "${BACKUP_FILE}")
        FILE_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
        FILE_DATE=$(stat -f "%Sm" -t "%Y-%m-%d %H:%M" "${BACKUP_FILE}" 2>/dev/null || stat -c "%y" "${BACKUP_FILE}" 2>/dev/null | cut -d'.' -f1 | cut -d'T' -f1-2)

        # Truncate filename if too long
        if [ ${#FILE_NAME} -gt 40 ]; then
            FILE_NAME="${FILE_NAME:0:37}..."
        fi

        printf "%-4s %-40s %-10s %-20s\n" "${INDEX}." "${FILE_NAME}" "${FILE_SIZE}" "${FILE_DATE}"
    done

    echo ""
    echo -e "${BOLD}Total backups: ${CYAN}${#BACKUPS[@]}${NC}"
    echo ""
}

delete_backup() {
    echo ""
    echo -e "${BOLD}═════════════════════════════════════════════════════${NC}"
    echo -e "${BOLD}  Delete Backup${NC}"
    echo -e "${BOLD}══════════════════════════════════════════════════════${NC}"
    echo ""

    BACKUPS=($(ls -t "${BACKUP_DIR}"/*.sql.gz 2>/dev/null))

    if [ ${#BACKUPS[@]} -eq 0 ]; then
        echo -e "${YELLOW}No backups found!${NC}"
        echo ""
        return
    fi

    echo "Available backups:"
    echo ""
    for i in "${!BACKUPS[@]}"; do
        INDEX=$((i + 1))
        BACKUP_FILE="${BACKUPS[$i]}"
        FILE_NAME=$(basename "${BACKUP_FILE}")
        echo -e "  ${CYAN}${INDEX})${NC} ${FILE_NAME}"
    done

    echo ""
    read -p "Select backup to delete [1-${#BACKUPS[@]}]: " CHOICE

    if [[ "${CHOICE}" =~ ^[0-9]+$ ]] && [ "${CHOICE}" -ge 1 ] && [ "${CHOICE}" -le ${#BACKUPS[@]} ]; then
        SELECTED_BACKUP="${BACKUPS[$((CHOICE - 1))]}"
        FILE_NAME=$(basename "${SELECTED_BACKUP}")
        echo ""
        echo -e "${YELLOW}Selected: ${FILE_NAME}${NC}"
        echo ""
        read -p "Are you sure you want to delete this backup? (yes/no): " CONFIRM

        if [ "${CONFIRM}" == "yes" ]; then
            rm -f "${SELECTED_BACKUP}"
            echo -e "${GREEN}Backup deleted successfully!${NC}"
        else
            echo "Deletion cancelled."
        fi
    else
        echo -e "${RED}Invalid selection!${NC}"
    fi
}

view_backup_info() {
    echo ""
    echo -e "${BOLD}═════════════════════════════════════════════════════${NC}"
    echo -e "${BOLD}  Backup Information${NC}"
    echo -e "${BOLD}═════════════════════════════════════════════════════${NC}"
    echo ""

    BACKUPS=($(ls -t "${BACKUP_DIR}"/*.sql.gz 2>/dev/null))

    if [ ${#BACKUPS[@]} -eq 0 ]; then
        echo -e "${YELLOW}No backups found!${NC}"
        echo ""
        return
    fi

    echo "Available backups:"
    echo ""
    for i in "${!BACKUPS[@]}"; do
        INDEX=$((i + 1))
        BACKUP_FILE="${BACKUPS[$i]}"
        FILE_NAME=$(basename "${BACKUP_FILE}")
        echo -e "  ${CYAN}${INDEX})${NC} ${FILE_NAME}"
    done

    echo ""
    read -p "Select backup to view [1-${#BACKUPS[@]}]: " CHOICE

    if [[ "${CHOICE}" =~ ^[0-9]+$ ]] && [ "${CHOICE}" -ge 1 ] && [ "${CHOICE}" -le ${#BACKUPS[@]} ]; then
        SELECTED_BACKUP="${BACKUPS[$((CHOICE - 1))]}"
        echo ""

        # Display backup information
        FILE_NAME=$(basename "${SELECTED_BACKUP}")
        FILE_SIZE=$(du -h "${SELECTED_BACKUP}" | cut -f1)
        FILE_SIZE_BYTES=$(du -b "${SELECTED_BACKUP}" | cut -f1)
        FILE_DATE=$(stat -f "%Sm" -t "%Y-%m-%d %H:%M:%S" "${SELECTED_BACKUP}" 2>/dev/null || stat -c "%y" "${SELECTED_BACKUP}" 2>/dev/null | cut -d'.' -f1)
        FILE_PERMS=$(stat -f "%Sp" "${SELECTED_BACKUP}" 2>/dev/null || stat -c "%a" "${SELECTED_BACKUP}" 2>/dev/null)

        echo -e "${BOLD}Backup Details:${NC}"
        echo "────────────────────────────────────────────────────────────────"
        echo -e "  ${BOLD}Filename:${NC}        ${FILE_NAME}"
        echo -e "  ${BOLD}Path:${NC}            ${SELECTED_BACKUP}"
        echo -e "  ${BOLD}Size:${NC}            ${FILE_SIZE} (${FILE_SIZE_BYTES} bytes)"
        echo -e "  ${BOLD}Created:${NC}         ${FILE_DATE}"
        echo -e "  ${BOLD}Permissions:${NC}     ${FILE_PERMS}"
        echo ""

        # Display SQL contents (first 20 lines)
        echo -e "${BOLD}Preview (first 20 lines):${NC}"
        echo "────────────────────────────────────────────────────────────────"
        gunzip -c "${SELECTED_BACKUP}" 2>/dev/null | head -20
        echo ""
        echo "────────────────────────────────────────────────────────────────"
        echo ""
    else
        echo -e "${RED}Invalid selection!${NC}"
    fi
}

################################################################################
# Main Loop
################################################################################

main() {
    while true; do
        print_banner
        print_menu
        read -r CHOICE

        case "${CHOICE}" in
            1)
                backup_database
                ;;
            2)
                restore_database
                ;;
            3)
                list_backups
                ;;
            4)
                delete_backup
                ;;
            5)
                view_backup_info
                ;;
            0|q|Q)
                echo ""
                echo -e "${GREEN}Goodbye!${NC}"
                echo ""
                exit 0
                ;;
            *)
                echo ""
                echo -e "${RED}Invalid option! Please try again.${NC}"
                ;;
        esac

        echo ""
        read -p "Press Enter to continue..." DUMMY
    done
}

# Run main function
main
