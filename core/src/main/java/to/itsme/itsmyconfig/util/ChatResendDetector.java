package to.itsme.itsmyconfig.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Detects chat resend patterns used by chat deletion/moderation plugins.
 * When plugins delete messages from chat, they typically flood blank lines
 * (often with invisible unicode characters) to clear the chat window,
 * then resend the chat history. This detector identifies that pattern
 * to prevent actionbar/sound effects from firing during resends.
 * 
 * Detection triggers on:
 * - 20+ blank lines (empty or whitespace-only messages)
 * - Messages containing invisible unicode characters (like Allium plugin uses)
 * 
 * Once triggered, burst mode lasts for 150ms to filter subsequent messages.
 */
public final class ChatResendDetector {

    private static final long BURST_DURATION_MS = 150; // How long burst mode lasts after detection
    private static final int BLANK_LINE_THRESHOLD = 20; // 20+ blank lines triggers burst
    
    private static final Map<String, BurstTracker> trackers = new ConcurrentHashMap<>();
    
    // Invisible unicode characters commonly used to prevent chat condensing
    // Includes: Zero Width Space, Zero Width Non-Joiner, Zero Width Joiner, 
    // Word Joiner, Zero Width No-Break Space (BOM), Mongolian Vowel Separator, etc.
    private static final Pattern INVISIBLE_UNICODE_PATTERN = Pattern.compile(
            "[\u200B\u200C\u200D\u2060\uFEFF\u180E\u00AD\u034F\u061C\u115F\u1160\u17B4\u17B5\u2061-\u2064\u206A-\u206F]"
    );

    /**
     * Checks a message for chat resend patterns and updates burst state.
     * Call this for every chat message to detect blank line floods or invisible unicode.
     * 
     * @param identifier Unique identifier (usually player UUID)
     * @param message The message content to check
     * @return true if currently in burst mode (either just triggered or already active)
     */
    public static boolean checkMessage(String identifier, String message) {
        final long currentTime = System.currentTimeMillis();
        final BurstTracker tracker = trackers.computeIfAbsent(identifier, k -> new BurstTracker());
        
        // Check if message contains invisible unicode (immediate burst trigger)
        if (containsInvisibleUnicode(message)) {
            tracker.triggerBurst(currentTime);
            return true;
        }
        
        // Check if message is blank (empty or whitespace only)
        if (isBlankMessage(message)) {
            tracker.recordBlankLine(currentTime);
        }
        
        return tracker.isInBurst(currentTime);
    }
    
    /**
     * Checks if player is currently in burst mode.
     * @param identifier Unique identifier (usually player UUID)
     * @return true if player is in burst mode
     */
    public static boolean isInBurst(String identifier) {
        final BurstTracker tracker = trackers.get(identifier);
        if (tracker == null) {
            return false;
        }
        return tracker.isInBurst(System.currentTimeMillis());
    }

    /**
     * Forcefully ends burst tracking for testing or manual intervention.
     */
    public static void endBurst(String identifier) {
        trackers.remove(identifier);
    }
    
    /**
     * Checks if a message is blank (null, empty, or whitespace only).
     */
    public static boolean isBlankMessage(String message) {
        return message == null || message.trim().isEmpty();
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
     * Tracks message patterns to identify chat resend behavior.
     * Counts blank lines and triggers burst mode when threshold reached.
     */
    private static class BurstTracker {
        private final AtomicInteger blankLineCount = new AtomicInteger(0);
        private final AtomicLong lastBlankLineTime = new AtomicLong(0);
        private final AtomicLong burstStartTime = new AtomicLong(0);
        
        // Window for counting blank lines (500ms - blank lines should come rapidly)
        private static final long BLANK_LINE_WINDOW_MS = 500;

        /**
         * Records a blank line and checks if threshold is reached.
         */
        public void recordBlankLine(long currentTime) {
            // Reset count if too much time has passed since last blank line
            if ((currentTime - lastBlankLineTime.get()) > BLANK_LINE_WINDOW_MS) {
                blankLineCount.set(0);
            }
            
            lastBlankLineTime.set(currentTime);
            final int count = blankLineCount.incrementAndGet();
            
            // Trigger burst if threshold reached
            if (count >= BLANK_LINE_THRESHOLD) {
                triggerBurst(currentTime);
            }
        }
        
        /**
         * Triggers burst mode immediately.
         */
        public void triggerBurst(long currentTime) {
            burstStartTime.set(currentTime);
            blankLineCount.set(0); // Reset counter
        }
        
        /**
         * Checks if currently in burst mode.
         */
        public boolean isInBurst(long currentTime) {
            final long burstStart = burstStartTime.get();
            if (burstStart == 0) {
                return false;
            }
            // Burst lasts for BURST_DURATION_MS after trigger
            return (currentTime - burstStart) <= BURST_DURATION_MS;
        }
    }
}
