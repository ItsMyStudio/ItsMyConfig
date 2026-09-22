package to.itsme.itsmyconfig.processor.packetevents;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import to.itsme.itsmyconfig.processor.PacketProcessor;

public interface PEventsProcessor extends PacketProcessor {

    void process(PacketSendEvent event);

<<<<<<< HEAD
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
=======
>>>>>>> 39afa1f44463bd1462b2497b6820e2ee86437eb5
}
