package cz.fg.oss.pmptt.dao.mysql;

import cz.fg.oss.pmptt.model.SectionWithBucket;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
class SectionRowMapper implements RowMapper<SectionWithBucket> {

	@Override
	public SectionWithBucket mapRow(ResultSet resultSet, int i) throws SQLException {
		return new SectionWithBucket(
				resultSet.getLong("leftBound"),
				resultSet.getLong("rightBound"),
				resultSet.getShort("bucket")
		);
	}

}
