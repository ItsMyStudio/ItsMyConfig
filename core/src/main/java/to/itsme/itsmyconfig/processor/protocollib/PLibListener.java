package to.itsme.itsmyconfig.processor.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.processor.PacketListener;
import to.itsme.itsmyconfig.processor.protocollib.packet.BungeeComponentProcessor;
import to.itsme.itsmyconfig.processor.protocollib.packet.JsonProcessor;
import to.itsme.itsmyconfig.processor.protocollib.packet.ServerAdventureProcessor;
import to.itsme.itsmyconfig.processor.protocollib.packet.WrappedComponentProcessor;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PLibListener extends PacketAdapter implements PacketListener {

    private static final List<PLibProcessor> PROCESSORS = List.of(
            ServerAdventureProcessor.INSTANCE,
            WrappedComponentProcessor.INSTANCE,
            BungeeComponentProcessor.INSTANCE,
            JsonProcessor.INSTANCE
    );

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

        final PLibProcessor processor = resolveProcessor(container);
        if (processor == null) {
            return;
        }

        processor.process(event);
        Utilities.debug(() -> Strings.DEBUG_HYPHEN);
    }

    private PLibProcessor resolveProcessor(final PacketContainer container) {
        final PacketType type = container.getType();
        final PLibProcessor cached = packetTypeMap.get(type);
        if (cached != null) {
            Utilities.debug(() -> "Using " + cached.name() + " to unpack the packet (cached)");
            return cached;
        }

        Utilities.debug(() -> "Figuring " + type.name() + "'s packet processor..");
        for (final PLibProcessor processor : PROCESSORS) {
            Utilities.debug(() -> "Trying " + processor.name() + "..");
            if (processor.canHandle(container)) {
                if (cacheProcessors) {
                    packetTypeMap.put(type, processor);
                    Utilities.debug(() -> "Caching " + processor.name() + " for packet " + type.name());
                } else {
                    Utilities.debug(() -> "Matched processor " + processor.name() + " for packet " + type.name());
                }
                return processor;
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
