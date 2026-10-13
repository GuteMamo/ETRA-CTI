package com.etra.cti.campaign;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignEventRepository extends JpaRepository<CampaignEvent, UUID> {

    List<CampaignEvent> findByCampaignIdOrderBySequenceNoAsc(UUID campaignId);

    boolean existsByCampaignIdAndEventId(UUID campaignId, UUID eventId);

    long countByCampaignId(UUID campaignId);
}
