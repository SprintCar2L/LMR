package ovh.not.javamusicbot.audio.guild;

import io.prometheus.client.Gauge;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GuildAudioControllerConnector {
    static final Gauge audioStreams = Gauge.build()
            .name("dab_streams_total").help("Total audio streams.")
            .register();

    private static final Logger LOGGER = LoggerFactory.getLogger(GuildAudioController.class);

    private final Lock lock = new ReentrantLock();

    private final ShardManager shardManager;
    private final GuildAudioControllerState state;
    private final ExecutorService executorService;
    private final long guildId;

    GuildAudioControllerConnector(ShardManager shardManager, GuildAudioControllerState state, ExecutorService executorService, long guildId) {
        this.shardManager = shardManager;
        this.state = state;
        this.executorService = executorService;
        this.guildId = guildId;
    }

    public void openConnection(final VoiceChannel voiceChannel, User user) {
        final Guild guild = voiceChannel.getGuild();
        Member selfMember = guild.getSelfMember();

        try {
            if (!selfMember.hasPermission(voiceChannel, Permission.VOICE_CONNECT)) {
                throw new PermissionException(Permission.VOICE_CONNECT.getName());
            }

            executorService.submit(() -> {
                lock.lock();
                try {
                    AudioManager audioManager = guild.getAudioManager();
                    boolean inc = !audioManager.isConnected();
                    audioManager.openAudioConnection(voiceChannel);
                    if (inc) {
                        audioStreams.inc();
                    }
                    audioManager.setSelfDeafened(true);
                    state.setVoiceConnectionOpen(voiceChannel.getIdLong());
                } finally {
                    lock.unlock();
                }
            });
        } catch (PermissionException e) {
            if (user != null && !user.isBot()) {
                user.openPrivateChannel().queue(privateChannel -> {
                    String message = String.format(
                            "**dabBot does not have permission to connect to the %s voice channel.**\nTo fix this, " +
                                    "allow dabBot to `View Channel`, `Connect` and `Speak` in that voice channel.\nIf you " +
                                    "are not the guild owner, please send this to them.", voiceChannel.getName());

                    privateChannel.sendMessage(message).queue();
                });
            } else {
                LOGGER.error("an error occurred opening voice connection", e);
            }
        }
    }

    public void closeConnection() {
        final Guild guild = shardManager.getGuildById(guildId);

        if (guild == null) {
            // todo handle if guild no longer exists
            LOGGER.error("Error getting guild with ID {} from shard manager", guildId);
            return;
        }

        executorService.submit(() -> {
            lock.lock();
            try {
                boolean dec = guild.getAudioManager().isConnected();
                guild.getAudioManager().closeAudioConnection();
                if (dec) {
                    audioStreams.dec();
                }
                state.setVoiceConnectionClosed();
            } finally {
                lock.unlock();
            }
        });
    }
}