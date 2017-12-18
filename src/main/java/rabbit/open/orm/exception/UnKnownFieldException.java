package rabbit.open.orm.exception;

@SuppressWarnings("serial")
public class UnKnownFieldException extends RabbitDMLException{

    public UnKnownFieldException(String message) {
        super(message);
    }

}
