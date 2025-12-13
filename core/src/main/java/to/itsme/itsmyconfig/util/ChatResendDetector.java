package to.itsme.itsmyconfig.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Detects rapid chat resend patterns used by chat deletion/moderation plugins.
 * When plugins delete messages from chat, they typically flood blank lines to clear
 * the chat window, then resend the chat history. This detector identifies that pattern
 * to prevent actionbar/sound effects from firing during resends.
 */
public final class ChatResendDetector {

    private static final long BURST_WINDOW_MS = 100; // 100ms window for detecting rapid packet bursts
    private static final int BURST_THRESHOLD = 6; // 6+ packets in 100ms = resend pattern detected
    
    private static final Map<String, BurstTracker> trackers = new ConcurrentHashMap<>();
    
    // Invisible unicode characters commonly used to prevent chat condensing
    // Includes: Zero Width Space, Zero Width Non-Joiner, Zero Width Joiner, 
    // Word Joiner, Zero Width No-Break Space (BOM), Mongolian Vowel Separator
    private static final Pattern INVISIBLE_UNICODE_PATTERN = Pattern.compile(
            "[\u200B\u200C\u200D\u2060\uFEFF\u180E\u00AD\u034F\u061C\u115F\u1160\u17B4\u17B5\u2061-\u2064\u206A-\u206F]"
    );

    /**
     * Records a packet for resend detection. Call this for EVERY chat packet to track
     * the "blank line flood" pattern used by chat deletion plugins.
     * @param identifier Unique identifier (usually player UUID)
     */
    public static void recordPacket(String identifier) {
        final long currentTime = System.currentTimeMillis();
        final BurstTracker tracker = trackers.computeIfAbsent(identifier, k -> new BurstTracker());
        tracker.addPacket(currentTime);
    }
    
    /**
     * Checks if player is currently in a resend burst state.
     * Call this when processing messages with actionbar/sound tags.
     * @param identifier Unique identifier (usually player UUID)
     * @return true if player is receiving a chat resend (rapid packet flood detected)
     */
    public static boolean isInBurst(String identifier) {
        final BurstTracker tracker = trackers.get(identifier);
        if (tracker == null) {
            return false;
        }
        return tracker.isInBurst();
    }

    /**
     * Forcefully ends burst tracking for testing or manual intervention.
     */
    public static void endBurst(String identifier) {
        trackers.remove(identifier);
    }
    
    /**
     * Checks if a message contains invisible unicode characters.
     * These are commonly used by chat clear plugins (like Allium) to prevent
     * chat mods from condensing repeated blank lines.
     * @param message The message to check
     * @return true if the message contains invisible unicode characters
     */
    public static boolean containsInvisibleUnicode(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        return INVISIBLE_UNICODE_PATTERN.matcher(message).find();
    }
    
    /**
     * Checks if a message appears to be part of a chat clear operation.
     * Returns true if the message contains invisible unicode OR if player is in burst.
     * @param identifier Unique identifier (usually player UUID)
     * @param message The message content to check
     * @return true if this appears to be a chat clear/resend operation
     */
    public static boolean isChatClearPacket(String identifier, String message) {
        return isInBurst(identifier) || containsInvisibleUnicode(message);
    }

    /**
     * Tracks packet patterns to identify chat resend behavior.
     * Uses pure timing-based detection - counts ALL packets including blank lines.
     */
    private static class BurstTracker {
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicLong windowStart = new AtomicLong(0);
        private volatile boolean inBurst = false;

        public void addPacket(long currentTime) {
            // If window expired, start new window
            if ((currentTime - windowStart.get()) > BURST_WINDOW_MS) {
                windowStart.set(currentTime);
                count.set(1);
                inBurst = false;
                return;
            }

            // Add packet to current window
            final int newCount = count.incrementAndGet();
            
            // Enter burst mode on high frequency (6+ packets in 100ms)
            if (newCount >= BURST_THRESHOLD) {
                inBurst = true;
            }
        }
        
        public boolean isInBurst() {
            // Check if window has expired
            if ((System.currentTimeMillis() - windowStart.get()) > BURST_WINDOW_MS) {
                inBurst = false;
                count.set(0);
            }
            return inBurst;
        }
    }
}
