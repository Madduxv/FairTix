package com.fairtix.inventory.application;

import com.fairtix.audit.application.AuditService;
import com.fairtix.common.ResourceNotFoundException;
import com.fairtix.events.infrastructure.EventRepository;
import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.dto.ImportResultResponse;
import com.fairtix.inventory.dto.ImportResultResponse.RowError;
import com.fairtix.inventory.infrastructure.SeatRepository;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class SeatImportService {

    private static final Logger log = LoggerFactory.getLogger(SeatImportService.class);

    static final String EXPECTED_HEADER = "section,row,seat_number,price";

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final AuditService auditService;

    public SeatImportService(SeatRepository seatRepository, EventRepository eventRepository,
                             AuditService auditService) {
        this.seatRepository = seatRepository;
        this.eventRepository = eventRepository;
        this.auditService = auditService;
    }

    public ImportResultResponse importSeats(UUID eventId, MultipartFile file, UUID actorId) {
        var event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        Set<String> existingKeys = buildExistingKeySet(eventId);

        List<RowError> errors = new ArrayList<>();
        List<Seat> toSave = new ArrayList<>();
        int skipped = 0;
        int rowNum = 0;

        try (var reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            rowNum = 1;
            if (header == null) {
                return new ImportResultResponse(0, 0, List.of());
            }
            if (!header.trim().toLowerCase().startsWith(EXPECTED_HEADER)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "CSV header must start with: " + EXPECTED_HEADER);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] cols = line.split(",", -1);
                if (cols.length < 4) {
                    errors.add(new RowError(rowNum, "Expected at least 4 columns: section,row,seat_number,price"));
                    continue;
                }

                String section = cols[0].trim();
                String row = cols[1].trim();
                String seatNumber = cols[2].trim();
                String priceStr = cols[3].trim();

                if (section.isEmpty() || row.isEmpty() || seatNumber.isEmpty()) {
                    errors.add(new RowError(rowNum, "section, row, and seat_number are required"));
                    continue;
                }

                BigDecimal price;
                try {
                    price = new BigDecimal(priceStr);
                    if (price.compareTo(BigDecimal.ZERO) < 0) {
                        errors.add(new RowError(rowNum, "price must be non-negative"));
                        continue;
                    }
                } catch (NumberFormatException e) {
                    errors.add(new RowError(rowNum, "invalid price: " + priceStr));
                    continue;
                }

                Double posX = null, posY = null, rotation = null;
                if (cols.length >= 6) {
                    posX = parseOptionalDouble(cols[4].trim(), rowNum, "pos_x", errors);
                    posY = parseOptionalDouble(cols[5].trim(), rowNum, "pos_y", errors);
                    if (posX == null && !cols[4].trim().isEmpty()) continue;
                    if (posY == null && !cols[5].trim().isEmpty()) continue;
                }
                if (cols.length >= 7) {
                    rotation = parseOptionalDouble(cols[6].trim(), rowNum, "rotation", errors);
                    if (rotation == null && !cols[6].trim().isEmpty()) continue;
                }

                String key = seatKey(section, row, seatNumber);
                if (existingKeys.contains(key)) {
                    skipped++;
                    continue;
                }

                existingKeys.add(key);
                var seat = new Seat(event, section, row, seatNumber, price);
                if (posX != null) seat.setPosX(posX);
                if (posY != null) seat.setPosY(posY);
                if (rotation != null) seat.setRotation(rotation);
                toSave.add(seat);
            }

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read CSV file: " + e.getMessage());
        }

        try {
            seatRepository.saveAll(toSave);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate detected during saveAll for event {}: {}", eventId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A duplicate seat was detected during save. Please retry the import.");
        }

        log.info("Seat import for event {}: imported={} skipped={} errors={}", eventId, toSave.size(), skipped, errors.size());
        auditService.log(actorId, "IMPORT_SEATS", "SEAT", eventId,
            "Imported %d seats, skipped %d duplicates, %d row errors".formatted(toSave.size(), skipped, errors.size()));

        return new ImportResultResponse(toSave.size(), skipped, errors);
    }

    private Set<String> buildExistingKeySet(UUID eventId) {
        var existing = seatRepository.findByEvent_Id(eventId);
        Set<String> keys = new HashSet<>(existing.size() * 2);
        for (var s : existing) {
            keys.add(seatKey(s.getSection(), s.getRowLabel(), s.getSeatNumber()));
        }
        return keys;
    }

    static String seatKey(String section, String row, String seatNumber) {
        return section + "|" + row + "|" + seatNumber;
    }

    private Double parseOptionalDouble(String value, int row, String field,
                                       List<RowError> errors) {
        if (value.isEmpty()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            errors.add(new RowError(row, "invalid " + field + ": " + value));
            return null;
        }
    }
}
