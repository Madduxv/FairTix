package com.fairtix.venues.application;

import com.fairtix.common.ResourceNotFoundException;
import com.fairtix.venues.domain.Venue;
import com.fairtix.venues.infrastructure.VenueRepository;
import com.fairtix.venues.dto.UpdateVenueRequest;

import jakarta.persistence.criteria.Predicate;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Transactional

@Service
public class VenueService {
    private final VenueRepository repository;

    public VenueService(VenueRepository repository){
        this.repository = repository;
    }

    /**
     * Creates a new link that persists {@link Venue}
     *
     * @param name the name of the venue
     * @param address the address of the venue
     * @return a newly created event
     */
    public Venue createVenue(String name, String address){
        Venue venue = new Venue(name, address);
        return repository.save(venue);
    }

    /**
     *
     * @param id the id of the venue.
     * @throws ResourceNotFoundException if the venue is not found.
     * @return the requested venue {@link Venue}
     */
    public Venue getVenue(UUID id){
        return repository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Venue not found!"));
    }

    public Venue update(UUID id, UpdateVenueRequest request){
        Venue venue = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
        venue.update(request.name(), request.address());
        return venue;
    }

    public void delete (UUID id){
        Venue venue = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + id));
        repository.delete(venue);
    }

    /**
     *
     * @param name the name of a venue
     * @param address the address of the venue
     * @param pageable determines if within a page.
     * @return finds venues within the pages.
     */
    public Page<Venue> search(
            String name,
            String address,
            Pageable pageable){
        Specification<Venue> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.isBlank()){
                predicates.add(
                        cb.like(
                                cb.lower(root.get("Venue Name")), "%" + name.toLowerCase() + "%"));
            }
            if (address != null && !address.isBlank()){
                predicates.add(
                        cb.like(
                                cb.lower(root.get("Venue Name")), "%" + address.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
            return repository.findAll(spec, pageable);
        }
}