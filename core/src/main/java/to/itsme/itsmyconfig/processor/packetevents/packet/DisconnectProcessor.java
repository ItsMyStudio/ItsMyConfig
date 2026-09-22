package to.itsme.itsmyconfig.processor.packetevents.packet;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import net.kyori.adventure.text.Component;
import to.itsme.itsmyconfig.processor.packetevents.PacketUtil;
import to.itsme.itsmyconfig.processor.packetevents.PEventsProcessor;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

public class DisconnectProcessor implements PEventsProcessor {

    public static final DisconnectProcessor INSTANCE = new DisconnectProcessor();

    @Override
    public String name() {
        return "DISCONNECT";
    }

    @Override
    public void process(final PacketSendEvent event) {
        PacketUtil.startDebug(this);
        final WrapperPlayServerDisconnect wrapper = new WrapperPlayServerDisconnect(event);
        final Component result = PacketUtil.processComponent(event.getPlayer(), wrapper.getReason());

        if (result == null) {
            return;
        }

        if (result.equals(Component.empty())) {
            event.setCancelled(true);
            return;
        }

        event.markForReEncode(true);
        wrapper.setReason(result);
        Utilities.debug(() -> Strings.DEBUG_HYPHEN);
    }

}
