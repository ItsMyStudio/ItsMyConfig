package to.itsme.itsmyconfig.util;

import net.kyori.adventure.text.Component;
import to.itsme.itsmyconfig.component.AbstractComponent;

import java.util.function.Function;
import java.util.regex.Pattern;

@SuppressWarnings("all")
public class IMCSerializer {

    private static final Pattern CLOSE_COLOR_TAG = Pattern.compile("(</#[0-9A-Fa-f]{6}>)+$");
    /**
     * A serializer that converts a JSON String to MiniMessage format.
     */
    public static Function<String, String> JSON_SERIALIZER;

    /**
     * A serializer that converts a Component to MiniMessage format.
     */
    public static Function<Component, String> COMPONENT_SERIALIZER;
    private static SerializerType currentSerializerType;

    static {
        UPDATE_SERIALIZERS();
    }

    /**
     * Updates the serializer implementations and tracks the current serializer type.
     */
    public static void UPDATE_SERIALIZERS() {
        currentSerializerType = SerializerType.JSON_SERIALIZER; /*HAS_SUBSTITUTE ? SerializerType.MM_COPY : SerializerType.JSON_SERIALIZER;*/
        JSON_SERIALIZER = createJsonSerializer(currentSerializerType);
        COMPONENT_SERIALIZER = createComponentSerializer(currentSerializerType);
    }

    /**
     * Gets the serializer type currently in use.
     *
     * @return the current SerializerType
     */
    public static SerializerType currentSerializerType() {
        return currentSerializerType;
    }

    public static String toMiniMessage(final String json) {
        return sanitize(JSON_SERIALIZER.apply(json));
    }

    public static String toMiniMessage(final Component component) {
        return sanitize(COMPONENT_SERIALIZER.apply(component));
    }

    private static String sanitize(final String input) {
        return CLOSE_COLOR_TAG.matcher(input).replaceAll("");
    }

    private static Function<String, String> createJsonSerializer(final SerializerType serializerType) {
        return switch (serializerType) {
            case MM_COPY -> text -> {
                if (text == null || text.isEmpty()) {
                    return "";
                }
                return toMiniMessage(
                        Utilities.GSON_SERIALIZER.deserialize(text)
                );
            };
            case JSON_SERIALIZER -> text -> AbstractComponent.parse(text).toMiniMessage();
        };
    }

    private static Function<Component, String> createComponentSerializer(final SerializerType serializerType) {
        return switch (serializerType) {
            case MM_COPY -> component -> MMSerializer.serialize(component, null, false);
            case JSON_SERIALIZER -> component -> AbstractComponent.parse(component).toMiniMessage();
        };
    }
}
