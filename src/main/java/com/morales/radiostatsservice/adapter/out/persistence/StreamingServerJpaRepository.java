package com.morales.radiostatsservice.adapter.out.persistence;

import com.morales.radiostatsservice.adapter.out.persistence.entity.StreamingServerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StreamingServerJpaRepository extends JpaRepository<StreamingServerJpaEntity, UUID> {
    List<StreamingServerJpaEntity> findTop10ByOrderByConfigOrderAsc();
}
