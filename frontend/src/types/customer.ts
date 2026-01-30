export enum CustomerStatus {
  NEW = 'NEW',
  NOTIFIED = 'NOTIFIED',
  ABORTED = 'ABORTED',
  SUBMITTED = 'SUBMITTED',
  CERTIFIED = 'CERTIFIED',
  CERTIFIED_ELSEWHERE = 'CERTIFIED_ELSEWHERE'
}

export enum CustomerType {
  NEW_CUSTOMER = 'NEW_CUSTOMER',
  RENEW_CUSTOMER = 'RENEW_CUSTOMER'
}

export enum EducationLevel {
  ELEMENTARY = 'ELEMENTARY',
  MIDDLE_SCHOOL = 'MIDDLE_SCHOOL',
  HIGH_SCHOOL = 'HIGH_SCHOOL',
  SECONDARY_VOCATIONAL = 'SECONDARY_VOCATIONAL',
  ASSOCIATE = 'ASSOCIATE',
  BACHELOR = 'BACHELOR',
  MASTER = 'MASTER',
  DOCTORATE = 'DOCTORATE',
  PROFESSIONAL = 'PROFESSIONAL',
  CERTIFICATE = 'CERTIFICATE',
  OTHER = 'OTHER'
}

export enum CertificateType {
  // Crane & Machinery Types (起重机械)
  Q1_COMMAND = 'Q1_COMMAND',
  Q2_MOBILE_CRANE = 'Q2_MOBILE_CRANE',
  Q2_BRIDGE_CRANE = 'Q2_BRIDGE_CRANE',
  Q2_GANTRY_CRANE = 'Q2_GANTRY_CRANE',
  Q2_TOWER_CRANE = 'Q2_TOWER_CRANE',
  Q2_HOIST = 'Q2_HOIST',

  // Forklift & Industrial Vehicles
  N1_FORKLIFT = 'N1_FORKLIFT',
  N2_SIGHTSEEING_CAR = 'N2_SIGHTSEEING_CAR',

  // Boiler & Pressure Vessels (锅炉压力容器)
  G1_INDUSTRIAL_BOILER = 'G1_INDUSTRIAL_BOILER',
  G3_BOILER_WATER_TREATMENT = 'G3_BOILER_WATER_TREATMENT',
  R1_QUICK_OPEN_PRESSURE_VESSEL = 'R1_QUICK_OPEN_PRESSURE_VESSEL',
  R2_MOBILE_PRESSURE_VESSEL = 'R2_MOBILE_PRESSURE_VESSEL',
  P_GAS_FILLING = 'P_GAS_FILLING',

  // Safety Management
  A_SPECIAL_EQUIPMENT_SAFETY = 'A_SPECIAL_EQUIPMENT_SAFETY',

  // Elevator Operations
  T_ELEVATOR_OPERATION = 'T_ELEVATOR_OPERATION',

  // Construction Trades (建筑施工)
  CONSTRUCTION_ELECTRICIAN = 'CONSTRUCTION_ELECTRICIAN',
  CONSTRUCTION_WELDER = 'CONSTRUCTION_WELDER',
  CONSTRUCTION_SCAFFOLDER = 'CONSTRUCTION_SCAFFOLDER',
  CONSTRUCTION_LIFTING_EQUIPMENT = 'CONSTRUCTION_LIFTING_EQUIPMENT',
  CONSTRUCTION_SIGNALMAN = 'CONSTRUCTION_SIGNALMAN',
  CONSTRUCTION_MATERIAL_HOIST_DRIVER = 'CONSTRUCTION_MATERIAL_HOIST_DRIVER',
  CONSTRUCTION_GONDOLA_INSTALLER = 'CONSTRUCTION_GONDOLA_INSTALLER',

  // Electrical Operations (电工作业)
  LOW_VOLTAGE_ELECTRICIAN = 'LOW_VOLTAGE_ELECTRICIAN',
  WELDING_THERMAL_CUTTING = 'WELDING_THERMAL_CUTTING',
  HIGH_VOLTAGE_ELECTRICIAN = 'HIGH_VOLTAGE_ELECTRICIAN',

