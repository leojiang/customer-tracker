package com.example.customers.service;

import com.example.customers.model.CertificateType;
import com.example.customers.model.Customer;
import com.example.customers.model.CustomerStaging;
import com.example.customers.model.CustomerStaging.ImportStatus;
import com.example.customers.model.CustomerStatus;
import com.example.customers.model.EducationLevel;
import com.example.customers.repository.CustomerRepository;
import com.example.customers.repository.CustomerStagingRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for customer import/export operations.
 *
 * <p>Handles Excel file processing, staging, validation, and import confirmation workflow.
 */
@Service
public class CustomerImportService {

  private static final Logger logger = LoggerFactory.getLogger(CustomerImportService.class);

  private final CustomerRepository customerRepository;
  private final CustomerStagingRepository stagingRepository;
  private final AnalyticsRefreshService analyticsRefreshService;

  // Excel column headers (Chinese template format)
  private static final String[] HEADERS = {
    "序号", "期号", "报名时间", "发证时间", "发证机关", "姓名", "身份证", "学历", "性别", "证件类型", "电话", "地址", "业务经理", "客户类型"
  };

  // Certificate type mapping: Chinese name -> Enum name
  private static final Map<String, CertificateType> CERTIFICATE_TYPE_MAP;

  // Certificate issuer mapping: Chinese name -> Enum name
  private static final Map<String, String> CERTIFICATE_ISSUER_MAP;

