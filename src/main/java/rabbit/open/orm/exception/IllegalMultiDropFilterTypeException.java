package rabbit.open.orm.exception;

@SuppressWarnings("serial")
public class IllegalMultiDropFilterTypeException extends RabbitDMLException {

    public IllegalMultiDropFilterTypeException(Class<?> clz) {
        super("Illegal MultiDropFilter Type[" + clz + "]");
    }

}
