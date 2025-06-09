package com.igot.cb.authentication.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.security.PublicKey;

import org.bouncycastle.pqc.crypto.rainbow.RainbowPublicKeyParameters;
import org.bouncycastle.pqc.jcajce.provider.rainbow.BCRainbowPublicKey;
import org.junit.jupiter.api.Test;

class KeyDataTest {
    @Test
    void testKeyDataGettersAndSetters() {
        // Arrange
        PublicKey publicKey = new BCRainbowPublicKey(new RainbowPublicKeyParameters(1,
                new short[][]{{1, -1, 1, -1}},
                new short[][]{{1, -1, 1, -1}},
                new short[]{1, -1, 1, -1}));
        KeyData keyData = new KeyData("key123", publicKey);

        // Act
        keyData.setKeyId("newKey123");
        keyData.setPublicKey(publicKey);

        // Assert
        assertEquals("newKey123", keyData.getKeyId());
        assertSame(publicKey, keyData.getPublicKey());
    }
}
