package com.github.MudPitBot.core;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.github.MudPitBot.command.Command;
import com.github.MudPitBot.command.CommandResponse;
import com.github.MudPitBot.command.Commands;
import com.github.MudPitBot.command.misc.MuteHelper;
import com.github.MudPitBot.command.misc.Poll;
import com.github.MudPitBot.sound.LavaPlayerAudioProvider;
import com.github.MudPitBot.sound.PlayerManager;
import com.github.MudPitBot.sound.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import discord4j.voice.VoiceConnection;
import discord4j.voice.VoiceConnection.State;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

/**
 * A receiver class for command pattern. A receiver is an object that performs a
 * set of cohesive actions. It's the component that performs the actual action
 * when the command's execute() method is called.
 * https://www.baeldung.com/java-command-pattern
 */
public class CommandReceiver {

	private static final Logger LOGGER = Loggers.getLogger(CommandReceiver.class);
	private static CommandReceiver instance;
	private static Random rand = new Random();

	public static CommandReceiver getInstance() {
		if (instance == null)
			instance = new CommandReceiver();
		return instance;
	}

	private CommandReceiver() {

		// scheduler = new TrackScheduler(PlayerManager.player);
	}

	/**
	 * Bot joins the same voice channel as the user who uses the command.
	 * 
	 * @param event The message event
	 * @return null
	 */
	public CommandResponse join(MessageCreateEvent event) {
		// get the voice channel of the member who sent the message
		Mono.justOrEmpty(event.getMember()).flatMap(Member::getVoiceState).flatMap(VoiceState::getChannel)
				.subscribe(channel -> {
					Mono.just(channel.getId())
							// dont join the channel if the bot is already connect to that channel
							.filter(channelId -> !channelId.equals(Mono.just(event.getMessage())
									.flatMap(Message::getGuild).flatMap(Guild::getVoiceConnection)
									.flatMap(VoiceConnection::getChannelId).block()))
							.subscribe(channelId -> {
								// joining a new channel
								// once we are connected put the scheduler in the map with the
								// channelId as the key
								TrackScheduler scheduler = new TrackScheduler(channelId);
								channel.join(
										spec -> spec.setProvider(new LavaPlayerAudioProvider(scheduler.getPlayer())))
										.subscribe(vc -> {
											// subscribe to connected/disconnected events
											vc.onConnectOrDisconnect().subscribe(newState -> {
												if (newState.equals(State.CONNECTED)) {
													LOGGER.info("Bot connected to channel");
												} else if (newState.equals(State.DISCONNECTED)) {
													// remove the scheduler from the map. This doesn't ever seem to
													// happen when the bot disconnects, though, so also remove it from
													// map during leave command
													TrackScheduler.remove(channelId);
													LOGGER.info("Bot disconnected to channel");
												}
											});
										});
							}, error -> LOGGER.error(error.getMessage()));
				}, error -> LOGGER.error(error.getMessage()));
		return null;
	}

	/**
	 * Bot leaves any voice channel it is connected to in the same guild. Also
	 * clears the queue of items.
	 * 
	 * @param event The message event
	 * @return null
	 */
	public CommandResponse leave(MessageCreateEvent event) {
		// get the voice channel the bot is connected to
		Mono.just(event.getMessage()).flatMap(Message::getGuild).flatMap(Guild::getVoiceConnection)
				.subscribe(botConnection -> {
					Mono.just(botConnection.getChannelId().subscribe(botChannelId -> {
						// get member who sent the command voice channel
						Mono.justOrEmpty(event.getMember()).flatMap(Member::getVoiceState)
								.flatMap(VoiceState::getChannel).map(VoiceChannel::getId).subscribe(memberChannelId -> {
									// if the members voice channel is one the bot is in
									if (memberChannelId.equals(botChannelId)) {
										botConnection.disconnect().subscribe(null,
												error -> LOGGER.error(error.getMessage()));
										TrackScheduler.remove(memberChannelId);
									}
								});
					}));
				}, error -> LOGGER.error(error.getMessage()));
		return null;
	}

	/**
	 * Bot replies with a simple echo message
	 * 
	 * @return "echo!"
	 */
	public CommandResponse echo() {
		return new CommandResponse("echo!");
	}

