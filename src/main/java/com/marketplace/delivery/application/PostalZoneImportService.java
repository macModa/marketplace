package com.marketplace.delivery.application;

import com.marketplace.delivery.domain.Governorate;
import com.marketplace.delivery.domain.PostalZone;
import com.marketplace.delivery.dto.ImportResult;
import com.marketplace.delivery.infrastructure.PostalZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostalZoneImportService {

    private final PostalZoneRepository repository;
    private static final int BATCH_SIZE = 200;

    /**
     * Imports postal zones from a CSV file.
     * Transactional per batch to ensure memory efficiency and performance.
     */
    public ImportResult importFromCsv(MultipartFile file) {
        ImportResult result = new ImportResult();
        List<PostalZone> buffer = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            // Skip header: Province,District,Localité,Code postal
            String header = reader.readLine();
            if (header == null) return result;

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    processLine(line, buffer, result, lineNumber);
                    
                    if (buffer.size() >= BATCH_SIZE) {
                        saveBatch(buffer, result);
                        buffer.clear();
                    }
                } catch (Exception e) {
                    log.error("Error processing line {}: {}", lineNumber, e.getMessage());
                    result.addError("Line " + lineNumber + ": " + e.getMessage());
                    result.incrementSkipped();
                }
            }

            // Save remaining
            if (!buffer.isEmpty()) {
                saveBatch(buffer, result);
            }

        } catch (Exception e) {
            log.error("Failed to read CSV file: {}", e.getMessage());
            result.addError("Critical file error: " + e.getMessage());
        }

        return result;
    }

    private void processLine(String line, List<PostalZone> buffer, ImportResult result, int lineNumber) {
        String[] columns = line.split(",");
        if (columns.length < 4) {
            throw new IllegalArgumentException("Invalid column count (expected at least 4)");
        }

        String govStr = columns[0].trim();
        String zoneName = columns[2].trim();
        String postalCode = columns[3].trim();

        // Validate postal code (4 digits)
        if (!postalCode.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("Postal code must be exactly 4 digits: " + postalCode);
        }

        // Parse Governorate
        Governorate governorate = parseGovernorate(govStr);
        if (governorate == null) {
            throw new IllegalArgumentException("Unsupported governorate: " + govStr);
        }

        PostalZone zone = new PostalZone();
        zone.setGovernorate(governorate);
        zone.setZoneName(zoneName);
        zone.setPostalCode(postalCode);

        buffer.add(zone);
    }

    @Transactional
    protected void saveBatch(List<PostalZone> zones, ImportResult result) {
        // Efficient duplicate check within the batch
        Set<String> codes = zones.stream().map(PostalZone::getPostalCode).collect(Collectors.toSet());
        List<PostalZone> existingZones = repository.findByPostalCodeIn(codes);

        List<PostalZone> toSave = zones.stream()
                .filter(newZone -> existingZones.stream()
                        .noneMatch(existing -> 
                            existing.getPostalCode().equals(newZone.getPostalCode()) && 
                            existing.getZoneName().equalsIgnoreCase(newZone.getZoneName())
                        ))
                .toList();

        int skippedInBatch = zones.size() - toSave.size();
        repository.saveAll(toSave);
        
        for (int i = 0; i < toSave.size(); i++) result.incrementImported();
        for (int i = 0; i < skippedInBatch; i++) result.incrementSkipped();
        
        log.info("Batch saved: {} imported, {} skipped (duplicates)", toSave.size(), skippedInBatch);
    }

    private Governorate parseGovernorate(String input) {
        if (input == null || input.isBlank()) return null;
        
        // Normalization: UpperCase, spaces to underscores (e.g., Grand Tunis -> GRAND_TUNIS)
        String normalized = input.trim()
                .toUpperCase()
                .replace(" ", "_");
        
        try {
            return Governorate.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Fallback: search by display name or partial match
            for (Governorate g : Governorate.values()) {
                if (g.getDisplayName().equalsIgnoreCase(input.trim())) {
                    return g;
                }
            }
            return null;
        }
    }
}
