package cz.fg.oss.pmptt.spring;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@Configuration
public class OracleSqlDataSource {

	@Bean
	public TransactionAwareDataSourceProxy dataSource(
			@Value("${oracle.driver}") String driver,
			@Value("${oracle.url}") String url,
			@Value("${oracle.user}") String user,
			@Value("${oracle.password}") String pwd
	) {
		final HikariConfig config = new HikariConfig();
		config.setJdbcUrl(url);
		config.setUsername(user);
		config.setPassword(pwd);
		config.setDriverClassName(driver);
		config.setConnectionTimeout(2000);
		config.setMaximumPoolSize(2);
		config.addDataSourceProperty( "cachePrepStmts" , "true" );
		config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
		config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );

		final HikariDataSource dataSource = new HikariDataSource(config);
		return new TransactionAwareDataSourceProxy(dataSource);
	}

	@Bean
	public DataSourceTransactionManager transactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

}
