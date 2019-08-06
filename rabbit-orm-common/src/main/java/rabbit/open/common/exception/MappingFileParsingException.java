package rabbit.open.common.exception;

/**
 * <b>Description: sql文件解析异常</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
@SuppressWarnings("serial")
public class MappingFileParsingException extends RuntimeException {

	public MappingFileParsingException(String message) {
		super(message);
	}

}
