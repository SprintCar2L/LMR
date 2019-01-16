package ovh.not.javamusicbot.audio.guild;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.core.entities.Guild;
import ovh.not.javamusicbot.MusicBot;
import ovh.not.javamusicbot.TrackScheduler;
import ovh.not.javamusicbot.audio.AudioPlayerSendHandler;

import java.util.concurrent.ExecutorService;

public class GuildAudioController {
    private final AudioPlayerManager playerManager;
    private final GuildAudioControllerState state;
    private final GuildAudioControllerConnector connector;

    private final AudioPlayer player;
    private final TrackScheduler scheduler;

    GuildAudioController(MusicBot bot, Guild guild, long textChannelId, AudioPlayerManager playerManager, ExecutorService executorService) {
        this.playerManager = playerManager;
        this.state = new GuildAudioControllerState();
        this.connector = new GuildAudioControllerConnector(bot.getShardManager(), state, executorService, guild.getIdLong());

        this.player = playerManager.createPlayer();
        this.scheduler = new TrackScheduler(bot, this, this.player, textChannelId);
        this.player.addListener(scheduler);

        AudioPlayerSendHandler sendHandler = new AudioPlayerSendHandler(this.player);
        guild.getAudioManager().setSendingHandler(sendHandler);
    }

    public AudioPlayerManager getPlayerManager() {
        return playerManager;
    }

    public GuildAudioControllerState getState() {
        return state;
    }

    public GuildAudioControllerConnector getConnector() {
        return connector;
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public TrackScheduler getScheduler() {
        return scheduler;
    }
}
