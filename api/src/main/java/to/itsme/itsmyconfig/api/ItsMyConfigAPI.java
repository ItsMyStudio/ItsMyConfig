package to.itsme.itsmyconfig.api;

import org.jetbrains.annotations.NotNull;
import to.itsme.itsmyconfig.processor.PacketListener;

public interface ItsMyConfigAPI {

    @NotNull PacketListener getPacketListener();
    /*
    Adventure has been relocated *again* inside ItsMyConfig
    @NotNull Component translate(String text, TagResolver... args);
    @NotNull Component translate(String text, Player player, TagResolver... args);
    @NotNull Component translate(String text, OfflinePlayer player, TagResolver... args);
    */

}
