package to.itsme.itsmyconfig.placeholder.type;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.OfflinePlayer;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderDependancy;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;
import to.itsme.itsmyconfig.util.Scheduler;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Represents an animated placeholder data object that rotates between different messages at a specified interval.
 * Extends the PlaceholderData class.
 */
public final class AnimatedPlaceholder extends Placeholder {

    private final BlockingQueue<String> queue;

    /**
     * Represents an animated placeholder data object that rotates between different messages at a specified interval.
     * Extends the PlaceholderData class.
     */
    public AnimatedPlaceholder(
            final String filePath,
            final Section section
    ) {
        super(section, filePath, PlaceholderType.ANIMATION, PlaceholderDependancy.NONE);
        final List<String> messages = section.getStringList("values");
        this.queue = new ArrayBlockingQueue<>(messages.size());

        if (messages.isEmpty()) return;

        for (final String message : messages) {
            this.queue.add(message);
            this.registerArguments(message);
        }

        if (messages.size() > 1) {
            Scheduler.runTimerAsync(this::rotateMessage, 0, section.getInt("interval"));
        }
    }

    /**
     * Rotates the current message in the queue by moving the first message to the back of the queue.
     * If the queue is empty, no action is taken.
     */
    private void rotateMessage() {
        final String entry = queue.poll();

        if (entry != null) {
            queue.add(entry);
        }
    }

    /**
     * Retrieves the result of the placeholder evaluation as a string.
     *
     * @param args The arguments used for the placeholder evaluation.
     * @return The result of the placeholder evaluation as a string.
     */
    @Override
    public String getResult(final OfflinePlayer player, final String[] args) {
        final String entry = queue.peek();

        if (entry == null) {
            return "";
        }

        return this.replaceArguments(args, entry);
    }

}
