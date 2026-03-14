package io.github.railgun19457.dummy.core.port;

import org.bukkit.command.CommandSender;

import java.util.Map;

public interface MessageGateway {

    void sendMessage(CommandSender sender, String messageKey);

    void sendMessage(CommandSender sender, String messageKey, Map<String, String> placeholders);

    String resolve(String messageKey, Map<String, String> placeholders);
}
