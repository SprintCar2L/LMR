package ovh.not.javamusicbot.command;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.managers.AudioManager;
import ovh.not.javamusicbot.Command;
import ovh.not.javamusicbot.audio.guild.GuildAudioController;
import ovh.not.javamusicbot.MusicBot;

public class StopCommand extends Command {
    public StopCommand(MusicBot bot) {
        super(bot, "stop", "leave", "clear");
        setDescription("Stops playing and leaves the voice channel");
    }

    @Override
    public void on(Context context) {
        Guild guild = context.getEvent().getGuild();
        long guildId = guild.getIdLong();
        GuildAudioController musicManager = this.bot.getGuildsManager().get(guildId);

        if (musicManager != null) {
            musicManager.getConnector().closeConnection();
            musicManager.getScheduler().getQueue().clear();
            musicManager.getScheduler().next(null);

            this.bot.getGuildsManager().remove(guildId);

            context.reply("Stopped playing music & left the voice channel.");
        } else {
            AudioManager audioManager = guild.getAudioManager();
            audioManager.closeAudioConnection();

            context.reply("Left the voice channel.");
        }
    }
}
