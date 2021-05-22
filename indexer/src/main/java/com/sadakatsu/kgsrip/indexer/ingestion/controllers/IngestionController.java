package com.sadakatsu.kgsrip.indexer.ingestion.controllers;

import com.sadakatsu.kgsrip.indexer.ingestion.services.IngestionService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@Profile("ingest")
@RequestMapping("ingestion")
public class IngestionController {
    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @GetMapping("start")
    public ResponseEntity<Void> start() {
        ingestionService.startIngestion();
        return ResponseEntity.of(Optional.empty());
    }

    @GetMapping("pause")
    public ResponseEntity<Void> pause() {
        ingestionService.pauseIngestion();
        return ResponseEntity.of(Optional.empty());
    }
}
