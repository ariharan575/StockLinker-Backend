package com.backend.StockLinker.Dashboard_Service.controller;

import com.backend.StockLinker.Dashboard_Service.dto.OmniSearchDto;
import com.backend.StockLinker.Dashboard_Service.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/welcome")
    public ResponseEntity<Map<String, Object>> getWelcomeInfo(Authentication auth) {
        return ResponseEntity.ok(dashboardService.getWelcomeInfo(auth.getName()));
    }

    @GetMapping("/search")
    public ResponseEntity<OmniSearchDto> globalSearch(@RequestParam String query, Authentication auth) {
        // Pass auth.getName() (userId) to the service so we know WHO is searching
        return ResponseEntity.ok(dashboardService.globalSearch(query, auth.getName()));
    }


}