package com.fairtix.config;

import com.fairtix.events.application.EventService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Async
@Component
public class EventSeeder {

  private final EventService eventService;
  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${SERPAPI_KEY}")
  private String apiKey;

  public EventSeeder(EventService eventService) {
    this.eventService = eventService;
  }

  public void run(String... args) {

    List<String> locations = readResourceFile("locations.txt");
    List<String> queries = readResourceFile("queries.txt");

    Set<String> seen = new HashSet<>();

    for (String loc : locations) {
      for (String query : queries) {

        System.out.println("Fetching " + query + " for " + loc);

        String url = "https://serpapi.com/search.json?engine=google"
            + "&q=" + query
            + "&location=" + loc
            + "&hl=en&gl=us"
            + "&api_key=" + apiKey;

        Map response = restTemplate.getForObject(url, Map.class);

        List<Map<String, Object>> events = (List<Map<String, Object>>) response.get("events_results");

        if (events == null)
          continue;

        for (Map<String, Object> e : events) {

          String title = (String) e.get("title");
          String venue = extractVenue(e);
          String date = (String) e.get("date");
          String time = (String) e.get("time");
          String thumbnail = extractThumbnail(e);

          String uniqueKey = title + "|" + date + "|" + time + "|" + venue;
          if (seen.contains(uniqueKey))
            continue;
          seen.add(uniqueKey);

          Instant startTime = parseDateTime(date, time);

          if (startTime == null)
            continue;

          eventService.createEvent(title, startTime, venue, thumbnail);
        }
      }
    }

    System.out.println("Seed complete. Inserted " + seen.size() + " events.");
  }

  private String extractVenue(Map<String, Object> e) {
    List<String> address = (List<String>) e.get("address");
    if (address != null && !address.isEmpty()) {
      return address.get(0);
    }
    return "Unknown Venue";
  }

  private String extractThumbnail(Map<String, Object> e) {
    Object directThumbnail = e.get("thumbnail");
    if (directThumbnail instanceof String thumbnailUrl && !thumbnailUrl.isBlank()) {
      return thumbnailUrl;
    }

    Object image = e.get("image");
    if (image instanceof String imageUrl && !imageUrl.isBlank()) {
      return imageUrl;
    }

    Object imageList = e.get("images");
    if (imageList instanceof List<?> list && !list.isEmpty()) {
      Object first = list.get(0);
      if (first instanceof String imageUrl && !imageUrl.isBlank()) {
        return imageUrl;
      }
      if (first instanceof Map<?, ?> imageMap) {
        Object original = imageMap.get("original");
        if (original instanceof String imageUrl && !imageUrl.isBlank()) {
          return imageUrl;
        }
      }
    }

    return null;
  }

  /**
   * Converts:
   * "Apr 1" + "7:00 PM" → Instant
   */
  private Instant parseDateTime(String date, String time) {
    try {
      if (date == null || time == null)
        return null;

      int year = Year.now().getValue();

      String combined = date + " " + year + " " + time;

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d yyyy h:mm a", Locale.ENGLISH);

      LocalDateTime ldt = LocalDateTime.parse(combined, formatter);

      return ldt.atZone(ZoneId.systemDefault()).toInstant();

    } catch (Exception e) {
      System.out.println("Failed to parse date: " + date + " " + time);
      return null;
    }
  }

  private List<String> readResourceFile(String filename) {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename)) {
      if (is == null) {
        throw new InternalError("File not found: " + filename);
      }
      return new BufferedReader(new InputStreamReader(is))
          .lines()
          .filter(line -> !line.isBlank())
          .toList();
    } catch (IOException e) {
      e.printStackTrace();
      throw new InternalError("Error Reading File");
    }
  }
}
