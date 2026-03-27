package github.com.railgun19457.dummy.nms.action;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActionSetting {

    public int interval;
    public int remains;
    public int wait;

    public static ActionSetting of(int interval, int remains) {
        return new ActionSetting(interval, remains, 0);
    }

    public static ActionSetting stop() {
        return new ActionSetting(0, 0, 0);
    }
}
