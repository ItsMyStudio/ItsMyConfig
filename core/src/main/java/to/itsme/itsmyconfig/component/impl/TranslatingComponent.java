package to.itsme.itsmyconfig.component.impl;

import com.google.gson.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.format.TextColor;
import to.itsme.itsmyconfig.component.AbstractComponent;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TranslatingComponent extends AbstractComponent {

    private String key, color;
    private final List<AbstractComponent> with = new LinkedList<>();

    /**
     * Empty Constructor
     */
    public TranslatingComponent() {}

    /**
     * {@link TranslatableComponent} converter to a {@link TranslatingComponent}
     */
    public TranslatingComponent(final TranslatableComponent component) {
        this.key = component.key();

        final TextColor color = component.color();
        if (color != null) {
            this.color = color.asHexString();
        }

        List<TranslationArgument> argumentLikes = component.arguments();
        List<Component> args = new ArrayList<>(argumentLikes.size());
        for (TranslationArgument argumentLike : argumentLikes) {
            args.add(argumentLike.asComponent());
        }
        if (!args.isEmpty()) {
            for (final Component arg : args) {
                extra.add(AbstractComponent.parse(arg));
            }
        }

        if (!component.children().isEmpty()) {
            for (final Component child : component.children()) {
                extra.add(AbstractComponent.parse(child));
            }
        }
    }

    @Override
    public String toMiniMessage() {
        final StringBuilder builder = new StringBuilder();
        if (color != null) {
            builder.append("<color:").append(color).append(">");
        }
        builder.append("<lang:").append(key);
        for (final AbstractComponent component : this.with) {
            builder.append(":\"").append(component.toMiniMessage()).append("\"");
        }
        builder.append(">");

        for (final AbstractComponent component : this.extra) {
            builder.append(component.toMiniMessage());
        }

        if (color != null) {
            builder.append("</color>");
        }
        return builder.toString();
    }

    public static final class Adapter implements JsonSerializer<TranslatingComponent>, JsonDeserializer<TranslatingComponent> {

        @Override
        public JsonElement serialize(
                final TranslatingComponent component,
                final Type type,
                final JsonSerializationContext context
        ) {
            final JsonObject jsonObject = new JsonObject();

            // add whether it's null or not to make sure it's a translatable component
            jsonObject.addProperty("translate", component.key);

            if (component.color != null) {
                jsonObject.addProperty("color", component.color);
            }

            if (!component.with.isEmpty()) {
                final JsonArray extraArray = new JsonArray();
                for (final AbstractComponent extraComponent : component.with) {
                    extraArray.add(context.serialize(extraComponent));
                }
                jsonObject.add("with", extraArray);
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
        public TranslatingComponent deserialize(
                final JsonElement json,
                final Type type,
                final JsonDeserializationContext jsonDeserializationContext
        ) throws JsonParseException {
            final JsonObject jsonObject = json.getAsJsonObject();
            final TranslatingComponent component = new TranslatingComponent();
            component.key = jsonObject.has("translate") ? jsonObject.get("translate").getAsString() : null;
            component.color = jsonObject.has("color") ? jsonObject.get("color").getAsString() : null;

            if (jsonObject.has("with")) {
                final JsonArray extraArray = jsonObject.getAsJsonArray("with");
                for (final JsonElement element : extraArray) {
                    final AbstractComponent extraComponent = AbstractComponent.parse(element);
                    if (extraComponent != null) {
                        component.with.add(extraComponent);
                    }
                }
            }

            if (jsonObject.has("extra")) {
                final JsonArray extraArray = jsonObject.getAsJsonArray("extra");
                for (final JsonElement element : extraArray) {
                    final AbstractComponent extraComponent = AbstractComponent.parse(element);
                    if (extraComponent != null) {
                        component.extra.add(extraComponent);
                    }
                }
            }

            return component;
        }

    }

}
