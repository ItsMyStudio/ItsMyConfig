package to.itsme.itsmyconfig.processor.protocollib;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import to.itsme.itsmyconfig.processor.PacketProcessor;
import to.itsme.itsmyconfig.processor.PacketContent;
import to.itsme.itsmyconfig.util.*;

public enum PLibProcessor implements PacketProcessor<PacketContainer> {

    SERVER_ADVENTURE {

        @Override
        public void edit(PacketContainer container, Component component) {
            final StructureModifier<Object> modifier = container.getModifier().withType(AdventureComponentConverter.getComponentClass());
            modifier.write(0, AdventureComponentConverter.fromJsonAsObject(
                Utilities.GSON_SERIALIZER.serialize(component)
            ));
        }

        @Override
        public PacketContent<PacketContainer> unpack(PacketContainer container) {
            if (!Versions.IS_PAPER || Versions.MINOR < 16) {
                return null;
            }

            final StructureModifier<Object> modifier = container.getModifier().withType(AdventureComponentConverter.getComponentClass());
            if (modifier.size() != 1) {
                return null;
            }

            final Object component = modifier.readSafely(0);
            if (component == null) {
                return null;
            }

            return this.of(container, IMCSerializer.toMiniMessage(
                AdventureUtil.toComponent(component)
            ));
        }
    },

    WRAPPED_COMPONENT {

        @Override
        public void edit(PacketContainer container, Component component) {
            container.getChatComponents().write(0, WrappedChatComponent.fromJson(
                    Utilities.GSON_SERIALIZER.serialize(component)
            ));
        }

        @Override
        public PacketContent<PacketContainer> unpack(PacketContainer container) {
            final WrappedChatComponent wrappedComponent = container.getChatComponents().readSafely(0);
            if (wrappedComponent == null) {
                return null;
            }

            final String found = wrappedComponent.getJson();
            if (found.isEmpty()) {
                return null;
            }

            try {
                return this.of(container, IMCSerializer.toMiniMessage(found));
            } catch (final Exception e) {
                Utilities.debug(() -> "An error happened while de/serializing " + found + ": ", e);
            }
            return null;
        }

    },

    @SuppressWarnings("deprecation") BUNGEE_COMPONENT {

        @Override
        public void edit(PacketContainer container, Component component) {
            container.getModifier().withType(TextComponent.class).write(0, new TextComponent(
                    Utilities.BUNGEE_SERIALIZER.serialize(component)
            ));
        }

        @Override
        public PacketContent<PacketContainer> unpack(PacketContainer container) {
            final StructureModifier<TextComponent> textComponentModifier = container.getModifier().withType(TextComponent.class);
            if (textComponentModifier.size() == 1) {
                return this.of(container, processBaseComponents(textComponentModifier.readSafely(0)));
            }
            return null;
        }

        private String processBaseComponents(final BaseComponent... components) {
            return IMCSerializer.toMiniMessage(Utilities.BUNGEE_SERIALIZER.deserialize(components));
        }

    },

    JSON {

        @Override
        public void edit(PacketContainer container, Component component) {
            container.getStrings().write(0, Utilities.GSON_SERIALIZER.serialize(component));
        }

        @Override
        public PacketContent<PacketContainer> unpack(final PacketContainer container) {
            final String rawMessage = container.getStrings().readSafely(0);
            if (rawMessage == null) {
                return null;
            }
            return this.of(container, IMCSerializer.toMiniMessage(rawMessage));
        }

    };

    PacketContent<PacketContainer> of(final PacketContainer container, final String message) {
        return new PacketContent<>(container, this, message);
    }

}