  // High-Altitude Work (高处作业)
  HIGH_ALTITUDE_INSTALLATION = 'HIGH_ALTITUDE_INSTALLATION',
  HIGH_ALTITUDE_SCAFFOLDING = 'HIGH_ALTITUDE_SCAFFOLDING',

  // Specialized Operations
  REFRIGERATION_AIR_CONDITIONING = 'REFRIGERATION_AIR_CONDITIONING',

  // Mining & Industry Safety (矿山安全作业)
  COAL_MINE_SAFETY = 'COAL_MINE_SAFETY',
  METAL_NONMETAL_MINE_SAFETY = 'METAL_NONMETAL_MINE_SAFETY',

  // Petroleum & Chemical Safety (石油化工安全)
  OIL_GAS_SAFETY = 'OIL_GAS_SAFETY',
  HAZARDOUS_CHEMICALS_SAFETY = 'HAZARDOUS_CHEMICALS_SAFETY',
  METALLURGY_SAFETY = 'METALLURGY_SAFETY',
  FIREWORKS_SAFETY = 'FIREWORKS_SAFETY',

  // Other types for backward compatibility
  OTHERS = 'OTHERS'
}

export enum CertificateIssuer {
  MARKET_SUPERVISION_ADMINISTRATION = 'MARKET_SUPERVISION_ADMINISTRATION',
  HOUSING_CONSTRUCTION_BUREAU = 'HOUSING_CONSTRUCTION_BUREAU',
  EMERGENCY_MANAGEMENT_DEPARTMENT = 'EMERGENCY_MANAGEMENT_DEPARTMENT',
  OTHER = 'OTHER'
}

export const EducationLevelDisplayNames: Record<EducationLevel, string> = {
  [EducationLevel.ELEMENTARY]: 'Elementary School',
  [EducationLevel.MIDDLE_SCHOOL]: 'Middle School',
  [EducationLevel.HIGH_SCHOOL]: 'High School',
  [EducationLevel.SECONDARY_VOCATIONAL]: 'Secondary Vocational School',
  [EducationLevel.ASSOCIATE]: 'Associate Degree',
  [EducationLevel.BACHELOR]: 'Bachelor\'s Degree',
  [EducationLevel.MASTER]: 'Master\'s Degree',
  [EducationLevel.DOCTORATE]: 'Doctorate/PhD',
  [EducationLevel.PROFESSIONAL]: 'Professional Degree',
  [EducationLevel.CERTIFICATE]: 'Certificate/Diploma',
  [EducationLevel.OTHER]: 'Other'
};

