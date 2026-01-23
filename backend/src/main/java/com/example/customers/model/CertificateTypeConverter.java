package com.example.customers.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter for CertificateType enum that handles conversion between
 * enum constants and database string values.
 */
@Converter(autoApply = true)
public class CertificateTypeConverter implements AttributeConverter<CertificateType, String> {

  @Override
  public String convertToDatabaseColumn(CertificateType certificateType) {
    if (certificateType == null) {
      return null;
    }
    // Store the enum constant name in the database
    return certificateType.name();
  }

  @Override
  public CertificateType convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.trim().isEmpty()) {
      return null;
    }

    String trimmedData = dbData.trim();

    // First try to match directly with enum constant names (new format)
    try {
      return CertificateType.valueOf(trimmedData);
    } catch (IllegalArgumentException e) {
      // Not an enum constant name, try Chinese display names (old format)
    }

    // Handle Chinese display names for backward compatibility
    // Map Chinese display names to enum constants
    switch (trimmedData) {
      case "Q1指挥":
        return CertificateType.Q1_COMMAND;
      case "Q2流动式":
        return CertificateType.Q2_MOBILE_CRANE;
      case "Q2桥式":
        return CertificateType.Q2_BRIDGE_CRANE;
      case "Q2门式":
        return CertificateType.Q2_GANTRY_CRANE;
      case "Q2塔式":
        return CertificateType.Q2_TOWER_CRANE;
      case "Q2升降机":
        return CertificateType.Q2_HOIST;
      case "N1叉车":
        return CertificateType.N1_FORKLIFT;
      case "N2观光车":
        return CertificateType.N2_SIGHTSEEING_CAR;
      case "G1工业锅炉":
        return CertificateType.G1_INDUSTRIAL_BOILER;
      case "G3锅炉水处理":
        return CertificateType.G3_BOILER_WATER_TREATMENT;
      case "R1快开门式压力容器":
        return CertificateType.R1_QUICK_OPEN_PRESSURE_VESSEL;
      case "R2移动式压力容器":
        return CertificateType.R2_MOBILE_PRESSURE_VESSEL;
      case "P气瓶充装":
        return CertificateType.P_GAS_FILLING;
      case "A特种设备安全管理":
        return CertificateType.A_SPECIAL_EQUIPMENT_SAFETY;
      case "T电梯作业":
        return CertificateType.T_ELEVATOR_OPERATION;
      case "建筑电工":
        return CertificateType.CONSTRUCTION_ELECTRICIAN;
      case "建筑焊工":
        return CertificateType.CONSTRUCTION_WELDER;
      case "建筑架子工":
        return CertificateType.CONSTRUCTION_SCAFFOLDER;
      case "建筑起重机械操作类":
        return CertificateType.CONSTRUCTION_LIFTING_EQUIPMENT;
      case "建筑起重信号司索工":
        return CertificateType.CONSTRUCTION_SIGNALMAN;
      case "建筑物料提升机司机":
        return CertificateType.CONSTRUCTION_MATERIAL_HOIST_DRIVER;
      case "建筑吊篮安装拆卸工":
        return CertificateType.CONSTRUCTION_GONDOLA_INSTALLER;
      case "低压电工作业":
        return CertificateType.LOW_VOLTAGE_ELECTRICIAN;
      case "焊接与热切割作业":
        return CertificateType.WELDING_THERMAL_CUTTING;
      case "高压电工作业":
        return CertificateType.HIGH_VOLTAGE_ELECTRICIAN;
      case "高处安装，维护，拆除作业":
        return CertificateType.HIGH_ALTITUDE_INSTALLATION;
      case "登高架设作业":
        return CertificateType.HIGH_ALTITUDE_SCAFFOLDING;
      case "制冷与空调作业":
        return CertificateType.REFRIGERATION_AIR_CONDITIONING;
      case "煤矿安全作业":
        return CertificateType.COAL_MINE_SAFETY;
      case "金属非金属矿山安全作业":
        return CertificateType.METAL_NONMETAL_MINE_SAFETY;
      case "石油天然气安全作业":
        return CertificateType.OIL_GAS_SAFETY;
      case "危险化学品安全作业":
        return CertificateType.HAZARDOUS_CHEMICALS_SAFETY;
      case "冶金（有色）生产安全作业":
        return CertificateType.METALLURGY_SAFETY;
      case "烟花爆竹安全作业":
        return CertificateType.FIREWORKS_SAFETY;
      case "其它":
        return CertificateType.OTHERS;
      // Handle old enum constant names for backward compatibility
      case "ELECTRICIAN":
        return CertificateType.LOW_VOLTAGE_ELECTRICIAN;
      case "WELDER":
        return CertificateType.WELDING_THERMAL_CUTTING;
      case "EXCAVATOR":
        return CertificateType.Q2_MOBILE_CRANE;
      default:
        // If no match found, return OTHERS as a safe fallback
        return CertificateType.OTHERS;
    }
  }
}