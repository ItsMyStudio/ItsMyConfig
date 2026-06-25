package to.itsme.itsmyconfig.processor.protocollib.packet;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.processor.protocollib.PLibProcessor;
import to.itsme.itsmyconfig.util.IMCSerializer;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

import java.util.Optional;

@SuppressWarnings("deprecation")
public class BungeeComponentProcessor implements PLibProcessor {

    public static final BungeeComponentProcessor INSTANCE = new BungeeComponentProcessor();

    @Override
    public String name() {
        return "BUNGEE_COMPONENT";
    }

    @Override
    public boolean canHandle(final PacketContainer container) {
        return container.getModifier().withType(TextComponent.class).size() == 1;
    }

    @Override
    public void process(final PacketEvent event) {
        final PacketContainer container = event.getPacket();
        final Player player = event.getPlayer();

        Utilities.debug(() -> "################# CHAT PACKET #################\nProcessing packet BUNGEE_COMPONENT");

        final StructureModifier<TextComponent> textComponentModifier = container.getModifier().withType(TextComponent.class);
        if (textComponentModifier.size() != 1) {
            Utilities.debug(() -> "Packet is null or empty\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final TextComponent textComponent = textComponentModifier.readSafely(0);
        if (textComponent == null) {
            Utilities.debug(() -> "Packet is null or empty\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final String message = IMCSerializer.toMiniMessage(Utilities.BUNGEE_SERIALIZER.deserialize(toArray(textComponent)));
        Utilities.debug(() -> "Found message: " + message);

        final Optional<String> parsed = Strings.parsePrefixedMessage(message);
        if (parsed.isEmpty()) {
            Utilities.debug(() -> "Message doesn't start w/ the symbol-prefix: " + message + "\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final Component translated = Utilities.translate(parsed.get(), player);
        if (translated.equals(Component.empty())) {
            event.setCancelled(true);
            Utilities.debug(() -> "Component is empty, cancelling...\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        Utilities.debug(() -> "Final Product: " + IMCSerializer.toMiniMessage(translated) + "\n" + "Overriding...");
        container.getModifier().withType(TextComponent.class).write(0, new TextComponent(
                Utilities.BUNGEE_SERIALIZER.serialize(translated)
        ));
        Utilities.debug(() -> Strings.DEBUG_HYPHEN);
    }

    public BaseComponent[] toArray(final TextComponent... components) {
        return components;
    }

}
