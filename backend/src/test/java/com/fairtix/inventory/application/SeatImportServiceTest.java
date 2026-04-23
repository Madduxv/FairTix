package com.fairtix.inventory.application;

import com.fairtix.audit.application.AuditService;
import com.fairtix.events.domain.Event;
import com.fairtix.events.infrastructure.EventRepository;
import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.infrastructure.SeatRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatImportServiceTest {

    @Mock SeatRepository seatRepository;
    @Mock EventRepository eventRepository;
    @Mock AuditService auditService;

    @InjectMocks SeatImportService service;

    private UUID eventId;
    private Event event;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        event = mock(Event.class);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(seatRepository.findByEvent_Id(eventId)).thenReturn(List.of());
        lenient().when(seatRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void imports_valid_rows() {
        var csv = """
                section,row,seat_number,price
                VIP,A,1,75.00
                VIP,A,2,75.00
                GA,B,1,25.00
                """;

        var file = csvFile(csv);
        var result = service.importSeats(eventId, file, UUID.randomUUID());

        assertThat(result.imported()).isEqualTo(3);
        assertThat(result.skipped()).isZero();
        assertThat(result.errors()).isEmpty();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Seat>> captor = ArgumentCaptor.forClass(List.class);
        verify(seatRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
    }

    @Test
    void imports_rows_with_position_columns() {
        var csv = """
                section,row,seat_number,price,pos_x,pos_y,rotation
                VIP,A,1,75.00,100,50,0
                """;

        var result = service.importSeats(eventId, csvFile(csv), UUID.randomUUID());

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Seat>> captor = ArgumentCaptor.forClass(List.class);
        verify(seatRepository).saveAll(captor.capture());
        Seat saved = captor.getValue().get(0);
        assertThat(saved.getPosX()).isEqualTo(100.0);
        assertThat(saved.getPosY()).isEqualTo(50.0);
        assertThat(saved.getRotation()).isEqualTo(0.0);
    }

    @Test
    void skips_duplicate_seats_already_in_db() {
        var existing = new Seat(event, "VIP", "A", "1", BigDecimal.TEN);
        when(seatRepository.findByEvent_Id(eventId)).thenReturn(List.of(existing));

        var csv = """
                section,row,seat_number,price
                VIP,A,1,75.00
                VIP,A,2,75.00
                """;

        var result = service.importSeats(eventId, csvFile(csv), UUID.randomUUID());

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void skips_intra_file_duplicates() {
        var csv = """
                section,row,seat_number,price
                VIP,A,1,75.00
                VIP,A,1,80.00
                """;

        var result = service.importSeats(eventId, csvFile(csv), UUID.randomUUID());

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
    }

    @Test
    void reports_error_for_missing_required_fields() {
        var csv = """
                section,row,seat_number,price
                ,A,1,75.00
                VIP,,1,75.00
                VIP,A,,75.00
                """;

        var result = service.importSeats(eventId, csvFile(csv), UUID.randomUUID());

        assertThat(result.imported()).isZero();
        assertThat(result.errors()).hasSize(3);
        result.errors().forEach(e -> assertThat(e.message()).contains("required"));
    }

    @Test
    void reports_error_for_invalid_price() {
        var csv = """
                section,row,seat_number,price
                VIP,A,1,notanumber
                VIP,A,2,-10.00
                """;

        var result = service.importSeats(eventId, csvFile(csv), UUID.randomUUID());

        assertThat(result.imported()).isZero();
        assertThat(result.errors()).hasSize(2);
    }

    @Test
    void reports_error_for_too_few_columns() {
        var csv = """
                section,row,seat_number,price
                VIP,A
                """;

        var result = service.importSeats(eventId, csvFile(csv), UUID.randomUUID());

        assertThat(result.imported()).isZero();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message()).contains("4 columns");
    }

    @Test
    void reports_error_for_invalid_pos_x() {
        var csv = """
                section,row,seat_number,price,pos_x,pos_y
                VIP,A,1,75.00,abc,50
                """;

        var result = service.importSeats(eventId, csvFile(csv), UUID.randomUUID());

        assertThat(result.imported()).isZero();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message()).contains("pos_x");
    }

    @Test
    void empty_file_returns_zero_counts() {
        var result = service.importSeats(eventId, csvFile(""), UUID.randomUUID());

        assertThat(result.imported()).isZero();
        assertThat(result.skipped()).isZero();
        assertThat(result.errors()).isEmpty();
        verify(seatRepository, never()).saveAll(any());
    }

    @Test
    void emits_audit_log_on_success() {
        var csv = """
                section,row,seat_number,price
                VIP,A,1,75.00
                """;

        var actorId = UUID.randomUUID();
        service.importSeats(eventId, csvFile(csv), actorId);

        verify(auditService).log(eq(actorId), eq("IMPORT_SEATS"), eq("SEAT"), eq(eventId), any());
    }

    // --- helpers ---

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "seats.csv", "text/csv",
            content.getBytes(StandardCharsets.UTF_8));
    }
}
