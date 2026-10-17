package com.proflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health Check", description = "Server status endpoints")
public class RootController {

    @GetMapping("/")
    @Operation(summary = "Root health check")
    public String root() {
        return "API Running";
    }

    @GetMapping("/test-route")
    @Operation(summary = "Test route")
    public String testRoute() {
        return "Test route is working!";
    }
}
