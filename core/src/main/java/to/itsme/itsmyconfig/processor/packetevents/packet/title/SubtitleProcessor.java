package to.itsme.itsmyconfig.processor.packetevents.packet.title;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleSubtitle;
import net.kyori.adventure.text.Component;
import to.itsme.itsmyconfig.processor.packetevents.PEventsProcessor;
import to.itsme.itsmyconfig.processor.packetevents.PacketUtil;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

public class SubtitleProcessor implements PEventsProcessor {

    public static final SubtitleProcessor INSTANCE = new SubtitleProcessor();

    @Override
    public String name() {
        return "SUBTITLE";
    }

    @Override
    public void process(final PacketSendEvent event) {
        PacketUtil.startDebug(this);
        final WrapperPlayServerSetTitleSubtitle wrapper = new WrapperPlayServerSetTitleSubtitle(event);
        final Component result = PacketUtil.processComponent(event.getPlayer(), wrapper.getSubtitle());
        if (result == null) {
            return;
        }

        if (result.equals(Component.empty())) {
            event.setCancelled(true);
            return;
        }

        event.markForReEncode(true);
        wrapper.setSubtitle(result);
        Utilities.debug(() -> Strings.DEBUG_HYPHEN);
    }

}
