package to.itsme.itsmyconfig;

import org.jetbrains.annotations.NotNull;
import to.itsme.itsmyconfig.api.ItsMyConfigAPI;
import to.itsme.itsmyconfig.processor.PacketListener;

public class DefaultIMCAPI implements ItsMyConfigAPI {

    private final ItsMyConfig plugin;

    public DefaultIMCAPI(final ItsMyConfig plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull PacketListener getPacketListener() {
        return this.plugin.processorManager.getListener();
    }

    /* Adventure has been relocated *again* inside ItsMyConfig
    @Override
    public @NotNull Component translate(String text, TagResolver... args) {
        return Utilities.translate(text, args);
    }

    @Override
    public @NotNull Component translate(String text, OfflinePlayer player, TagResolver... args) {
        return Utilities.translate(text, player, args);
    }

    @Override
    public @NotNull Component translate(String text, Player player, TagResolver... args) {
        return Utilities.translate(text, player, args);
    }*/

}
