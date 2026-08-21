package id.mfx.rpg.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import java.util.Map;

public final class TextService {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public Component parse(String input) {
        return MINI_MESSAGE.deserialize(input == null ? " " : input);
    }

    public Component parse(String input, Map<String, Component> placeholders) {
        TagResolver.Builder resolver = TagResolver.builder();
        placeholders.forEach((key, vaule) -> resolver.resolver(Placeholder.component(key, vaule)));
        return MINI_MESSAGE.deserialize(input == null ? " " : input, resolver.build());
    }

    public void send(CommandSender sender, String input) {
        sender.sendMessage(parse(input));
    }

    public void send(CommandSender sender, String input, Map<String, Component> placeholders) {
        sender.sendMessage(parse(input, placeholders));
    }
}
