package com.example.hbaseapi.controller;

import com.example.hbaseapi.service.HBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
public class HBaseController {

    @Autowired
    private HBaseService hBaseService;

    @GetMapping("/get-latest")
    public ResponseEntity<String> getLatestValue(
            @RequestParam String table,
            @RequestParam String id) {
        try {
            String response = hBaseService.getLatestValueAndTimestamp(table, id);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error retrieving latest value");
        }
    }

    @GetMapping("/get-historical")
    public ResponseEntity<List<String>> getHistoricalData(
            @RequestParam String table,
            @RequestParam String id,
            @RequestParam long startTs,
            @RequestParam long endTs) {
        try {
            List<String> response = hBaseService.getHistoricalDataInRange(table, id, startTs, endTs);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}
