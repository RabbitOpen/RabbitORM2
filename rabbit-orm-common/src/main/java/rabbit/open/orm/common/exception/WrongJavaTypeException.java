package rabbit.open.orm.common.exception;

@SuppressWarnings("serial")
public class WrongJavaTypeException extends RabbitDMLException {

    public WrongJavaTypeException(Class<?> expected, Object valueActual) {
        super("unexpected value type exception: [" + expected.getName() + "] is expected, but ["
                + (null == valueActual ? null : valueActual.getClass()) + "] is found");
    }
}
