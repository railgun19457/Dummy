package github.com.railgun19457.dummy.core.monitor;

import org.bukkit.Bukkit;

public class TpsMonitor {

    private static final int TPS_SAMPLE_INTERVAL = 40;
    private long lastCheck = System.currentTimeMillis();
    private double lastTps = 20.0;

    public double getCurrentTps() {
        try {
            Object server = Bukkit.getServer();
            Object minecraftServer = server.getClass().getMethod("getServer").invoke(server);
            double[] recentTps = (double[]) minecraftServer.getClass().getField("recentTps").get(minecraftServer);
            return recentTps[0];
        } catch (Exception e) {
            return estimateTps();
        }
    }

    private double estimateTps() {
        long now = System.currentTimeMillis();
        long diff = now - lastCheck;
        if (diff >= TPS_SAMPLE_INTERVAL * 50) {
            lastTps = Math.min(20.0, (TPS_SAMPLE_INTERVAL * 1000.0) / diff);
            lastCheck = now;
        }
        return lastTps;
    }

    public boolean isLowTps(double threshold) {
        return getCurrentTps() < threshold;
    }
}
