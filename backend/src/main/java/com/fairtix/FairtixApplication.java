package com.fairtix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FairtixApplication {

  public static void main(String[] args) {
    SpringApplication.run(FairtixApplication.class, args);
  }

}
