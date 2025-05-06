package to.itsme.itsmyconfig.processor;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a packet that has been unpacked.
 *
 * @param <C> The packet container type.
 */
public interface PacketProcessor<C> {

    /**
     * Gets the name of the packet processor.
     *
     * @return The name.
     */
    String name();

    /**
     * Edits the packet container with the given component.
     *
     * @param container The packet container.
     * @param component The component to edit with.
     */
    void edit(final C container, final Component component);

    /**
     * Unpacks the packet container into a {@link PacketContent}.
     *
     * @param container The packet container.
     * @return The unpacked packet.
     */
    @Nullable
    PacketContent<C> unpack(final C container);

}
