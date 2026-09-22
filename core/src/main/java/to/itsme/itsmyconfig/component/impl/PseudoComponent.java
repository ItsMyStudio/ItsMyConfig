package to.itsme.itsmyconfig.component.impl;

import net.kyori.adventure.text.Component;
import to.itsme.itsmyconfig.component.AbstractComponent;
import to.itsme.itsmyconfig.util.MMSerializer;
import to.itsme.itsmyconfig.util.Utilities;

public final class PseudoComponent extends AbstractComponent {

    private final Component component;

    @SuppressWarnings("unused")
    public PseudoComponent(final Component component) {
        this.component = component;
    }

    public PseudoComponent(final String json) {
        this.component = Utilities.GSON_SERIALIZER.deserialize(json);
    }

    @Override
    public String toMiniMessage() {
        return MMSerializer.serialize(component, null, false);
    }

}
