package cz.fg.oss.pmptt.dao.oracle;

import cz.fg.oss.pmptt.model.DbHierarchy;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@RequiredArgsConstructor
class HierarchyRowMapper implements RowMapper<DbHierarchy> {
	private final PlatformTransactionManager transactionManager;

	@Override
	public DbHierarchy mapRow(ResultSet resultSet, int i) throws SQLException {
		return new DbHierarchy(
				resultSet.getString("code"),
				resultSet.getShort("levels"),
				resultSet.getShort("sectionSize"),
				transactionManager
		);
	}

}
