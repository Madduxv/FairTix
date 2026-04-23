package com.fairtix.analytics.application;

import com.fairtix.analytics.api.AnalyticsResponse;
import com.fairtix.analytics.api.AnalyticsResponse.DailyHoldCount;
import com.fairtix.analytics.api.AnalyticsResponse.DailyRevenue;
import com.fairtix.analytics.api.AnalyticsResponse.EventInventory;
import com.fairtix.analytics.api.AnalyticsResponse.OverviewStats;
import com.fairtix.analytics.api.AnalyticsResponse.VenueCount;
import com.fairtix.events.infrastructure.EventRepository;
import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.inventory.domain.SeatStatus;
import com.fairtix.inventory.infrastructure.SeatHoldRepository;
import com.fairtix.inventory.infrastructure.SeatRepository;
import com.fairtix.orders.infrastructure.OrderRepository;
import com.fairtix.refunds.infrastructure.RefundRepository;
import com.fairtix.tickets.infrastructure.TicketRepository;
import com.fairtix.users.infrastructure.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

  private final EventRepository eventRepository;
  private final SeatRepository seatRepository;
  private final SeatHoldRepository seatHoldRepository;
  private final UserRepository userRepository;
  private final OrderRepository orderRepository;
  private final TicketRepository ticketRepository;
  private final RefundRepository refundRepository;

  public AnalyticsService(EventRepository eventRepository,
                          SeatRepository seatRepository,
                          SeatHoldRepository seatHoldRepository,
                          UserRepository userRepository,
                          OrderRepository orderRepository,
                          TicketRepository ticketRepository,
                          RefundRepository refundRepository) {
    this.eventRepository = eventRepository;
    this.seatRepository = seatRepository;
    this.seatHoldRepository = seatHoldRepository;
    this.userRepository = userRepository;
    this.orderRepository = orderRepository;
    this.ticketRepository = ticketRepository;
    this.refundRepository = refundRepository;
  }

  public AnalyticsResponse getDashboardAnalytics() {
    Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
    Map<String, Long> seatsByStatus = buildSeatsByStatus();
    OverviewStats overview = buildOverview(seatsByStatus);
    List<VenueCount> eventsByVenue = buildEventsByVenue();
    List<EventInventory> topEvents = buildTopEventsByBookings();
    Map<String, Long> holdsByStatus = buildHoldsByStatus();
    double holdConfirmationRate = computeHoldConfirmationRate(holdsByStatus);
    List<DailyHoldCount> holdsPerDay = buildHoldsPerDay(since);
    Map<String, Long> usersByRole = buildUsersByRole();
    BigDecimal totalRevenue = orderRepository.totalRevenue();
    List<DailyRevenue> revenuePerDay = buildRevenuePerDay(since);
    List<DailyHoldCount> ticketsSoldPerDay = buildTicketsSoldPerDay(since);
    List<DailyHoldCount> refundsPerDay = buildRefundsPerDay(since);

    return new AnalyticsResponse(
        overview, eventsByVenue, seatsByStatus, topEvents,
        holdsByStatus, holdConfirmationRate, holdsPerDay, usersByRole,
        totalRevenue, revenuePerDay, ticketsSoldPerDay, refundsPerDay
    );
  }

  private OverviewStats buildOverview(Map<String, Long> seatsByStatus) {
    long totalEvents = eventRepository.count();
    long upcomingEvents = eventRepository.countByStartTimeAfter(Instant.now());
    long totalUsers = userRepository.count();
    long totalSeats = seatRepository.count();

    long bookedSeats = seatsByStatus.getOrDefault("BOOKED", 0L);
    long activeHolds = seatsByStatus.getOrDefault("HELD", 0L);
    long soldSeats = seatsByStatus.getOrDefault("SOLD", 0L);

    return new OverviewStats(totalEvents, upcomingEvents, totalUsers, totalSeats, bookedSeats, activeHolds, soldSeats);
  }

  private List<VenueCount> buildEventsByVenue() {
    return eventRepository.countByVenueGrouped().stream()
        .map(row -> new VenueCount((String) row[0], (Long) row[1]))
        .sorted(Comparator.comparingLong(VenueCount::count).reversed())
        .toList();
  }

  private Map<String, Long> buildSeatsByStatus() {
    Map<String, Long> map = new LinkedHashMap<>();
    for (SeatStatus status : SeatStatus.values()) {
      map.put(status.name(), 0L);
    }
    for (Object[] row : seatRepository.countByStatusGrouped()) {
      map.put(((SeatStatus) row[0]).name(), (Long) row[1]);
    }
    return map;
  }

  private List<EventInventory> buildTopEventsByBookings() {
    Map<UUID, String> titleById = new HashMap<>();
    Map<UUID, Map<String, Long>> statusCounts = new HashMap<>();

    for (Object[] row : seatRepository.countByEventAndStatus()) {
      UUID eventId = (UUID) row[0];
      String title = (String) row[1];
      SeatStatus status = (SeatStatus) row[2];
      long count = (Long) row[3];

      titleById.put(eventId, title);
      statusCounts.computeIfAbsent(eventId, k -> new HashMap<>())
          .put(status.name(), count);
    }

    return statusCounts.entrySet().stream()
        .map(e -> {
          UUID id = e.getKey();
          Map<String, Long> counts = e.getValue();
          return new EventInventory(
              id,
              titleById.get(id),
              counts.getOrDefault("AVAILABLE", 0L),
              counts.getOrDefault("HELD", 0L),
              counts.getOrDefault("BOOKED", 0L),
              counts.getOrDefault("SOLD", 0L)
          );
        })
        .sorted(Comparator.comparingLong(EventInventory::booked).reversed())
        .limit(10)
        .toList();
  }

  private Map<String, Long> buildHoldsByStatus() {
    Map<String, Long> map = new LinkedHashMap<>();
    for (HoldStatus status : HoldStatus.values()) {
      map.put(status.name(), 0L);
    }
    for (Object[] row : seatHoldRepository.countByStatusGrouped()) {
      map.put(((HoldStatus) row[0]).name(), (Long) row[1]);
    }
    return map;
  }

  private double computeHoldConfirmationRate(Map<String, Long> holdsByStatus) {
    long confirmed = holdsByStatus.getOrDefault("CONFIRMED", 0L);
    long expired = holdsByStatus.getOrDefault("EXPIRED", 0L);
    long released = holdsByStatus.getOrDefault("RELEASED", 0L);
    long completed = confirmed + expired + released;
    if (completed == 0) return 0.0;
    return Math.round((double) confirmed / completed * 1000.0) / 10.0;
  }

  private List<DailyHoldCount> buildHoldsPerDay(Instant since) {
    return seatHoldRepository.countHoldsPerDay(since).stream()
        .map(row -> new DailyHoldCount(row[0].toString(), ((Number) row[1]).longValue()))
        .toList();
  }

  private List<DailyRevenue> buildRevenuePerDay(Instant since) {
    return orderRepository.revenuePerDay(since).stream()
        .map(row -> new DailyRevenue(row[0].toString(), new BigDecimal(row[1].toString())))
        .toList();
  }

  private List<DailyHoldCount> buildTicketsSoldPerDay(Instant since) {
    return ticketRepository.soldPerDay(since).stream()
        .map(row -> new DailyHoldCount(row[0].toString(), ((Number) row[1]).longValue()))
        .toList();
  }

  private List<DailyHoldCount> buildRefundsPerDay(Instant since) {
    return refundRepository.requestedPerDay(since).stream()
        .map(row -> new DailyHoldCount(row[0].toString(), ((Number) row[1]).longValue()))
        .toList();
  }

  private Map<String, Long> buildUsersByRole() {
    Map<String, Long> map = new LinkedHashMap<>();
    for (Object[] row : userRepository.countByRoleGrouped()) {
      map.put(row[0].toString(), (Long) row[1]);
    }
    return map;
  }
}
