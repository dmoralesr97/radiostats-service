package com.morales.radiostatsservice.adapter.out.persistence;

import com.morales.radiostatsservice.domain.model.StreamingServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(StreamingServerJpaAdapter.class)
class StreamingServerJpaAdapterIntegrationTest {

    @Autowired
    private StreamingServerJpaAdapter adapter;

    @Test
    void findFirstTenByConfigOrder_returnsAtMostTenResults() {
        List<StreamingServer> result = adapter.findFirstTenByConfigOrder();

        assertThat(result).hasSizeLessThanOrEqualTo(10);
    }

    @Test
    void findFirstTenByConfigOrder_returnsResultsOrderedByConfigOrderAsc() {
        List<StreamingServer> result = adapter.findFirstTenByConfigOrder();

        for (int i = 1; i < result.size(); i++) {
            assertThat(result.get(i).configOrder())
                    .isGreaterThanOrEqualTo(result.get(i - 1).configOrder());
        }
    }

    @Test
    void findFirstTenByConfigOrder_returnsTenRowsWhenMoreExist() {
        // V3 seed inserts 12 rows (V2 inserts 3 more = 15 total in test context)
        // findFirstTenByConfigOrder must return exactly 10
        List<StreamingServer> result = adapter.findFirstTenByConfigOrder();

        assertThat(result).hasSize(10);
    }

    @Test
    void save_persistsAndReturnsDomainEntity() {
        com.morales.radiostatsservice.domain.model.StreamingServer server =
                new com.morales.radiostatsservice.domain.model.StreamingServer(
                        null, "New Server",
                        com.morales.radiostatsservice.domain.model.ServerType.ICECAST,
                        "newhost.example.com", 8000, 99, null);

        StreamingServer saved = adapter.save(server);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.name()).isEqualTo("New Server");
        assertThat(saved.host()).isEqualTo("newhost.example.com");
    }
}
