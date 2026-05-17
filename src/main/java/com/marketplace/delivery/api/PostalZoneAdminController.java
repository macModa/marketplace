package com.marketplace.delivery.api;

import com.marketplace.delivery.application.PostalZoneImportService;
import com.marketplace.delivery.dto.ImportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/postal-zones")
@RequiredArgsConstructor
public class PostalZoneAdminController {

    private final PostalZoneImportService importService;

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImportResult> importPostalZones(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        ImportResult result = importService.importFromCsv(file);
        return ResponseEntity.ok(result);
    }
}
