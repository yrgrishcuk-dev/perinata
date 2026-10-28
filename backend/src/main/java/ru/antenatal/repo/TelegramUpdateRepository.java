package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.TelegramUpdate;

public interface TelegramUpdateRepository extends JpaRepository<TelegramUpdate, Long> {
}
