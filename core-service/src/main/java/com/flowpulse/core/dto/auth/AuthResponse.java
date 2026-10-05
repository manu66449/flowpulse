package com.flowpulse.core.dto.auth;

import com.flowpulse.core.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String token;
    
    @Builder.Default
    private String tokenType = "Bearer";

    private Long userId;
    private String email;
    private String fullName;
    private Role role;
}