export const CertificateTypeTranslationKeys: Record<CertificateType, string> = {
  // Crane & Machinery Types (起重机械)
  [CertificateType.Q1_COMMAND]: 'certificateType.q1Command',
  [CertificateType.Q2_MOBILE_CRANE]: 'certificateType.q2MobileCrane',
  [CertificateType.Q2_BRIDGE_CRANE]: 'certificateType.q2BridgeCrane',
  [CertificateType.Q2_GANTRY_CRANE]: 'certificateType.q2GantryCrane',
  [CertificateType.Q2_TOWER_CRANE]: 'certificateType.q2TowerCrane',
  [CertificateType.Q2_HOIST]: 'certificateType.q2Hoist',

  // Forklift & Industrial Vehicles
  [CertificateType.N1_FORKLIFT]: 'certificateType.n1Forklift',
  [CertificateType.N2_SIGHTSEEING_CAR]: 'certificateType.n2SightseeingCar',

  // Boiler & Pressure Vessels (锅炉压力容器)
  [CertificateType.G1_INDUSTRIAL_BOILER]: 'certificateType.g1IndustrialBoiler',
  [CertificateType.G3_BOILER_WATER_TREATMENT]: 'certificateType.g3BoilerWaterTreatment',
  [CertificateType.R1_QUICK_OPEN_PRESSURE_VESSEL]: 'certificateType.r1QuickOpenPressureVessel',
  [CertificateType.R2_MOBILE_PRESSURE_VESSEL]: 'certificateType.r2MobilePressureVessel',
  [CertificateType.P_GAS_FILLING]: 'certificateType.pGasFilling',

  // Safety Management
  [CertificateType.A_SPECIAL_EQUIPMENT_SAFETY]: 'certificateType.aSpecialEquipmentSafety',

  // Elevator Operations
  [CertificateType.T_ELEVATOR_OPERATION]: 'certificateType.tElevatorOperation',

  // Construction Trades (建筑施工)
  [CertificateType.CONSTRUCTION_ELECTRICIAN]: 'certificateType.constructionElectrician',
  [CertificateType.CONSTRUCTION_WELDER]: 'certificateType.constructionWelder',
  [CertificateType.CONSTRUCTION_SCAFFOLDER]: 'certificateType.constructionScaffolder',
  [CertificateType.CONSTRUCTION_LIFTING_EQUIPMENT]: 'certificateType.constructionLiftingEquipment',
  [CertificateType.CONSTRUCTION_SIGNALMAN]: 'certificateType.constructionSignalman',
  [CertificateType.CONSTRUCTION_MATERIAL_HOIST_DRIVER]: 'certificateType.constructionMaterialHoistDriver',
  [CertificateType.CONSTRUCTION_GONDOLA_INSTALLER]: 'certificateType.constructionGondolaInstaller',

  // Electrical Operations (电工作业)
  [CertificateType.LOW_VOLTAGE_ELECTRICIAN]: 'certificateType.lowVoltageElectrician',
  [CertificateType.WELDING_THERMAL_CUTTING]: 'certificateType.weldingThermalCutting',
  [CertificateType.HIGH_VOLTAGE_ELECTRICIAN]: 'certificateType.highVoltageElectrician',

  // High-Altitude Work (高处作业)
  [CertificateType.HIGH_ALTITUDE_INSTALLATION]: 'certificateType.highAltitudeInstallation',
  [CertificateType.HIGH_ALTITUDE_SCAFFOLDING]: 'certificateType.highAltitudeScaffolding',

  // Specialized Operations
  [CertificateType.REFRIGERATION_AIR_CONDITIONING]: 'certificateType.refrigerationAirConditioning',

  // Mining & Industry Safety (矿山安全作业)
  [CertificateType.COAL_MINE_SAFETY]: 'certificateType.coalMineSafety',
  [CertificateType.METAL_NONMETAL_MINE_SAFETY]: 'certificateType.metalNonmetalMineSafety',

  // Petroleum & Chemical Safety (石油化工安全)
  [CertificateType.OIL_GAS_SAFETY]: 'certificateType.oilGasSafety',
  [CertificateType.HAZARDOUS_CHEMICALS_SAFETY]: 'certificateType.hazardousChemicalsSafety',
  [CertificateType.METALLURGY_SAFETY]: 'certificateType.metallurgySafety',
  [CertificateType.FIREWORKS_SAFETY]: 'certificateType.fireworksSafety',

  // Other types for backward compatibility
  [CertificateType.OTHERS]: 'certificateType.others'
};

export const CertificateIssuerTranslationKeys: Record<CertificateIssuer, string> = {
  [CertificateIssuer.MARKET_SUPERVISION_ADMINISTRATION]: 'certificateIssuer.marketSupervisionAdministration',
  [CertificateIssuer.HOUSING_CONSTRUCTION_BUREAU]: 'certificateIssuer.housingConstructionBureau',
  [CertificateIssuer.EMERGENCY_MANAGEMENT_DEPARTMENT]: 'certificateIssuer.emergencyManagementDepartment',
  [CertificateIssuer.OTHER]: 'certificateIssuer.other'
};

// Status translation keys mapping for use with translation function
export const CustomerStatusTranslationKeys: Record<CustomerStatus, string> = {
  [CustomerStatus.NEW]: 'status.new',
  [CustomerStatus.NOTIFIED]: 'status.notified',
  [CustomerStatus.ABORTED]: 'status.aborted',
  [CustomerStatus.SUBMITTED]: 'status.submitted',
  [CustomerStatus.CERTIFIED]: 'status.certified',
  [CustomerStatus.CERTIFIED_ELSEWHERE]: 'status.certifiedElsewhere',
};

