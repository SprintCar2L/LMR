package ovh.not.javamusicbot.command;

import ovh.not.javamusicbot.Command;
import ovh.not.javamusicbot.audio.guild.GuildAudioController;
import ovh.not.javamusicbot.MusicBot;

public class SkipCommand extends Command {
    public SkipCommand(MusicBot bot) {
        super(bot, "skip", "s", "next");
        setDescription("Plays the next song in the queue");
    }

    @Override
    public void on(Context context) {
        GuildAudioController musicManager = this.bot.getGuildsManager().get(context.getEvent().getGuild().getIdLong());
        if (musicManager == null || musicManager.getPlayer().getPlayingTrack() == null) {
            context.reply("No music is playing on this guild! To play a song use `{{prefix}}play`");
            return;
        }

        musicManager.getScheduler().next(musicManager.getPlayer().getPlayingTrack());
    }
}
