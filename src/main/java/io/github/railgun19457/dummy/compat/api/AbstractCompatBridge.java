package io.github.railgun19457.dummy.compat.api;

import io.github.railgun19457.dummy.common.InventoryCodec;
import io.github.railgun19457.dummy.core.model.DummyDefinition;
import io.github.railgun19457.dummy.core.model.DummyId;
import io.github.railgun19457.dummy.core.model.DummyInventoryState;
import io.github.railgun19457.dummy.core.model.Rotation;
import io.github.railgun19457.dummy.core.model.SkinProfile;
import io.github.railgun19457.dummy.core.model.DummyTraitSet;
import io.github.railgun19457.dummy.core.model.WorldPosition;
import io.github.railgun19457.dummy.core.port.CompatBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractCompatBridge implements CompatBridge {

    public static final String DUMMY_ID_KEY = "dummy_id";

    private final AtomicInteger entityIdSequence = new AtomicInteger(100000);
    private final Map<DummyId, FakePlayerHandle> handles = new ConcurrentHashMap<>();
    private final JavaPlugin plugin;
    private final NamespacedKey dummyIdKey;

    protected AbstractCompatBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dummyIdKey = new NamespacedKey(plugin, DUMMY_ID_KEY);
    }

    @Override
    public FakePlayerHandle createFakePlayer(DummyDefinition definition) {
        FakePlayerHandle existingHandle = handles.get(definition.id());
        if (existingHandle != null && resolveEntity(existingHandle).isPresent()) {
            return existingHandle;
        }

        World world = requireWorld(definition.lastKnownPosition().worldName());
        Location spawnLocation = toLocation(world, definition.lastKnownPosition());
        Zombie zombie = world.spawn(spawnLocation, Zombie.class, entity -> {
            entity.setAdult();
            entity.setAI(false);
            entity.setSilent(true);
            entity.setCanPickupItems(false);
            entity.setRemoveWhenFarAway(false);
            entity.setCustomName(definition.name());
            entity.setCustomNameVisible(true);
            entity.setInvulnerable(definition.traitSet().invulnerable());
            entity.setCollidable(definition.traitSet().collidable());
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(dummyIdKey, PersistentDataType.STRING, definition.id().toString());
        });
        applySkin(zombie, definition.skinProfile());
        applyInventoryToEntity(zombie, definition.inventoryState());

        FakePlayerHandle handle = new SimpleFakePlayerHandle(
                definition.id(),
                entityIdSequence.incrementAndGet(),
                zombie.getUniqueId()
        );
        handles.put(definition.id(), handle);
        return handle;
    }

    @Override
    public void removeFakePlayer(DummyId dummyId) {
        FakePlayerHandle handle = handles.remove(dummyId);
        if (handle != null) {
            resolveEntity(handle).ifPresent(Entity::remove);
        }
    }

    @Override
    public void teleport(FakePlayerHandle fakePlayerHandle, WorldPosition worldPosition) {
        resolveEntity(fakePlayerHandle).ifPresent(entity -> entity.teleport(toLocation(requireWorld(worldPosition.worldName()), worldPosition)));
    }

    @Override
    public void setSkin(FakePlayerHandle fakePlayerHandle, SkinProfile skinProfile) {
        resolveEntity(fakePlayerHandle)
                .filter(Zombie.class::isInstance)
                .map(Zombie.class::cast)
                .ifPresent(zombie -> applySkin(zombie, skinProfile));
    }

    @Override
    public void attack(FakePlayerHandle fakePlayerHandle, Entity entity) {
        resolveLivingEntity(fakePlayerHandle).ifPresent(attacker -> {
            if (entity instanceof Damageable damageable) {
                damageable.damage(1.0D, attacker);
            }
        });
    }

    @Override
    public void useItem(FakePlayerHandle fakePlayerHandle) {
        resolveLivingEntity(fakePlayerHandle).ifPresent(livingEntity -> {
            try {
                livingEntity.swingMainHand();
            } catch (NoSuchMethodError ignored) {
            }
        });
    }

    @Override
    public void startDig(FakePlayerHandle fakePlayerHandle, Block block) {
        if (block.getType().isAir()) {
            return;
        }
        block.breakNaturally();
    }

    @Override
    public void sleep(FakePlayerHandle fakePlayerHandle, Location location) {
        resolveLivingEntity(fakePlayerHandle).ifPresent(entity -> {
            entity.teleport(location);
            entity.setAI(false);
        });
    }

    @Override
    public void wakeUp(FakePlayerHandle fakePlayerHandle) {
        resolveLivingEntity(fakePlayerHandle).ifPresent(entity -> entity.setAI(false));
    }

    @Override
    public void look(FakePlayerHandle fakePlayerHandle, Rotation rotation) {
        resolveEntity(fakePlayerHandle).ifPresent(entity -> entity.setRotation(rotation.yaw(), rotation.pitch()));
    }

    @Override
    public void lookAtEntity(FakePlayerHandle fakePlayerHandle, Entity entity) {
        resolveEntity(fakePlayerHandle).ifPresent(dummy -> {
            Location from = dummy.getLocation();
            Location to = entity.getLocation();
            Vector direction = to.toVector().subtract(from.toVector());
            Location target = from.clone().setDirection(direction);
            dummy.setRotation(target.getYaw(), target.getPitch());
        });
    }

    @Override
    public void setSneaking(FakePlayerHandle fakePlayerHandle, boolean sneaking) {
        resolveLivingEntity(fakePlayerHandle).ifPresent(entity -> entity.setGlowing(sneaking));
    }

    @Override
    public void setSprinting(FakePlayerHandle fakePlayerHandle, boolean sprinting) {
        resolveLivingEntity(fakePlayerHandle).ifPresent(entity -> entity.setVisualFire(sprinting));
    }

    @Override
    public void jump(FakePlayerHandle fakePlayerHandle) {
        resolveEntity(fakePlayerHandle).ifPresent(entity -> entity.setVelocity(entity.getVelocity().setY(0.42D)));
    }

    @Override
    public void mount(FakePlayerHandle fakePlayerHandle, Entity entity) {
        resolveEntity(fakePlayerHandle).ifPresent(entity::addPassenger);
    }

    @Override
    public void swapHands(FakePlayerHandle fakePlayerHandle) {
        resolveLivingEntity(fakePlayerHandle).ifPresent(livingEntity -> {
            ItemStack mainHand = livingEntity.getEquipment().getItemInMainHand();
            ItemStack offHand = livingEntity.getEquipment().getItemInOffHand();
            livingEntity.getEquipment().setItemInMainHand(offHand);
            livingEntity.getEquipment().setItemInOffHand(mainHand);
        });
    }

    @Override
    public void holdItem(FakePlayerHandle fakePlayerHandle, ItemStack itemStack) {
        resolveLivingEntity(fakePlayerHandle).ifPresent(livingEntity -> livingEntity.getEquipment().setItemInMainHand(itemStack));
    }

    @Override
    public void applyInventory(FakePlayerHandle fakePlayerHandle, DummyInventoryState inventoryState) {
        resolveLivingEntity(fakePlayerHandle).ifPresent(entity -> applyInventoryToEntity(entity, inventoryState));
    }

    @Override
    public void updateTraits(FakePlayerHandle fakePlayerHandle, DummyTraitSet traitSet) {
        resolveLivingEntity(fakePlayerHandle).ifPresent(entity -> {
            entity.setInvulnerable(traitSet.invulnerable());
            entity.setCollidable(traitSet.collidable());
            entity.setGlowing(!traitSet.allowInteractionConfigure());
        });
    }

    public Optional<DummyId> resolveDummyId(Entity entity) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        String value = container.get(dummyIdKey, PersistentDataType.STRING);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new DummyId(UUID.fromString(value)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Optional<Entity> resolveEntity(FakePlayerHandle fakePlayerHandle) {
        return Optional.ofNullable(Bukkit.getEntity(fakePlayerHandle.entityUuid()));
    }

    private Optional<LivingEntity> resolveLivingEntity(FakePlayerHandle fakePlayerHandle) {
        return resolveEntity(fakePlayerHandle)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast);
    }

    private void applySkin(Zombie zombie, SkinProfile skinProfile) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta skullMeta) {
            String ownerName = skinProfile.sourcePlayerName();
            if (ownerName != null && !ownerName.isBlank()) {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerName));
                head.setItemMeta(skullMeta);
            }
        }
        zombie.getEquipment().setHelmet(head);
    }

    private void applyInventoryToEntity(LivingEntity entity, DummyInventoryState inventoryState) {
        java.util.List<String> contents = inventoryState.contents();
        java.util.List<String> armor = inventoryState.armorContents();
        entity.getEquipment().setItemInMainHand(contents.isEmpty() ? null : InventoryCodec.decode(contents.get(0)));
        entity.getEquipment().setItemInOffHand(InventoryCodec.decode(inventoryState.offhandItem()));
        entity.getEquipment().setBoots(armor.size() > 0 ? InventoryCodec.decode(armor.get(0)) : null);
        entity.getEquipment().setLeggings(armor.size() > 1 ? InventoryCodec.decode(armor.get(1)) : null);
        entity.getEquipment().setChestplate(armor.size() > 2 ? InventoryCodec.decode(armor.get(2)) : null);
        if (!(entity instanceof Zombie zombie)) {
            entity.getEquipment().setHelmet(armor.size() > 3 ? InventoryCodec.decode(armor.get(3)) : null);
            return;
        }
        ItemStack skinHead = zombie.getEquipment().getHelmet();
        ItemStack armorHead = armor.size() > 3 ? InventoryCodec.decode(armor.get(3)) : null;
        zombie.getEquipment().setHelmet(armorHead != null && !armorHead.getType().isAir() ? armorHead : skinHead);
    }

    private World requireWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return world;
        }
        if (!Bukkit.getWorlds().isEmpty()) {
            return Bukkit.getWorlds().get(0);
        }
        throw new IllegalStateException("No world available for dummy spawn");
    }

    private Location toLocation(World world, WorldPosition worldPosition) {
        return new Location(world, worldPosition.x(), worldPosition.y(), worldPosition.z(), worldPosition.rotation().yaw(), worldPosition.rotation().pitch());
    }
}
