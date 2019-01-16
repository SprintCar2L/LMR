package ovh.not.javamusicbot.command;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.VoiceChannel;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.not.javamusicbot.Command;
import ovh.not.javamusicbot.MusicBot;
import ovh.not.javamusicbot.audio.guild.GuildAudioController;
import ovh.not.javamusicbot.utils.Utils;

import java.io.IOException;

public class LoadCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(LoadCommand.class);

    private final AudioPlayerManager playerManager;

    public LoadCommand(MusicBot bot, AudioPlayerManager playerManager) {
        super(bot, "load", "undump");
        setDescription("Loads in a queue dump");
        this.playerManager = playerManager;
    }

    @Override
    public void on(Context context) {
        VoiceChannel channel = context.getEvent().getMember().getVoiceState().getChannel();
        if (channel == null) {
            context.reply("You must be in a voice channel!");
            return;
        }

        if (context.getArgs().length == 0) {
            context.reply("Usage: `{{prefix}}load <dumped playlist url>`");
            return;
        }

        // todo clean up this absolute mess
        GuildAudioController musicManager = this.bot.getGuildsManager().getOrCreate(context.getEvent().getGuild(),
                context.getEvent().getTextChannel(), playerManager);
        if (musicManager.getState().isConnectionOpen() && musicManager.getPlayer().getPlayingTrack() != null
                && musicManager.getState().getVoiceChannelId().get() != channel.getIdLong()
                && !context.getEvent().getMember().hasPermission(context.getEvent().getJDA().getVoiceChannelById(musicManager.getState().getVoiceChannelId().get()), Permission.VOICE_MOVE_OTHERS)) {
            context.reply("dabBot is already playing music in %s so it cannot be moved. Members with the `Move Members` permission can do this.", context.getEvent().getJDA().getVoiceChannelById(musicManager.getState().getVoiceChannelId().get()).getName());
            return;
        }

        String url = context.getArgs()[0];
        if (url.contains("hastebin.com") && !url.contains("raw")) {
            String name = url.substring(url.lastIndexOf("/") + 1);
            url = "https://hastebin.com/raw/" + name;
        }

        JSONArray tracks;
        try {
            Request request = new Request.Builder().url(url).build();
            Response response = MusicBot.HTTP_CLIENT.newCall(request).execute();
            tracks = new JSONArray(response.body().string());
        } catch (Exception e) { // catch all exceptions PLEASE
            logger.error("error occurred loading tracks from a dump", e);
            context.reply("An error occurred! %s", e.getMessage());
            return;
        }

        musicManager.getScheduler().getQueue().clear();
        musicManager.getScheduler().setRepeat(false);
        musicManager.getScheduler().setLoop(false);
        musicManager.getPlayer().stopTrack();

        logger.info(String.format("loaddbg> %s in %s on %s trying to load %s from %s",
                context.getEvent().getAuthor().getId(),
                context.getEvent().getChannel().getId(),
                context.getEvent().getJDA().getShardInfo().getShardString(),
                tracks.length(),
                url));

        for (int i = 0; i < tracks.length(); i++) {
            String encoded = tracks.getString(i);

            try {
                AudioTrack track = Utils.decode(playerManager, encoded);
                musicManager.getScheduler().queue(track);
            } catch (IOException e) {
                logger.error("error occurred decoding encoded tracks", e);
                context.reply("An error occurred! %s", e.getMessage());
                return;
            }
        }

        context.reply("Loaded %d tracks from <%s>!", tracks.length(), url);

        if (!musicManager.getState().isConnectionOpen()) {
            musicManager.getConnector().openConnection(channel, context.getEvent().getAuthor());
        }
    }
}
