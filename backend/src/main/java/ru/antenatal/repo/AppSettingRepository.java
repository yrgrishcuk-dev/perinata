package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.AppSetting;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
