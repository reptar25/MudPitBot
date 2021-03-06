package com.github.mubot.command.commands.general;

import static com.github.mubot.command.util.PermissionsHelper.requireNotPrivateMessage;

import java.util.function.Consumer;

import com.github.mubot.command.Command;
import com.github.mubot.command.CommandResponse;
import com.github.mubot.command.help.CommandHelpSpec;
import com.github.mubot.database.DatabaseManager;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

public class SetPrefixCommand extends Command {

	public SetPrefixCommand() {
		super("setprefix");
	}

	@Override
	public Mono<CommandResponse> execute(MessageCreateEvent event, String[] args) {
		return requireNotPrivateMessage(event).flatMap(ignored -> prefix(event, args));
	}

	private Mono<CommandResponse> prefix(MessageCreateEvent event, String[] args) {
		if (args.length >= 1) {
			DatabaseManager.getInstance().getPrefixCache().addPrefix(event.getGuildId().get().asLong(), args[0])
					.onErrorResume(error -> Mono.empty()).subscribe();

			return CommandResponse.create("Set guild command prefix to " + args[0]);
		}
		return getHelp(event);
	}

	@Override
	public Consumer<? super CommandHelpSpec> createHelpSpec() {
		return spec -> spec.setDescription("Set the command-prefix of this server.")
				.addArg("prefix", "New prefix for bot-commands.", false).addExample("$").addExample("!");
	}

}
