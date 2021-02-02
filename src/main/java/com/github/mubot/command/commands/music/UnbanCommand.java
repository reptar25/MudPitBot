package com.github.mubot.command.commands.music;

import java.util.function.Consumer;

import com.github.mubot.command.CommandResponse;
import com.github.mubot.command.exceptions.CommandException;
import com.github.mubot.command.help.CommandHelpSpec;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Mono;

public class UnbanCommand extends AbstractBanCommand {

	public UnbanCommand() {
		super("unban");
	}

	protected Mono<CommandResponse> action(MessageCreateEvent event, String[] args) {
		return event.getGuild().flatMapMany(guild -> guild.getBans().map(ban -> {
			if (ban.getUser().getMention().equals(args[0]))
				return ban.getUser();
			return null;
		}).switchIfEmpty(Mono.error(new CommandException("User not found in ban list.")))
				.flatMap(user -> guild.unban(user.getId())))
				.then(CommandResponse.create("User " + args[0] + " has been unbanned.")).onErrorResume(
						ClientException.class, error -> Mono.error(new CommandException("Error unbanning user")));
	}

	@Override
	public Consumer<? super CommandHelpSpec> createHelpSpec() {
		return spec -> spec.setDescription("Unban a user.").addArg("@user", "The user's mention", false);
	}

}
