package com.codaily.retrospect;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/rankings")
public class RankingController {

    private final RetrospectService retrospectService;

    public RankingController(RetrospectService retrospectService) {
        this.retrospectService = retrospectService;
    }

    @GetMapping("/retrospects")
    public List<RetrospectSummaryResponse> topRetrospects(
        @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit
    ) {
        return retrospectService.getTopRetrospects(limit);
    }
}
