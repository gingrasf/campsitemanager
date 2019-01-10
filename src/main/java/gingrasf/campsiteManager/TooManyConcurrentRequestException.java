package gingrasf.campsiteManager;

public class TooManyConcurrentRequestException extends RuntimeException {

    public TooManyConcurrentRequestException(String message) {
        super(message);
    }
}
