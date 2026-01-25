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
import org.etsi.uri.x01903.v13.impl.CertificateValuesTypeImpl;
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

  // Excel column headers (Chinese template format)
  private static final String[] HEADERS = {
    "序号", "期号", "报名时间", "发证时间", "发证机关", "姓名", "身份证", "学历", "性别", "证件类型", "电话", "地址", "业务经理"
  };

  // Certificate type mapping: Chinese name -> Enum name
  private static final Map<String, CertificateType> CERTIFICATE_TYPE_MAP;

  // Certificate issuer mapping: Chinese name -> Enum name
  private static final Map<String, String> CERTIFICATE_ISSUER_MAP;

  static {
    CERTIFICATE_TYPE_MAP = new HashMap<>();
    // Crane & Machinery Types (起重机械)
    CERTIFICATE_TYPE_MAP.put("Q1指挥", CertificateType.Q1_COMMAND);
    CERTIFICATE_TYPE_MAP.put("Q2流动式", CertificateType.Q2_MOBILE_CRANE);
    CERTIFICATE_TYPE_MAP.put("Q2桥式", CertificateType.Q2_BRIDGE_CRANE);
    CERTIFICATE_TYPE_MAP.put("Q2门式", CertificateType.Q2_GANTRY_CRANE);
    CERTIFICATE_TYPE_MAP.put("Q2塔式", CertificateType.Q2_TOWER_CRANE);
    CERTIFICATE_TYPE_MAP.put("Q2升降机", CertificateType.Q2_HOIST);
    // Forklift & Industrial Vehicles
    CERTIFICATE_TYPE_MAP.put("N1叉车", CertificateType.N1_FORKLIFT);
    CERTIFICATE_TYPE_MAP.put("N2观光车", CertificateType.N2_SIGHTSEEING_CAR);
    // Boiler & Pressure Vessels (锅炉压力容器)
    CERTIFICATE_TYPE_MAP.put("G1工业锅炉", CertificateType.G1_INDUSTRIAL_BOILER);
    CERTIFICATE_TYPE_MAP.put("G3锅炉水处理", CertificateType.G3_BOILER_WATER_TREATMENT);
    CERTIFICATE_TYPE_MAP.put("R1快开门式压力容器", CertificateType.R1_QUICK_OPEN_PRESSURE_VESSEL);
    CERTIFICATE_TYPE_MAP.put("R2移动式压力容器", CertificateType.R2_MOBILE_PRESSURE_VESSEL);
    CERTIFICATE_TYPE_MAP.put("P气瓶充装", CertificateType.P_GAS_FILLING);
    // Safety Management
    CERTIFICATE_TYPE_MAP.put("A特种设备安全管理", CertificateType.A_SPECIAL_EQUIPMENT_SAFETY);
    // Elevator Operations
    CERTIFICATE_TYPE_MAP.put("T电梯作业", CertificateType.T_ELEVATOR_OPERATION);
    // Construction Trades (建筑施工)
    CERTIFICATE_TYPE_MAP.put("建筑电工", CertificateType.CONSTRUCTION_ELECTRICIAN);
    CERTIFICATE_TYPE_MAP.put("建筑焊工", CertificateType.CONSTRUCTION_WELDER);
    CERTIFICATE_TYPE_MAP.put("建筑架子工", CertificateType.CONSTRUCTION_SCAFFOLDER);
    CERTIFICATE_TYPE_MAP.put("建筑起重机械操作", CertificateType.CONSTRUCTION_LIFTING_EQUIPMENT);
    CERTIFICATE_TYPE_MAP.put("建筑起重信号司索工", CertificateType.CONSTRUCTION_SIGNALMAN);
    CERTIFICATE_TYPE_MAP.put("建筑物料提升机司机", CertificateType.CONSTRUCTION_MATERIAL_HOIST_DRIVER);
    CERTIFICATE_TYPE_MAP.put("建筑吊篮安装拆卸工", CertificateType.CONSTRUCTION_GONDOLA_INSTALLER);
    // Electrical Operations (电工作业)
    CERTIFICATE_TYPE_MAP.put("低压电工作业", CertificateType.LOW_VOLTAGE_ELECTRICIAN);
    CERTIFICATE_TYPE_MAP.put("焊接与热切割作业", CertificateType.WELDING_THERMAL_CUTTING);
    CERTIFICATE_TYPE_MAP.put("高压电工作业", CertificateType.HIGH_VOLTAGE_ELECTRICIAN);
    // High-Altitude Work (高处作业)
    CERTIFICATE_TYPE_MAP.put("高处安装维护拆除作业", CertificateType.HIGH_ALTITUDE_INSTALLATION);
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
    CERTIFICATE_ISSUER_MAP.put("应急管理局", "EMERGENCY_MANAGEMENT_DEPARTMENT");
    CERTIFICATE_ISSUER_MAP.put("其它", "OTHER");
    CERTIFICATE_ISSUER_MAP.put("其他", "OTHER");
  }

  @Autowired
  public CustomerImportService(
      CustomerRepository customerRepository, CustomerStagingRepository stagingRepository) {
    this.customerRepository = customerRepository;
    this.stagingRepository = stagingRepository;
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
        if (row == null) continue;

        CustomerStaging staging = new CustomerStaging();
        staging.setRowNumber(i + 1);

        try {
          // Parse row data using dynamic column lookup
          staging.setName(getCellValueAsString(row.getCell(columnIndexMap.get("姓名"))));
          staging.setPhone(getCellValueAsString(row.getCell(columnIndexMap.get("电话"))));
          staging.setCertificateIssuer(
              parseCertificateIssuer(
                  getCellValueAsString(row.getCell(columnIndexMap.get("发证机关")))));
          staging.setBusinessRequirements(null); // Not in template
          staging.setCertificateType(
              parseCertificateType(getCellValueAsString(row.getCell(columnIndexMap.get("证件类型")))));
          staging.setAge(null); // Not in template
          staging.setEducation(
              parseEducationLevel(getCellValueAsString(row.getCell(columnIndexMap.get("学历")))));
          staging.setGender(getCellValueAsString(row.getCell(columnIndexMap.get("性别"))));
          staging.setAddress(getCellValueAsString(row.getCell(columnIndexMap.get("地址"))));
          staging.setIdCard(getCellValueAsString(row.getCell(columnIndexMap.get("身份证"))));
          staging.setCustomerAgent(getCellValueAsString(row.getCell(columnIndexMap.get("业务经理"))));
          staging.setCertifiedAt(
              parseChineseDateToISOString(
                  getCellValueAsString(row.getCell(columnIndexMap.get("发证时间")))));

          // Set default status to CERTIFIED for imported records
          staging.setCurrentStatus(CustomerStatus.CERTIFIED);

          // Validate and determine import status
          validateAndSetStatus(staging);

        } catch (Exception e) {
          staging.setImportStatus(ImportStatus.INVALID);
          staging.setValidationMessage("Error parsing row: " + e.getMessage());
          logger.warn("Error parsing row {}: {}", i + 1, e.getMessage());
        }

        stagingRecords.add(staging);
      }
    }

    // Save all staging records
    stagingRepository.saveAll(stagingRecords);
    logger.info("Staged {} customer records from Excel file", stagingRecords.size());

    return stagingRecords.size();
  }

  /**
   * Get all staged records with pagination.
   *
   * @param page page number (1-based)
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
    Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("rowNumber").ascending());
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
   * @return import summary
   */
  @Transactional
  public ImportSummary confirmImport() {
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
            customerRepository.findByPhoneAndCertificateType(
                staging.getPhone(), staging.getCertificateType());
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
  private void validateAndSetStatus(CustomerStaging staging) {
    // Check required fields
    if (staging.getName() == null || staging.getName().trim().isEmpty()) {
      staging.setImportStatus(ImportStatus.INVALID);
      staging.setValidationMessage("Name is required");
      return;
    }

    if (staging.getPhone() == null || staging.getPhone().trim().isEmpty()) {
      staging.setImportStatus(ImportStatus.INVALID);
      staging.setValidationMessage("Phone is required");
      return;
    }

    if (staging.getCertificateType() == null) {
      staging.setImportStatus(ImportStatus.INVALID);
      staging.setValidationMessage("Certificate Type is required");
      return;
    }

    if (staging.getCertificateIssuer() == null) {
      staging.setImportStatus(ImportStatus.INVALID);
      staging.setValidationMessage("Certificate Issuer is required");
      return;
    }

    if (staging.getCertifiedAt() == null) {
      staging.setImportStatus(ImportStatus.INVALID);
      staging.setValidationMessage("Certificate time is required");
      return;
    }

    // Check if customer already exists (by phone + certificate type)
    Optional<Customer> existingOpt =
        customerRepository.findByPhoneAndCertificateType(
            staging.getPhone(), staging.getCertificateType());

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

      // Compare certificate issuer
      if (!nullSafeEquals(staging.getCertificateIssuer(), existing.getCertificateIssuer())) {
        hasChanges = true;
        changedFields.add("certificateIssuer");
      }

      // Compare business requirements
      if (!nullSafeEquals(staging.getBusinessRequirements(), existing.getBusinessRequirements())) {
        hasChanges = true;
        changedFields.add("businessRequirements");
      }

      // Compare age
      if (!nullSafeEquals(staging.getAge(), existing.getAge())) {
        hasChanges = true;
        changedFields.add("age");
      }

      // Compare education
      if (staging.getEducation() != existing.getEducation()) {
        hasChanges = true;
        changedFields.add("education");
      }

      // Compare gender
      if (!nullSafeEquals(staging.getGender(), existing.getGender())) {
        hasChanges = true;
        changedFields.add("gender");
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

      // Compare certificate type
      if (staging.getCertificateType() != existing.getCertificateType()) {
        hasChanges = true;
        changedFields.add("certificateType");
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
