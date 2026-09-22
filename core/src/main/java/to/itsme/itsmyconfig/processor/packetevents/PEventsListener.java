package to.itsme.itsmyconfig.processor.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import to.itsme.itsmyconfig.processor.PacketListener;
import to.itsme.itsmyconfig.processor.packetevents.packet.ActionbarProcessor;
import to.itsme.itsmyconfig.processor.packetevents.packet.BossBarProcessor;
import to.itsme.itsmyconfig.processor.packetevents.packet.DisconnectProcessor;
import to.itsme.itsmyconfig.processor.packetevents.packet.SystemChatMessageProcessor;
import to.itsme.itsmyconfig.processor.packetevents.packet.title.SubtitleProcessor;
import to.itsme.itsmyconfig.processor.packetevents.packet.title.TitleProcessor;

import java.util.Map;

public class PEventsListener implements PacketListener, com.github.retrooper.packetevents.event.PacketListener {

    private final Map<PacketType.Play.Server, PEventsProcessor> packetHandlerMap = Map.of(
            PacketType.Play.Server.SYSTEM_CHAT_MESSAGE, SystemChatMessageProcessor.INSTANCE,
            PacketType.Play.Server.DISCONNECT, DisconnectProcessor.INSTANCE,
            PacketType.Play.Server.ACTION_BAR, ActionbarProcessor.INSTANCE,
            PacketType.Play.Server.BOSS_BAR, BossBarProcessor.INSTANCE,

            // titles
            PacketType.Play.Server.SET_TITLE_TEXT, TitleProcessor.INSTANCE,
            PacketType.Play.Server.SET_TITLE_SUBTITLE, SubtitleProcessor.INSTANCE
    );
    private PacketListenerCommon common;

    @Override
    public String name() {
        return "PacketEvents";
    }

    @Override
    public void load() {
        this.common = PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.NORMAL);
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        final PacketTypeCommon type = event.getPacketType();

        if (!(type instanceof PacketType.Play.Server server)) {
            return;
        }

        final PEventsProcessor handler = packetHandlerMap.get(server);
        if (handler == null) {
            return;
        }

<<<<<<< HEAD
        Utilities.debug(() -> "################# CHAT PACKET #################\nProcessing packet " + server.name());

        // Convert to wrapped packet only once
        Object wrappedPacket = switch (server) {
            case CHAT_MESSAGE -> new WrapperPlayServerChatMessage(event);
            case SYSTEM_CHAT_MESSAGE -> new WrapperPlayServerSystemChatMessage(event);
            case DISCONNECT -> new WrapperPlayServerDisconnect(event);
            default -> null;
        };

        if (wrappedPacket == null) {
            return;
        }

        // Unpack the wrapped packet
        final PacketContent<?> packet;

        try {
            packet = ((PacketProcessor<Object>) processor).unpack(wrappedPacket);
        } catch (final Throwable ex) {
            Utilities.debug(() -> "Exception while unpacking the packet: ", ex);
            Utilities.debug(() -> Strings.DEBUG_HYPHEN);
            return;
        }

        if (packet == null || packet.isEmpty()) {
            Utilities.debug(() -> "Packet is null or empty\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final String message = packet.message();
        Utilities.debug(() -> "Found message: " + message);

        if (message.startsWith(FAIL_MESSAGE_PREFIX)) {
            Utilities.debug(() -> "Message send failure message, cancelling...");
            event.setCancelled(true);
            return;
        }

        final Optional<String> parsed = Strings.parsePrefixedMessage(message);
        if (parsed.isEmpty()) {
            Utilities.debug(() -> "Message doesn't start w/ the symbol-prefix: " + message + "\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final Player player = event.getPlayer();
        final Component translated = Utilities.translate(parsed.get(), player);
        if (translated.equals(Component.empty())) {
            event.setCancelled(true);
            Utilities.debug(() -> "Component is empty, cancelling...\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        Utilities.debug(() -> "Final Product: " + IMCSerializer.toMiniMessage(translated) + "\n" + "Overriding...");
        try {
            event.markForReEncode(true);
            packet.save(translated);
        } catch (final Throwable ex) {
            Utilities.debug(() -> "Exception while saving packet: ", ex);
        }
        Utilities.debug(() -> Strings.DEBUG_HYPHEN);
=======
        handler.process(event);
>>>>>>> 39afa1f44463bd1462b2497b6820e2ee86437eb5
    }

    @Override
    public void close() {
        if (this.common != null && PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(this.common);
        }
    }

}
