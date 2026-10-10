package com.hostel.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_name", unique = true, nullable = false)
    private String settingName;

    @Column(name = "setting_value", precision = 12, scale = 2, nullable = false)
    private BigDecimal settingValue;

    public SystemSetting() {
    }

    public SystemSetting(String settingName, BigDecimal settingValue) {
        this.settingName = settingName;
        this.settingValue = settingValue;
    }

    public Long getId() {
        return id;
    }

    public String getSettingName() {
        return settingName;
    }

    public BigDecimal getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(BigDecimal settingValue) {
        this.settingValue = settingValue;
    }
}
