package com.example.customers.model;

/**
 * Enumeration of certificate types for customers.
 *
 * <p>Provides standardized certificate type options for customer data.
 * <p>Enum constant names are used as the canonical identifiers, while
 * display names are handled by the frontend localization system.
 */
public enum CertificateType {
  // Crane & Machinery Types (起重机械)
  Q1_COMMAND,
  Q2_MOBILE_CRANE,
  Q2_BRIDGE_CRANE,
  Q2_GANTRY_CRANE,
  Q2_TOWER_CRANE,
  Q2_HOIST,

  // Forklift & Industrial Vehicles
  N1_FORKLIFT,
  N2_SIGHTSEEING_CAR,

  // Boiler & Pressure Vessels (锅炉压力容器)
  G1_INDUSTRIAL_BOILER,
  G3_BOILER_WATER_TREATMENT,
  R1_QUICK_OPEN_PRESSURE_VESSEL,
  R2_MOBILE_PRESSURE_VESSEL,
  P_GAS_FILLING,

  // Safety Management
  A_SPECIAL_EQUIPMENT_SAFETY,

  // Elevator Operations
  T_ELEVATOR_OPERATION,

  // Construction Trades (建筑施工)
  CONSTRUCTION_ELECTRICIAN,
  CONSTRUCTION_WELDER,
  CONSTRUCTION_SCAFFOLDER,
  CONSTRUCTION_LIFTING_EQUIPMENT,
  CONSTRUCTION_SIGNALMAN,
  CONSTRUCTION_MATERIAL_HOIST_DRIVER,
  CONSTRUCTION_GONDOLA_INSTALLER,

  // Electrical Operations (电工作业)
  LOW_VOLTAGE_ELECTRICIAN,
  WELDING_THERMAL_CUTTING,
  HIGH_VOLTAGE_ELECTRICIAN,

  // High-Altitude Work (高处作业)
  HIGH_ALTITUDE_INSTALLATION,
  HIGH_ALTITUDE_SCAFFOLDING,

  // Specialized Operations
  REFRIGERATION_AIR_CONDITIONING,

  // Mining & Industry Safety (矿山安全作业)
  COAL_MINE_SAFETY,
  METAL_NONMETAL_MINE_SAFETY,

  // Petroleum & Chemical Safety (石油化工安全)
  OIL_GAS_SAFETY,
  HAZARDOUS_CHEMICALS_SAFETY,
  METALLURGY_SAFETY,
  FIREWORKS_SAFETY,

  // Other types for backward compatibility
  OTHERS
}
