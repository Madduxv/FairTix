package com.fairtix.queue.application;

import com.fairtix.common.ResourceNotFoundException;
import com.fairtix.queue.domain.QueueEntry;
import com.fairtix.queue.domain.QueueStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueSseServiceTest {

    @Mock
    private QueueService queueService;

    private QueueSseService sseService;

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sseService = new QueueSseService(queueService);
    }

    @Test
    void broadcastSkipsWhenNoEmitters() {
        sseService.broadcast(EVENT_ID);
        verifyNoInteractions(queueService);
    }

    @Test
    void broadcastSendsStatusToConnectedEmitter() throws Exception {
        QueueEntry entry = mockWaitingEntry();
        when(queueService.getStatus(EVENT_ID, USER_ID)).thenReturn(entry);
        when(queueService.countWaiting(EVENT_ID)).thenReturn(3L);

        SseEmitter emitter = sseService.register(EVENT_ID, USER_ID);
        assertThat(emitter).isNotNull();

        reset(queueService);
        when(queueService.getStatus(EVENT_ID, USER_ID)).thenReturn(entry);
        when(queueService.countWaiting(EVENT_ID)).thenReturn(2L);

        sseService.broadcast(EVENT_ID);

        verify(queueService).getStatus(EVENT_ID, USER_ID);
    }

    @Test
    void broadcastRemovesDeadEmitterOnError() {
        QueueEntry entry = mockWaitingEntry();
        when(queueService.getStatus(EVENT_ID, USER_ID)).thenReturn(entry);
        when(queueService.countWaiting(EVENT_ID)).thenReturn(1L);

        sseService.register(EVENT_ID, USER_ID);

        reset(queueService);
        when(queueService.getStatus(EVENT_ID, USER_ID))
                .thenThrow(new ResourceNotFoundException("not found"));

        sseService.broadcast(EVENT_ID);

        // A second broadcast should find no live emitters and not call queueService
        reset(queueService);
        sseService.broadcast(EVENT_ID);
        verifyNoInteractions(queueService);
    }

    @Test
    void registerSendsInitialStatusImmediately() {
        QueueEntry entry = mockWaitingEntry();
        when(queueService.getStatus(EVENT_ID, USER_ID)).thenReturn(entry);
        when(queueService.countWaiting(EVENT_ID)).thenReturn(5L);

        SseEmitter emitter = sseService.register(EVENT_ID, USER_ID);

        assertThat(emitter).isNotNull();
        verify(queueService).getStatus(EVENT_ID, USER_ID);
        verify(queueService).countWaiting(EVENT_ID);
    }

    private QueueEntry mockWaitingEntry() {
        QueueEntry entry = mock(QueueEntry.class);
        when(entry.getStatus()).thenReturn(QueueStatus.WAITING);
        when(entry.getPosition()).thenReturn(1);
        when(entry.getId()).thenReturn(UUID.randomUUID());
        return entry;
    }
}