// Education level translation keys mapping for use with translation function
export const EducationLevelTranslationKeys: Record<EducationLevel, string> = {
  [EducationLevel.ELEMENTARY]: 'education.elementary',
  [EducationLevel.MIDDLE_SCHOOL]: 'education.middleSchool',
  [EducationLevel.HIGH_SCHOOL]: 'education.highSchool',
  [EducationLevel.SECONDARY_VOCATIONAL]: 'education.secondaryVocational',
  [EducationLevel.ASSOCIATE]: 'education.associate',
  [EducationLevel.BACHELOR]: 'education.bachelor',
  [EducationLevel.MASTER]: 'education.master',
  [EducationLevel.DOCTORATE]: 'education.doctorate',
  [EducationLevel.PROFESSIONAL]: 'education.professional',
  [EducationLevel.CERTIFICATE]: 'education.certificate',
  [EducationLevel.OTHER]: 'education.other',
};

// Customer type translation keys mapping for use with translation function
export const CustomerTypeTranslationKeys: Record<CustomerType, string> = {
  [CustomerType.NEW_CUSTOMER]: 'customerType.newCustomer',
  [CustomerType.RENEW_CUSTOMER]: 'customerType.renewCustomer',
};

// Helper function to get translated status name
export const getTranslatedStatusName = (status: string, t: (key: string) => string): string => {
  // Check if the status exists in our enum
  if (Object.values(CustomerStatus).includes(status as CustomerStatus)) {
    return t(CustomerStatusTranslationKeys[status as CustomerStatus]);
  }
  // Fallback to formatted string if not in our enum
  return status
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, l => l.toUpperCase());
};

// Helper function to get translated education level name
export const getTranslatedEducationLevelName = (educationLevel: EducationLevel, t: (key: string) => string): string => {
  return t(EducationLevelTranslationKeys[educationLevel]);
};

// Helper function to get translated customer type name
export const getTranslatedCustomerTypeName = (customerType: CustomerType, t: (key: string) => string): string => {
  return t(CustomerTypeTranslationKeys[customerType]);
};

export interface Customer {
  id: string;
  name: string;
  phone: string;
  certificateIssuer?: string;
  businessRequirements?: string;
  certificateType?: CertificateType;
  age?: number;
  education?: EducationLevel;
  gender?: string;
  address?: string;
  idCard?: string;
  currentStatus: CustomerStatus;
  salesPhone?: string;
  customerAgent?: string;
  customerType?: CustomerType;
  certifiedAt?: string;
  createdAt: string;
  updatedAt: string;
  deletedAt?: string;
  deleted: boolean;
}

export interface StatusHistory {
  id: string;
  customer: Customer;
  fromStatus?: CustomerStatus;
  toStatus: CustomerStatus;
  reason?: string;
  changedAt: string;
}

export interface CreateCustomerRequest {
  name: string;
  phone: string;
  certificateIssuer?: string;
  businessRequirements?: string;
  certificateType?: CertificateType;
  age?: number;
  education?: EducationLevel;
  gender?: string;
  address?: string;
  idCard?: string;
  currentStatus?: CustomerStatus;
  customerAgent?: string;
  customerType?: CustomerType;
  certifiedAt?: string;
}

export interface UpdateCustomerRequest {
  name: string;
  phone: string;
  certificateIssuer?: string;
  businessRequirements?: string;
  certificateType?: CertificateType;
  age?: number;
  education?: EducationLevel;
  gender?: string;
  address?: string;
  idCard?: string;
  customerAgent?: string;
  customerType?: CustomerType;
  certifiedAt?: string;
}

export interface StatusTransitionRequest {
  toStatus: CustomerStatus;
  reason?: string;
}

export interface CustomerPageResponse {
  items: Customer[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface CustomerSearchParams {
  q?: string;
  phone?: string;
  status?: CustomerStatus[];
  certificateIssuer?: string;
  includeDeleted?: boolean;
  certificateType?: CertificateType;
  customerAgent?: string;
  customerType?: CustomerType;
  certifiedStartDate?: string;
  certifiedEndDate?: string;
  page?: number;
  limit?: number;
}
