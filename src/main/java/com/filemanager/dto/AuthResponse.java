package com.filemanager.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {}
