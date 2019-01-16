package ovh.not.javamusicbot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.not.javamusicbot.audio.guild.GuildAudioController;
import ovh.not.javamusicbot.utils.Utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class TrackScheduler extends AudioEventAdapter {
    private static final Logger logger = LoggerFactory.getLogger(TrackScheduler.class);

    private final MusicBot musicBot;
    private final GuildAudioController musicManager;
    private final AudioPlayer player;

    private long textChannelId;
    private final Queue<AudioTrack> queue;
    private boolean repeat = false;
    private boolean loop = false;

    public TrackScheduler(MusicBot musicBot, GuildAudioController musicManager, AudioPlayer player, long textChannelId) {
        this.musicBot = musicBot;
        this.musicManager = musicManager;
        this.player = player;
        this.textChannelId = textChannelId;
        this.queue = new LinkedList<>();
    }

    public void setTextChannelId(long textChannelId) {
        this.textChannelId = textChannelId;
    }

    public synchronized Queue<AudioTrack> getQueue() {
        return queue;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    @SuppressWarnings("unchecked")
    public synchronized void queue(AudioTrack track, boolean... first) {
        if (!player.startTrack(track, true)) {
            if (first != null && first.length > 0 && first[0]) {
                ((List<AudioTrack>) queue).add(0, track.makeClone());
            } else {
                queue.offer(track.makeClone());
            }
        }
    }

    public synchronized void next(AudioTrack last) {
        AudioTrack track;
        if (repeat && last != null) {
            track = last.makeClone();
        } else {
            if (loop && last != null) {
                queue.add(last.makeClone());
            }
            track = queue.poll();

            // potential fix #84
            if (track != null) {
                track = track.makeClone();
            }
        }
        if (!player.startTrack(track, false)) {
            musicManager.getConnector().closeConnection();
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            next(track);
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        TextChannel textChannel = musicBot.getShardManager().getTextChannelById(textChannelId);

        if (textChannel == null) {
            // todo what if the text channel is deleted
            logger.error("Error getting text channel with ID {} from shard manager", textChannelId);
            return;
        }

        textChannel.sendMessage(String.format("Now playing **%s** by **%s** `[%s]`", track.getInfo().title,
                track.getInfo().author, Utils.formatTrackDuration(track))).complete();
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        logger.error("track exception for track " + track.getInfo(), exception);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        logger.error("track {} stuck with thresholdMs {}", track.getIdentifier(), thresholdMs);
    }
}
