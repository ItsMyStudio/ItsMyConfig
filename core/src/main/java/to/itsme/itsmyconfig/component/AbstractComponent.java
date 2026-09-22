package to.itsme.itsmyconfig.component;

import com.google.gson.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import to.itsme.itsmyconfig.component.event.ClickEvent;
import to.itsme.itsmyconfig.component.event.HoverEvent;
import to.itsme.itsmyconfig.component.impl.KeybindedComponent;
import to.itsme.itsmyconfig.component.impl.PseudoComponent;
import to.itsme.itsmyconfig.component.impl.TextfulComponent;
import to.itsme.itsmyconfig.component.impl.TranslatingComponent;
import to.itsme.itsmyconfig.util.Utilities;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractComponent {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ClickEvent.class, new ClickEvent.Adapter())
            .registerTypeAdapter(HoverEvent.class, new HoverEvent.Adapter())
            .registerTypeAdapter(TextfulComponent.class, new TextfulComponent.Adapter())
            .registerTypeAdapter(KeybindedComponent.class, new KeybindedComponent.Adapter())
            .registerTypeAdapter(TranslatingComponent.class, new TranslatingComponent.Adapter())
            .create();

    protected final List<AbstractComponent> extra = new LinkedList<>();

    /**
     * Translates {@link AbstractComponent} to a JSON String
     *
     * @param component The parsed {@link AbstractComponent}.
     * @return a JSON string representing the component.
     */
    @SuppressWarnings("unused")
    public static String toJson(@NotNull final AbstractComponent component) {
        return GSON.toJson(component);
    }

    /**
     * Parses a json string to an {@link AbstractComponent}
     *
     * @param json The parsed json String.
     * @return an instance of {@link AbstractComponent}.
     */
    public static AbstractComponent parse(@NotNull final String json) {
        try {
            return parse(JsonParser.parseString(json));
        } catch (final Throwable throwable) {
            Utilities.debug(() -> "Couldn't parse json: " + json + " so returning PseudoComponent", throwable);
            return new PseudoComponent(json);
        }
    }

    /**
     * Parses a {@link Component} into an {@link AbstractComponent}
     *
     * @param component The parsed {@link Component}.
     * @return an instance of {@link AbstractComponent}.
     */
    public static AbstractComponent parse(@NotNull final Component component) {
        if (component instanceof TextComponent) {
            return new TextfulComponent((TextComponent) component);
        } else if (component instanceof KeybindComponent) {
            return new KeybindedComponent((KeybindComponent) component);
        } else if (component instanceof TranslatableComponent) {
            return new TranslatingComponent((TranslatableComponent) component);
        }
        return parse(Utilities.GSON_SERIALIZER.serialize(component));
    }

    /**
     * Parses {@link JsonElement} to an {@link AbstractComponent}
     *
     * @param element The parsed {@link JsonElement}.
     * @return an instance of {@link AbstractComponent}.
     */
    public static AbstractComponent parse(@NotNull final JsonElement element) {
        if (element.isJsonObject()) {
            final JsonObject jsonObject = element.getAsJsonObject();
            if (jsonObject.has("keybind")) {
                return GSON.fromJson(element, KeybindedComponent.class);
            } else if (jsonObject.has("translate")) {
                return GSON.fromJson(element, TranslatingComponent.class);
            }
            return GSON.fromJson(element, TextfulComponent.class);
        } else if (element.isJsonArray()) {
            final TextfulComponent component = new TextfulComponent();
            for (final JsonElement found : element.getAsJsonArray()) {
                component.extra.add(parse(found));
            }
            return component;
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return new TextfulComponent(element.getAsString());
        }

        return new PseudoComponent(element.getAsString());
    }

    public abstract String toMiniMessage();

    public String toJson() {
        return GSON.toJson(this);
    }

    public JsonElement toJsonElement() {
        return GSON.toJsonTree(this);
    }

}
