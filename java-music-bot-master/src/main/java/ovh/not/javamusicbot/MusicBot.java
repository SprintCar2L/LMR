package ovh.not.javamusicbot;

import com.google.gson.Gson;
import com.moandjiezana.toml.Toml;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.not.javamusicbot.audio.guild.GuildAudioManager;
import ovh.not.javamusicbot.listener.*;
import ovh.not.javamusicbot.utils.PermissionReader;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

public final class MusicBot {
    private static final Logger logger = LoggerFactory.getLogger(MusicBot.class);

    public static final String CONFIG_PATH = "config.toml";
    public static final String CONSTANTS_PATH = "constants.toml";
    public static final String USER_AGENT = "JavaMusicBot v1.0-BETA (https://github.com/ducc/JavaMusicBot)";
    public static final Gson GSON = new Gson();
    public static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    
    private static final Object CONFIG_LOCK = new Object();

    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                // copying the request to a builder
                Request.Builder builder = chain.request().newBuilder();

                // adding user agent header
                builder.addHeader("User-Agent", MusicBot.USER_AGENT);

                // building the new request
                Request request = builder.build();

                // logging
                String method = request.method();
                String uri = request.url().uri().toString();
                logger.info("OkHttpClient: {} {}", method, uri);

                return chain.proceed(request);
            }).build();

    private volatile ConfigLoadResult configs = null;

    private PermissionReader permissionReader;
    private GuildAudioManager guildsManager;
    private CommandManager commandManager;
    private ShardManager shardManager;

    public static void main(String[] args) {
        MusicBot bot = new MusicBot();
        Config config = bot.getConfigs().config;
        bot.permissionReader = new PermissionReader(bot);
        bot.guildsManager = new GuildAudioManager(bot);
        bot.commandManager = new CommandManager(bot);

        ListenerAdapter[] eventListeners = new ListenerAdapter[] {
                new GuildJoinListener(bot),
                new GuildLeaveListener(bot.guildsManager),
                new GuildVoiceMoveListener(bot.guildsManager),
                new MessageReceiveListener(bot.commandManager, Pattern.compile(config.regex)),
                new StartupChangeListener(bot, args),
                new PromStatsListener()
        };

        DefaultShardManagerBuilder builder = new DefaultShardManagerBuilder()
                .addEventListeners(eventListeners)
                .setToken(config.token)
                .setAudioEnabled(true)
                .setGame(Game.of(Game.GameType.DEFAULT, config.game));

        if (args.length < 3) {
            builder.setShardsTotal(1).setShards(0);
        } else {
            try {
                int shardTotal = Integer.parseInt(args[0]);
                int minShardId = Integer.parseInt(args[1]);
                int maxShardId = Integer.parseInt(args[2]);

                builder.setShardsTotal(shardTotal).setShards(minShardId, maxShardId);
            } catch (Exception ex) {
                logger.warn("Could not instantiate with given args! Usage: <shard total> <min shard> <max shard>");
                return;
            }
        }


        if (args.length > 3) {
            int port = Integer.parseInt(args[3]);
            try {
                DefaultExports.initialize();
                HTTPServer server = new HTTPServer(port);
            } catch (IOException e) {
                logger.error("initializing prometheus failed", e);
            }

        }

        // todo set reconnect ipc queue

        try {
            bot.shardManager = builder.build();
        } catch (LoginException e) {
            logger.error("error on call to ShardManager#buildBlocking", e);
        }
    }

    public ConfigLoadResult getConfigs() {
        synchronized (CONFIG_LOCK) {
            if (configs == null) {
                Config config = new Toml().read(new File(CONFIG_PATH)).to(Config.class);
                Constants constants = new Toml().read(new File(CONSTANTS_PATH)).to(Constants.class);
                configs = new ConfigLoadResult(config, constants);
            }
            return configs;
        }
    }

    public ConfigLoadResult reloadConfigs() {
        synchronized (CONFIG_LOCK) {
            configs = null;
            return getConfigs();
        }
    }

    public PermissionReader getPermissionReader() {
        return permissionReader;
    }

    public GuildAudioManager getGuildsManager() {
        return guildsManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public class ConfigLoadResult {
        public final Config config;
        public final Constants constants;

        ConfigLoadResult(Config config, Constants constants) {
            this.config = config;
            this.constants = constants;
        }
    }

    public ShardManager getShardManager() {
        return shardManager;
    }
}