	/**
	 * Bot rolls dice and returns results
	 * 
	 * @param params The number and type of dice to roll, eg "1d20"
	 * @return The results of the dice roll
	 */
	public CommandResponse roll(String[] params) {

		if (params == null || params.length <= 0) {
			return null;
		}

		String dice = params[0];

		// only roll if 2nd part of command matches the reg ex
		if (Pattern.matches("[1-9][0-9]*[Dd][1-9][0-9]*", dice)) {

			StringBuilder sb = new StringBuilder();
			sb.append("Rolling " + dice + "\n");

			String[] splitDiceString = dice.split("[Dd]");
			int numOfDice = Integer.parseInt(splitDiceString[0]);
			int numOfSides = Integer.parseInt(splitDiceString[1]);
			int diceSum = 0;

			for (int i = 0; i < numOfDice; i++) {
				int roll = rand.nextInt(numOfSides) + 1;
				sb.append("Dice " + (i + 1) + " was a " + roll + "\n");
				diceSum += roll;
			}

			sb.append("Rolled a " + diceSum + "\n");
			return new CommandResponse(sb.toString());
		}

		return null;
	}

	/**
	 * Attempts to play the link in the message
	 * 
	 * @param event  The message event
	 * @param params The link of the audio
	 * @return null
	 */
	public CommandResponse play(TrackScheduler scheduler, String[] params) {
		if (scheduler != null && params != null) {
			// unpause
			if ((params.length == 0 || params[0].isEmpty())) {
				scheduler.pause(!scheduler.isPaused());
				return null;
			}

			if (params.length <= 0 || params.length > 1 || params[0].isEmpty()) {
				// LOGGER.error("Too many or few params for play");
				return null;
			}
			PlayerManager.loadItem(params[0], scheduler);
			if (!scheduler.getQueue().isEmpty()) {
				return new CommandResponse("New track added to the queue (#" + scheduler.getQueue().size() + ")");
			}
			LOGGER.info("Loaded music item: " + params[0]);
		}
		return null;
	}

	/**
	 * Sets the volume of the
	 * {@link com.sedmelluq.discord.lavaplayer.player.AudioPlayer}
	 * 
	 * @param event  The message event
	 * @param params The new volume setting
	 * @return Responds with new volume setting
	 */
	public CommandResponse volume(TrackScheduler scheduler, String[] params) {
		if (scheduler != null && params != null) {

			StringBuilder sb = new StringBuilder();
			if (params.length == 0) {
				return new CommandResponse(
						sb.append("Volume is currently " + scheduler.getPlayer().getVolume()).toString());
			} else if (params[0].equalsIgnoreCase("reset")) {
				scheduler.getPlayer().setVolume(TrackScheduler.DEFAULT_VOLUME);
				return new CommandResponse(sb.append("Volume reset to default").toString());
			}

			if (Pattern.matches("^[1-9][0-9]?$|^100$", params[0])) {
				int volume = Integer.parseInt(params[0]);
				sb.append("Changing volume from ").append(scheduler.getPlayer().getVolume()).append(" to ")
						.append(volume);
				scheduler.getPlayer().setVolume(volume);
				return new CommandResponse(sb.toString());

			}
		}
		return null;
	}

	/**
	 * Stops the LavaPlayer if it is playing anything
	 * 
	 * @param event The message event
	 * @return "Player stopped" if successful, null if not
	 */
	public CommandResponse stop(TrackScheduler scheduler) {
		if (scheduler != null) {
			scheduler.getPlayer().stopTrack();
			scheduler.clearQueue();
			LOGGER.info("Stopped music");
			return new CommandResponse("Player stopped");
		}

		return null;
	}

	/**
	 * Stops the current song and plays the next in queue if there is any
	 * 
	 * @param event The message event
	 * @return The message event
	 */
	public CommandResponse skip(TrackScheduler scheduler) {
		if (scheduler != null) {
			scheduler.nextTrack();
		}

		return null;
	}

	/**
	 * Mutes all {@link Member} in the channel besides bots
	 * 
	 * @param event The message event
	 * @return null
	 */

