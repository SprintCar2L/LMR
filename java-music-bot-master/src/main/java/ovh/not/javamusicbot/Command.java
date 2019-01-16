package ovh.not.javamusicbot;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Command {
    private static final Pattern FLAG_PATTERN = Pattern.compile("\\s+-([a-zA-Z]+)");

    protected final MusicBot bot;
    private final String[] names;
    private Optional<String> description = Optional.empty();

    protected Command(MusicBot bot, String name, String... names) {
        this.bot = bot;
        this.names = new String[names.length + 1];
        this.names[0] = name;
        System.arraycopy(names, 0, this.names, 1, names.length);
    }

    public String[] getNames(){
        return names;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Optional.of(description);
    }

    public abstract void on(Context context);

    public class Context {

        private MessageReceivedEvent event;
        private String[] args;

        public MessageReceivedEvent getEvent() {
            return event;
        }

        public void setEvent(MessageReceivedEvent event) {
            this.event = event;
        }

        public String[] getArgs() {
            return args;
        }

        public void setArgs(String[] args) {
            this.args = args;
        }

        public Message reply(String message) {
            try {
                return event.getChannel()
                        .sendMessage(message.replace("{{prefix}}", Command.this.bot.getConfigs().config.prefix))
                        .complete();
            } catch (PermissionException e) {
                event.getAuthor().openPrivateChannel().queue(privateChannel -> {
                    privateChannel.sendMessage("**dabBot does not have permission to talk in the #"
                            + event.getTextChannel().getName() + " text channel.**\nTo fix this, allow dabBot to " +
                            "`Read Messages` and `Send Messages` in that text channel.\nIf you are not the guild " +
                            "owner, please send this to them.").queue();
                });
                return null;
            }
        }

        public Message reply(String format, Object... args) {
            return reply(String.format(format, args));
        }

        public Set<String> parseFlags() {
            String content = String.join(" ", args);
            Matcher matcher = FLAG_PATTERN.matcher(content);
            Set<String> matches = new HashSet<>();
            while (matcher.find()) {
                matches.add(matcher.group().replaceFirst("\\s+-", ""));
            }
            content = content.replaceAll("\\s+-([a-zA-Z]+)", "");
            args = content.split("\\s+");
            return matches;
        }
    }
}
