package to.itsme.itsmyconfig.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChatResendDetectorTest {

    @Test
    public void testNoBurstOnSinglePacket() {
        final String testId = "test-player-single";
        
        // Clean up any existing state
        ChatResendDetector.endBurst(testId);
        
        // Single packet should not trigger burst
        ChatResendDetector.recordPacket(testId);
        assertFalse(ChatResendDetector.isInBurst(testId));
    }
    
    @Test
    public void testNoBurstOnFewPackets() {
        final String testId = "test-player-few";
        
        // Clean up any existing state
        ChatResendDetector.endBurst(testId);
        
        // Few packets (under threshold of 6) should not trigger burst
        for (int i = 0; i < 4; i++) {
            ChatResendDetector.recordPacket(testId);
            assertFalse(ChatResendDetector.isInBurst(testId));
        }
    }
    
    @Test
    public void testBurstOnManyRapidPackets() {
        final String testId = "test-player-burst";
        
        // Clean up any existing state
        ChatResendDetector.endBurst(testId);
        
        // 6+ packets in rapid succession should trigger burst
        for (int i = 0; i < 5; i++) {
            ChatResendDetector.recordPacket(testId);
            assertFalse(ChatResendDetector.isInBurst(testId)); // 1-5
        }
        ChatResendDetector.recordPacket(testId); // 6th
        assertTrue(ChatResendDetector.isInBurst(testId)); // Should now be in burst
    }
    
    @Test
    public void testBurstResetAfterDelay() throws InterruptedException {
        final String testId = "test-player-reset";
        
        // Clean up any existing state
        ChatResendDetector.endBurst(testId);
        
        // Send rapid packets to trigger burst
        for (int i = 0; i < 6; i++) {
            ChatResendDetector.recordPacket(testId);
        }
        assertTrue(ChatResendDetector.isInBurst(testId));
        
        // Wait for window to expire
        Thread.sleep(120); // Longer than 100ms threshold
        
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
        for (int i = 0; i < 6; i++) {
            ChatResendDetector.recordPacket(player1);
        }
        
        // Player1 should be in burst, player2 should not
        assertTrue(ChatResendDetector.isInBurst(player1));
        assertFalse(ChatResendDetector.isInBurst(player2));
    }
}
