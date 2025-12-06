package com.xenotask.xeno.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private Integer id;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean isActive;
}

