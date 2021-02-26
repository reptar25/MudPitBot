package mubot.command.commands.general;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.gateway.GatewayClient;
import mubot.command.Command;
import mubot.command.CommandResponse;
import mubot.command.help.CommandHelpSpec;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Consumer;

public class PingCommand extends Command {

    public PingCommand() {
        super("ping");
    }

    @Override
    public Mono<CommandResponse> execute(MessageCreateEvent event, String[] args) {
        return ping(event);
    }

    private Mono<CommandResponse> ping(MessageCreateEvent event) {
        String pingTime = event.getClient().getGatewayClientGroup().find(event.getShardInfo().getIndex())
                .map(GatewayClient::getResponseTime).map(Duration::toMillis).orElse(-1L).toString();
        String responseTime = String.format("Response time: %sms", pingTime);
        return CommandResponse.create(responseTime);
    }

    @Override
    public Consumer<? super CommandHelpSpec> createHelpSpec() {
        return spec -> spec.setDescription("Display the bot's current response time to the Discord API.");
    }

}
