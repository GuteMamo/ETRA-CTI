package com.etra.cti.threatintel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IocRepository extends JpaRepository<Ioc, UUID> {

    Optional<Ioc> findByTypeAndValue(IocType type, String value);

    List<Ioc> findByTypeAndActiveTrueOrderByCreatedAtDesc(IocType type);

    List<Ioc> findByActiveTrueOrderByCreatedAtDesc();

    boolean existsByTypeAndValue(IocType type, String value);
}
