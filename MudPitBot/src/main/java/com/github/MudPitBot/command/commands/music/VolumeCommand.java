package com.github.MudPitBot.command.commands.music;

import static com.github.MudPitBot.command.CommandUtil.requireSameVoiceChannel;

import java.util.regex.Pattern;

import com.github.MudPitBot.command.Command;
import com.github.MudPitBot.command.CommandResponse;
import com.github.MudPitBot.music.GuildMusicManager;
import com.github.MudPitBot.music.TrackScheduler;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

public class VolumeCommand extends Command {

	public VolumeCommand() {
		super("volume");
	}

	@Override
	public Mono<CommandResponse> execute(MessageCreateEvent event, String[] args) {
		return requireSameVoiceChannel(event).flatMap(channel -> getScheduler(channel))
				.flatMap(scheduler -> volume(scheduler, args));
	}

	/**
	 * Sets the volume of the
	 * {@link com.sedmelluq.discord.lavaplayer.player.AudioPlayer}
	 * 
	 * @param event  The message event
	 * @param args The new volume setting
	 * @return Responds with new volume setting
	 */
	public Mono<CommandResponse> volume(TrackScheduler scheduler, String[] args) {
		if (scheduler != null && args != null) {

			StringBuilder sb = new StringBuilder();
			if (args.length == 0) {
				return CommandResponse
						.create(sb.append("Volume is currently " + scheduler.getPlayer().getVolume()).toString());
			} else if (args[0].equalsIgnoreCase("reset")) {
				scheduler.getPlayer().setVolume(GuildMusicManager.DEFAULT_VOLUME);
				return CommandResponse.create(sb.append("Volume reset to default").toString());
			}

			if (Pattern.matches("^[1-9][0-9]?$|^100$", args[0])) {
				int volume = Integer.parseInt(args[0]);
				sb.append("Changing volume from ").append(scheduler.getPlayer().getVolume()).append(" to ")
						.append(volume);
				scheduler.getPlayer().setVolume(volume);
				return CommandResponse.create(sb.toString());
			} else
				return CommandResponse.create("Invalid volume amount");
		}
		return CommandResponse.empty();
	}

}
