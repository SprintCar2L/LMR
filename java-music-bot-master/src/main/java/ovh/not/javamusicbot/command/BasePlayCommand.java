package ovh.not.javamusicbot.command;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.VoiceChannel;
import ovh.not.javamusicbot.*;
import ovh.not.javamusicbot.audio.guild.GuildAudioController;
import ovh.not.javamusicbot.utils.LoadResultHandler;

import java.util.Set;

abstract class BasePlayCommand extends Command {
    private final CommandManager commandManager;
    private final AudioPlayerManager playerManager;
    boolean allowSearch = true;
    boolean isSearch = false;

    BasePlayCommand(MusicBot bot, CommandManager commandManager, AudioPlayerManager playerManager, String name, String... names) {
        super(bot, name, names);
        this.commandManager = commandManager;
        this.playerManager = playerManager;
    }

    @Override
    public void on(Context context) {
        if (context.getArgs().length == 0) {
            context.reply(this.noArgumentMessage());
            return;
        }
      
        VoiceChannel channel = context.getEvent().getMember().getVoiceState().getChannel();
        if (channel == null) {
            context.reply("You must be in a voice channel!");
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

        LoadResultHandler handler = new LoadResultHandler(commandManager, musicManager, playerManager, context);
        handler.setAllowSearch(allowSearch);
        handler.setSearch(isSearch);
      
        Set<String> flags = context.parseFlags();
        if (flags.contains("first") || flags.contains("f")) {
            handler.setSetFirstInQueue(true);
        }

        context.setArgs(this.transformQuery(context.getArgs()));

        playerManager.loadItem(String.join(" ", context.getArgs()), handler);
        if (!musicManager.getState().isConnectionOpen()) {
            musicManager.getConnector().openConnection(channel, context.getEvent().getAuthor());
        }
    }

    protected abstract String noArgumentMessage();

    protected String[] transformQuery(String[] args) {
        return args;
    }
}
