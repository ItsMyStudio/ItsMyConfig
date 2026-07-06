package to.itsme.itsmyconfig.component.event;

import com.google.gson.*;
import net.kyori.adventure.text.Component;
import to.itsme.itsmyconfig.component.AbstractComponent;
import to.itsme.itsmyconfig.component.impl.TextfulComponent;
import to.itsme.itsmyconfig.util.JsonUtil;
import to.itsme.itsmyconfig.util.Versions;

import java.lang.reflect.Type;
import java.util.UUID;

@SuppressWarnings({"rawtypes", "unused"})
public class HoverEvent {

    private String action;
    private Object value;

    /**
     * Empty Constructor
     */
    public HoverEvent() {
    }

    /**
     * {@link net.kyori.adventure.text.event.HoverEvent} convetrer to a {@link HoverEvent}
     */
    public HoverEvent(net.kyori.adventure.text.event.HoverEvent event) {
        this.action = event.action().toString();
        switch (this.action) {
            case "show_text":
                this.value = AbstractComponent.parse((Component) event.value());
                break;
            case "show_achievement":
                this.value = new TextfulComponent(event.value().toString());
                break;
            case "show_item":
                this.value = new ShowItem((net.kyori.adventure.text.event.HoverEvent.ShowItem) event.value());
                break;
            case "show_entity":
                this.value = new ShowEntity((net.kyori.adventure.text.event.HoverEvent.ShowEntity) event.value());
                break;
        }
    }

    public String toMiniMessage() {
        if (this.value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("<hover:").append(this.action).append(":\"");
        switch (this.value) {
            case String s -> builder.append(this.value);
            case AbstractComponent component -> builder.append(component.toMiniMessage());
            case ShowItem item -> builder.append(item.toMMArg());
            case ShowEntity entity -> builder.append(entity.toMMArg());
            default -> {}
        }
        return builder.append("\">").toString();
    }

    public static final class ShowItem {
        private String id;
        private String tag;
        private int count;

        public ShowItem() {
        }

        public ShowItem(net.kyori.adventure.text.event.HoverEvent.ShowItem value) {
            this.id = value.item().value();
            this.count = value.count();
            if (value.nbt() != null) {
                this.tag = value.nbt().string();
            }
        }

        public String toMMArg() {
            return this.id + ":" + this.count + (this.tag == null ? "" : (":\"" + this.tag + "\""));
        }
    }

    public static final class ShowEntity {

        private String type;
        private UUID id;
        private AbstractComponent name;

        public ShowEntity() {
        }

        public ShowEntity(net.kyori.adventure.text.event.HoverEvent.ShowEntity value) {
            this.type = value.type().value();
            this.id = value.id();
            if (value.name() != null)
                this.name = AbstractComponent.parse(value.name());
        }

        public String toMMArg() {
            final String nameStr = this.name != null ? this.name.toMiniMessage() : "";
            return this.type + ":" + this.id + ":\"" + nameStr + "\"";
        }
    }

    public static final class Adapter implements JsonSerializer<HoverEvent>, JsonDeserializer<HoverEvent> {
        private static final boolean MODERN = Versions.isOrOver(1, 21, 5);

        @Override
        public JsonElement serialize(HoverEvent event, Type type, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("action", event.action);

            if (event.value == null) return json;

            switch (event.action) {
                case "show_text" -> {
                    if (event.value instanceof TextfulComponent tc) {
                        json.add("value", tc.toJsonElement());
                    }
                }
                case "show_item" -> {
                    if (event.value instanceof ShowItem item) {
                        if (MODERN) {
                            json.addProperty("id", item.id);
                            json.addProperty("count", item.count);
                            if (item.tag != null && !item.tag.isEmpty()) {
                                json.addProperty("tag", item.tag);
                            }
                        } else {
                            json.add("value", context.serialize(item));
                        }
                    }
                }
                case "show_entity" -> {
                    if (event.value instanceof ShowEntity entity) {
                        if (MODERN) {
                            json.addProperty("id", entity.type); // renamed from type
                            json.addProperty("uuid", entity.id.toString()); // renamed from id
                            if (entity.name != null)
                                json.add("name", context.serialize(entity.name));
                        } else {
                            json.add("value", context.serialize(entity));
                        }
                    }
                }
                case "show_achievement" -> {
                    if (event.value instanceof String str) {
                        json.addProperty("value", str);
                    }
                }
            }
            return json;
        }

        @Override
        public HoverEvent deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            HoverEvent event = new HoverEvent();
            JsonObject obj = json.getAsJsonObject();
            event.action = obj.get("action").getAsString();

            JsonElement element = JsonUtil.findElement(obj, "value", "contents", "text");

            switch (event.action) {
                case "show_text" -> {
                    JsonElement textElement = JsonUtil.findElement(obj, "text", "value", "contents");
                    if (textElement != null) {
                        if (textElement.isJsonPrimitive()) {
                            event.value = new TextfulComponent(textElement.getAsString());
                        } else if (textElement.isJsonArray()) {
                            event.value = AbstractComponent.parse(textElement.getAsJsonArray());
                        } else {
                            event.value = context.deserialize(textElement, TextfulComponent.class);
                        }
                    }
                }
                case "show_item" -> {
                    if (MODERN) {
                        ShowItem item = new ShowItem();
                        item.id = obj.get("id").getAsString();
                        item.count = obj.has("count") ? obj.get("count").getAsInt() : 1;
                        if (obj.has("tag")) item.tag = obj.get("tag").getAsString();
                        event.value = item;
                    } else {
                        event.value = context.deserialize(element, ShowItem.class);
                    }
                }
                case "show_entity" -> {
                    if (MODERN) {
                        ShowEntity entity = new ShowEntity();
                        entity.type = obj.get("id").getAsString(); // renamed from type
                        entity.id = UUID.fromString(obj.get("uuid").getAsString()); // renamed from id
                        if (obj.has("name")) {
                            entity.name = context.deserialize(obj.get("name"), TextfulComponent.class);
                        }
                        event.value = entity;
                    } else {
                        event.value = context.deserialize(element, ShowEntity.class);
                    }
                }
                case "show_achievement" -> {
                    if (element != null && element.isJsonPrimitive()) {
                        event.value = element.getAsString();
                    }
                }
            }

            return event;
        }
    }

}
