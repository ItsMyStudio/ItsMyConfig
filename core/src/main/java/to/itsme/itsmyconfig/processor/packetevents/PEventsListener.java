package to.itsme.itsmyconfig.processor.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.Plugin;
import to.itsme.itsmyconfig.processor.PacketListener;
import to.itsme.itsmyconfig.processor.packetevents.packet.ActionbarProcessor;
import to.itsme.itsmyconfig.processor.packetevents.packet.BossBarProcessor;
import to.itsme.itsmyconfig.processor.packetevents.packet.DisconnectProcessor;
import to.itsme.itsmyconfig.processor.packetevents.packet.SystemChatMessageProcessor;
import to.itsme.itsmyconfig.processor.packetevents.packet.title.SubtitleProcessor;
import to.itsme.itsmyconfig.processor.packetevents.packet.title.TitleProcessor;

import java.util.EnumMap;
import java.util.Map;

public class PEventsListener implements PacketListener, com.github.retrooper.packetevents.event.PacketListener {

    private final Plugin plugin;
    private PacketListenerCommon common;

    /**
     * {@link PacketType.Play.Server} is an enum, so an {@link EnumMap} indexes by
     * {@code ordinal()} and avoids hashing the key on every outgoing packet.
     */
    private final Map<PacketType.Play.Server, PEventsProcessor> packetHandlerMap = createPacketHandlerMap();

    public PEventsListener(final Plugin plugin) {
        this.plugin = plugin;
    }

    private static Map<PacketType.Play.Server, PEventsProcessor> createPacketHandlerMap() {
        final EnumMap<PacketType.Play.Server, PEventsProcessor> handlers = new EnumMap<>(PacketType.Play.Server.class);

        handlers.put(PacketType.Play.Server.SYSTEM_CHAT_MESSAGE, SystemChatMessageProcessor.INSTANCE);
        handlers.put(PacketType.Play.Server.DISCONNECT, DisconnectProcessor.INSTANCE);
        handlers.put(PacketType.Play.Server.ACTION_BAR, ActionbarProcessor.INSTANCE);
        handlers.put(PacketType.Play.Server.BOSS_BAR, BossBarProcessor.INSTANCE);

        // titles
        handlers.put(PacketType.Play.Server.SET_TITLE_TEXT, TitleProcessor.INSTANCE);
        handlers.put(PacketType.Play.Server.SET_TITLE_SUBTITLE, SubtitleProcessor.INSTANCE);

        return handlers;
    }

    /**
     * Creates and loads the shaded PacketEvents instance, once.
     * <p>
     * Loading injects netty handlers into the server channel (and so into every
     * new connection), so only call this when PacketEvents is the listener that
     * will actually be used. Normally called from {@code onLoad}.
     */
    public static void loadPacketEvents(final Plugin plugin) {
        if (PacketEvents.getAPI() == null) {
            PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin));
        }
        PacketEvents.getAPI().load();
    }

    @Override
    public String name() {
        return "PacketEvents";
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void load() {
        // Normally already done in onLoad. This covers a listener choice that
        // changed between onLoad and onEnable (for example a config migration).
        loadPacketEvents(plugin);
        // Unmodified packets must pass through untouched; processors opt in with markForReEncode(true).
        PacketEvents.getAPI().getSettings().reEncodeByDefault(false);
        PacketEvents.getAPI().init();
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

        handler.process(event);
    }

    @Override
    public void close() {
        PacketEvents.getAPI().getEventManager().unregisterListener(this.common);
        PacketEvents.getAPI().terminate();
    }

}
