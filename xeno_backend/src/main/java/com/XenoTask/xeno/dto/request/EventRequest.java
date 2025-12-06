package com.xenotask.xeno.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class EventRequest {
    private Integer customerId; // optional
    @NotBlank
    private String eventType;
    private Map<String,Object> data;
    private String sessionId;
    private String ip; // optional override
    private String userAgent; // optional override
}

