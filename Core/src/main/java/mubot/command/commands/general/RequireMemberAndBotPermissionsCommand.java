package mubot.command.commands.general;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Permission;
import mubot.command.CommandResponse;
import reactor.core.publisher.Mono;

import java.util.List;

import static mubot.command.util.PermissionsHelper.*;

public abstract class RequireMemberAndBotPermissionsCommand extends RequirePermissionsCommand {

    public RequireMemberAndBotPermissionsCommand(String commandTrigger, Permission... permissions) {
        super(commandTrigger, permissions);
    }

    public RequireMemberAndBotPermissionsCommand(String commandTrigger, List<String> aliases,
                                                 Permission... permissions) {
        super(commandTrigger, aliases, permissions);
    }

    @Override
    public Mono<CommandResponse> execute(MessageCreateEvent event, String[] args) {
        return requireNotPrivateMessage(event).flatMap(ignored -> requireUserGuildPermissions(event, this.permissions))
                .flatMap(ignored -> requireBotGuildPermissions(event, this.permissions))
                .flatMap(ignored -> action(event, args));
    }

}
