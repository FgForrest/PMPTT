package one.edee.oss.pmptt.dao.mysql;

import lombok.RequiredArgsConstructor;
import one.edee.oss.pmptt.dao.DbHierarchyStorage;
import one.edee.oss.pmptt.model.DbHierarchy;
import org.springframework.jdbc.core.RowMapper;

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
	private final DbHierarchyStorage dbHierarchyStorage;

	@Override
	public DbHierarchy mapRow(ResultSet resultSet, int i) throws SQLException {
		return new DbHierarchy(
				resultSet.getString("code"),
				((short)(resultSet.getShort("levels") - 1)),
				((short)(resultSet.getShort("sectionSize") - 1)),
				dbHierarchyStorage
		);
	}

}
