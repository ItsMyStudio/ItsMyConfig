package to.itsme.itsmyconfig.component.impl;

import com.google.gson.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import to.itsme.itsmyconfig.component.AbstractComponent;
import to.itsme.itsmyconfig.component.event.ClickEvent;
import to.itsme.itsmyconfig.component.event.HoverEvent;
import to.itsme.itsmyconfig.util.JsonUtil;

import java.lang.reflect.Type;

public final class TextfulComponent extends AbstractComponent {

    private static final boolean SHADOW_COLOR_SUPPORTED;

    static {
        boolean supported;
        try {
            Class.forName("net.kyori.adventure.text.format.ShadowColor");
            supported = true;
        } catch (ClassNotFoundException e) {
            supported = false;
        }
        SHADOW_COLOR_SUPPORTED = supported;
    }

    private String text;
    private String color;
    private String shadowColor;

    private boolean bold;
    private boolean italic;
    private boolean underlined;
    private boolean strikethrough;
    private boolean obfuscated;

    public boolean forceUnitalic;

    private String insertion;
    private ClickEvent clickEvent;
    private HoverEvent hoverEvent;

    /**
     * Empty Constructor
     */
    public TextfulComponent() {
    }

    /**
     * Only String component
     */
    public TextfulComponent(final String text) {
        this.text = text;
    }

    /**
     * {@link TextComponent} converter to a {@link TextfulComponent}
     */
    @SuppressWarnings("all")
    public TextfulComponent(final TextComponent component) {
        this.text = component.content();

        final TextColor color = component.color();
        if (color != null) {
            this.color = color.asHexString();
        }

        if (SHADOW_COLOR_SUPPORTED) {
            final ShadowColor shadowColor = component.style().shadowColor();
            if (shadowColor != null) {
                this.shadowColor = shadowColor.asHexString();
            }
        }

        // decorations
        this.bold = component.style().hasDecoration(TextDecoration.BOLD);
        this.italic = component.style().hasDecoration(TextDecoration.ITALIC);
        this.underlined = component.style().hasDecoration(TextDecoration.UNDERLINED);
        this.strikethrough = component.style().hasDecoration(TextDecoration.STRIKETHROUGH);
        this.obfuscated = component.style().hasDecoration(TextDecoration.OBFUSCATED);

        // properties
        this.insertion = component.insertion();

        // events
        if (component.clickEvent() != null) {
            this.clickEvent = new ClickEvent(component.clickEvent());
        }

        if (component.hoverEvent() != null) {
            this.hoverEvent = new HoverEvent(component.hoverEvent());
        }

        // children
        if (!component.children().isEmpty()) {
            for (final Component child : component.children()) {
                this.extra.add(AbstractComponent.parse(child));
            }
        }
    }

    @Override
    public String toMiniMessage() {
        final StringBuilder builder = new StringBuilder();
        if (color != null) {
            builder.append("<").append(color).append(">");
        }

        if (shadowColor != null) {
            builder.append("<shadow:").append(shadowColor).append(">");
        }

        if (bold) {
            builder.append("<bold>");
        }

        if (italic) {
            builder.append("<italic>");
        } else if (forceUnitalic) {
            builder.append("<!italic>");
        }

        if (underlined) {
            builder.append("<underlined>");
        }

        if (strikethrough) {
            builder.append("<strikethrough>");
        }

        if (obfuscated) {
            builder.append("<obfuscated>");
        }

        if (insertion != null) {
            builder.append("<insert:").append(insertion).append(">");
        }

        if (clickEvent != null) {
            builder.append(clickEvent.toMiniMessage());
        }

        if (hoverEvent != null) {
            builder.append(hoverEvent.toMiniMessage());
        }

        if (text != null && !text.isEmpty()) {
            builder.append(text);
        }

        for (final AbstractComponent component : this.extra) {
            builder.append(component.toMiniMessage());
        }

        if (hoverEvent != null) {
            builder.append("</hover>");
        }

        if (clickEvent != null) {
            builder.append("</click>");
        }

        if (insertion != null) {
            builder.append("</insert>");
        }

        if (obfuscated) {
            builder.append("</obfuscated>");
        }

        if (strikethrough) {
            builder.append("</strikethrough>");
        }

        if (underlined) {
            builder.append("</underlined>");
        }

        if (italic) {
            builder.append("</italic>");
        } else if (forceUnitalic) {
            builder.append("</!italic>");
        }

        if (bold) {
            builder.append("</bold>");
        }

        if (shadowColor != null) {
            builder.append("</shadow>");
        }

        if (color != null) {
            builder.append("</").append(color).append(">");
        }

        return builder.toString();
    }

    public static final class Adapter implements JsonSerializer<TextfulComponent>, JsonDeserializer<TextfulComponent> {