  static {
    CERTIFICATE_TYPE_MAP = new HashMap<>();
    // Crane & Machinery Types (起重机械)
    CERTIFICATE_TYPE_MAP.put("Q1指挥", CertificateType.Q1_COMMAND);
    CERTIFICATE_TYPE_MAP.put("Q1指挥证", CertificateType.Q1_COMMAND);
    CERTIFICATE_TYPE_MAP.put("起重机Q1指挥", CertificateType.Q1_COMMAND);
    CERTIFICATE_TYPE_MAP.put("指挥证Q1", CertificateType.Q1_COMMAND);
    CERTIFICATE_TYPE_MAP.put("Q2流动式", CertificateType.Q2_MOBILE_CRANE);
    CERTIFICATE_TYPE_MAP.put("Q2桥式", CertificateType.Q2_BRIDGE_CRANE);
    CERTIFICATE_TYPE_MAP.put("桥式起重机Q2", CertificateType.Q2_BRIDGE_CRANE);
    CERTIFICATE_TYPE_MAP.put("Q2门式", CertificateType.Q2_GANTRY_CRANE);
    CERTIFICATE_TYPE_MAP.put("Q2门式起重机司机", CertificateType.Q2_GANTRY_CRANE);
    CERTIFICATE_TYPE_MAP.put("Q2塔式", CertificateType.Q2_TOWER_CRANE);
    CERTIFICATE_TYPE_MAP.put("Q2升降机", CertificateType.Q2_HOIST);
    // Forklift & Industrial Vehicles
    CERTIFICATE_TYPE_MAP.put("N1叉车", CertificateType.N1_FORKLIFT);
    CERTIFICATE_TYPE_MAP.put("N2观光车", CertificateType.N2_SIGHTSEEING_CAR);
    CERTIFICATE_TYPE_MAP.put("观光车N2", CertificateType.N2_SIGHTSEEING_CAR);
    // Boiler & Pressure Vessels (锅炉压力容器)
    CERTIFICATE_TYPE_MAP.put("G1工业锅炉", CertificateType.G1_INDUSTRIAL_BOILER);
    CERTIFICATE_TYPE_MAP.put("G3锅炉水处理", CertificateType.G3_BOILER_WATER_TREATMENT);
    CERTIFICATE_TYPE_MAP.put("锅炉水处理G3", CertificateType.G3_BOILER_WATER_TREATMENT);
    CERTIFICATE_TYPE_MAP.put("R1快开门式压力容器", CertificateType.R1_QUICK_OPEN_PRESSURE_VESSEL);
    CERTIFICATE_TYPE_MAP.put("快开门式压力容器操作R1", CertificateType.R1_QUICK_OPEN_PRESSURE_VESSEL);
    CERTIFICATE_TYPE_MAP.put("压力容器操作证R1", CertificateType.R1_QUICK_OPEN_PRESSURE_VESSEL);
    CERTIFICATE_TYPE_MAP.put("R2移动式压力容器", CertificateType.R2_MOBILE_PRESSURE_VESSEL);
    CERTIFICATE_TYPE_MAP.put("移动式气瓶充装R2", CertificateType.R2_MOBILE_PRESSURE_VESSEL);
    CERTIFICATE_TYPE_MAP.put("P气瓶充装", CertificateType.P_GAS_FILLING);
    // Safety Management
    CERTIFICATE_TYPE_MAP.put("A特种设备安全管理", CertificateType.A_SPECIAL_EQUIPMENT_SAFETY);
    CERTIFICATE_TYPE_MAP.put("特种设备安全员证A", CertificateType.A_SPECIAL_EQUIPMENT_SAFETY);
    CERTIFICATE_TYPE_MAP.put("特种设备安全管理A", CertificateType.A_SPECIAL_EQUIPMENT_SAFETY);
    CERTIFICATE_TYPE_MAP.put("特种设备安全管理", CertificateType.A_SPECIAL_EQUIPMENT_SAFETY);
    CERTIFICATE_TYPE_MAP.put("A 特种设备安全管理证", CertificateType.A_SPECIAL_EQUIPMENT_SAFETY);
    // Elevator Operations
    CERTIFICATE_TYPE_MAP.put("T电梯作业", CertificateType.T_ELEVATOR_OPERATION);
    // Construction Trades (建筑施工)
    CERTIFICATE_TYPE_MAP.put("建筑电工", CertificateType.CONSTRUCTION_ELECTRICIAN);
    CERTIFICATE_TYPE_MAP.put("建筑焊工", CertificateType.CONSTRUCTION_WELDER);
    CERTIFICATE_TYPE_MAP.put("建筑架子工", CertificateType.CONSTRUCTION_SCAFFOLDER);
    CERTIFICATE_TYPE_MAP.put("建筑起重机械操作", CertificateType.CONSTRUCTION_LIFTING_EQUIPMENT);
    CERTIFICATE_TYPE_MAP.put("建筑起重机械操作类", CertificateType.CONSTRUCTION_LIFTING_EQUIPMENT);
    CERTIFICATE_TYPE_MAP.put("建筑起重信号司索工", CertificateType.CONSTRUCTION_SIGNALMAN);
    CERTIFICATE_TYPE_MAP.put("建筑物料提升机司机", CertificateType.CONSTRUCTION_MATERIAL_HOIST_DRIVER);
    CERTIFICATE_TYPE_MAP.put("建筑吊篮安装拆卸工", CertificateType.CONSTRUCTION_GONDOLA_INSTALLER);
    // Electrical Operations (电工作业)
    CERTIFICATE_TYPE_MAP.put("低压电工作业", CertificateType.LOW_VOLTAGE_ELECTRICIAN);
    CERTIFICATE_TYPE_MAP.put("低压电工作业证", CertificateType.LOW_VOLTAGE_ELECTRICIAN);
    CERTIFICATE_TYPE_MAP.put("低压电工作业作业", CertificateType.LOW_VOLTAGE_ELECTRICIAN);
    CERTIFICATE_TYPE_MAP.put("焊接与热切割作业", CertificateType.WELDING_THERMAL_CUTTING);
    CERTIFICATE_TYPE_MAP.put("焊接与热切割", CertificateType.WELDING_THERMAL_CUTTING);
    CERTIFICATE_TYPE_MAP.put("高压电工作业", CertificateType.HIGH_VOLTAGE_ELECTRICIAN);
    // High-Altitude Work (高处作业)
    CERTIFICATE_TYPE_MAP.put("高处安装，维护，拆除作业", CertificateType.HIGH_ALTITUDE_INSTALLATION);
    CERTIFICATE_TYPE_MAP.put("高处维护、安装、拆除作业", CertificateType.HIGH_ALTITUDE_INSTALLATION);
    CERTIFICATE_TYPE_MAP.put("登高架设作业", CertificateType.HIGH_ALTITUDE_SCAFFOLDING);
    // Specialized Operations
    CERTIFICATE_TYPE_MAP.put("制冷与空调作业", CertificateType.REFRIGERATION_AIR_CONDITIONING);
    // Mining & Industry Safety (矿山安全作业)
    CERTIFICATE_TYPE_MAP.put("煤矿安全作业", CertificateType.COAL_MINE_SAFETY);
    CERTIFICATE_TYPE_MAP.put("金属非金属矿山安全作业", CertificateType.METAL_NONMETAL_MINE_SAFETY);
    // Petroleum & Chemical Safety (石油化工安全)
    CERTIFICATE_TYPE_MAP.put("石油天然气安全作业", CertificateType.OIL_GAS_SAFETY);
    CERTIFICATE_TYPE_MAP.put("危险化学品安全作业", CertificateType.HAZARDOUS_CHEMICALS_SAFETY);
    CERTIFICATE_TYPE_MAP.put("冶金（有色）生产安全作业", CertificateType.METALLURGY_SAFETY);
    CERTIFICATE_TYPE_MAP.put("烟花爆竹安全作业", CertificateType.FIREWORKS_SAFETY);
    // Other types
    CERTIFICATE_TYPE_MAP.put("其它", CertificateType.OTHERS);
    CERTIFICATE_TYPE_MAP.put("其他", CertificateType.OTHERS);

    // Certificate issuer mapping
    CERTIFICATE_ISSUER_MAP = new HashMap<>();
    CERTIFICATE_ISSUER_MAP.put("市场监督管理局", "MARKET_SUPERVISION_ADMINISTRATION");
    CERTIFICATE_ISSUER_MAP.put("住建局", "HOUSING_CONSTRUCTION_BUREAU");
    CERTIFICATE_ISSUER_MAP.put("住建厅", "HOUSING_CONSTRUCTION_BUREAU");
    CERTIFICATE_ISSUER_MAP.put("应急管理厅", "EMERGENCY_MANAGEMENT_DEPARTMENT");
    CERTIFICATE_ISSUER_MAP.put("其它", "OTHER");
    CERTIFICATE_ISSUER_MAP.put("其他", "OTHER");
  }

