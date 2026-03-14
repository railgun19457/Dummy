package io.github.railgun19457.dummy.core.action;

public record ActionResult(boolean success, String messageKey) {

    public static ActionResult success(String messageKey) {
        return new ActionResult(true, messageKey);
    }

    public static ActionResult failure(String messageKey) {
        return new ActionResult(false, messageKey);
    }
}
