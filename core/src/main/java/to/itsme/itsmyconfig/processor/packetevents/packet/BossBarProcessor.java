package to.itsme.itsmyconfig.processor.packetevents.packet;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBossBar;
import net.kyori.adventure.text.Component;
import to.itsme.itsmyconfig.processor.packetevents.PEventsProcessor;
import to.itsme.itsmyconfig.processor.packetevents.PacketUtil;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

public class BossBarProcessor implements PEventsProcessor {

    public static final BossBarProcessor INSTANCE = new BossBarProcessor();

    @Override
    public String name() {
        return "BOSSBAR";
    }

    @Override
    public void process(final PacketSendEvent event) {
        PacketUtil.startDebug(this);
        final WrapperPlayServerBossBar wrapper = new WrapperPlayServerBossBar(event);
        if (wrapper.getAction() != WrapperPlayServerBossBar.Action.ADD && wrapper.getAction() != WrapperPlayServerBossBar.Action.UPDATE_TITLE) {
            return;
        }

        final Component result = PacketUtil.processComponent(event.getPlayer(), wrapper.getTitle());
        if (result == null) {
            return;
        }

        if (result.equals(Component.empty())) {
            event.setCancelled(true);
            return;
        }

        event.markForReEncode(true);
        wrapper.setTitle(result);
        Utilities.debug(() -> Strings.DEBUG_HYPHEN);
    }

}
