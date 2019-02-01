package rabbit.open.orm.exception;

@SuppressWarnings("serial")
public class PropagationException extends RabbitDMLException {

	public PropagationException() {
		super("only Propagation.NESTED or Propagation.REQUIRED is supported by RabbitTransactionManager!");
	}

}
