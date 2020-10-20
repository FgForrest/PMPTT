package cz.fg.oss.pmptt.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
@CommonsLog
public class JdbcUtils {
	private static final Map<String, DatabaseType> CACHED_RESULTS = new ConcurrentHashMap<>();
	private static final String JDBC_DRIVE_NAME_PREFIX = "jdbc:";

	@RequiredArgsConstructor
	public enum DatabaseType {
		MYSQL("mysql"), ORACLE("oracle");

		@Getter private final String urlStringBase;

	}

	private JdbcUtils() {
	}

	public static DatabaseType getPlatformFromJdbcUrl(DataSource dataSource) {
		DatabaseType result = CACHED_RESULTS.get(Integer.toString(dataSource.hashCode()));
		if (result == null) {
			try {
				try (Connection connection = dataSource.getConnection()) {
					String driverName = connection.getMetaData().getURL();
					result = getPlatformFromJdbcUrl(driverName);

					if (result != null) {
						if (log.isInfoEnabled()) {
							log.info("Recognized database platform: " + result);
						}
					} else {
						if (log.isErrorEnabled()) {
							log.error("Unrecognized database platform for driver: " + driverName);
						}
					}

					if (result != null) {
						CACHED_RESULTS.put(Integer.toString(dataSource.hashCode()), result);
					}
				}
			}
			catch(SQLException ex) {
				String msg = "Cannot connect to database.";
				log.fatal(msg, ex);
				throw new IllegalStateException(ex);
			}
		} else {
			if (log.isTraceEnabled()) {
				log.trace("Returning previously recognized platform: " + result);
			}
			return result;
		}

		return result;
	}

	private static DatabaseType getPlatformFromJdbcUrl(String jdbcUrl) {
		if (jdbcUrl.startsWith(JDBC_DRIVE_NAME_PREFIX + DatabaseType.MYSQL.getUrlStringBase())) {
			return DatabaseType.MYSQL;
		}
		if (jdbcUrl.startsWith(JDBC_DRIVE_NAME_PREFIX + DatabaseType.ORACLE.getUrlStringBase())) {
			return DatabaseType.ORACLE;
		}
		return null;
	}
}
