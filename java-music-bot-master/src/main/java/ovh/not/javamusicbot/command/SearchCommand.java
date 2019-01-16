package ovh.not.javamusicbot.command;

import ovh.not.javamusicbot.Command;
import ovh.not.javamusicbot.CommandManager;
import ovh.not.javamusicbot.MusicBot;

public class SearchCommand extends Command {
    private final CommandManager commandManager;

    public SearchCommand(MusicBot bot, CommandManager commandManager) {
        super(bot, "search", "lookup", "youtube", "yt", "find");
        setDescription("Searches for a song on youtube");
        this.commandManager = commandManager;
    }

    @Override
    public void on(Context context) {
        if (context.getArgs().length == 0) {
            context.reply("Usage: `{{prefix}}search <term>` - searches for a song on youtube\n" +
                    "To add the song as first in the queue, use `{{prefix}}search <term> -first`");
            return;
        }

        String[] args = new String[context.getArgs().length + 1];
        args[0] = "ytsearch: ";

        System.arraycopy(context.getArgs(), 0, args, 1, context.getArgs().length);
        context.setArgs(args);

        commandManager.getCommands().get("play").on(context);
    }
}
