package com.morales.radiostatsservice.adapter.out.persistence;

import com.morales.radiostatsservice.adapter.out.persistence.entity.StreamingServerJpaEntity;
import com.morales.radiostatsservice.domain.model.StreamingServer;
import com.morales.radiostatsservice.domain.port.out.StreamingServerRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StreamingServerJpaAdapter implements StreamingServerRepository {

    private final StreamingServerJpaRepository jpaRepository;

    public StreamingServerJpaAdapter(StreamingServerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<StreamingServer> findFirstTenByConfigOrder() {
        return jpaRepository.findTop10ByOrderByConfigOrderAsc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public StreamingServer save(StreamingServer server) {
        StreamingServerJpaEntity entity = toJpa(server);
        StreamingServerJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<StreamingServer> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    private StreamingServer toDomain(StreamingServerJpaEntity entity) {
        return new StreamingServer(
                entity.getId(),
                entity.getName(),
                entity.getServerType(),
                entity.getHost(),
                entity.getPort(),
                entity.getConfigOrder(),
                entity.getCredentialsRef()
        );
    }

    private StreamingServerJpaEntity toJpa(StreamingServer domain) {
        StreamingServerJpaEntity entity = new StreamingServerJpaEntity();
        if (domain.id() != null) {
            entity.setId(domain.id());
        }
        entity.setName(domain.name());
        entity.setServerType(domain.serverType());
        entity.setHost(domain.host());
        entity.setPort(domain.port());
        entity.setConfigOrder(domain.configOrder());
        entity.setCredentialsRef(domain.credentialsRef());
        return entity;
    }
}
