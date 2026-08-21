package com.codewithpcodes.glimserver.auth.dtos;

import com.codewithpcodes.glimserver.user.UserResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthenticationResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        UserResponse user
) {
    public static AuthenticationResponse fromAuth(
            String accessToken,
            String refreshToken,
            UserResponse user
    ) {
        return new AuthenticationResponse(
                accessToken,
                refreshToken,
                user
        );
    }
}
