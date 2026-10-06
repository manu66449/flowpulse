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

//    private String token;
    private String token;          // Short-lived Access Token
    private String refreshToken;   // Long-lived Refresh Token
    
    @Builder.Default
    private String tokenType = "Bearer";

    private Long userId;
    private String email;
    private String fullName;
    private Role role;
}
