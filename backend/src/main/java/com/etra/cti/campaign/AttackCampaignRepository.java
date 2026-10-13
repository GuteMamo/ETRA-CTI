package com.etra.cti.campaign;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttackCampaignRepository extends JpaRepository<AttackCampaign, UUID> {

    List<AttackCampaign> findAllByOrderByLastSeenDesc();

    Optional<AttackCampaign> findFirstByApplicationIdAndSourceIpAndLastSeenAfterOrderByLastSeenDesc(
        UUID applicationId, String sourceIp, Instant after);

    List<AttackCampaign> findByApplicationIdOrderByLastSeenDesc(UUID applicationId);
}
