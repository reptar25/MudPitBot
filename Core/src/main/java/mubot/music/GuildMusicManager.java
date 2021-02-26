package mubot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.VoiceChannel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all of the TrackSchedulers for each guild
 */
public class GuildMusicManager {

    public static final int DEFAULT_VOLUME = 15;
    private static final AudioPlayerManager playerManager;
    /**
     * Maps a GuildMusic object for each new guild joined. Key is guild id snowflake
     */
    private static final Map<Snowflake, GuildMusic> guildMusicMap = new ConcurrentHashMap<>();
    private static final int RETRY_AMOUNT = 100;

    static {
        // Creates AudioPlayer instances and translates URLs to AudioTrack instances
        playerManager = new DefaultAudioPlayerManager();
        // This is an optimization strategy that Discord4J can utilize. It is not
        // important to understand
        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);

        // Allow playerManager to parse remote sources like YouTube links
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    public static Mono<GuildMusic> getOrCreate(Snowflake guildId) {
        return Mono.justOrEmpty(getGuildMusic(guildId)).switchIfEmpty(Mono.defer(() -> {
            final AudioPlayer player = playerManager.createPlayer();
            final LavaPlayerAudioProvider audioProvider = new LavaPlayerAudioProvider(player);
            final TrackScheduler scheduler = new TrackScheduler(player);
            final GuildMusic guildMusic = new GuildMusic(guildId, scheduler, audioProvider);

            player.setVolume(DEFAULT_VOLUME);
            guildMusicMap.put(guildId, guildMusic);

            return Mono.just(guildMusic);
        }));
    }

    public static void loadItemOrdered(String identifier, TrackScheduler scheduler, MessageCreateEvent event) {
        playerManager.loadItemOrdered(event.getGuildId().orElseThrow(), identifier,
                new TrackLoadResultHandler(scheduler, event));
    }

    public static AudioPlayerManager getPlayerManager() {
        return playerManager;
    }

    private static Optional<GuildMusic> getGuildMusic(Snowflake guildId) {
        return Optional.ofNullable(guildMusicMap.get(guildId));
    }

    public static void destroy(Snowflake guildId) {
        final GuildMusic guildMusic = guildMusicMap.remove(guildId);
        if (guildMusic != null)
            guildMusic.destroy();
    }

    /**
     * Gets the {@link TrackScheduler} that was mapped when the bot joined a voice
     * channel of the guild the message was sent in.
     *
     * @param channel the voice channel of the scheduler
     * @return the {@link TrackScheduler} that is mapped to the voice channel of the
     * bot in the guild the message was sent from.
     */
    public static Mono<TrackScheduler> getScheduler(VoiceChannel channel) {
        return Mono.justOrEmpty(getGuildMusic(channel.getGuildId())).repeatWhenEmpty(RETRY_AMOUNT, Flux::repeat)
                .flatMap(guildMusic -> Mono.just(guildMusic.getTrackScheduler()));
    }

}
