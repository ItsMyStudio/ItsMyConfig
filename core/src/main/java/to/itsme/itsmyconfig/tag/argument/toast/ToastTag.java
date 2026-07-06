package to.itsme.itsmyconfig.tag.argument.toast;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.tag.api.ArgumentsTag;
import to.itsme.itsmyconfig.util.Utilities;

public class ToastTag extends ArgumentsTag {

    private final ToastSender toastSender = new BukkitToastSender();

    @Override
    public String name() {
        return "toast";
    }

    @Override
    public int minArguments() {
        return 1;
    }

    @Override
    public int maxArguments() {
        return 3;
    }

    @Override
    public String process(final Player player, final String[] arguments) {
        String titleRaw = arguments[0];
        String iconRaw = arguments.length > 1 ? arguments[1] : "book";
        String frame = arguments.length > 2 ? arguments[2] : "task";

        Component title = Utilities.translate(titleRaw, player);

        Material icon;
        try {
            icon = Material.valueOf(iconRaw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            icon = Material.BOOK; // fallback icon
        }

        toastSender.sendToast(player, title, icon, frame);

        return "";
    }
}
