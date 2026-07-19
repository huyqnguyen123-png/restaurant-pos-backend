package com.restaurantpos.pos_backend.repository;

import com.restaurantpos.pos_backend.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}