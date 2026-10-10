package com.hostel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hostel.entity.SystemSetting;
import com.hostel.repository.SystemSettingRepository;

@Service
public class SettingService {

    public static final String STANDARD_MONTHLY_FEE = "standard_monthly_fee";

    private final SystemSettingRepository systemSettingRepository;

    public SettingService(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal getStandardMonthlyFee() {
        return systemSettingRepository.findBySettingName(STANDARD_MONTHLY_FEE)
                .map(SystemSetting::getSettingValue)
                .orElseThrow(() -> new IllegalStateException("Standard monthly fee not configured in system settings"));
    }

    @Transactional
    public BigDecimal updateStandardMonthlyFee(BigDecimal value) {
        BigDecimal scaled = value.setScale(2, RoundingMode.HALF_UP);
        SystemSetting setting = systemSettingRepository.findBySettingName(STANDARD_MONTHLY_FEE)
                .orElseGet(() -> new SystemSetting(STANDARD_MONTHLY_FEE, scaled));
        setting.setSettingValue(scaled);
        return systemSettingRepository.save(setting).getSettingValue();
    }
}
