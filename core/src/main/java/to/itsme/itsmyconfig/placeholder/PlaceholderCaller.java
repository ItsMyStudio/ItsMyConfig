package to.itsme.itsmyconfig.placeholder;

import org.bukkit.OfflinePlayer;

@FunctionalInterface
public interface PlaceholderCaller {

    String call(OfflinePlayer player, String[] args);

}