	public CommandResponse mute(MessageCreateEvent event) {
		/*
		 * gets the member's channel who sent the message, and then all the VoiceStates
		 * connected to that channel. From there we can get the Member of the VoiceState
		 */
		Mono.justOrEmpty(event.getMember()).map(Member::getGuildId).subscribe(guildId -> {
			Mono.justOrEmpty(event.getMember()).flatMap(Member::getVoiceState).flatMap(VoiceState::getChannel)
					.map(VoiceChannel::getVoiceStates).subscribe(users -> {
						// gets the channel id of the member if present
						Mono.justOrEmpty(event.getMember()).flatMap(Member::getVoiceState).map(VoiceState::getChannelId)
								.filter(id -> id.isPresent()).subscribe(idOpt -> {
									boolean mute = true;
									Snowflake id = idOpt.get();
									ArrayList<Snowflake> channelIds = MuteHelper.mutedChannels.get(guildId);
									if (channelIds != null) {
										// channel is muted, so unmute
										if (channelIds.contains(id)) {
											mute = false;
											channelIds.remove(id);
										} else {
											channelIds.add(id);
										}
									} else {
										// channel should be muted
										ArrayList<Snowflake> ids = new ArrayList<Snowflake>();
										ids.add(id);
										MuteHelper.mutedChannels.put(guildId, ids);
									}

									if (mute)
										users.flatMap(VoiceState::getMember).filter(Predicate.not(Member::isBot))
												.subscribe(member -> {
													LOGGER.info(new StringBuilder("Muting ")
															.append(member.getUsername()).toString());
													member.edit(spec -> spec.setMute(true)).block();
												});
									else
										users.flatMap(VoiceState::getMember).filter(Predicate.not(Member::isBot))
												.subscribe(member -> {
													LOGGER.info(new StringBuilder("Unmuting ")
															.append(member.getUsername()).toString());
													member.edit(spec -> spec.setMute(false)).block();
												});
								});
					});
		});

		return null;
	}

	/**
	 * Clears the current queue of all objects
	 * 
	 * @param event The message event
	 * @return "Queue cleared" if successful, null if not
	 */
	public CommandResponse clearQueue(TrackScheduler scheduler) {
		if (scheduler != null) {
			scheduler.clearQueue();
			return new CommandResponse("Queue cleared");
		}

		return null;
	}

	/**
	 * Returns a list of the currently queued songs
	 * 
	 * @param event The message event
	 * @return List of songs in the queue, or "The queue is empty" if empty
	 */
	public CommandResponse viewQueue(TrackScheduler scheduler) {
		if (scheduler != null) {
			// get list of songs currently in the queue
			List<AudioTrack> queue = scheduler.getQueue();
			StringBuilder sb = new StringBuilder();
			// if the queue is not empty
			if (queue.size() > 0) {
				// print total number of songs
				sb.append("Number of songs in queue: ").append(queue.size()).append("\n");
				for (AudioTrack track : queue) {
					// print title and author of song on its own line
					sb.append("\"").append(track.getInfo().title).append("\"").append(" by ")
							.append(track.getInfo().author).append("\n");
				}
			} else {
				sb.append("The queue is empty");
			}

			String retString = sb.toString();

			// if the message is longer than 2000 character, trim it so that its not over
			// the max character limit.
			if (sb.toString().length() >= Message.MAX_CONTENT_LENGTH)
				retString = sb.substring(0, Message.MAX_CONTENT_LENGTH - 1);

			return new CommandResponse(retString);
		}
		return null;
	}

	/**
	 * Shuffles the songs currently in the queue
	 * 
	 * @param event The message event
	 * @return null
	 */
	public CommandResponse shuffleQueue(TrackScheduler scheduler) {
		if (scheduler != null) {
			scheduler.shuffleQueue();
		}
		return null;
	}

	/**
	 * Return the info for the currently playing song
	 * 
	 * @param event The message event
	 * @return Info of song currently playing
	 */
	public CommandResponse nowPlaying(TrackScheduler scheduler) {
		if (scheduler != null) {
			StringBuilder sb = new StringBuilder("Now playing: ");
			// get the track that's currently playing
			AudioTrack track = scheduler.getNowPlaying();
			if (track != null) {
				// add track title and author
				sb.append("\"").append(track.getInfo().title).append("\"").append(" by ")
						.append(track.getInfo().author);
			}

			return new CommandResponse(sb.toString());
		}
		return null;
	}

