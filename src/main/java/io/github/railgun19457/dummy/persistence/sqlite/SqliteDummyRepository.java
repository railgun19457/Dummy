package io.github.railgun19457.dummy.persistence.sqlite;

import io.github.railgun19457.dummy.core.model.DummyDefinition;
import io.github.railgun19457.dummy.core.model.DummyId;
import io.github.railgun19457.dummy.core.model.DummyInventoryState;
import io.github.railgun19457.dummy.core.model.DummyRuntimeState;
import io.github.railgun19457.dummy.core.model.DummyTraitSet;
import io.github.railgun19457.dummy.core.model.Rotation;
import io.github.railgun19457.dummy.core.model.SkinProfile;
import io.github.railgun19457.dummy.core.model.WorldPosition;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.port.DummyRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SqliteDummyRepository implements DummyRepository {

    private final SqliteDatabase database;

    public SqliteDummyRepository(SqliteDatabase database) {
        this.database = database;
    }

    @Override
    public void save(DummyDefinition definition) {
        try (Connection connection = database.openConnection()) {
            upsertProfile(connection, definition);
            upsertInventory(connection, definition.id(), definition.inventoryState());
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save dummy: " + definition.id(), exception);
        }
    }

    @Override
    public Optional<DummyDefinition> findById(DummyId dummyId) {
        List<DummyDefinition> definitions = queryDefinitions("WHERE p.id = ?", statement -> statement.setString(1, dummyId.toString()));
        return definitions.stream().findFirst();
    }

    @Override
    public List<DummyDefinition> findByOwner(UUID ownerId) {
        return queryDefinitions("WHERE p.owner_id = ?", statement -> statement.setString(1, ownerId.toString()));
    }

    @Override
    public List<DummyDefinition> findAll() {
        return queryDefinitions("", statement -> {
        });
    }

    @Override
    public void delete(DummyId dummyId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM dummy_profile WHERE id = ?")) {
            statement.setString(1, dummyId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete dummy: " + dummyId, exception);
        }
    }

    @Override
    public void updateTraits(DummyId dummyId, DummyTraitSet traitSet) {
        executeUpdate("""
                UPDATE dummy_profile SET
                    trait_collidable = ?,
                    trait_invulnerable = ?,
                    trait_auto_restock = ?,
                    trait_allow_inventory_open = ?,
                    trait_allow_interaction_configure = ?
                WHERE id = ?
                """, statement -> {
            statement.setInt(1, toSqlBoolean(traitSet.collidable()));
            statement.setInt(2, toSqlBoolean(traitSet.invulnerable()));
            statement.setInt(3, toSqlBoolean(traitSet.autoRestock()));
            statement.setInt(4, toSqlBoolean(traitSet.allowInventoryOpen()));
            statement.setInt(5, toSqlBoolean(traitSet.allowInteractionConfigure()));
            statement.setString(6, dummyId.toString());
        });
    }

    @Override
    public void updateLocation(DummyId dummyId, WorldPosition worldPosition) {
        executeUpdate("""
                UPDATE dummy_profile SET
                    world_name = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?
                WHERE id = ?
                """, statement -> {
            statement.setString(1, worldPosition.worldName());
            statement.setDouble(2, worldPosition.x());
            statement.setDouble(3, worldPosition.y());
            statement.setDouble(4, worldPosition.z());
            statement.setFloat(5, worldPosition.rotation().yaw());
            statement.setFloat(6, worldPosition.rotation().pitch());
            statement.setString(7, dummyId.toString());
        });
    }

    @Override
    public void updateSkin(DummyId dummyId, SkinProfile skinProfile) {
        executeUpdate("""
                UPDATE dummy_profile SET
                    skin_source_player_name = ?, skin_texture = ?, skin_signature = ?
                WHERE id = ?
                """, statement -> {
            statement.setString(1, skinProfile.sourcePlayerName());
            statement.setString(2, skinProfile.texture());
            statement.setString(3, skinProfile.signature());
            statement.setString(4, dummyId.toString());
        });
    }

    @Override
    public void updateInventory(DummyId dummyId, DummyInventoryState inventoryState) {
        try (Connection connection = database.openConnection()) {
            upsertInventory(connection, dummyId, inventoryState);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update inventory: " + dummyId, exception);
        }
    }

    @Override
    public void saveRuntimeState(DummyRuntimeState runtimeState) {
        executeUpdate("""
                INSERT INTO dummy_runtime_snapshot (
                    dummy_id, spawned, entity_id, current_action, sleeping, sneaking, sprinting, mounted_target
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(dummy_id) DO UPDATE SET
                    spawned = excluded.spawned,
                    entity_id = excluded.entity_id,
                    current_action = excluded.current_action,
                    sleeping = excluded.sleeping,
                    sneaking = excluded.sneaking,
                    sprinting = excluded.sprinting,
                    mounted_target = excluded.mounted_target
                """, statement -> {
            statement.setString(1, runtimeState.dummyId().toString());
            statement.setInt(2, toSqlBoolean(runtimeState.spawned()));
            if (runtimeState.entityId() == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, runtimeState.entityId());
            }
            statement.setString(4, runtimeState.currentAction() == null ? null : runtimeState.currentAction().name());
            statement.setInt(5, toSqlBoolean(runtimeState.sleeping()));
            statement.setInt(6, toSqlBoolean(runtimeState.sneaking()));
            statement.setInt(7, toSqlBoolean(runtimeState.sprinting()));
            if (runtimeState.mountedTarget() == null) {
                statement.setNull(8, java.sql.Types.INTEGER);
            } else {
                statement.setInt(8, runtimeState.mountedTarget());
            }
        });
    }

    @Override
    public Map<DummyId, DummyRuntimeState> findAllRuntimeStates() {
        Map<DummyId, DummyRuntimeState> snapshots = new LinkedHashMap<>();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM dummy_runtime_snapshot ORDER BY dummy_id ASC");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                DummyId dummyId = new DummyId(UUID.fromString(resultSet.getString("dummy_id")));
                String actionName = resultSet.getString("current_action");
                snapshots.put(dummyId, new DummyRuntimeState(
                        dummyId,
                        resultSet.getInt("spawned") == 1,
                        resultSet.getObject("entity_id", Integer.class),
                        actionName == null || actionName.isBlank() ? null : ActionType.valueOf(actionName),
                        resultSet.getInt("sleeping") == 1,
                        resultSet.getInt("sneaking") == 1,
                        resultSet.getInt("sprinting") == 1,
                        resultSet.getObject("mounted_target", Integer.class)
                ));
            }
            return snapshots;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load runtime snapshots", exception);
        }
    }

    @Override
    public Map<DummyId, Integer> findAllStoredExperience() {
        Map<DummyId, Integer> values = new LinkedHashMap<>();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT dummy_id, stored_exp FROM dummy_experience ORDER BY dummy_id ASC");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                values.put(new DummyId(UUID.fromString(resultSet.getString("dummy_id"))), resultSet.getInt("stored_exp"));
            }
            return values;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load stored experience", exception);
        }
    }

    @Override
    public void saveStoredExperience(DummyId dummyId, int amount) {
        executeUpdate("""
                INSERT INTO dummy_experience (dummy_id, stored_exp)
                VALUES (?, ?)
                ON CONFLICT(dummy_id) DO UPDATE SET
                    stored_exp = excluded.stored_exp
                """, statement -> {
            statement.setString(1, dummyId.toString());
            statement.setInt(2, amount);
        });
    }

    @Override
    public void deleteStoredExperience(DummyId dummyId) {
        executeUpdate("DELETE FROM dummy_experience WHERE dummy_id = ?", statement -> statement.setString(1, dummyId.toString()));
    }

    @Override
    public void deleteRuntimeState(DummyId dummyId) {
        executeUpdate("DELETE FROM dummy_runtime_snapshot WHERE dummy_id = ?", statement -> statement.setString(1, dummyId.toString()));
    }

    private List<DummyDefinition> queryDefinitions(String whereClause, SqlConsumer<PreparedStatement> binder) {
        String sql = """
                SELECT p.*, i.contents, i.armor_contents, i.offhand_item
                FROM dummy_profile p
                LEFT JOIN dummy_inventory i ON i.dummy_id = p.id
                %s
                ORDER BY p.created_at ASC
                """.formatted(whereClause);
        List<DummyDefinition> definitions = new ArrayList<>();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.accept(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    definitions.add(mapDefinition(resultSet));
                }
            }
            return definitions;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query dummy definitions", exception);
        }
    }

    private DummyDefinition mapDefinition(ResultSet resultSet) throws SQLException {
        DummyId dummyId = new DummyId(UUID.fromString(resultSet.getString("id")));
        WorldPosition position = new WorldPosition(
                resultSet.getString("world_name"),
                resultSet.getDouble("x"),
                resultSet.getDouble("y"),
                resultSet.getDouble("z"),
                new Rotation(resultSet.getFloat("yaw"), resultSet.getFloat("pitch"))
        );
        SkinProfile skinProfile = new SkinProfile(
                resultSet.getString("skin_source_player_name"),
                resultSet.getString("skin_texture"),
                resultSet.getString("skin_signature")
        );
        DummyTraitSet traitSet = new DummyTraitSet(
                resultSet.getInt("trait_collidable") == 1,
                resultSet.getInt("trait_invulnerable") == 1,
                resultSet.getInt("trait_auto_restock") == 1,
                resultSet.getInt("trait_allow_inventory_open") == 1,
                resultSet.getInt("trait_allow_interaction_configure") == 1
        );
        DummyInventoryState inventoryState = new DummyInventoryState(
                deserializeList(resultSet.getString("contents")),
                deserializeList(resultSet.getString("armor_contents")),
                resultSet.getString("offhand_item")
        );
        return new DummyDefinition(
                dummyId,
                resultSet.getString("name"),
                UUID.fromString(resultSet.getString("owner_id")),
                resultSet.getString("owner_name"),
                position,
                skinProfile,
                traitSet,
                inventoryState,
                Instant.parse(resultSet.getString("created_at")),
                Instant.parse(resultSet.getString("updated_at"))
        );
    }

    private void upsertProfile(Connection connection, DummyDefinition definition) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO dummy_profile (
                    id, name, owner_id, owner_name, world_name, x, y, z, yaw, pitch,
                    skin_source_player_name, skin_texture, skin_signature,
                    trait_collidable, trait_invulnerable, trait_auto_restock,
                    trait_allow_inventory_open, trait_allow_interaction_configure,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    name = excluded.name,
                    owner_id = excluded.owner_id,
                    owner_name = excluded.owner_name,
                    world_name = excluded.world_name,
                    x = excluded.x,
                    y = excluded.y,
                    z = excluded.z,
                    yaw = excluded.yaw,
                    pitch = excluded.pitch,
                    skin_source_player_name = excluded.skin_source_player_name,
                    skin_texture = excluded.skin_texture,
                    skin_signature = excluded.skin_signature,
                    trait_collidable = excluded.trait_collidable,
                    trait_invulnerable = excluded.trait_invulnerable,
                    trait_auto_restock = excluded.trait_auto_restock,
                    trait_allow_inventory_open = excluded.trait_allow_inventory_open,
                    trait_allow_interaction_configure = excluded.trait_allow_interaction_configure,
                    updated_at = excluded.updated_at
                """)) {
            statement.setString(1, definition.id().toString());
            statement.setString(2, definition.name());
            statement.setString(3, definition.ownerId().toString());
            statement.setString(4, definition.ownerName());
            statement.setString(5, definition.lastKnownPosition().worldName());
            statement.setDouble(6, definition.lastKnownPosition().x());
            statement.setDouble(7, definition.lastKnownPosition().y());
            statement.setDouble(8, definition.lastKnownPosition().z());
            statement.setFloat(9, definition.lastKnownPosition().rotation().yaw());
            statement.setFloat(10, definition.lastKnownPosition().rotation().pitch());
            statement.setString(11, definition.skinProfile().sourcePlayerName());
            statement.setString(12, definition.skinProfile().texture());
            statement.setString(13, definition.skinProfile().signature());
            statement.setInt(14, toSqlBoolean(definition.traitSet().collidable()));
            statement.setInt(15, toSqlBoolean(definition.traitSet().invulnerable()));
            statement.setInt(16, toSqlBoolean(definition.traitSet().autoRestock()));
            statement.setInt(17, toSqlBoolean(definition.traitSet().allowInventoryOpen()));
            statement.setInt(18, toSqlBoolean(definition.traitSet().allowInteractionConfigure()));
            statement.setString(19, definition.createdAt().toString());
            statement.setString(20, definition.updatedAt().toString());
            statement.executeUpdate();
        }
    }

    private void upsertInventory(Connection connection, DummyId dummyId, DummyInventoryState inventoryState) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO dummy_inventory (dummy_id, contents, armor_contents, offhand_item)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(dummy_id) DO UPDATE SET
                    contents = excluded.contents,
                    armor_contents = excluded.armor_contents,
                    offhand_item = excluded.offhand_item
                """)) {
            statement.setString(1, dummyId.toString());
            statement.setString(2, serializeList(inventoryState.contents()));
            statement.setString(3, serializeList(inventoryState.armorContents()));
            statement.setString(4, inventoryState.offhandItem());
            statement.executeUpdate();
        }
    }

    private void executeUpdate(String sql, SqlConsumer<PreparedStatement> binder) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.accept(statement);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to execute repository update", exception);
        }
    }

    private int toSqlBoolean(boolean value) {
        return value ? 1 : 0;
    }

    private String serializeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream().map(value -> value.replace("|", "\\|")).collect(Collectors.joining("|"));
    }

    private List<String> deserializeList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("(?<!\\\\)\\|", -1)).stream()
                .map(entry -> entry.replace("\\|", "|"))
                .toList();
    }

    @FunctionalInterface
    private interface SqlConsumer<T> {
        void accept(T value) throws SQLException;
    }
}
