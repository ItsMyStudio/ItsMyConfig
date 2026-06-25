package to.itsme.itsmyconfig.processor.protocollib;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import to.itsme.itsmyconfig.processor.PacketProcessor;

public interface PLibProcessor extends PacketProcessor {
    boolean canHandle(PacketContainer container);
    void process(PacketEvent event);
}
