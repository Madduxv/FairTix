package com.fairtix;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple test controller to serve as a test endpoint to verify that the rate limiting functionality is working correctly.
 */
@RestController
public class TestController {

    @GetMapping("/test")
    public String test() {
        return "Hello, world!";
    }
}