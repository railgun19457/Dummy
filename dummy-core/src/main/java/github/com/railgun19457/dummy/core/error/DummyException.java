package github.com.railgun19457.dummy.core.error;

public class DummyException extends RuntimeException {

    private final ErrorCode code;

    public DummyException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public DummyException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
