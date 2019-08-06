package rabbit.open.orm.dml.policy;

import java.util.UUID;

public class UUIDPolicy{

	public static String getID() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}
	
}
