package com.fairtix.inventory.dto;

import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.domain.SeatStatus;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Seat data for map rendering, including visual coordinates.
 * When a seat has no stored coordinates, positions are auto-computed
 * from section/row/seatNumber using a grid layout algorithm.
 */
public record SeatMapResponse(
        UUID id,
        UUID eventId,
        String section,
        String rowLabel,
        String seatNumber,
        BigDecimal price,
        SeatStatus status,
        double posX,
        double posY) {

    private static final double SEAT_SIZE = 28;
    private static final double SEAT_GAP = 6;
    private static final double ROW_GAP = 10;
    private static final double SECTION_GAP = 36;
    private static final double PADDING_X = 60;
    private static final double PADDING_Y = 20;

    /**
     * Builds map responses for a list of seats. Seats with stored coordinates use them;
     * seats without coordinates are positioned via grid auto-layout.
     */
    public static List<SeatMapResponse> fromSeats(List<Seat> seats) {
        List<SeatMapResponse> result = new ArrayList<>();

        // Separate seats with and without stored coordinates
        List<Seat> withCoords = new ArrayList<>();
        List<Seat> autoLayout = new ArrayList<>();
        for (Seat s : seats) {
            if (s.getPosX() != null && s.getPosY() != null) {
                withCoords.add(s);
            } else {
                autoLayout.add(s);
            }
        }

        // Seats with stored coordinates
        for (Seat s : withCoords) {
            result.add(new SeatMapResponse(
                    s.getId(), s.getEvent().getId(), s.getSection(), s.getRowLabel(),
                    s.getSeatNumber(), s.getPrice(), s.getStatus(), s.getPosX(), s.getPosY()));
        }

        // Auto-layout: group by section → row → seat
        Map<String, Map<String, List<Seat>>> bySectionRow = new TreeMap<>();
        for (Seat s : autoLayout) {
            bySectionRow
                    .computeIfAbsent(s.getSection(), k -> new TreeMap<>())
                    .computeIfAbsent(s.getRowLabel(), k -> new ArrayList<>())
                    .add(s);
        }
        // Sort seats within each row numerically if possible
        for (var rowMap : bySectionRow.values()) {
            for (var seatList : rowMap.values()) {
                seatList.sort(Comparator.comparing(s -> naturalOrder(s.getSeatNumber())));
            }
        }

        double currentY = PADDING_Y;
        for (var sectionEntry : bySectionRow.entrySet()) {
            Map<String, List<Seat>> rows = sectionEntry.getValue();
            for (var rowEntry : rows.entrySet()) {
                List<Seat> rowSeats = rowEntry.getValue();
                double currentX = PADDING_X;
                for (Seat s : rowSeats) {
                    result.add(new SeatMapResponse(
                            s.getId(), s.getEvent().getId(), s.getSection(), s.getRowLabel(),
                            s.getSeatNumber(), s.getPrice(), s.getStatus(), currentX, currentY));
                    currentX += SEAT_SIZE + SEAT_GAP;
                }
                currentY += SEAT_SIZE + ROW_GAP;
            }
            currentY += SECTION_GAP;
        }

        return result;
    }

    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private static String naturalOrder(String s) {
        // zero-pad numeric parts so "2" < "10"
        Matcher m = DIGITS.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, String.format("%010d", Long.parseLong(m.group())));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
