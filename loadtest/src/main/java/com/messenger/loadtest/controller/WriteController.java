package com.messenger.loadtest.controller;

import com.messenger.loadtest.WriteLoop;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/write")
public class WriteController {

    @GetMapping("/stop")
    public Map<String, Boolean> stop() {
        WriteLoop.STOP = true;
        return Map.of("stop", true);
    }
}
