package to.itsme.itsmyconfig.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChatResendDetectorTest {

    @Test
    public void testNoBurstOnSingleMessage() {
        final String testId = "test-player-single";
        
        // Clean up any existing state
        ChatResendDetector.endBurst(testId);
        
        // Single non-blank message should not trigger burst
        assertFalse(ChatResendDetector.checkMessage(testId, "Hello world"));
        assertFalse(ChatResendDetector.isInBurst(testId));
    }
    
    @Test
    public void testNoBurstOnFewBlankLines() {
        final String testId = "test-player-few";
        
        // Clean up any existing state
        ChatResendDetector.endBurst(testId);
        
        // Few blank lines (under threshold of 20) should not trigger burst
        for (int i = 0; i < 10; i++) {
            assertFalse(ChatResendDetector.checkMessage(testId, ""));
        }
        assertFalse(ChatResendDetector.isInBurst(testId));
    }
    
    @Test
    public void testBurstOnManyBlankLines() {
        final String testId = "test-player-burst";
        
        // Clean up any existing state
        ChatResendDetector.endBurst(testId);
        
        // 20+ blank lines should trigger burst
        for (int i = 0; i < 19; i++) {
            assertFalse(ChatResendDetector.checkMessage(testId, "")); // 1-19
        }
        assertTrue(ChatResendDetector.checkMessage(testId, "")); // 20th - triggers burst
        assertTrue(ChatResendDetector.isInBurst(testId));
    }
    
    @Test
    public void testBurstOnInvisibleUnicode() {
        final String testId = "test-player-unicode";
        
        // Clean up any existing state
        ChatResendDetector.endBurst(testId);
        
        // Message with invisible unicode should immediately trigger burst
        assertTrue(ChatResendDetector.checkMessage(testId, "\u200B")); // Zero Width Space
        assertTrue(ChatResendDetector.isInBurst(testId));
    }
    
    @Test
    public void testBurstResetAfterDelay() throws InterruptedException {
        final String testId = "test-player-reset";
        
        // Clean up any existing state
        ChatResendDetector.endBurst(testId);
        
        // Trigger burst with invisible unicode
        assertTrue(ChatResendDetector.checkMessage(testId, "\u200B"));
        assertTrue(ChatResendDetector.isInBurst(testId));
        
        // Wait for burst to expire (150ms)
        Thread.sleep(160);
        
        // Should no longer be in burst
        assertFalse(ChatResendDetector.isInBurst(testId));
    }
    
    @Test
    public void testIndependentPlayerTracking() {
        final String player1 = "test-player-1";
        final String player2 = "test-player-2";
        
        // Clean up
        ChatResendDetector.endBurst(player1);
        ChatResendDetector.endBurst(player2);
        
        // Trigger burst for player1
        ChatResendDetector.checkMessage(player1, "\u200B");
        
        // Player1 should be in burst, player2 should not
        assertTrue(ChatResendDetector.isInBurst(player1));
        assertFalse(ChatResendDetector.isInBurst(player2));
    }
    
    @Test
    public void testInvisibleUnicodeDetection() {
        // Test various invisible unicode characters
        assertTrue(ChatResendDetector.containsInvisibleUnicode("\u200B")); // Zero Width Space
        assertTrue(ChatResendDetector.containsInvisibleUnicode("\u200C")); // Zero Width Non-Joiner
        assertTrue(ChatResendDetector.containsInvisibleUnicode("\u200D")); // Zero Width Joiner
        assertTrue(ChatResendDetector.containsInvisibleUnicode("\u2060")); // Word Joiner
        assertTrue(ChatResendDetector.containsInvisibleUnicode("\uFEFF")); // BOM
        assertTrue(ChatResendDetector.containsInvisibleUnicode("Hello\u200Bworld")); // Mixed
        
        // Normal text should not be detected
        assertFalse(ChatResendDetector.containsInvisibleUnicode("Hello world"));
        assertFalse(ChatResendDetector.containsInvisibleUnicode(""));
        assertFalse(ChatResendDetector.containsInvisibleUnicode(null));
    }
    
    @Test
    public void testBlankMessageDetection() {
        assertTrue(ChatResendDetector.isBlankMessage(null));
        assertTrue(ChatResendDetector.isBlankMessage(""));
        assertTrue(ChatResendDetector.isBlankMessage("   "));
        assertTrue(ChatResendDetector.isBlankMessage("\t\n"));
        
        assertFalse(ChatResendDetector.isBlankMessage("Hello"));
        assertFalse(ChatResendDetector.isBlankMessage(" a "));
    }
}