	/**
	 * Creates a poll in the channel
	 * 
	 * @param event The message event
	 * @return null
	 * 
	 */
	public CommandResponse poll(MessageCreateEvent event) {
		// create a new poll object
		Poll poll = new Poll(event);

		// if the poll is invalid just stop
		if (poll.getAnswers().size() <= 1) {
			return null;
		}

		// create the embed to put the poll into
		Consumer<? super MessageCreateSpec> spec = s1 -> s1
				.setEmbed(s2 -> s2.setColor(Color.of(23, 53, 77)).setFooter(poll.getFooter(), poll.getFooterURL())
						.setTitle(poll.getTitle()).setDescription(poll.getDescription()));

		return new CommandResponse.Builder().spec(spec).poll(poll).build();

	}

	/**
	 * Pauses/unpauses the player
	 * 
	 * @param event The message event
	 * @return null
	 */
	public CommandResponse pause(TrackScheduler scheduler) {
		if (scheduler != null)
			scheduler.pause(!scheduler.isPaused());

		return null;
	}

	/**
	 * Returns a list of all commands in the channel the message was sent.
	 * 
	 * @return List of available commands
	 */
	public CommandResponse printCommands() {
		StringBuilder sb = new StringBuilder("Available commands:");
		Set<Entry<String, Command>> entries = Commands.getEntries();
		for (Entry<String, Command> entry : entries) {
			sb.append(", ").append(Commands.COMMAND_PREFIX).append(entry.getKey());
		}
		return new CommandResponse((sb.toString().replaceAll(":,", ":")));
	}

	/**
	 * @param event  The message event
	 * @param params The position to move the current song to in seconds
	 * @return null
	 */
	public CommandResponse seek(TrackScheduler scheduler, String[] params) {
		if (scheduler != null && params != null) {
			if (params.length > 0) {
				try {
					int positionInSeconds = Integer.parseInt(params[0]);
					scheduler.seek(positionInSeconds);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * @param event  The message event
	 * @param params The amount of time in seconds to rewind
	 * @return null
	 */
	public CommandResponse rewind(TrackScheduler scheduler, String[] params) {
		if (scheduler != null && params != null) {
			if (params.length > 0) {
				try {
					int amountInSeconds = Integer.parseInt(params[0]);
					scheduler.rewind(amountInSeconds);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * @param event  The message event
	 * @param params The amount of time in seconds to fast forward
	 * @return null
	 */
	public CommandResponse fastForward(TrackScheduler scheduler, String[] params) {
		if (scheduler != null && params != null) {
			if (params.length > 0) {
				try {
					int amountInSeconds = Integer.parseInt(params[0]);
					scheduler.fastForward(amountInSeconds);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * Calculate and responds with the time left until release of Cyberpunk
	 * 
	 * @return the time left in days, hours, minutes, seconds left until Cyberpunk
	 *         releases or that it's already out if its past.
	 */
	public CommandResponse cyberpunk() {

		// gets current time in EST
		LocalDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York")).toLocalDateTime();

		// release date in EST
		LocalDateTime cprelease = LocalDateTime.of(2020, 12, 9, 19, 0);

		long days = ChronoUnit.DAYS.between(now, cprelease);
		long hours = ChronoUnit.HOURS.between(now, cprelease) - TimeUnit.DAYS.toHours(days);
		long minutes = ChronoUnit.MINUTES.between(now, cprelease) - TimeUnit.HOURS.toMinutes(hours)
				- TimeUnit.DAYS.toMinutes(days);
		long seconds = ChronoUnit.SECONDS.between(now, cprelease) - TimeUnit.MINUTES.toSeconds(minutes)
				- TimeUnit.HOURS.toSeconds(hours) - TimeUnit.DAYS.toSeconds(days);

		if (days <= 0 && hours <= 0 && minutes <= 0 && seconds <= 0)
			return new CommandResponse("Cyberpunk is out dumbass, the wait is over!");

		StringBuilder sb = new StringBuilder("Cyberpunk will release in: ").append(days).append(" days ").append(hours)
				.append(" hours ").append(minutes).append(" minutes ").append(seconds).append(" seconds");

		return new CommandResponse(sb.toString());
	}

}
