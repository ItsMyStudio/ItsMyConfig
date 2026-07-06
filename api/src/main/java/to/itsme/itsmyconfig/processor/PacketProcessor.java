package to.itsme.itsmyconfig.processor;

/**
 * Base interface for all packet processors.
 * <p>
 * Each packet type that can be intercepted and modified must have a corresponding
 * implementation of this interface. Implementations should be stateless singletons
 * exposing a {@code public static final} {@code INSTANCE} field.
 */
public interface PacketProcessor {

    /**
     * Returns a human-readable identifier for this processor.
     * <p>
     * Used in debug output and for diagnostic purposes.
     *
     * @return the processor name (e.g. {@code "SYSTEM_CHAT_MESSAGE"}, {@code "TITLE"})
     */
    String name();

}
