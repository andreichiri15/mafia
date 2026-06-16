package com.andreichiri.mafia_backend.controller;

import com.andreichiri.mafia_backend.dto.RankedDTO;
import com.andreichiri.mafia_backend.service.RankedConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ranked/config")
public class RankedConfigController {

    @Autowired
    private RankedConfigService rankedConfigService;

    @GetMapping
    public ResponseEntity<RankedDTO.RankedConfigDTO> get() {
        return ResponseEntity.ok(rankedConfigService.getConfigDTO());
    }
}
