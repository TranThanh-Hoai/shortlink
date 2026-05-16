package com.hoaitran.shortlink.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QrCodeServiceTest {

    private final QrCodeService qrCodeService = new QrCodeService();

    @Test
    void generateQrCodeImage_ShouldReturnNonEmptyByteArray() {
        byte[] qrCode = qrCodeService.generateQrCodeImage("https://example.com", 200, 200);
        assertNotNull(qrCode);
        assertTrue(qrCode.length > 0);
    }

    @Test
    void generateQrCodeBase64_ShouldReturnValidBase64String() {
        String base64 = qrCodeService.generateQrCodeBase64("https://example.com", 200, 200);
        assertNotNull(base64);
        assertFalse(base64.isEmpty());
        // Simple check if it looks like base64
        assertTrue(base64.matches("^[a-zA-Z0-9+/=]+$"));
    }
}
