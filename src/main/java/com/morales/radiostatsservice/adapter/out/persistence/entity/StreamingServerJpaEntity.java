package com.morales.radiostatsservice.adapter.out.persistence.entity;

import com.morales.radiostatsservice.domain.model.ServerType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "streaming_server",
        indexes = @Index(name = "idx_streaming_server_config_order", columnList = "config_order"))
@Getter
@Setter
@NoArgsConstructor
public class StreamingServerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "server_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ServerType serverType;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private int port;

    @Column(name = "config_order", nullable = false)
    private int configOrder;

    @Column(name = "credentials_ref")
    private String credentialsRef;
}
