package to.itsme.itsmyconfig.placeholder;

public record CompiledPlaceholder(
        Placeholder placeholder,
        PlaceholderCaller caller,
        int minArguments,
        int maxArguments
) {

    public boolean accepts(final int arguments) {
        return arguments >= this.minArguments
                && (this.maxArguments == -1 || arguments <= this.maxArguments);
    }

    public String invalidArgumentsMessage(final int arguments) {
        if (this.maxArguments == -1) {
            return String.format(
                    "Invalid argument count, provided: %d, required at least: %d",
                    arguments,
                    this.minArguments
            );
        }

        return String.format(
                "Invalid argument count, provided: %d, required: %d-%d",
                arguments,
                this.minArguments,
                this.maxArguments
        );
    }
}
