package com.fairtix.venues.application;

import com.fairtix.audit.application.AuditService;
import com.fairtix.common.ResourceNotFoundException;
import com.fairtix.inventory.dto.ImportResultResponse;
import com.fairtix.inventory.dto.ImportResultResponse.RowError;
import com.fairtix.venues.domain.VenueSection;
import com.fairtix.venues.infrastructure.VenueRepository;
import com.fairtix.venues.infrastructure.VenueSectionRepository;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class SectionImportService {

    private static final Logger log = LoggerFactory.getLogger(SectionImportService.class);

    static final String EXPECTED_HEADER = "name,pos_x,pos_y,width,height,color,sort_order";

    private final VenueSectionRepository sectionRepository;
    private final VenueRepository venueRepository;
    private final AuditService auditService;

    public SectionImportService(VenueSectionRepository sectionRepository,
                                VenueRepository venueRepository,
                                AuditService auditService) {
        this.sectionRepository = sectionRepository;
        this.venueRepository = venueRepository;
        this.auditService = auditService;
    }

    public ImportResultResponse importSections(UUID venueId, MultipartFile file, UUID actorId) {
        var venue = venueRepository.findById(venueId)
            .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + venueId));

        Set<String> existingNames = buildExistingNameSet(venueId);

        List<RowError> errors = new ArrayList<>();
        List<VenueSection> toSave = new ArrayList<>();
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
                if (cols.length < 7) {
                    errors.add(new RowError(rowNum, "Expected 7 columns: name,pos_x,pos_y,width,height,color,sort_order"));
                    continue;
                }

                String name = cols[0].trim();
                if (name.isEmpty()) {
                    errors.add(new RowError(rowNum, "name is required"));
                    continue;
                }

                Double posX = parseRequiredDouble(cols[1].trim(), rowNum, "pos_x", errors);
                Double posY = parseRequiredDouble(cols[2].trim(), rowNum, "pos_y", errors);
                Double width = parseRequiredDouble(cols[3].trim(), rowNum, "width", errors);
                Double height = parseRequiredDouble(cols[4].trim(), rowNum, "height", errors);
                if (posX == null || posY == null || width == null || height == null) continue;

                if (width <= 0 || height <= 0) {
                    errors.add(new RowError(rowNum, "width and height must be positive"));
                    continue;
                }

                String color = cols[5].trim();
                if (!color.matches("^#[0-9A-Fa-f]{6}$")) {
                    errors.add(new RowError(rowNum, "color must be a 6-digit hex code (e.g. #1a73e8)"));
                    continue;
                }

                Integer sortOrder = parseRequiredInt(cols[6].trim(), rowNum, "sort_order", errors);
                if (sortOrder == null) continue;

                if (existingNames.contains(name.toLowerCase())) {
                    skipped++;
                    continue;
                }

                existingNames.add(name.toLowerCase());
                toSave.add(new VenueSection(venue, name, "STANDARD", posX, posY, width, height, color, sortOrder));
            }

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read CSV file: " + e.getMessage());
        }

        sectionRepository.saveAll(toSave);

        log.info("Section import for venue {}: imported={} skipped={} errors={}", venueId, toSave.size(), skipped, errors.size());
        auditService.log(actorId, "IMPORT_SECTIONS", "VENUE_SECTION", venueId,
            "Imported %d sections, skipped %d duplicates, %d row errors".formatted(toSave.size(), skipped, errors.size()));

        return new ImportResultResponse(toSave.size(), skipped, errors);
    }

    private Set<String> buildExistingNameSet(UUID venueId) {
        var existing = sectionRepository.findByVenue_Id(venueId);
        Set<String> names = new HashSet<>(existing.size() * 2);
        for (var s : existing) {
            names.add(s.getName().toLowerCase());
        }
        return names;
    }

    private Double parseRequiredDouble(String value, int row, String field, List<RowError> errors) {
        if (value.isEmpty()) {
            errors.add(new RowError(row, field + " is required"));
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            errors.add(new RowError(row, "invalid " + field + ": " + value));
            return null;
        }
    }

    private Integer parseRequiredInt(String value, int row, String field, List<RowError> errors) {
        if (value.isEmpty()) {
            errors.add(new RowError(row, field + " is required"));
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            errors.add(new RowError(row, "invalid " + field + ": " + value));
            return null;
        }
    }
}
