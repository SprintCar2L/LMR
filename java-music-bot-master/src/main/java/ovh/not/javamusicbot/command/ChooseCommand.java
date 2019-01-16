package ovh.not.javamusicbot.command;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.Member;
import ovh.not.javamusicbot.Command;
import ovh.not.javamusicbot.CommandManager;
import ovh.not.javamusicbot.MusicBot;
import ovh.not.javamusicbot.utils.Selection;

public class ChooseCommand extends Command {
    private final CommandManager commandManager;

    public ChooseCommand(MusicBot bot, CommandManager commandManager) {
        super(bot,"choose", "pick", "select", "cancel", "c", "choos", "chose");
        setDescription("Picks from a selection menu");
        this.commandManager = commandManager;
    }

    @Override
    public void on(Context context) {
        Member member = context.getEvent().getMember();
        if (!commandManager.getSelectors().containsKey(member)) {
            context.reply("There's no selection active in this guild - are you sure you ran `{{prefix}}play`?\n\n" +
                    "To play a song...\n" +
                    "* Join a voice channel\n" +
                    "* Use `{{prefix}}play <song name/link>`\n" +
                    "* Choose one of the song options with `{{prefix}}choose <song number>`");
            return;
        }

        Selection<AudioTrack, String> selection = commandManager.getSelectors().get(member);
        if (context.getArgs().length == 0) {
            commandManager.getSelectors().remove(member);
            selection.getCallback().accept(false, null);
            return;
        }

        switch (context.getArgs()[0].toLowerCase()) {
            case "c":
            case "cancel":
                commandManager.getSelectors().remove(member);
                selection.getCallback().accept(false, null);
                return;
        }

        for (String arg : context.getArgs()) {
            int selected;
            try {
                selected = Integer.parseInt(arg);
            } catch (NumberFormatException e) {
                context.reply("Invalid input `%s`. Must be an integer with the range 1 - %d. **To cancel selection**, "
                        + "use `{{prefix}}cancel`.", arg, selection.getItemCount());
                return;
            }

            if (selected < 1 || selected > selection.getItemCount()) {
                context.reply("Invalid input `%s`. Must be an integer with the range 1 - %d. **To cancel selection**, "
                        + "use `{{prefix}}cancel`.", arg, selection.getItemCount());
                return;
            }

            AudioTrack track = selection.getItem(selected - 1);
            selection.getCallback().accept(true, track);
        }

        commandManager.getSelectors().remove(member);
    }
}
