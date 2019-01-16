package ovh.not.javamusicbot.command;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.utils.IOUtil;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.not.javamusicbot.*;
import ovh.not.javamusicbot.audio.guild.GuildAudioController;
import ovh.not.javamusicbot.utils.LoadResultHandler;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DiscordFMCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(DiscordFMCommand.class);

    private static final String DFM_DIRECTORY_PATH = "discordfm/json";

    private final CommandManager commandManager;
    private final AudioPlayerManager playerManager;
    private Collection<Library> libraries = null;
    private String usageResponse = null;

    public DiscordFMCommand(MusicBot bot, CommandManager commandManager, AudioPlayerManager playerManager) {
        super(bot, "discordfm", "dfm");
        setDescription("Plays music from discord.fm playlists");
        this.commandManager = commandManager;
        this.playerManager = playerManager;
    }

    private void load() {
        libraries = Arrays.stream(new File(DFM_DIRECTORY_PATH).listFiles())
                .map(file -> new Library(file.getName(), file))
                .sorted(Comparator.comparing(o -> o.name))
                .collect(Collectors.toCollection(ArrayList::new));

        usageResponse = String.format("Uses a song playlist from the now defunct Discord.FM" +
                "\nUsage: `{{prefix}}dfm <library>`\n\n**Available libraries:**\n%s", libraries.stream()
                        .map(library -> library.name)
                        .collect(Collectors.joining(", ")));
    }

    @Override
    public void on(Context context) {
        VoiceChannel channel = context.getEvent().getMember().getVoiceState().getChannel();
        if (channel == null) {
            context.reply("You must be in a voice channel!");
            return;
        }

        if (libraries == null || usageResponse == null) {
            Message msg = context.reply("Loading libraries..");
            load();
            msg.delete().queue();
        }

        if (context.getArgs().length == 0) {
            context.reply(usageResponse);
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

        String libraryName = String.join(" ", context.getArgs());

        Optional<Library> library = libraries.stream()
                .filter(lib -> lib != null && lib.name.equalsIgnoreCase(libraryName))
                .findFirst();

        if (!library.isPresent()) {
            context.reply("Invalid library! Use `{{prefix}}dfm` to see usage & libraries.");
            return;
        }

        String[] songs;
        try {
            songs = library.get().getSongs();
        } catch (IOException e) {
            logger.error("error getting discord.fm queue", e);
            context.reply("An error occurred!");
            return;
        }

        if (songs == null) {
            context.reply("An error occurred!");
            return;
        }

        musicManager.getScheduler().getQueue().clear();
        musicManager.getScheduler().setRepeat(false);
        musicManager.getScheduler().setLoop(false);
        musicManager.getPlayer().stopTrack();

        LoadResultHandler handler = new LoadResultHandler(commandManager, musicManager, playerManager, context);
        handler.setVerbose(false);

        List<String> shuffledSongs = Arrays.asList(songs);
        Collections.shuffle(shuffledSongs);
        
        for (String song : shuffledSongs) {
            playerManager.loadItem(song, handler);
        }

        if (!musicManager.getState().isConnectionOpen()) {
            musicManager.getConnector().openConnection(channel, context.getEvent().getAuthor());
        }
    }

    private class Library {
        private final String name;
        private final File file;

        private String[] songs = null;

        private Library(String name, File file) {
            name = name.replace("_", " ");
            this.name = name.substring(0, name.length() - 5);
            this.file = file;
        }

        private String[] getSongs() throws IOException {
            if (songs != null) {
                return songs;
            }

            JSONArray array = new JSONArray(new String(IOUtil.readFully(file)));

            String[] songs = new String[array.length()];
            for (int i = 0; i < array.length(); i++) {
                songs[i] = array.getJSONObject(i).getString("identifier");
            }

            this.songs = songs;
            return songs;
        }
    }
}
