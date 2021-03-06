package com.github.mubot.command.commands.music;

import java.util.function.Consumer;

import com.github.mubot.command.CommandResponse;
import com.github.mubot.command.exceptions.CommandException;
import com.github.mubot.command.help.CommandHelpSpec;
import com.github.mubot.command.util.EmojiHelper;
import com.github.mubot.music.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.VoiceChannel;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

public class RemoveCommand extends MusicCommand {

	public RemoveCommand() {
		super("remove");
	}

	@Override
	protected Mono<CommandResponse> action(MessageCreateEvent event, String[] args, TrackScheduler scheduler,
			VoiceChannel channel) {
		return remove(event, args, scheduler);
	}

	public Mono<CommandResponse> remove(MessageCreateEvent event, @NonNull String[] args,
			@NonNull TrackScheduler scheduler) {
		if (args.length >= 1) {
			if (args[0].equalsIgnoreCase("all")) {
				return removeAll(scheduler);
			} else {
				try {
					return remove(Integer.parseInt(args[0]), scheduler);
				} catch (NumberFormatException ignored) {
					return getHelp(event);
				}
			}
		}
		return getHelp(event);
	}

	private Mono<CommandResponse> remove(int index, TrackScheduler scheduler) {
		AudioTrack removed = scheduler.removeFromQueue(index - 1);
		if (removed != null)
			return CommandResponse.create(EmojiHelper.RED_X + " Removed \"" + removed.getInfo().title
					+ "\" from the queue " + EmojiHelper.RED_X);

		return Mono.error(new CommandException("There is no track at position " + index));
	}

	private Mono<CommandResponse> removeAll(TrackScheduler scheduler) {
		scheduler.clearQueue();
		return CommandResponse.create(EmojiHelper.RED_X + " Removed everything from the queue " + EmojiHelper.RED_X);
	}

	@Override
	public Consumer<? super CommandHelpSpec> createHelpSpec() {
		return spec -> spec.setDescription("Removes the song at the given position number from the queue.").addArg(
				"position",
				"The song to be remove's number position in the queue i.e. \"1\" to remove the song at the top of the queue or \"all\" to remove all the songs from the queue.",
				false).addExample("1").addExample("all");
	}
}
