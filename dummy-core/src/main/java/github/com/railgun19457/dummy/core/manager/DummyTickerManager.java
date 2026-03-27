package github.com.railgun19457.dummy.core.manager;

import github.com.railgun19457.dummy.nms.NMSServerPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DummyTickerManager {

    private final Map<UUID, DummyTicker> tickers = new ConcurrentHashMap<>();

    public void register(@NotNull Player player, @NotNull NMSServerPlayer nmsPlayer) {
        tickers.put(player.getUniqueId(), new DummyTicker(nmsPlayer));
    }

    public void unregister(@NotNull Player player) {
        tickers.remove(player.getUniqueId());
    }

    public void tick() {
        tickers.values().forEach(DummyTicker::tick);
    }

    public void clear() {
        tickers.clear();
    }

    private static class DummyTicker {
        private final NMSServerPlayer player;

        DummyTicker(NMSServerPlayer player) {
            this.player = player;
        }

        void tick() {
            player.doTick();
        }
    }
}
