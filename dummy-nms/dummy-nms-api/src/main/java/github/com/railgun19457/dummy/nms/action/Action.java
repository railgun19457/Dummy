package github.com.railgun19457.dummy.nms.action;

public interface Action {

    boolean tick();

    void inactiveTick();

    void stop();
}
