package rabbit.open.orm.core.dml;

import rabbit.open.orm.core.dialect.dml.DeleteDialectAdapter;


/**
 * <b>Description: 	oracle删除语句方言生成器</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
public class OracleDeleteGenerator extends DeleteDialectAdapter {

	@Override
	public StringBuilder createDeleteSql(Delete<?> delete) {
		StringBuilder sql = new StringBuilder("DELETE FROM " + delete.metaData.getTableName());
		String primaryKeyColumnName = delete.getColumnName(delete.metaData.getPrimaryKey());
        sql.append(DMLObject.WHERE + delete.metaData.getTableName() + "." + primaryKeyColumnName + " IN (");
		sql.append("SELECT " + delete.metaData.getTableName() + "." + primaryKeyColumnName + " FROM " 
					+ delete.metaData.getTableName());
		sql.append(delete.generateInnerJoinSql());
		sql.append(delete.generateFilterSql());
		sql.append(")");
		return sql;
	}

}
