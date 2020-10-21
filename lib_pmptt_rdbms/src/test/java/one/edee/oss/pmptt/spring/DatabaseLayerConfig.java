package one.edee.oss.pmptt.spring;

import one.edee.oss.pmptt.dao.HierarchyStorage;
import one.edee.oss.pmptt.dao.mysql.MySqlStorage;
import one.edee.oss.pmptt.dao.oracle.OracleSqlStorage;
import one.edee.oss.pmptt.util.JdbcUtils;
import one.edee.oss.pmptt.util.JdbcUtils.DatabaseType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
@Configuration
public class DatabaseLayerConfig {

	@Bean
	public HierarchyStorage hierarchyStorage(DataSource dataSource, PlatformTransactionManager transactionManager) {
		final DatabaseType platform = JdbcUtils.getPlatformFromJdbcUrl(dataSource);
		if (platform == DatabaseType.MYSQL) {
			return new MySqlStorage(dataSource, transactionManager);
		} else if (platform == DatabaseType.ORACLE) {
			return new OracleSqlStorage(dataSource, transactionManager);
		} else {
			throw new IllegalStateException("Unsupported platform " + platform + " for MPTT implementation!");
		}
	}


}
