package rabbit.open.orm.core.dml;

import rabbit.open.orm.common.annotation.FilterType;
import rabbit.open.orm.core.dialect.dml.DeleteDialectAdapter;
import rabbit.open.orm.core.dml.meta.FilterDescriptor;

/**
 * <b>Description: 	mysql删除语句方言生成器</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
public class MySQLDeleteGenerator extends DeleteDialectAdapter{

	@Override
	public StringBuilder createDeleteSql(Delete<?> delete) {
		StringBuilder sql = new StringBuilder("DELETE " + delete.metaData.getTableName() + " FROM " 
							+ delete.metaData.getTableName());
		for (FilterDescriptor fd : delete.filterDescriptors) {
			if (fd.isJoinOn()) {
				sql.append(", " + fd.getFilterTable());
			}
		}
		sql.append(" WHERE 1 = 1");
		for (FilterDescriptor fd : delete.filterDescriptors) {
			if (!fd.isJoinOn()) {
				sql.append(" AND ");
				String key = fd.getKey();
				if (FilterType.IS.value().equals(fd.getFilter().trim())
						|| FilterType.IS_NOT.value().equals(fd.getFilter().trim())) {
					sql.append(key + " " + fd.getFilter() + DMLAdapter.NULL);
				} else {
					delete.cachePreparedValues(fd.getValue(), fd.getField());
					sql.append(key + fd.getFilter() + delete.createPlaceHolder(fd.getFilter(), fd.getValue()));
				}
			} else {
				sql.append(" AND " + fd.getKey() + " = " + fd.getValue());
			}
		}
		if (!delete.hasMultiDropFilters()) {
            return sql;
        }
		sql.append(delete.createMultiDropSql());
		return sql;
	}

}
