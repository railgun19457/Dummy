package github.com.railgun19457.dummy.core.storage;

import org.bukkit.inventory.ItemStack;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public final class InventoryCodec {

    private static final Gson GSON = new Gson();

    private InventoryCodec() {
    }

    public static String encode(ItemStack[] contents) {
        if (contents == null) {
            return "";
        }
        List<String> slots = java.util.Arrays.stream(contents)
                .map(item -> item == null ? "" : Base64.getEncoder().encodeToString(item.serializeAsBytes()))
                .collect(Collectors.toList());
        return GSON.toJson(slots);
    }

    public static ItemStack[] decode(String data) {
        if (data == null || data.isBlank()) {
            return new ItemStack[0];
        }
        try {
            List<String> slots = GSON.fromJson(data, new TypeToken<List<String>>() {}.getType());
            if (slots == null) {
                return new ItemStack[0];
            }
            ItemStack[] items = new ItemStack[slots.size()];
            for (int i = 0; i < slots.size(); i++) {
                String raw = slots.get(i);
                if (raw == null || raw.isBlank()) {
                    items[i] = null;
                    continue;
                }
                items[i] = ItemStack.deserializeBytes(Base64.getDecoder().decode(raw));
            }
            return items;
        } catch (Exception exception) {
            throw new RuntimeException("decode inventory failed", exception);
        }
    }
}
