package io.github.railgun19457.dummy.platform.gateway;

import io.github.railgun19457.dummy.i18n.MessageBundleRegistry;
import io.github.railgun19457.dummy.core.port.MessageGateway;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class BukkitMessageGateway implements MessageGateway {

    private final MessageBundleRegistry registry;
    private final String locale;

    public BukkitMessageGateway(MessageBundleRegistry registry, String locale) {
        this.registry = registry;
        this.locale = locale;
    }

    @Override
    public void sendMessage(CommandSender sender, String messageKey) {
        sender.sendMessage(resolve(messageKey, Map.of()));
    }

    @Override
    public void sendMessage(CommandSender sender, String messageKey, Map<String, String> placeholders) {
        sender.sendMessage(resolve(messageKey, placeholders));
    }

    @Override
    public String resolve(String messageKey, Map<String, String> placeholders) {
        String message = registry.getMessage(locale, messageKey);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }
}
