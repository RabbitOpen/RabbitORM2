package rabbit.open.orm.core.dml;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rabbit.open.orm.core.dialect.ddl.DDLHelper;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.meta.TableMeta;

/**
 * 
 * <b>@description 分区表监控器 </b>
 */
public class ShardedTableMonitor extends Thread {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private SessionFactory factory;

	private Semaphore semaphore = new Semaphore(0);

	public ShardedTableMonitor(SessionFactory factory) {
		super();
		this.factory = factory;
		setName("Sharded-Table-Monitor");
	}

	@Override
	public void run() {
		while (true) {
			try {
				if (semaphore.tryAcquire(60, TimeUnit.MINUTES)) {
					break;
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			try {
				reloadShardedTables();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		logger.info("ShardedTableMonitor is closed");
	}

	/**
	 * <b>@description 刷新实体对应的分片表 </b>
	 * @throws ClassNotFoundException
	 */
	protected void reloadShardedTables() throws ClassNotFoundException {
		for (String entity : factory.getEntities()) {
			Class<?> clz = Class.forName(entity);
			MetaData<?> meta = MetaData.getMetaByClass(clz);
			if (meta.isShardingTable()) {
				reloadByEntityMeta(meta);
			}
		}
	}

	/**
	 * <b>@description 按分区表刷新 </b>
	 * @param meta
	 */
	private void reloadByEntityMeta(MetaData<?> meta) {
		List<TableMeta> list = new ArrayList<>();
		for (DataSource ds : factory.getAllDataSources()) {
			Connection conn = null;
			try {
				conn = ds.getConnection();
				Set<String> tables = DDLHelper.readTablesFromDB(conn);
				for (String table : tables) {
					if (factory.getShardedNameMatcher().match(meta.getEntityClz(), meta.getTableName(), table)) {
						list.add(new TableMeta(table, ds));
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				DMLObject.closeConnection(conn);
			}
		}
		factory.setShardTables(meta.getShardingPolicy().getClass(), meta.getEntityClz(), list);
	}

	public void shutdown() {
		logger.info("begin to close ShardedTableMonitor");
		semaphore.release();
	}

}
