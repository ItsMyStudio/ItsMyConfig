package to.itsme.itsmyconfig.processor.packetevents.packet;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerActionBar;
import net.kyori.adventure.text.Component;
import to.itsme.itsmyconfig.processor.packetevents.PEventsProcessor;
import to.itsme.itsmyconfig.processor.packetevents.PacketUtil;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

public class ActionbarProcessor implements PEventsProcessor {

    public static final ActionbarProcessor INSTANCE = new ActionbarProcessor();

    @Override
    public String name() {
        return "ACTIONBAR";
    }

    @Override
    public void process(final PacketSendEvent event) {
        PacketUtil.startDebug(this);
        final WrapperPlayServerActionBar wrapper = new WrapperPlayServerActionBar(event);
        final Component result = PacketUtil.processComponent(event.getPlayer(), wrapper.getActionBarText());
        if (result == null) {
            return;
        }

        if (result.equals(Component.empty())) {
            event.setCancelled(true);
            return;
        }

        event.markForReEncode(true);
        wrapper.setActionBarText(result);
        Utilities.debug(() -> Strings.DEBUG_HYPHEN);
    }

}
