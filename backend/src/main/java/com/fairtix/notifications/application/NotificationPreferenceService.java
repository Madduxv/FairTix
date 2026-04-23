package com.fairtix.notifications.application;

import com.fairtix.notifications.domain.NotificationPreference;
import com.fairtix.notifications.dto.NotificationPreferenceRequest;
import com.fairtix.notifications.infrastructure.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    public NotificationPreferenceService(NotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    public NotificationPreference getPreferences(UUID userId) {
        return repository.findById(userId)
                .orElseGet(() -> createDefault(userId));
    }

    @Transactional
    public NotificationPreference updatePreferences(UUID userId, NotificationPreferenceRequest request) {
        NotificationPreference prefs = repository.findById(userId)
                .orElseGet(() -> new NotificationPreference(userId));
        prefs.setEmailOrder(request.emailOrder());
        prefs.setEmailTicket(request.emailTicket());
        prefs.setEmailHold(request.emailHold());
        prefs.setEmailMarketing(request.emailMarketing());
        if (request.emailSupport() != null) prefs.setEmailSupport(request.emailSupport());
        return repository.save(prefs);
    }

    @Transactional
    public NotificationPreference createDefault(UUID userId) {
        return repository.save(new NotificationPreference(userId));
    }
}
