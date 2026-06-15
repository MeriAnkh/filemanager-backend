package com.filemanager.dto;

/**
 * Correspond à DeleteResponse du frontend :
 *   { success: boolean, message: string }
 */
public record DeleteResponse(
        boolean success,
        String message
) {}
