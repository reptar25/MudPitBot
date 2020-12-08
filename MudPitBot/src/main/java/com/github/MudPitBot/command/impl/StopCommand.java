package com.github.MudPitBot.command.impl;

import com.github.MudPitBot.CommandCore.CommandReceiver;
import com.github.MudPitBot.command.Command;
import com.github.MudPitBot.command.CommandResponse;

import discord4j.core.event.domain.message.MessageCreateEvent;

public class StopCommand extends Command {

	public StopCommand(CommandReceiver receiver) {
		super(receiver);
	}

	@Override
	public CommandResponse execute(MessageCreateEvent event, String[] params) {
		return receiver.stop(getScheduler(event));
	}

	@Override
	public String getCommandTrigger() {
		return "stop";
	}

}
