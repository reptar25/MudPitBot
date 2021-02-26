package mubot.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import mubot.command.help.CommandHelpSpec;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * Interface for Command
 */
public interface CommandInterface {

    /**
     * Executes the command
     *
     * @param event the MessageCreateEvent
     * @param args  the arguments for the command
     * @return the response of the command
     */
    Mono<CommandResponse> execute(MessageCreateEvent event, String[] args);

    /**
     * Used to create the {@link CommandHelpSpec} for this command
     *
     * @return returns the created {@link CommandHelpSpec} for this command
     */
    Consumer<? super CommandHelpSpec> createHelpSpec();

}
