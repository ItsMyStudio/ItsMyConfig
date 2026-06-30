package to.itsme.itsmyconfig.shade.kyori.adventure.util;

import net.kyori.adventure.builder.AbstractBuilder;

/**
 * Compatibility shim for libraries still compiled against Adventure 4's removed nested builder type.
 */
@Deprecated
public interface Buildable$Builder<R> extends AbstractBuilder<R> {
}
