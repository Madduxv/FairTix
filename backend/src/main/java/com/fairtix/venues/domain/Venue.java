package com.fairtix.venues.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "Venues")

public class Venue {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    @ManyToOne
    @JoinColumn(name = "event_id")
    private String event;

    @Column(nullable = false)
    private Instant startTime;


    public Venue(String name, String address, String event, Instant startTime){
        this.name = name;
        this.address = address;
        this.event = event;
        this.startTime = startTime;

    }

    protected Venue(){
    }

    public UUID getId() {
        return id;
    }

    public String getName(){
        return name;
    }

    public String getAddress(){
        return address;
    }

    public String getEvent(){
        return event;
    }
    public Instant getStartTime(){
        return startTime;
    }
}
