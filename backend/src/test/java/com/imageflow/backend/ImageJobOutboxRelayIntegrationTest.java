package com.imageflow.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.imageflow.backend.queue.ImageJobOutboxMessageEntity;
import com.imageflow.backend.queue.ImageJobOutboxRelay;
import com.imageflow.backend.queue.ImageJobOutboxRepository;
import com.imageflow.backend.queue.ImageJobOutboxStatus;
import com.imageflow.backend.queue.ImageJobQueueMessage;
import com.imageflow.backend.queue.ImageJobQueuePublisher;

@SpringBootTest(properties = "app.queue.outbox.poll-interval-millis=600000")
class ImageJobOutboxRelayIntegrationTest {

    @Autowired
    private ImageJobQueuePublisher imageJobQueuePublisher;

    @Autowired
    private ImageJobOutboxRepository imageJobOutboxRepository;

    @Autowired
    private ImageJobOutboxRelay imageJobOutboxRelay;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @SuppressWarnings("unchecked")
    private final ListOperations<String, String> listOperations = Mockito.mock(ListOperations.class);

    @BeforeEach
    void setUp() {
        imageJobOutboxRepository.deleteAll();
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPush(anyString(), anyString())).thenReturn(1L);
    }

    @Test
    void persistsToOutboxAndRelaysAfterward() {
        ImageJobQueueMessage message = new ImageJobQueueMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "http://localhost:8080/api/files/input/sample.jpg",
                "optimize sample image",
                800,
                null,
                82,
                "jpg",
                "original",
                null,
                "sans",
                null,
                null,
                null,
                null,
                "bottom-right",
                56,
                18,
                "fit",
                null,
                null,
                null,
                null,
                "optimized/sample.jpg",
                null,
                "http://localhost:8080/api/files/output/sample.jpg"
        );

        imageJobQueuePublisher.publish(message);

        assertThat(imageJobOutboxRepository.countByStatus(ImageJobOutboxStatus.PENDING)).isEqualTo(1);

        imageJobOutboxRelay.relayPendingMessages();

        ImageJobOutboxMessageEntity savedOutboxMessage = imageJobOutboxRepository.findAll().get(0);
        assertThat(savedOutboxMessage.getStatus()).isEqualTo(ImageJobOutboxStatus.SENT);
        assertThat(savedOutboxMessage.getAttemptCount()).isEqualTo(1);
        assertThat(savedOutboxMessage.getPublishedAt()).isNotNull();

        verify(listOperations).rightPush(eq("imageflow:image-jobs"), anyString());
    }
}
