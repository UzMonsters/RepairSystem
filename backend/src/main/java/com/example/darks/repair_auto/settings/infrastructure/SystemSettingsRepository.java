package com.example.darks.repair_auto.settings.infrastructure;

import com.example.darks.repair_auto.settings.domain.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {
}
