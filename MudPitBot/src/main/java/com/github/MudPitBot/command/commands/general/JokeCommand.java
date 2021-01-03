package com.github.MudPitBot.command.commands.general;

import com.github.MudPitBot.command.Command;
import com.github.MudPitBot.command.CommandResponse;
import com.github.MudPitBot.command.menu.JokeMenu;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import static com.github.MudPitBot.command.CommandUtil.requireNotPrivateMessage;

public class JokeCommand extends Command {

	public JokeCommand() {
		super("joke");
	}

	@Override
	public Mono<CommandResponse> execute(MessageCreateEvent event, String[] args) {
		return requireNotPrivateMessage(event).flatMap(ignored -> joke(args));
	}

	private Mono<CommandResponse> joke(@NonNull String[] args) {
		boolean unsafe = false;
		if (args.length > 0)
			unsafe = !args[0].equals("unsafe");
		JokeMenu menu = new JokeMenu(unsafe);
		return new CommandResponse.Builder().withCreateSpec(menu.createMessage()).withMenu(menu).build();
	}

}