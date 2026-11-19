package com.iconcile.expenses.web;

import com.iconcile.expenses.service.RuleService;
import com.iconcile.expenses.web.dto.ReapplyResultDto;
import com.iconcile.expenses.web.dto.RuleDto;
import com.iconcile.expenses.web.dto.RuleRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public List<RuleDto> list() {
        return ruleService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RuleDto upsert(@Valid @RequestBody RuleRequest request) {
        return ruleService.upsert(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ruleService.delete(id);
    }

    /** Re-categorise existing (non-manual) expenses with the current rules. */
    @PostMapping("/reapply")
    public ReapplyResultDto reapply() {
        return ruleService.reapplyToExisting();
    }
}
