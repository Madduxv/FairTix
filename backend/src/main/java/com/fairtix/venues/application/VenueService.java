package com.fairtix.venues.application;

import com.fairtix.venues.domain.Venue;
import com.fairtix.venues.infrastructure.VenueRepository;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class VenueService {
    private final VenueRepository repository;

    public VenueService(VenueRepository repository){
        this.repository = repository;
    }

    public Venue createVenue(String name, String address, String event, Instant startTime){
        Venue venue = new Venue(name, address, event, startTime);
        return repository.save(venue);
    }

    public Venue getVenue(UUID id){
        return repository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("Venue not found!"));
    }

    public Page<Venue> findAll(Pageable pageable){
        return repository.findAll(pageable);
    }

    public List<Venue> getAllVenues(){
        return repository.findAll();
    }
}
