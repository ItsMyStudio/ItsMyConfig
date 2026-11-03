package to.itsme.itsmyconfig.util;

import net.kyori.adventure.text.Component;
import to.itsme.itsmyconfig.component.AbstractComponent;

import java.util.function.Function;

@SuppressWarnings("all")
public class IMCSerializer {

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

    /**
     * Removes trailing MiniMessage hex-color closing tags ({@code </#RRGGBB>}) from
     * the serialized output.
     * <p>
     * Equivalent to the previous regex-based implementation
     * ({@code CLOSE_COLOR_TAG.matcher(input).replaceAll("")}), but without allocating
     * a {@link java.util.regex.Matcher} or entering the regex engine for the common
     * case of an input that has no trailing color tag.
     *
     * @param input the serialized component
     * @return the input without its trailing hex-color closing tags
     */
    static String sanitize(final String input) {
        if (input == null) {
            return null;
        }

        int cut = input.length();
        while (cut >= 10) {
            if (input.charAt(cut - 1) != '>'
                    || input.charAt(cut - 10) != '<'
                    || input.charAt(cut - 9) != '/'
                    || input.charAt(cut - 8) != '#') {
                break;
            }

            boolean hex = true;
            for (int i = cut - 7; i <= cut - 2; i++) {
                if (!isHex(input.charAt(i))) {
                    hex = false;
                    break;
                }
            }
            if (!hex) {
                break;
            }

            cut -= 10;
        }

        return cut == input.length() ? input : input.substring(0, cut);
    }

    private static boolean isHex(final char character) {
        return (character >= '0' && character <= '9')
                || (character >= 'a' && character <= 'f')
                || (character >= 'A' && character <= 'F');
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
