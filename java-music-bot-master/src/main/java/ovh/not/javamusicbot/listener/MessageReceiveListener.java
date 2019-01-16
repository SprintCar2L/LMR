package ovh.not.javamusicbot.listener;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import ovh.not.javamusicbot.Command;
import ovh.not.javamusicbot.CommandManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageReceiveListener extends ListenerAdapter {
    static final Counter messages = Counter.build()
            .name("dab_messages_total").help("Total messages received.")
            .labelNames("shard").register();
    static final Histogram commandLatency = Histogram.build()
            .name("dab_commands_latency_seconds").help("Command latency in seconds")
            .labelNames("shard", "command_name").register();

    private final CommandManager commandManager;
    private final Pattern commandPattern;

    public MessageReceiveListener(CommandManager commandManager, Pattern commandPattern) {
        this.commandManager = commandManager;
        this.commandPattern = commandPattern;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String shard = Integer.toString(event.getJDA().getShardInfo().getShardId());
        messages.labels(shard).inc();

        User author = event.getAuthor();
        if (author.isBot() || author.getId().equalsIgnoreCase(event.getJDA().getSelfUser().getId())) {
            return;
        }

        String content = event.getMessage().getContentDisplay();

        Matcher matcher = commandPattern.matcher(content.replace("\r", " ").replace("\n", " "));
        if (!matcher.find()) {
            return;
        }

        if (!event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_WRITE)) {
            return;
        }

        String name = matcher.group(1).toLowerCase();
        Command command = commandManager.getCommand(name);
        if (command == null) {
            return;
        }

        Command.Context context = command.new Context();
        context.setEvent(event);

        if (matcher.groupCount() > 1) {
            String[] matches = matcher.group(2).split("\\s+");
            if (matches.length > 0 && matches[0].equals("")) {
                matches = new String[0];
            }
            context.setArgs(matches);
        }

        Histogram.Timer requestTimer = commandLatency.labels(shard, command.getNames()[0]).startTimer();
        try {
            command.on(context);
        } finally {
            requestTimer.observeDuration();
        }
    }
}