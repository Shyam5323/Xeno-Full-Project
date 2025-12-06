package com.xenotask.xeno.controller;

import com.xenotask.xeno.dto.request.EventRequest;
import com.xenotask.xeno.entity.CustomerEvent;
import com.xenotask.xeno.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<CustomerEvent> recordEvent(@RequestHeader("X-Tenant-ID") String tenantHeader,
                                                @Valid @RequestBody EventRequest req,
                                                HttpServletRequest servletRequest) {
        String ip = req.getIp() != null ? req.getIp() : servletRequest.getRemoteAddr();
        String ua = req.getUserAgent() != null ? req.getUserAgent() : servletRequest.getHeader("User-Agent");
        CustomerEvent ev = eventService.recordEvent(tenantHeader,
                req.getCustomerId(),
                req.getEventType(),
                req.getData(),
                req.getSessionId(),
                ip,
                ua);
        return ResponseEntity.ok(ev);
    }
}

