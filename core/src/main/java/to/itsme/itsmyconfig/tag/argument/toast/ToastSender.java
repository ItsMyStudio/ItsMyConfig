package to.itsme.itsmyconfig.tag.argument.toast;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Sends custom toast messages to players using the Minecraft advancements system.
 * <p>
 * A toast message appears in the top-right corner of the screen, similar to when
 * advancements are normally triggered. Implementations of this interface may vary
 * between Bukkit APIs, packet-based systems, or Adventure integrations.
 */
public interface ToastSender {

    /**
     * Sends a toast message to the specified player with the given title, frame type, and icon.
     *
     * @param player    The player who should receive the toast.
     * @param title     The main title of the toast (usually bold and gold).
     * @param icon      The item icon to display on the left side of the toast.
     * @param frameType The type of toast frame: "task", "goal", or "challenge".
     */
    void sendToast(Player player, Component title, Material icon, String frameType);
}