  @Autowired
  public CustomerImportService(
      CustomerRepository customerRepository,
      CustomerStagingRepository stagingRepository,
      AnalyticsRefreshService analyticsRefreshService) {
    this.customerRepository = customerRepository;
    this.stagingRepository = stagingRepository;
    this.analyticsRefreshService = analyticsRefreshService;
  }

  /**
   * Process uploaded Excel file and stage data.
   *
   * @param file uploaded Excel file
   * @return number of rows processed
   * @throws IOException if file reading fails
   */
  @Transactional
  public int processUploadFile(MultipartFile file) throws IOException {
    // Clear existing staging data
    stagingRepository.deleteAll();

    List<CustomerStaging> stagingRecords = new ArrayList<>();

    try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);

      // Read header row and build column index map
      Row headerRow = sheet.getRow(0);
      if (headerRow == null) {
        throw new IOException("Excel file has no header row");
      }

      Map<String, Integer> columnIndexMap = new LinkedHashMap<>();
      for (int i = 0; i < headerRow.getLastCellNum(); i++) {
        String headerValue = getCellValueAsString(headerRow.getCell(i));
        if (headerValue != null && !headerValue.trim().isEmpty()) {
          columnIndexMap.put(headerValue.trim(), i);
        }
      }

      // Log the detected columns for debugging
      logger.info("Detected columns in Excel file: {}", columnIndexMap.keySet());

