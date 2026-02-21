package net.mexish.libs.command.exception;

public class CommandException extends RuntimeException {
    public CommandException(final String message) {
        super("Caught a command exception: " + message);
    }

    public CommandException() {
        super("Caught a command exception");
    }

    public CommandException(final Throwable cause) {
        super("Caught a command exception", cause);
    }

    public CommandException(final String message, final Throwable cause) {
        super("Caught a command exception: " + message, cause);
    }

}
