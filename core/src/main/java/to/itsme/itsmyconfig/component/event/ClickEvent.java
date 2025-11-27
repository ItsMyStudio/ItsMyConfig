package to.itsme.itsmyconfig.component.event;

import com.google.gson.*;
import java.lang.reflect.Type;
import net.kyori.adventure.text.event.ClickEvent.Payload;
import to.itsme.itsmyconfig.util.Versions;

public class ClickEvent {

    private Action action;
    private String value;

    public ClickEvent() {}

    @SuppressWarnings("deprecation")
    public ClickEvent(net.kyori.adventure.text.event.ClickEvent event) {
        this.action = Action.fromName(event.action().toString());

        final Payload payload = event.payload();
        if (payload instanceof Payload.Text textPayload) {
            this.value = textPayload.value();
        } else if (payload instanceof Payload.Int intPayload) {
            this.value = String.valueOf(intPayload.integer());
        } else if (payload instanceof Payload.Dialog dialogPayload) {
            this.value = String.valueOf(dialogPayload.dialog()); // incorrect handling, but prevents errors
        } else if (payload instanceof Payload.Custom customPayload) {
            this.value = customPayload.nbt().string();
        } else {
            this.value = event.value();
        }
    }

    public String toMiniMessage() {
        return "<click:" + action.getName() + ":\"" + value + "\">";
    }

    public enum Action {
        OPEN_URL("open_url", "url"),
        OPEN_FILE("open_file", "path"),
        RUN_COMMAND("run_command", "command"),
        SUGGEST_COMMAND("suggest_command", "command"),
        CHANGE_PAGE("change_page", "page"),
        CUSTOM("minecraft:custom", "id"),
        SHOW_DIALOG("show_dialog", "dialog"),
        UNKNOWN("unknown", "value");

        private final String name;
        private final String field;

        Action(final String name, final String field) {
            this.name = name;
            this.field = field;
        }

        public String getName() {
            return name;
        }

        public String getField() {
            return field;
        }

        public static Action fromName(String name) {
            for (Action action : values()) {
                if (action.name.equalsIgnoreCase(name)) {
                    return action;
                }
            }
            return UNKNOWN;
        }
    }

    public static final class Adapter implements JsonSerializer<ClickEvent>, JsonDeserializer<ClickEvent> {
        private final boolean modern = Versions.isOrOver(1, 21, 5);

        public Adapter() {
        }

        @Override
        public JsonElement serialize(ClickEvent event, Type type, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("action", event.action.getName());

            if (modern) {
                String key = event.action.getField();
                if (event.action == Action.CHANGE_PAGE) {
                    try {
                        json.addProperty(key, Integer.parseInt(event.value));
                    } catch (NumberFormatException e) {
                        json.addProperty(key, 1);
                    }
                } else {
                    json.addProperty(key, event.value);
                }
            } else {
                json.addProperty("value", event.value);
            }

            return json;
        }

        @Override
        public ClickEvent deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String actionName = obj.has("action") ? obj.get("action").getAsString() : "unknown";

            Action action = Action.fromName(actionName);
            String value = null;

            if (obj.has(action.getField())) {
                value = obj.get(action.getField()).getAsString();
            } else if (obj.has("value")) {
                value = obj.get("value").getAsString();
            }

            ClickEvent event = new ClickEvent();
            event.action = action;
            event.value = value;
            return event;
        }
    }

}
