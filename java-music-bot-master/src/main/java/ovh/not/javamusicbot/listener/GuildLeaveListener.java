package ovh.not.javamusicbot.listener;

import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import ovh.not.javamusicbot.audio.guild.GuildAudioController;
import ovh.not.javamusicbot.audio.guild.GuildAudioManager;

public class GuildLeaveListener extends ListenerAdapter {
    private final GuildAudioManager guildAudioManager;

    public GuildLeaveListener(GuildAudioManager guildAudioManager) {
        this.guildAudioManager = guildAudioManager;
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        GuildAudioController musicManager = guildAudioManager.remove(event.getGuild().getIdLong());
        if (musicManager != null) {
            musicManager.getPlayer().stopTrack();
            musicManager.getScheduler().getQueue().clear();
            musicManager.getConnector().closeConnection();
        }
        event.getGuild().getAudioManager().closeAudioConnection();
    }
}