      // Start from row 1 (skip header)
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) {
          continue;
        }

        // Read and validate the '序号' (row number) column
        String sequenceNumberStr = getCellValueAsString(row.getCell(columnIndexMap.get("序号")));
        if (sequenceNumberStr == null || sequenceNumberStr.trim().isEmpty()) {
          // Skip rows with empty sequence number
          logger.debug("Skipping row {} due to empty 序号 value", i + 1);
          continue;
        }

        // Try to parse the sequence number
        int sequenceNumber;
        try {
          sequenceNumber = Integer.parseInt(sequenceNumberStr.trim());
          if (sequenceNumber <= 0) {
            logger.debug("Skipping row {} due to invalid 序号 value: {}", i + 1, sequenceNumberStr);
            continue;
          }
        } catch (NumberFormatException e) {
          logger.debug("Skipping row {} due to non-numeric 序号 value: {}", i + 1, sequenceNumberStr);
          continue;
        }

        CustomerStaging staging = new CustomerStaging();
        staging.setExcelRowNumber(sequenceNumber);

        // Track missing columns for better error reporting
        List<String> missingColumns = new ArrayList<>();

        try {
          // Parse row data using dynamic column lookup with null checks
          // Helper method to safely get cell value by column name
          staging.setName(getCellValueSafely(row, columnIndexMap, "姓名", missingColumns));
          staging.setPhone(getCellValueSafely(row, columnIndexMap, "电话", missingColumns));
          staging.setCertificateIssuer(
              parseCertificateIssuer(
                  getCellValueSafely(row, columnIndexMap, "发证机关", missingColumns)));
          staging.setBusinessRequirements(null); // Not in template
          staging.setCertificateType(
              parseCertificateType(
                  getCellValueSafely(row, columnIndexMap, "证件类型", missingColumns)));
          staging.setAge(null); // Not in template
          staging.setEducation(
              parseEducationLevel(getCellValueSafely(row, columnIndexMap, "学历", missingColumns)));
          staging.setGender(getCellValueSafely(row, columnIndexMap, "性别", missingColumns));
          staging.setAddress(getCellValueSafely(row, columnIndexMap, "地址", missingColumns));
          staging.setIdCard(getCellValueSafely(row, columnIndexMap, "身份证", missingColumns));
          staging.setCustomerAgent(getCellValueSafely(row, columnIndexMap, "业务经理", missingColumns));
          staging.setCustomerType(
              parseCustomerType(getCellValueSafely(row, columnIndexMap, "客户类型", missingColumns)));
          staging.setCertifiedAt(
              parseChineseDateToISOString(
                  getCellValueSafely(row, columnIndexMap, "发证时间", missingColumns)));

          // Set default status to CERTIFIED for imported records
          staging.setCurrentStatus(CustomerStatus.CERTIFIED);

          // Check if there were missing columns
          if (!missingColumns.isEmpty()) {
            staging.setImportStatus(ImportStatus.INVALID);
            staging.setValidationMessage("文件缺少以下列: " + String.join(", ", missingColumns));
            logger.warn("Row {} has missing columns: {}", i + 1, String.join(", ", missingColumns));
          } else {
            // Validate and determine import status
            validateAndSetStatus(staging, stagingRecords);
          }

        } catch (Exception e) {
          staging.setImportStatus(ImportStatus.INVALID);
          staging.setValidationMessage("Error parsing row: " + e.getMessage());
          logger.warn("Error parsing row {}: {}", i + 1, e.getMessage());
        }

        stagingRecords.add(staging);
      }
    }

    // Save all staging records
    logger.info("before save staging customer");
    stagingRepository.saveAll(stagingRecords);
    logger.info("Staged {} customer records from Excel file", stagingRecords.size());

    return stagingRecords.size();
  }

  /**
   * Get all staged records with pagination.
   *
   * @param page page number (1-based)O
   * @param limit page size
   * @return paginated staged records
   */
  public StagingPageResponse getStagedRecords(int page, int limit) {
    return getStagedRecords(page, limit, null);
  }

  /**
   * Get all staged records with pagination and optional status filter.
   *
   * @param page page number (1-based)
   * @param limit page size
   * @param importStatus optional import status filter
   * @return paginated staged records
   */
  public StagingPageResponse getStagedRecords(int page, int limit, ImportStatus importStatus) {
    // Convert to 0-based page number
    Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("excelRowNumber").ascending());
    Page<CustomerStaging> stagingPage =
        stagingRepository.findByImportStatusOptional(importStatus, pageable);

    return new StagingPageResponse(
        stagingPage.getContent(), (int) stagingPage.getTotalElements(), page, limit);
  }

  /**
   * Get overall statistics for all staged records.
   *
   * @return statistics grouped by import status
   */
  public StagingStatistics getStagingStatistics() {
    long valid = stagingRepository.countByImportStatus(ImportStatus.VALID);
    long update = stagingRepository.countByImportStatus(ImportStatus.UPDATE);
    long duplicate = stagingRepository.countByImportStatus(ImportStatus.DUPLICATE);
    long invalid = stagingRepository.countByImportStatus(ImportStatus.INVALID);

    return new StagingStatistics(valid, update, duplicate, invalid);
  }

  /**
   * Get all staged records (legacy method for confirmImport).
   *
   * @return list of all staged records
   */
  public List<CustomerStaging> getAllStagedRecords() {
    return stagingRepository.findAll();
  }

  /**
   * Confirm import and move valid records to customers table.
   *
   * <p>This method is not transactional. It delegates to a transactional method for the actual
   * import, then triggers analytics update in a separate transaction to ensure the import is
   * committed before analytics queries run.
   *
   * @return import summary
   */
  public ImportSummary confirmImport() {
    // Perform the import in a transaction
    ImportSummary summary = doImport();

    // Update analytics tables if any records were imported or updated
    // This runs in a separate transaction and can see the committed import data
    if (summary.getImported() > 0 || summary.getUpdated() > 0) {
      logger.info(
          "Triggering analytics refresh for last 12 months... ({} new, {} updated)",
          summary.getImported(),
          summary.getUpdated());
      analyticsRefreshService.updateRecentAnalyticalTables();
    }

    return summary;
  }

  /**
   * Perform the actual import in a transaction.
   *
   * @return import summary
   */
  @Transactional
  private ImportSummary doImport() {
    List<CustomerStaging> stagingRecords = getAllStagedRecords();

    int imported = 0;
    int updated = 0;
    int skipped = 0;

    for (CustomerStaging staging : stagingRecords) {
      if (staging.getImportStatus() == ImportStatus.VALID) {
        // Import as new customer
        Customer customer = staging.toCustomer();
        customerRepository.save(customer);
        imported++;

      } else if (staging.getImportStatus() == ImportStatus.UPDATE) {
        // Update existing customer
        Optional<Customer> existing =
            customerRepository.findByIdCardAndCertificateType(
                staging.getIdCard(), staging.getCertificateType());
        if (existing.isPresent()) {
          staging.updateCustomer(existing.get());
          customerRepository.save(existing.get());
          updated++;
        } else {
          logger.warn(
              "Customer not found for update: {} - {}",
              staging.getPhone(),
              staging.getCertificateType());
          skipped++;
        }

      } else {
        // Skip INVALID and DUPLICATE records
        skipped++;
      }
    }

    // Clear staging table
    stagingRepository.deleteAll();

    logger.info("Import completed: {} new, {} updated, {} skipped", imported, updated, skipped);

    return new ImportSummary(imported, updated, skipped, stagingRecords.size());
  }

  /** Clear all staged records (cancel import). */
  @Transactional
  public void clearStaging() {
    stagingRepository.deleteAll();
    logger.info("Staging table cleared");
  }

  /** Validate staging record and determine import status. */
  private void validateAndSetStatus(CustomerStaging staging, List<CustomerStaging> stagingRecords) {
    // Check for duplicate in staging records (by id_card + certificate type)
    for (CustomerStaging existingStaging : stagingRecords) {
      if (staging.getIdCard() != null
          && staging.getIdCard().equals(existingStaging.getIdCard())
          && staging.getCertificateType() != null
          && staging.getCertificateType().equals(existingStaging.getCertificateType())) {
        staging.setImportStatus(ImportStatus.INVALID);
        staging.setValidationMessage("重复的客户记录 (身份证 + 证件类型)");
        return;
      }
    }

    // Check required fields
    if (staging.getName() == null || staging.getName().trim().isEmpty()) {
      staging.setImportStatus(ImportStatus.INVALID);
      staging.setValidationMessage("名字不能为空");
      return;
    }

    if (staging.getIdCard() == null || staging.getIdCard().trim().isEmpty()) {
      staging.setImportStatus(ImportStatus.INVALID);
      staging.setValidationMessage("身份证不能为空");
      return;
    }

    if (staging.getPhone() == null || staging.getPhone().trim().isEmpty()) {
      staging.setImportStatus(ImportStatus.INVALID);
      staging.setValidationMessage("电话号码异常或为空");
      return;
    }

    if (staging.getCertificateType() == null) {
      staging.setImportStatus(ImportStatus.INVALID);
      staging.setValidationMessage("证件类型异常或为空");
      return;
    }

    if (staging.getCertificateIssuer() == null || staging.getCertificateIssuer().trim().isEmpty()) {
      staging.setImportStatus(ImportStatus.INVALID);
      staging.setValidationMessage("发证机构异常或为空");
      return;
    }

    if (staging.getCertifiedAt() == null || staging.getCertifiedAt().trim().isEmpty()) {
      staging.setImportStatus(ImportStatus.INVALID);
      staging.setValidationMessage("发证时间不能为空");
      return;
    }

    // Check if customer already exists (by id_card + certificate type)
    Optional<Customer> existingOpt =
        customerRepository.findByIdCardAndCertificateType(
            staging.getIdCard(), staging.getCertificateType());

    if (existingOpt.isPresent()) {
      Customer existing = existingOpt.get();

      // Compare all fields to determine if this is a duplicate or an update
      boolean hasChanges = false;
      List<String> changedFields = new ArrayList<>();

      // Compare name
      if (!nullSafeEquals(staging.getName(), existing.getName())) {
        hasChanges = true;
        changedFields.add("name");
      }

      // Compare phone
      if (!nullSafeEquals(staging.getPhone(), existing.getPhone())) {
        hasChanges = true;
        changedFields.add("phone");
      }

      // Compare certificate issuer
      if (!nullSafeEquals(staging.getCertificateIssuer(), existing.getCertificateIssuer())) {
        hasChanges = true;
        changedFields.add("certificateIssuer");
      }

      // Compare education
      if (staging.getEducation() != existing.getEducation()) {
        hasChanges = true;
        changedFields.add("education");
      }

      // Compare address
      if (!nullSafeEquals(staging.getAddress(), existing.getAddress())) {
        hasChanges = true;
        changedFields.add("address");
      }

      // Compare id card
      if (!nullSafeEquals(staging.getIdCard(), existing.getIdCard())) {
        hasChanges = true;
        changedFields.add("idCard");
      }

      // Compare current status
      if (staging.getCurrentStatus() != existing.getCurrentStatus()) {
        hasChanges = true;
        changedFields.add("currentStatus");
      }

      // Compare customer agent
      if (!nullSafeEquals(staging.getCustomerAgent(), existing.getCustomerAgent())) {
        hasChanges = true;
        changedFields.add("customerAgent");
      }

      // Compare certified at
      if (!nullSafeEquals(staging.getCertifiedAt(), existing.getCertifiedAt())) {
        hasChanges = true;
        changedFields.add("certifiedAt");
      }

      // Compare customer type
      if (staging.getCustomerType() != existing.getCustomerType()) {
        hasChanges = true;
        changedFields.add("customerType");
      }

      if (hasChanges) {
        staging.setImportStatus(ImportStatus.UPDATE);
        // Store changed fields as comma-separated string
        staging.setChangedFields(String.join(",", changedFields));
        staging.setValidationMessage(
            "Will update existing customer. Changed fields: " + String.join(", ", changedFields));
      } else {
        staging.setImportStatus(ImportStatus.DUPLICATE);
        staging.setValidationMessage("Duplicate: All fields are identical to existing customer");
      }
      return;
    }

    // All validations passed - new customer
    staging.setImportStatus(ImportStatus.VALID);
    staging.setValidationMessage("Valid - ready to import");
  }

  /** Helper method for null-safe string comparison. */
  private boolean nullSafeEquals(String str1, String str2) {
    if (str1 == null && str2 == null) return true;
    if (str1 == null || str2 == null) return false;
    return str1.equals(str2);
  }

  /** Helper method for null-safe integer comparison. */
  private boolean nullSafeEquals(Integer int1, Integer int2) {
    if (int1 == null && int2 == null) return true;
    if (int1 == null || int2 == null) return false;
    return int1.equals(int2);
  }

  /** Get cell value as string. */
  private String getCellValueAsString(Cell cell) {
    if (cell == null) return null;

    if (cell.getCellType() == CellType.BLANK) {
      return null;
    } else if (cell.getCellType() == CellType.STRING) {
      return cell.getStringCellValue().trim();
    } else if (cell.getCellType() == CellType.NUMERIC) {
      return String.valueOf((long) cell.getNumericCellValue());
    } else if (cell.getCellType() == CellType.BOOLEAN) {
      return String.valueOf(cell.getBooleanCellValue());
    } else {
      return null;
    }
  }

  /**
   * Safely get cell value by column name with error handling.
   *
   * <p>This method checks if the column exists in the column index map before accessing the cell.
   * If the column is missing, it adds the column name to the missingColumns list and returns null
   * instead of throwing a NullPointerException.
   *
   * @param row the Excel row
   * @param columnIndexMap the map of column names to their indices
   * @param columnName the name of the column to retrieve
   * @param missingColumns list to track missing column names
   * @return the cell value as a string, or null if the column doesn't exist
   */
  private String getCellValueSafely(
      Row row,
      Map<String, Integer> columnIndexMap,
      String columnName,
      List<String> missingColumns) {

    Integer columnIndex = columnIndexMap.get(columnName);
    if (columnIndex == null) {
      // Column not found in Excel file
      missingColumns.add(columnName);
      return null;
    }

    // Column exists, get the cell value
    return getCellValueAsString(row.getCell(columnIndex));
  }

  /** Parse enum value from string. */
  private <T extends Enum<T>> T parseEnum(String value, Class<T> enumType) {
    if (value == null || value.trim().isEmpty()) return null;
    try {
      return Enum.valueOf(enumType, value.trim());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /** Parse integer from string. */
  private Integer parseInteger(String value) {
    if (value == null || value.trim().isEmpty()) return null;
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** Parse Chinese date format (e.g., "2022.4.10" or "2022.04.10") to YYYY-MM-DD format. */
  private String parseChineseDateToISOString(String value) {
    if (value == null || value.trim().isEmpty()) return null;

    try {
      // Try to parse Chinese date format: YYYY.M.D or YYYY.MM.DD
      String dateStr = value.trim();
      String[] parts = dateStr.split("\\.");

      if (parts.length >= 3) {
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]);

        // Return in YYYY-MM-DD format
        return String.format("%04d-%02d-%02d", year, month, day);
      }
    } catch (Exception e) {
      logger.warn("Failed to parse Chinese date: {}", value);
    }

    return null;
  }

  /** Parse Chinese education level to enum. */
  private EducationLevel parseEducationLevel(String value) {
    if (value == null || value.trim().isEmpty()) return null;

    String level = value.trim();

    // Map Chinese education levels to enums
    switch (level) {
      case "小学":
        return EducationLevel.ELEMENTARY;
      case "初中":
        return EducationLevel.MIDDLE_SCHOOL;
      case "高中":
        return EducationLevel.HIGH_SCHOOL;
      case "中专":
        return EducationLevel.SECONDARY_VOCATIONAL;
      case "大专":
        return EducationLevel.ASSOCIATE;
      case "本科":
        return EducationLevel.BACHELOR;
      case "硕士":
        return EducationLevel.MASTER;
      case "博士":
        return EducationLevel.DOCTORATE;
      case "专业学位":
        return EducationLevel.PROFESSIONAL;
      case "证书/文凭":
        return EducationLevel.CERTIFICATE;
      default:
        return EducationLevel.OTHER;
    }
  }

  /** Parse Chinese customer type to enum. */
  private com.example.customers.model.CustomerType parseCustomerType(String value) {
    if (value == null || value.trim().isEmpty()) {
      // Default to NEW_CUSTOMER if empty
      return com.example.customers.model.CustomerType.NEW_CUSTOMER;
    }

    String type = value.trim();

    // Map "复审" to RENEW_CUSTOMER, everything else defaults to NEW_CUSTOMER
    if ("复审".equals(type)) {
      return com.example.customers.model.CustomerType.RENEW_CUSTOMER;
    }

    // Default to NEW_CUSTOMER for any other value
    return com.example.customers.model.CustomerType.NEW_CUSTOMER;
  }

  /** Parse Chinese certificate issuer to enum string. */
  private String parseCertificateIssuer(String value) {
    if (value == null || value.trim().isEmpty()) return null;

    String issuer = value.trim();

    // Look up certificate issuer in map
    String enumValue = CERTIFICATE_ISSUER_MAP.get(issuer);
    if (enumValue != null) {
      return enumValue;
    }

    return null;
  }

  /** Parse Chinese certificate type to enum. */
  private CertificateType parseCertificateType(String value) {
    if (value == null || value.trim().isEmpty()) return null;

    String type = value.trim();

    // Look up certificate type in map
    CertificateType certificateType = CERTIFICATE_TYPE_MAP.get(type);
    if (certificateType != null) {
      return certificateType;
    }

    // Fallback: Try direct enum parsing
    try {
      return CertificateType.valueOf(type.toUpperCase().replaceAll(" ", "_").replaceAll("-", "_"));
    } catch (IllegalArgumentException e) {
      logger.warn("Unknown certificate type: {}", type);
      return null;
    }
  }

  /** Import summary DTO. */
  public static class ImportSummary {
    private final int imported;
    private final int updated;
    private final int skipped;
    private final int total;

    public ImportSummary(int imported, int updated, int skipped, int total) {
      this.imported = imported;
      this.updated = updated;
      this.skipped = skipped;
      this.total = total;
    }

    public int getImported() {
      return imported;
    }

    public int getUpdated() {
      return updated;
    }

    public int getSkipped() {
      return skipped;
    }

    public int getTotal() {
      return total;
    }
  }

  /** Staging page response DTO. */
  public static class StagingPageResponse {
    private final List<CustomerStaging> items;
    private final int total;
    private final int page;
    private final int limit;
    private final int totalPages;

    public StagingPageResponse(List<CustomerStaging> items, int total, int page, int limit) {
      this.items = items;
      this.total = total;
      this.page = page;
      this.limit = limit;
      this.totalPages = (int) Math.ceil((double) total / limit);
    }

    public List<CustomerStaging> getItems() {
      return items;
    }

    public int getTotal() {
      return total;
    }

    public int getPage() {
      return page;
    }

    public int getLimit() {
      return limit;
    }

    public int getTotalPages() {
      return totalPages;
    }
  }

  /** Staging statistics DTO. */
  public static class StagingStatistics {
    private final long valid;
    private final long update;
    private final long duplicate;
    private final long invalid;

    public StagingStatistics(long valid, long update, long duplicate, long invalid) {
      this.valid = valid;
      this.update = update;
      this.duplicate = duplicate;
      this.invalid = invalid;
    }

    public long getValid() {
      return valid;
    }

    public long getUpdate() {
      return update;
    }

    public long getDuplicate() {
      return duplicate;
    }

    public long getInvalid() {
      return invalid;
    }
  }
}