        @Override
        public JsonElement serialize(
                final TextfulComponent component,
                final Type type,
                final JsonSerializationContext context
        ) {
            final JsonObject jsonObject = new JsonObject();

            if (component.text != null) {
                jsonObject.addProperty("text", component.text);
            }

            if (component.color != null) {
                jsonObject.addProperty("color", component.color);
            }

            if (component.shadowColor != null) {
                jsonObject.addProperty("shadowColor", component.shadowColor);
            }

            if (component.bold) {
                jsonObject.addProperty("bold", true);
            }

            if (component.italic || component.forceUnitalic) {
                jsonObject.addProperty("italic", component.italic);
            }

            if (component.underlined) {
                jsonObject.addProperty("underlined", true);
            }

            if (component.strikethrough) {
                jsonObject.addProperty("strikethrough", true);
            }

            if (component.obfuscated) {
                jsonObject.addProperty("obfuscated", true);
            }

            if (component.insertion != null) {
                jsonObject.addProperty("insertion", component.insertion);
            }

            if (component.clickEvent != null) {
                jsonObject.add("clickEvent", context.serialize(component.clickEvent));
            }

            if (component.hoverEvent != null) {
                jsonObject.add("hoverEvent", context.serialize(component.hoverEvent));
            }

            if (!component.extra.isEmpty()) {
                final JsonArray extraArray = new JsonArray();
                for (final AbstractComponent extraComponent : component.extra) {
                    extraArray.add(context.serialize(extraComponent));
                }
                jsonObject.add("extra", extraArray);
            }

            return jsonObject;
        }

        @Override
        public TextfulComponent deserialize(
                final JsonElement json,
                final Type type,
                final JsonDeserializationContext context
        ) throws JsonParseException {
            final JsonObject jsonObject = json.getAsJsonObject();
            final TextfulComponent component = new TextfulComponent();

            if (jsonObject.has("component")) {
                // New format
                JsonObject componentObject = jsonObject.getAsJsonObject("component");
                component.text = componentObject.get("content").getAsString();

                if (componentObject.has("style")) {
                    JsonObject styleObject = componentObject.getAsJsonObject("style");
                    if (styleObject.has("color")) {
                        JsonObject colorObject = styleObject.getAsJsonObject("color");
                        component.color = String.format("#%06X", colorObject.get("value").getAsInt());
                    }
                    if (styleObject.has("shadowColor")) {
                        JsonObject shadowColorObject = styleObject.getAsJsonObject("shadowColor");
                        component.shadowColor = String.format("#%06X", shadowColorObject.get("value").getAsInt());
                    }
                    if (styleObject.has("decorations")) {
                        JsonObject decorationsObject = styleObject.getAsJsonObject("decorations");
                        component.bold = decorationsObject.has("bold") && decorationsObject.get("bold").getAsBoolean();
                        component.italic = decorationsObject.has("italic") && decorationsObject.get("italic").getAsBoolean();
                        component.underlined = decorationsObject.has("underlined") && decorationsObject.get("underlined").getAsBoolean();
                        component.strikethrough = decorationsObject.has("strikethrough") && decorationsObject.get("strikethrough").getAsBoolean();
                        component.obfuscated = decorationsObject.has("obfuscated") && decorationsObject.get("obfuscated").getAsBoolean();
                    }
                }

                if (componentObject.has("children")) {
                    JsonArray childrenArray = componentObject.getAsJsonArray("children");
                    for (JsonElement element : childrenArray) {
                        TextfulComponent childComponent = context.deserialize(element, TextfulComponent.class);
                        if (childComponent != null) {
                            component.extra.add(childComponent);
                        }
                    }
                }
            } else {
                // Old format
                component.text = jsonObject.has("text") ? jsonObject.get("text").getAsString() : null;
                component.color = jsonObject.has("color") ? jsonObject.get("color").getAsString() : null;
                component.shadowColor = jsonObject.has("shadowColor") ? jsonObject.get("shadowColor").getAsString() : null;
                component.bold = jsonObject.has("bold") && jsonObject.get("bold").getAsBoolean();
                component.italic = jsonObject.has("italic") && jsonObject.get("italic").getAsBoolean();
                component.underlined = jsonObject.has("underlined") && jsonObject.get("underlined").getAsBoolean();
                component.strikethrough = jsonObject.has("strikethrough") && jsonObject.get("strikethrough").getAsBoolean();
                component.obfuscated = jsonObject.has("obfuscated") && jsonObject.get("obfuscated").getAsBoolean();
                component.insertion = jsonObject.has("insertion") ? jsonObject.get("insertion").getAsString() : null;

                final JsonElement clickEventElement = JsonUtil.findElement(jsonObject, "clickEvent", "click_event");
                component.clickEvent = clickEventElement != null ? context.deserialize(clickEventElement, ClickEvent.class) : null;

                final JsonElement hoverEventElement = JsonUtil.findElement(jsonObject, "hoverEvent", "hover_event");
                component.hoverEvent = hoverEventElement != null ? context.deserialize(hoverEventElement, HoverEvent.class) : null;

                if (jsonObject.has("extra")) {
                    final JsonArray extraArray = jsonObject.getAsJsonArray("extra");
                    for (final JsonElement element : extraArray) {
                        final AbstractComponent extraComponent = AbstractComponent.parse(element);
                        if (extraComponent != null) {
                            component.extra.add(extraComponent);
                        }
                    }
                }
            }

            return component;
        }
    }
}
