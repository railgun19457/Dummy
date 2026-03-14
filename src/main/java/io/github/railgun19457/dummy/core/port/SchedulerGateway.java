package io.github.railgun19457.dummy.core.port;

public interface SchedulerGateway {

    void runSync(Runnable runnable);

    void runAsync(Runnable runnable);
}
