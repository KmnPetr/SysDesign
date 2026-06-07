package com.messenger.loadtest.controller;

import com.messenger.loadtest.LoadtestApplication;
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

        Thread current = Thread.currentThread();
        System.out.printf(
                "GET /api/write/stop: thread=%s, virtual=%s%n",
                current.getName(),
                current.isVirtual()
        );

        LoadtestApplication.WRITE_TEST_DATA = false;
        return Map.of(
                "WRITE_TEST_DATA", LoadtestApplication.WRITE_TEST_DATA,
                "IS_RUNNING_LOOP",WriteLoop.IS_RUNNING_LOOP
                );

    }
}
