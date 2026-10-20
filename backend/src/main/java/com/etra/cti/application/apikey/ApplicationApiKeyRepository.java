package com.etra.cti.application.apikey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationApiKeyRepository extends JpaRepository<ApplicationApiKey, UUID> {
    Optional<ApplicationApiKey> findByKeyHashAndStatus(String keyHash, ApiKeyStatus status);
    List<ApplicationApiKey> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
    Optional<ApplicationApiKey> findByIdAndApplicationId(UUID id, UUID applicationId);
}
