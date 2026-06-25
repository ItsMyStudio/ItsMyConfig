package to.itsme.itsmyconfig.processor;

/**
 * Represents a packet listener.
 */
public interface PacketListener {

    /**
     * Gets the name of the packet listener.
     *
     * @return The name.
     */
    String name();

    /**
     * Loads the packet listener.
     */
    void load();

    /**
     * Closes the packet listener.
     */
    default void close() {}

}
