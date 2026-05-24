package com.codaily.retrospect;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
@Controller
public class RetrospectPageController {

    private final RetrospectService retrospectService;

    public RetrospectPageController(RetrospectService retrospectService) {
        this.retrospectService = retrospectService;
    }

    @GetMapping("/retrospects/{id}")
    public String retrospect(@PathVariable UUID id, Model model) {
        model.addAttribute("retrospect", retrospectService.getRetrospect(id));
        return "retrospect";
    }

    @GetMapping("/rankings")
    public String rankings(
        @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit,
        Model model
    ) {
        model.addAttribute("items", retrospectService.getTopRetrospects(limit));
        return "rankings";
    }
}
