package to.itsme.itsmyconfig.processor.packetevents;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import to.itsme.itsmyconfig.processor.PacketProcessor;

public interface PEventsProcessor extends PacketProcessor {

    void process(PacketSendEvent event);

}
