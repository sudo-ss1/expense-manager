package com.iconcile.expenses.web;

import com.iconcile.expenses.service.CsvImportService;
import com.iconcile.expenses.service.ExpenseService;
import com.iconcile.expenses.web.dto.CategorizationPreviewDto;
import com.iconcile.expenses.web.dto.CsvImportResultDto;
import com.iconcile.expenses.web.dto.ExpenseDto;
import com.iconcile.expenses.web.dto.ExpenseRequest;
import com.iconcile.expenses.web.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final CsvImportService csvImportService;

    public ExpenseController(ExpenseService expenseService, CsvImportService csvImportService) {
        this.expenseService = expenseService;
        this.csvImportService = csvImportService;
    }

    /**
     * @param month YYYY-MM shortcut for one calendar month
     * @param from  inclusive start date (YYYY-MM-DD); combine with {@code to} for "last 3 months" or a custom range
     * @param to    inclusive end date (YYYY-MM-DD)
     */
    @GetMapping
    public PageDto<ExpenseDto> list(@RequestParam(required = false) YearMonth month,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                 @RequestParam(required = false) Long categoryId,
                                 @RequestParam(defaultValue = "false") boolean anomaliesOnly,
                                 @RequestParam(required = false) String q,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "25") int size) {
        return expenseService.list(month, from, to, categoryId, anomaliesOnly, q, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseDto create(@Valid @RequestBody ExpenseRequest request) {
        return expenseService.create(request);
    }

    @PutMapping("/{id}")
    public ExpenseDto update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        expenseService.delete(id);
    }

    @PostMapping(path = "/upload", consumes = "multipart/form-data")
    public CsvImportResultDto upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a non-empty CSV file.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".csv")) {
            throw new IllegalArgumentException("Only .csv files are supported.");
        }
        return csvImportService.importCsv(new String(file.getBytes(), StandardCharsets.UTF_8));
    }

    /** Live preview of the rule engine: which category would this vendor get? */
    @GetMapping("/categorize")
    public CategorizationPreviewDto categorize(@RequestParam String vendor) {
        return expenseService.preview(vendor);
    }
}
