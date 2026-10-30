package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.CalendarFeedOwnerType;
import ru.antenatal.domain.CalendarFeedToken;

import java.util.Optional;

public interface CalendarFeedTokenRepository extends JpaRepository<CalendarFeedToken, Long> {

    Optional<CalendarFeedToken> findByOwnerTypeAndOwnerIdAndRevokedAtIsNull(
            CalendarFeedOwnerType ownerType, Long ownerId);

    Optional<CalendarFeedToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);
}
