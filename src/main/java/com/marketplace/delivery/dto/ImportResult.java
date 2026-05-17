package com.marketplace.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {
    private int imported;
    private int skipped;
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    public void incrementImported() {
        this.imported++;
    }

    public void incrementSkipped() {
        this.skipped++;
    }

    public void addError(String error) {
        this.errors.add(error);
    }
}
