package mubot.command.menu.menus;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import mubot.command.menu.Menu;
import mubot.command.util.EmojiHelper;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class PollMenu extends Menu {

    public static final Color DEFAULT_POLL_EMBED_COLOR = Color.of(23, 53, 77);
    private static final Logger LOGGER = Loggers.getLogger(PollMenu.class);
    private final String[] args;
    private final Member member;
    private final ArrayList<String> answers = new ArrayList<>();
    private String authorName;
    private String authorIcon;
    private String title;
    private String description;

    public PollMenu(String[] args, Member member) {
        this.args = args;
        this.member = member;
        createPoll();
    }

    @Override
    public void setMessage(Message message) {
        super.setMessage(message);
        addReactions();
    }

    private void addReactions() {
        for (int i = 0; i < answers.size(); i++) {
            message.addReaction(EmojiHelper.getUnicodeFromNum(i)).subscribe();
        }
    }

    private void createPoll() {

        setAnswers(args);

        if (answers.isEmpty())
            return;

        setAuthor();

        setDescription();
    }

    private void setAnswers(String[] args) {
        if (args == null || args.length < 3) {
            LOGGER.info("Not enough arguments for poll command");
            return;
        }
        // only allow of 1 question and 10 answers as arguments
        final int MAX_ANSWERS = 11;
        List<String> arguments = Arrays.stream(args).limit(MAX_ANSWERS).collect(Collectors.toList());
        title = arguments.get(0);
        for (int i = 1; i < arguments.size(); i++) {
            answers.add(arguments.get(i));
        }
    }

    private void setAuthor() {
        if (member != null) {
            this.authorName = member.getUsername();
            this.authorIcon = member.getAvatarUrl();
        }
    }

    private void setDescription() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < answers.size(); i++) {
            String answer = answers.get(i);
            sb.append(EmojiHelper.getPlainLetterFromNum(i)).append(" ").append(answer).append("\r\n\n");
        }

        description = sb.toString();
    }

    @Override
    public String toString() {
        return title + " " + description + " " + answers;
    }

    @Override
    public Consumer<? super MessageCreateSpec> createMessage() {
        return spec -> spec.setEmbed(createEmbed()).setContent("**" + title + "**");
    }

    private Consumer<? super EmbedCreateSpec> createEmbed() {
        return embed -> embed.setColor(DEFAULT_POLL_EMBED_COLOR).setFooter("Poll created by " + authorName, authorIcon)
                .setTimestamp(Instant.now()).setDescription(description);
    }

}
