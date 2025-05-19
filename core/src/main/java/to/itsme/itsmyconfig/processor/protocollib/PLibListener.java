package to.itsme.itsmyconfig.processor.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.processor.PacketContent;
import to.itsme.itsmyconfig.processor.PacketListener;
import to.itsme.itsmyconfig.util.IMCSerializer;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

import java.util.HashMap;
import java.util.Map;

public final class PLibListener extends PacketAdapter implements PacketListener {

    /* Here we cache the packet check types for faster handling */
    private final Map<PacketType, PLibProcessor> packetTypeMap = new HashMap<>(4);
    private final boolean cacheProcessors;

    public PLibListener(
            final ItsMyConfig plugin,
            final boolean cacheProcessors
    ) {
        super(
                plugin,
                ListenerPriority.NORMAL,
                PacketType.Play.Server.CHAT,
                PacketType.Play.Server.SYSTEM_CHAT,
                PacketType.Play.Server.KICK_DISCONNECT
        );
        this.cacheProcessors = cacheProcessors;
    }

    @Override
    public String name() {
        return "ProtocolLib";
    }

    @Override
    public void load() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onPacketSending(final PacketEvent event) {
        final PacketContainer container = event.getPacket();
        final PacketType type = container.getType();
        Utilities.debug(() -> "################# CHAT PACKET #################\nProccessing packet " + type.name());
        final PacketContent<PacketContainer> packet = this.processPacket(container);
        if (packet == null || packet.isEmpty()) {
            Utilities.debug(() -> "Packet is null or empty\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final String message = packet.message();
        Utilities.debug(() -> "Found message: " + message);
        if (!Strings.startsWithSymbol(message)) {
            Utilities.debug(() -> "Message doesn't start w/ the symbol-prefix: " + message + "\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final Player player = event.getPlayer();
        final Component parsed = Utilities.translate(Strings.processMessage(message), player);
        if (parsed.equals(Component.empty())) {
            event.setCancelled(true);
            Utilities.debug(() -> "Component is empty, cancelling...\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        Utilities.debug(() -> "Final Product: " + IMCSerializer.toMiniMessage(parsed) + "\n" + "Overriding...");
        packet.save(parsed);
        Utilities.debug(() -> Strings.DEBUG_HYPHEN);
    }

    private PacketContent<PacketContainer> processPacket(final PacketContainer container) {
        final PacketType type = container.getType();
        final PLibProcessor foundProcessor = packetTypeMap.get(type);
        if (foundProcessor != null) {
            Utilities.debug(() -> "Using " + foundProcessor.name() + " to unpack the packet (cached)");
            return foundProcessor.unpack(container);
        }

        Utilities.debug(() -> "Figuring " + type.name() + "'s packet processor..");
        for (final PLibProcessor processor : PLibProcessor.values()) {
            Utilities.debug(() -> "Trying " + processor.name() + "..");
            final PacketContent<PacketContainer> unpacked = processor.unpack(container);
            if (unpacked != null) {
                if (cacheProcessors) {
                    packetTypeMap.put(type, processor);
                    Utilities.debug(() -> "Caching " + processor.name() + " for packet " + type.name());
                } else Utilities.debug(() -> "Matched processor " + processor.name() + " for packet " + type.name());
                return unpacked;
            }
            Utilities.debug(() -> "Didn't work, trying next (if there is) ..");
        }

        return null;
    }

    @Override
    public void close() {
        ProtocolLibrary.getProtocolManager().removePacketListener(this);
    }
}
