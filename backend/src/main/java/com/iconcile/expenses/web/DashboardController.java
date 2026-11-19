package com.iconcile.expenses.web;

import com.iconcile.expenses.service.DashboardService;
import com.iconcile.expenses.web.dto.DashboardDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** @param month YYYY-MM; omit for all-time figures */
    @GetMapping
    public DashboardDto summary(@RequestParam(required = false) YearMonth month) {
        return dashboardService.summary(month);
    }
}
