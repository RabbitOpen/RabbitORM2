package rabbit.open.orm.dml.meta;

import rabbit.open.orm.annotation.FilterType;

/**
 * 
 * 动态新增过滤条件时的描述符
 * @author	肖乾斌
 * 
 */
public class DynamicFilterDescriptor {

	//修改的key的正则表达式
	private String keyReg;
	
	private FilterType filter;
	
	private Object value;
	
	private boolean isReg = false;
	
	public String getKeyReg() {
		return keyReg;
	}

	public FilterType getFilter() {
		return filter;
	}

	public Object getValue() {
		return value;
	}

	public boolean isReg() {
		return isReg;
	}

	public DynamicFilterDescriptor(String keyReg, FilterType filter,
			Object value, boolean isReg) {
		super();
		this.keyReg = keyReg;
		this.filter = filter;
		this.value = value;
		this.isReg = isReg;
	}

}
