package to.itsme.itsmyconfig.processor.packetevents;

import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import to.itsme.itsmyconfig.processor.PacketContent;
import to.itsme.itsmyconfig.processor.PacketProcessor;
import to.itsme.itsmyconfig.util.IMCSerializer;

public class PEventsProcessor {

    public static final PacketProcessor<WrapperPlayServerChatMessage> CHAT_MESSAGE = new PacketProcessor<>() {
        @Override
        public String name() {
            return "CHAT_MESSAGE";
        }

        @Override
        public void edit(WrapperPlayServerChatMessage wrappedPacket, Component component) {
            ChatMessage chatMessage = wrappedPacket.getMessage();
            if (chatMessage != null) {
                chatMessage.setChatContent(component);
            }
        }

        @Override
        public @NotNull PacketContent<WrapperPlayServerChatMessage> unpack(WrapperPlayServerChatMessage wrappedPacket) {
            ChatMessage chatMessage = wrappedPacket.getMessage();
            Component internal = chatMessage != null ? chatMessage.getChatContent() : null;
            return new PacketContent<>(wrappedPacket, this, internal != null ? IMCSerializer.toMiniMessage(internal) : "");
        }
    };

    public static final PacketProcessor<WrapperPlayServerSystemChatMessage> SYSTEM_CHAT_MESSAGE = new PacketProcessor<>() {
        @Override
        public String name() {
            return "SYSTEM_CHAT_MESSAGE";
        }

        @Override
        public void edit(WrapperPlayServerSystemChatMessage wrappedPacket, Component component) {
            wrappedPacket.setMessage(component);
        }

        @Override
        public @NotNull PacketContent<WrapperPlayServerSystemChatMessage> unpack(WrapperPlayServerSystemChatMessage wrappedPacket) {
            Component internal = wrappedPacket.getMessage();
            return new PacketContent<>(wrappedPacket, this, internal != null ? IMCSerializer.toMiniMessage(internal) : "");
        }
    };

    public static final PacketProcessor<WrapperPlayServerDisconnect> DISCONNECT = new PacketProcessor<>() {
        @Override
        public String name() {
            return "DISCONNECT";
        }

        @Override
        public void edit(WrapperPlayServerDisconnect wrappedPacket, Component component) {
            wrappedPacket.setReason(component);
        }

        @Override
        public @NotNull PacketContent<WrapperPlayServerDisconnect> unpack(WrapperPlayServerDisconnect wrappedPacket) {
            Component internal = wrappedPacket.getReason();
            return new PacketContent<>(wrappedPacket, this, internal != null ? IMCSerializer.toMiniMessage(internal) : "");
        }
    };
}
