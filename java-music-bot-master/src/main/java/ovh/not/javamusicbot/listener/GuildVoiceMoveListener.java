package ovh.not.javamusicbot.listener;

import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.not.javamusicbot.audio.guild.GuildAudioController;
import ovh.not.javamusicbot.audio.guild.GuildAudioManager;

public class GuildVoiceMoveListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuildVoiceMoveListener.class);

    private final GuildAudioManager guildAudioManager;

    public GuildVoiceMoveListener(GuildAudioManager guildAudioManager) {
        this.guildAudioManager = guildAudioManager;
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if (!(event.getMember() == event.getGuild().getSelfMember())) {
            return; // user is not self
        }

        GuildAudioController musicManager = guildAudioManager.get(event.getGuild().getIdLong());
        if (musicManager == null) {
            return; // this guild doesn't have a music manager so doesnt matter
        }

        VoiceChannel joinedChannel = event.getChannelJoined();
        musicManager.getState().setVoiceConnectionOpen(joinedChannel.getIdLong()); // update the voice channel for this guild

        LOGGER.info("Moved from voice channel {} to {}. Updated GuildAudioController.",
                event.getChannelLeft().toString(), joinedChannel.toString());
    }
}
