package com.restaurantpos.pos_backend.service;

import com.restaurantpos.pos_backend.entity.AppSetting;
import com.restaurantpos.pos_backend.exception.BadRequestException;
import com.restaurantpos.pos_backend.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class HistoryPasswordService {

    // Fixed key: only one shared History password exists app-wide, so there's
    // just a single row in app_settings for it (no per-user accounts here).
    private static final String SETTING_KEY = "history_password_hash";

    private final AppSettingRepository appSettingRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public boolean isPasswordSet() {
        return appSettingRepository.findById(SETTING_KEY).isPresent();
    }

    public boolean verify(String password) {
        return appSettingRepository.findById(SETTING_KEY)
                .map(setting -> passwordEncoder.matches(password, setting.getValue()))
                .orElse(false);
    }

    public void setPassword(String oldPassword, String newPassword) {
        Optional<AppSetting> existing = appSettingRepository.findById(SETTING_KEY);

        if (existing.isPresent()) {
            if (oldPassword == null || !passwordEncoder.matches(oldPassword, existing.get().getValue())) {
                throw new BadRequestException("Current password is incorrect");
            }
        }

        if (newPassword.length() < 4) {
            throw new BadRequestException("Password must be at least 4 characters");
        }

        AppSetting setting = existing.orElseGet(AppSetting::new);
        setting.setKey(SETTING_KEY);
        setting.setValue(passwordEncoder.encode(newPassword));
        appSettingRepository.save(setting);
    }

    public void removePassword(String password) {
        AppSetting setting = appSettingRepository.findById(SETTING_KEY)
                .orElseThrow(() -> new BadRequestException("No password is currently set"));

        if (!passwordEncoder.matches(password, setting.getValue())) {
            throw new BadRequestException("Current password is incorrect");
        }

        appSettingRepository.deleteById(SETTING_KEY);
    }
}