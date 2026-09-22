package to.itsme.itsmyconfig.processor.packetevents.packet;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import net.kyori.adventure.text.Component;
import to.itsme.itsmyconfig.processor.packetevents.PacketUtil;
import to.itsme.itsmyconfig.processor.packetevents.PEventsProcessor;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

public class ChatMessageProcessor implements PEventsProcessor {

    public static final ChatMessageProcessor INSTANCE = new ChatMessageProcessor();

    @Override
    public String name() {
        return "CHAT_MESSAGE";
    }

    @Override
    public void process(final PacketSendEvent event) {
        PacketUtil.startDebug(this);
        final WrapperPlayServerChatMessage wrapper = new WrapperPlayServerChatMessage(event);
        final Component result = PacketUtil.processComponent(event.getPlayer(), wrapper.getMessage().getChatContent());

        if (result == null) {
            return;
        }

        if (result.equals(Component.empty())) {
            event.setCancelled(true);
            return;
        }

        event.markForReEncode(true);
        wrapper.getMessage().setChatContent(result);
        Utilities.debug(() -> Strings.DEBUG_HYPHEN);
    }

}
