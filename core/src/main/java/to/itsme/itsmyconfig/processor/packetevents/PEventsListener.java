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
            PacketType.Play.Server.TITLE, TitleProcessor.INSTANCE,
            PacketType.Play.Server.SET_TITLE_SUBTITLE, SubtitleProcessor.INSTANCE
    );
    private PacketListenerCommon common;

    @Override
    public String name() {
        return "PacketEvents";
    }

    @Override
    public void load() {
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
