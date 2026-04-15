package com.imageflow.backend.api;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.imageflow.backend.domain.auth.AuthService;
import com.imageflow.backend.domain.watermark.WatermarkPresetService;
import com.imageflow.backend.domain.watermark.dto.GenerateWatermarkPresetsRequest;
import com.imageflow.backend.domain.watermark.dto.WatermarkPresetResponse;

@RestController
@RequestMapping("/api/watermarks")
public class WatermarkController {

    private final WatermarkPresetService watermarkPresetService;
    private final AuthService authService;

    public WatermarkController(WatermarkPresetService watermarkPresetService, AuthService authService) {
        this.watermarkPresetService = watermarkPresetService;
        this.authService = authService;
    }

    @PostMapping("/generate")
    public List<WatermarkPresetResponse> generate(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody GenerateWatermarkPresetsRequest request
    ) {
        authService.requireAuthenticatedUser(authorizationHeader);
        return watermarkPresetService.generatePresets(request);
    }
}
