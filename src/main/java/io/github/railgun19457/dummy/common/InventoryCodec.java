package io.github.railgun19457.dummy.common;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class InventoryCodec {

    private InventoryCodec() {
    }

    public static String encode(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream objectOutputStream = new BukkitObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(itemStack);
            objectOutputStream.flush();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode item stack", exception);
        }
    }

    public static ItemStack decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        byte[] bytes = Base64.getDecoder().decode(encoded);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
             BukkitObjectInputStream objectInputStream = new BukkitObjectInputStream(inputStream)) {
            Object value = objectInputStream.readObject();
            return value instanceof ItemStack itemStack ? itemStack : null;
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Failed to decode item stack", exception);
        }
    }

    public static List<String> encodeAll(ItemStack[] itemStacks) {
        List<String> encoded = new ArrayList<>(itemStacks.length);
        for (ItemStack itemStack : itemStacks) {
            encoded.add(encode(itemStack));
        }
        return encoded;
    }

    public static ItemStack[] decodeAll(List<String> encoded) {
        ItemStack[] itemStacks = new ItemStack[encoded.size()];
        for (int index = 0; index < encoded.size(); index++) {
            itemStacks[index] = decode(encoded.get(index));
        }
        return itemStacks;
    }
}