package ovh.not.javamusicbot.command;

import ovh.not.javamusicbot.Command;
import ovh.not.javamusicbot.MusicBot;

import java.util.stream.Collectors;

public class HelpCommand extends Command {
    public HelpCommand(MusicBot bot) {
        super(bot, "help", "commands", "h", "music");
        setDescription("Shows command help");
    }

    @Override
    public void on(Context context) {
        String descriptions = bot.getCommandManager().getCommands().entrySet().stream()
                .filter(entry -> entry.getValue().getDescription().isPresent()
                        && entry.getKey().equals(entry.getValue().getNames()[0]))
                .map(e -> String.format("`%s` %s", e.getKey(), e.getValue().getDescription().get()))
                .sorted(String::compareTo)
                .collect(Collectors.joining("\n"));

        context.reply("**Commands:**\n%s\n\n**Quick start:** Use `{{prefix}}play <link>` to start playing a song, " +
                "use the same command to add another song, `{{prefix}}skip` to go to the next song and " +
                "`{{prefix}}stop` to stop playing and leave.", descriptions);
    }
}
