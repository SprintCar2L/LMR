package ovh.not.javamusicbot.listener;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.not.javamusicbot.Config;
import ovh.not.javamusicbot.MusicBot;

import java.io.IOException;

import static ovh.not.javamusicbot.MusicBot.JSON_MEDIA_TYPE;

public class GuildJoinListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuildJoinListener.class);

    private static final String CARBON_DATA_URL = "https://www.carbonitex.net/discord/data/botdata.php";
    private static final String DBOTS_STATS_URL = "https://bots.discord.pw/api/bots/%s/stats";
    private static final String DBOTS_ORG_STATS_URL = "https://discordbots.org/api/bots/%s/stats";

    private final MusicBot bot;

    public GuildJoinListener(MusicBot bot) {
        this.bot = bot;
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        JDA jda = event.getJDA();
        Guild guild = event.getGuild();
        TextChannel defaultChannel = guild.getDefaultChannel();

        long guilds = jda.getGuildCache().size();
        LOGGER.info("Joined guild: {}", guild.getName());

        Config config = bot.getConfigs().config;

        if (defaultChannel != null && defaultChannel.canTalk()) {
            defaultChannel.sendMessage(config.join).complete();
        }

        if (config.patreon) {
            if (bot.getPermissionReader().allowedSupporterPatronAccess(guild)) {
                return;
            }

            if (defaultChannel != null && defaultChannel.canTalk()) {
                try {
                    event.getGuild().getDefaultChannel().sendMessage("**Sorry, this is the patreon only dabBot!**\nTo have this " +
                            "bot on your server, you must become a patreon at https://patreon.com/dabbot").complete();
                } catch (Exception ignored) {
                }
            }
            event.getGuild().leave().queue();
            return;
        }

        if (config.dev) {
            return;
        }

        JDA.ShardInfo shardInfo = event.getJDA().getShardInfo();
        int shardCount = shardInfo.getShardTotal();
        int shardId = shardInfo.getShardId();

        if (config.carbon != null && !config.carbon.isEmpty()) {
            RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, new JSONObject()
                    .put("key", config.carbon)
                    .put("servercount", guilds)
                    .put("shardcount", shardCount)
                    .put("shardid", shardId)
                    .toString());

            Request request = new Request.Builder()
                    .url(CARBON_DATA_URL)
                    .method("POST", body)
                    .build();

            try {
                MusicBot.HTTP_CLIENT.newCall(request).execute().close();
            } catch (IOException e) {
                LOGGER.error("Error posting stats to carbonitex.net", e);
            }
        }

        if (config.dbots != null && !config.dbots.isEmpty()) {
            RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, new JSONObject()
                    .put("server_count", guilds)
                    .put("shard_count", shardCount)
                    .put("shard_id", shardId)
                    .toString());

            Request request = new Request.Builder()
                    .url(String.format(DBOTS_STATS_URL, event.getJDA().getSelfUser().getId()))
                    .method("POST", body)
                    .addHeader("Authorization", config.dbots)
                    .build();

            try {
                MusicBot.HTTP_CLIENT.newCall(request).execute().close();
            } catch (IOException e) {
                LOGGER.error("Error posting stats to bots.discord.pw", e);
            }
        }

        if (config.dbotsOrg != null && !config.dbotsOrg.isEmpty()) {
            RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, new JSONObject()
                    .put("server_count", guilds)
                    .put("shard_count", shardCount)
                    .put("shard_id", shardId)
                    .toString());

            Request request = new Request.Builder()
                    .url(String.format(DBOTS_ORG_STATS_URL, event.getJDA().getSelfUser().getId()))
                    .method("POST", body)
                    .addHeader("Authorization", config.dbotsOrg)
                    .build();

            try {
                MusicBot.HTTP_CLIENT.newCall(request).execute().close();
            } catch (IOException e) {
                LOGGER.error("Error posting stats to discordbots.org", e);
            }
        }
    }
}
