package to.itsme.itsmyconfig.processor.protocollib.packet;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.processor.protocollib.PLibProcessor;
import to.itsme.itsmyconfig.util.AdventureUtil;
import to.itsme.itsmyconfig.util.IMCSerializer;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;
import to.itsme.itsmyconfig.util.Versions;

import java.util.Optional;

public class ServerAdventureProcessor implements PLibProcessor {

    public static final ServerAdventureProcessor INSTANCE = new ServerAdventureProcessor();

    @Override
    public String name() {
        return "SERVER_ADVENTURE";
    }

    @Override
    public boolean canHandle(final PacketContainer container) {
        if (!Versions.IS_PAPER || Versions.isBelow(1, 16, 0)) {
            return false;
        }
        final StructureModifier<Object> modifier = container.getModifier().withType(AdventureComponentConverter.getComponentClass());
        return modifier.size() == 1 && modifier.readSafely(0) != null;
    }

    @Override
    public void process(final PacketEvent event) {
        final PacketContainer container = event.getPacket();
        final Player player = event.getPlayer();

        Utilities.debug(() -> "################# CHAT PACKET #################\nProcessing packet SERVER_ADVENTURE");

        if (!Versions.IS_PAPER || Versions.isBelow(1, 16, 0)) {
            Utilities.debug(() -> "Packet is null or empty\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final StructureModifier<Object> modifier = container.getModifier().withType(AdventureComponentConverter.getComponentClass());
        if (modifier.size() != 1) {
            Utilities.debug(() -> "Packet is null or empty\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final Object component = modifier.readSafely(0);
        if (component == null) {
            Utilities.debug(() -> "Packet is null or empty\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final String message = IMCSerializer.toMiniMessage(AdventureUtil.toComponent(component));
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

        final StructureModifier<Object> writeModifier = container.getModifier().withType(AdventureComponentConverter.getComponentClass());
        writeModifier.write(0, AdventureComponentConverter.fromJsonAsObject(
                Utilities.GSON_SERIALIZER.serialize(translated)
        ));

        Utilities.debug(() -> Strings.DEBUG_HYPHEN);
    }

}
