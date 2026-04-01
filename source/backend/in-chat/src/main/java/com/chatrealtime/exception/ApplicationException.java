package con.chatrealtime.exception;

public abstract class ApplicationException  extends RuntimeException{
    private final HttpStatus status;

    protected ApplicationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}