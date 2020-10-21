package one.edee.oss.pmptt.dao.mysql;

import one.edee.oss.pmptt.model.HierarchyItem;
import one.edee.oss.pmptt.model.HierarchyItemWithHistory;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
class HierarchyItemRowMapper implements RowMapper<HierarchyItem> {

	@Override
	public HierarchyItem mapRow(ResultSet resultSet, int i) throws SQLException {
		return new HierarchyItemWithHistory(
				resultSet.getString("hierarchyCode"),
				resultSet.getString("code"),
				resultSet.getShort("level"),
				resultSet.getLong("leftBound"),
				resultSet.getLong("rightBound"),
				resultSet.getShort("numberOfChildren"),
				resultSet.getShort("order"),
				resultSet.getShort("bucket")
		);
	}

}
