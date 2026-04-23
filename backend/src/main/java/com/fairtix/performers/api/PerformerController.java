package com.fairtix.performers.api;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.performers.application.PerformerService;
import com.fairtix.performers.dto.CreatePerformerRequest;
import com.fairtix.performers.dto.PerformerResponse;
import com.fairtix.performers.dto.UpdatePerformerRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Performers", description = "Performer management")
@RestController
@RequestMapping("/api/performers")
public class PerformerController {

    private final PerformerService service;

    public PerformerController(PerformerService service) {
        this.service = service;
    }

    @Operation(summary = "Create a performer", description = "Admin-only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PerformerResponse create(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreatePerformerRequest request) {
        return PerformerResponse.from(service.create(request, principal.getUserId()));
    }

    @Operation(summary = "List performers", description = "Public. Paginated.")
    @PermitAll
    @GetMapping
    public Page<PerformerResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(PageRequest.of(page, Math.min(size, 100))).map(PerformerResponse::from);
    }

    @Operation(summary = "Get performer by ID", description = "Public.")
    @PermitAll
    @GetMapping("/{id}")
    public PerformerResponse get(@PathVariable UUID id) {
        return PerformerResponse.from(service.get(id));
    }

    @Operation(summary = "Delete a performer", description = "Admin-only. Removes performer from all events before deleting.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id) {
        service.delete(id, principal.getUserId());
    }

    @Operation(summary = "Update a performer", description = "Admin-only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public PerformerResponse update(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePerformerRequest request) {
        return PerformerResponse.from(service.update(id, request, principal.getUserId()));
    }
}
