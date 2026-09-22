package to.itsme.itsmyconfig;

/**
 * SeriaPrefix main plugin class extending {@link ItsMyConfig}.
 */
public final class SeriaPrefix extends ItsMyConfig {

    /**
     * Gets the instance of SeriaPrefix.
     *
     * @return The instance of SeriaPrefix.
     */
    public static SeriaPrefix getInstance() {
        return (SeriaPrefix) ItsMyConfig.getInstance();
    }

}
