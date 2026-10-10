package com.hostel.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hostel.dto.SettingRequestDTO;
import com.hostel.dto.SettingResponseDTO;
import com.hostel.service.SettingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/standard-monthly-fee")
    public ResponseEntity<SettingResponseDTO> getStandardMonthlyFee() {
        return ResponseEntity.ok(new SettingResponseDTO(
                SettingService.STANDARD_MONTHLY_FEE, settingService.getStandardMonthlyFee()));
    }

    @PutMapping("/standard-monthly-fee")
    public ResponseEntity<SettingResponseDTO> updateStandardMonthlyFee(@Valid @RequestBody SettingRequestDTO dto) {
        return ResponseEntity.ok(new SettingResponseDTO(
                SettingService.STANDARD_MONTHLY_FEE, settingService.updateStandardMonthlyFee(dto.value())));
    }
}
