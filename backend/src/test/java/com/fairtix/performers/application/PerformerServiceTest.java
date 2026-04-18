package com.fairtix.performers.application;

import com.fairtix.performers.domain.Performer;
import com.fairtix.performers.dto.CreatePerformerRequest;
import com.fairtix.performers.dto.UpdatePerformerRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PerformerServiceTest {

    @Autowired
    private PerformerService performerService;

    private final UUID actorId = UUID.randomUUID();

    @Test
    void createPerformerSucceeds() {
        var request = new CreatePerformerRequest("Test Artist", "Jazz", "Bio text", null);

        Performer performer = performerService.create(request, actorId);

        assertThat(performer.getId()).isNotNull();
        assertThat(performer.getName()).isEqualTo("Test Artist");
        assertThat(performer.getGenre()).isEqualTo("Jazz");
    }

    @Test
    void createPerformerBlocksDuplicateNameCaseInsensitive() {
        performerService.create(new CreatePerformerRequest("Unique Artist", "Rock", null, null), actorId);

        assertThatThrownBy(() ->
                performerService.create(new CreatePerformerRequest("unique artist", "Pop", null, null), actorId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createPerformerBlocksDuplicateNameExactMatch() {
        performerService.create(new CreatePerformerRequest("The Band", "Rock", null, null), actorId);

        assertThatThrownBy(() ->
                performerService.create(new CreatePerformerRequest("The Band", "Pop", null, null), actorId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void listPerformersIsPaginated() {
        performerService.create(new CreatePerformerRequest("Artist A", "Rock", null, null), actorId);
        performerService.create(new CreatePerformerRequest("Artist B", "Pop", null, null), actorId);
        performerService.create(new CreatePerformerRequest("Artist C", "Jazz", null, null), actorId);

        Page<Performer> page = performerService.list(PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSizeLessThanOrEqualTo(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void updatePerformerChangesFields() {
        Performer created = performerService.create(
                new CreatePerformerRequest("Old Name", "Rock", "Old bio", null), actorId);

        Performer updated = performerService.update(
                created.getId(),
                new UpdatePerformerRequest("New Name", "Pop", "New bio", null),
                actorId);

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getGenre()).isEqualTo("Pop");
    }

    @Test
    void updatePerformerBlocksDuplicateNameFromOtherPerformer() {
        performerService.create(new CreatePerformerRequest("Taken Name", "Rock", null, null), actorId);
        Performer other = performerService.create(
                new CreatePerformerRequest("Another Artist", "Pop", null, null), actorId);

        assertThatThrownBy(() ->
                performerService.update(other.getId(),
                        new UpdatePerformerRequest("taken name", "Jazz", null, null), actorId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void updatePerformerAllowsSameNameForSameId() {
        Performer created = performerService.create(
                new CreatePerformerRequest("Same Name", "Rock", null, null), actorId);

        Performer updated = performerService.update(
                created.getId(),
                new UpdatePerformerRequest("Same Name", "Pop", null, null),
                actorId);

        assertThat(updated.getName()).isEqualTo("Same Name");
        assertThat(updated.getGenre()).isEqualTo("Pop");
    }
}
