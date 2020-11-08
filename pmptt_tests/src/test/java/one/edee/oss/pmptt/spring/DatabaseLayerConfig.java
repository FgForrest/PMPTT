package one.edee.oss.pmptt.spring;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import one.edee.oss.pmptt.dao.HierarchyStorage;
import one.edee.oss.pmptt.dao.mysql.MySqlStorage;
import one.edee.oss.pmptt.dao.oracle.OracleSqlStorage;
import one.edee.oss.pmptt.util.JdbcUtils;
import one.edee.oss.pmptt.util.JdbcUtils.DatabaseType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
@Configuration
@Import(PmpttSpringConfiguration.class)
public class DatabaseLayerConfig {

	@Bean
	public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		configurer.setLocation(new ClassPathResource("test.properties"));
		return configurer;
	}

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

	@Bean
	public PlatformTransactionManager transactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean("dataSource")
	@Profile("MYSQL")
	public DataSource mysqlDataSource(
			@Value("${jdbc.url.mysql}") String url,
			@Value("${jdbc.user.mysql}") String user,
			@Value("${jdbc.password.mysql}") String password
	) {
		final HikariConfig cfg = new HikariConfig();

		cfg.setJdbcUrl(url);
		cfg.setUsername(user);
		cfg.setPassword(password);
		cfg.setAutoCommit(true);
		cfg.setMaximumPoolSize(5);
		cfg.setMaxLifetime(10000);
		cfg.addDataSourceProperty( "cachePrepStmts" , "true" );
		cfg.addDataSourceProperty( "prepStmtCacheSize" , "250" );
		cfg.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
		return new TransactionAwareDataSourceProxy(
				new HikariDataSource(cfg)
		);
	}

	@Bean("dataSource")
	@Profile("ORACLE")
	public DataSource oracleDataSource(
			@Value("${jdbc.url.oracle}") String url,
			@Value("${jdbc.user.oracle}") String user,
			@Value("${jdbc.password.oracle}") String password
	) {
		final HikariConfig cfg = new HikariConfig();

		cfg.setJdbcUrl(url);
		cfg.setUsername(user);
		cfg.setPassword(password);
		cfg.setAutoCommit(true);
		cfg.setMaximumPoolSize(5);
		cfg.setMaxLifetime(10000);
		cfg.addDataSourceProperty( "cachePrepStmts" , "true" );
		cfg.addDataSourceProperty( "prepStmtCacheSize" , "250" );
		cfg.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
		return new TransactionAwareDataSourceProxy(
				new HikariDataSource(cfg)
		);
	}


}
