package one.edee.oss.pmptt.dao.oracle;

import lombok.Getter;
import one.edee.oss.pmptt.dao.DbHierarchyStorage;
import one.edee.oss.pmptt.dao.HierarchyStorage;
import one.edee.oss.pmptt.model.DbHierarchy;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItem;
import one.edee.oss.pmptt.model.HierarchyItemWithHistory;
import one.edee.oss.pmptt.model.SectionWithBucket;
import one.edee.oss.pmptt.spi.HierarchyChangeListener;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Oracle implementation of {@link HierarchyStorage}
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public class OracleSqlStorage implements DbHierarchyStorage {
	private final List<HierarchyChangeListener> changeListeners = new LinkedList<>();
	@Getter private final PlatformTransactionManager transactionManager;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public OracleSqlStorage(DataSource dataSource, PlatformTransactionManager transactionManager) {
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
		this.transactionManager = transactionManager;
	}

	@Override
	public void registerChangeListener(HierarchyChangeListener listener) {
		this.changeListeners.add(listener);
	}

	@Override
	public void createHierarchy(Hierarchy hierarchy) {
		namedParameterJdbcTemplate
				.update(
						"insert into T_MPTT_HIERARCHY (\"code\", \"levels\", \"sectionSize\") values (:code, :levels, :sectionSize)",
						new BeanPropertySqlParameterSource(hierarchy)
				);
		hierarchy.setStorage(this);
	}

	@Override
	public DbHierarchy getHierarchy(String code) {
		try {
			return namedParameterJdbcTemplate
					.queryForObject(
							"select * from T_MPTT_HIERARCHY where \"code\" = :code",
							Collections.singletonMap("code", code),
							new HierarchyRowMapper(this)
					);
		} catch (EmptyResultDataAccessException ex) {
			return null;
		}
	}

	@Override
	public void createItem(HierarchyItem newItem, HierarchyItem parent) {
		namedParameterJdbcTemplate
				.update(
						"insert into T_MPTT_ITEM (\"code\", \"hierarchyCode\", \"level\", \"leftBound\", \"rightBound\", \"numberOfChildren\", \"order\", \"bucket\") " +
								"values (:code, :hierarchyCode, :level, :leftBound, :rightBound, :numberOfChildren, :order, :bucket)",
						new BeanPropertySqlParameterSource(newItem)
				);
		for (HierarchyChangeListener changeListener : changeListeners) {
			changeListener.itemCreated(newItem);
		}
	}

	@Override
	public void updateItem(HierarchyItem updatedItem) {
		final int affectedRows = namedParameterJdbcTemplate
				.update(
						"update T_MPTT_ITEM " +
								"set \"numberOfChildren\" = :numberOfChildren, " +
								"    \"leftBound\" = :leftBound, " +
								"    \"rightBound\" = :rightBound, " +
								"    \"level\" = :level, " +
								"    \"order\" = :order " +
								"where \"code\" = :code and \"hierarchyCode\" = :hierarchyCode",
						new BeanPropertySqlParameterSource(updatedItem)
				);
		Assert.isTrue(affectedRows == 1, "Removed unexpected count of rows: " + affectedRows + "!");
		for (HierarchyChangeListener changeListener : changeListeners) {
			Assert.isTrue(updatedItem instanceof HierarchyItemWithHistory);
			final HierarchyItemWithHistory hiwh = (HierarchyItemWithHistory) updatedItem;
			changeListener.itemUpdated(hiwh.getDelegate(), hiwh.getOriginal());
		}
	}

	@Override
	public void removeItem(HierarchyItem removedItem) {
		final int affectedRows = namedParameterJdbcTemplate
				.update(
						"delete from T_MPTT_ITEM " +
								"where \"code\" = :code and \"hierarchyCode\" = :hierarchyCode",
						new BeanPropertySqlParameterSource(removedItem)
				);
		Assert.isTrue(affectedRows == 1, "Removed unexpected count of rows: " + affectedRows + "!");
		for (HierarchyChangeListener changeListener : changeListeners) {
			changeListener.itemRemoved(removedItem);
		}
	}

	@Override
	public HierarchyItem getItem(String hierarchyCode, String code) {
		try {
			final HashMap<String, Object> params = new HashMap<>();
			params.put("code", code);
			params.put("hierarchyCode", hierarchyCode);
			return namedParameterJdbcTemplate
					.queryForObject(
							"select * from T_MPTT_ITEM where \"code\" = :code and \"hierarchyCode\" = :hierarchyCode",
							params,
							new HierarchyItemRowMapper()
					);
		} catch (EmptyResultDataAccessException ex) {
			return null;
		}
	}

	@Override
	public HierarchyItem getParentItem(HierarchyItem pivot) {
		try {
			final HashMap<String, Object> params = new HashMap<>();
			params.put("hierarchyCode", pivot.getHierarchyCode());
			params.put("level", (short)(pivot.getLevel() - 1));
			params.put("leftBound", pivot.getLeftBound());
			params.put("rightBound", pivot.getRightBound());
			return namedParameterJdbcTemplate
					.queryForObject(
							"select * from T_MPTT_ITEM " +
									"where \"hierarchyCode\" = :hierarchyCode " +
									"  and \"level\" = :level " +
									"  and \"leftBound\" <= :leftBound " +
									"  and \"rightBound\" >= :rightBound ",
							params,
							new HierarchyItemRowMapper()
					);
		} catch (EmptyResultDataAccessException ex) {
			return null;
		}
	}

	@Override
	public List<HierarchyItem> getParentsOfItem(HierarchyItem pivot) {
		return namedParameterJdbcTemplate
				.query(
						"select * from T_MPTT_ITEM " +
								"where \"hierarchyCode\" = :hierarchyCode " +
								"  and \"level\" < :level " +
								"  and \"leftBound\" <= :leftBound " +
								"  and \"rightBound\" >= :rightBound " +
								"order by \"level\" asc",
						new BeanPropertySqlParameterSource(pivot),
						new HierarchyItemRowMapper()
				);
	}

	@Override
	public List<HierarchyItem> getRootItems(String hierarchyCode) {
		return namedParameterJdbcTemplate
				.query(
						"select * from T_MPTT_ITEM " +
								"where \"hierarchyCode\" = :hierarchyCode " +
								"  and \"level\" = 1 " +
								"order by \"order\" asc",
						Collections.singletonMap("hierarchyCode", hierarchyCode),
						new HierarchyItemRowMapper()
				);
	}

	@Override
	public List<HierarchyItem> getChildItems(HierarchyItem parent) {
		final HashMap<String, Object> params = new HashMap<>();
		params.put("hierarchyCode", parent.getHierarchyCode());
		params.put("level", (short)(parent.getLevel() + 1));
		params.put("leftBound", parent.getLeftBound());
		params.put("rightBound", parent.getRightBound());
		return namedParameterJdbcTemplate
				.query(
						"select * from T_MPTT_ITEM " +
								"where \"hierarchyCode\" = :hierarchyCode " +
								"  and \"level\" = :level " +
								"  and \"leftBound\" >= :leftBound " +
								"  and \"rightBound\" <= :rightBound " +
								"order by \"order\" asc",
						params,
						new HierarchyItemRowMapper()
				);
	}

	@Override
	public List<HierarchyItem> getAllChildrenItems(HierarchyItem parent) {
		return namedParameterJdbcTemplate
				.query(
						"select * from T_MPTT_ITEM " +
								"where \"hierarchyCode\" = :hierarchyCode " +
								"  and \"level\" > :level " +
								"  and \"leftBound\" >= :leftBound " +
								"  and \"rightBound\" <= :rightBound " +
								"order by \"level\" asc, \"order\" asc",
						new BeanPropertySqlParameterSource(parent),
						new HierarchyItemRowMapper()
				);
	}

	@Override
	public List<HierarchyItem> getLeafItems(HierarchyItem parent) {
		return namedParameterJdbcTemplate
				.query(
						"select * from T_MPTT_ITEM " +
								"where\"hierarchyCode\" = :hierarchyCode " +
								"  and\"level\" > :level " +
								"  and\"leftBound\" >= :leftBound " +
								"  and\"rightBound\" <= :rightBound " +
								"  and\"numberOfChildren\" = 0 " +
								"order by \"level\" asc, \"order\" asc",
						new BeanPropertySqlParameterSource(parent),
						new HierarchyItemRowMapper()
				);
	}

	@Override
	public List<HierarchyItem> getLeafItems(String hierarchyCode) {
		return namedParameterJdbcTemplate
				.query(
						"select * from T_MPTT_ITEM " +
								"where \"hierarchyCode\" = :hierarchyCode " +
								"  and \"numberOfChildren\" = 0 " +
								"order by \"leftBound\" asc, \"rightBound\" asc",
						Collections.singletonMap("hierarchyCode", hierarchyCode),
						new HierarchyItemRowMapper()
				);
	}

	@Override
	public SectionWithBucket getFirstEmptySection(String hierarchyCode, long sectionSize, short maxCount) {
		final HashMap<String, Object> params = new HashMap<>();
		params.put("hierarchyCode", hierarchyCode);
		params.put("sectionSize", sectionSize);

		final Short childrenCount = namedParameterJdbcTemplate.queryForObject(
				"select count(0) from T_MPTT_ITEM where \"hierarchyCode\" = :hierarchyCode and \"level\" = 1",
				params,
				Short.class
		);

		if (childrenCount >= maxCount) {
			return null;
		} else if (childrenCount == 0) {
			return new SectionWithBucket(1L, sectionSize, (short) 1);
		} else {
			try {
				return namedParameterJdbcTemplate
						.queryForObject(
								"select * from (select t1.\"leftBound\" - :sectionSize as \"leftBound\", t1.\"leftBound\" - 1 as \"rightBound\", t1.\"bucket\" - 1 as \"bucket\" " +
										"from T_MPTT_ITEM t1 " +
										"left join T_MPTT_ITEM t2 on t2.\"leftBound\" = t1.\"leftBound\" - :sectionSize and t2.\"hierarchyCode\" = t1.\"hierarchyCode\" and t2.\"level\" = 1 " +
										"where t1.\"hierarchyCode\" = :hierarchyCode " +
										"  and t1.\"level\" = 1 " +
										"  and t1.\"leftBound\" - :sectionSize > 0 " +
										"  and t2.\"leftBound\" is null " +
										" order by t1.\"leftBound\" asc " +
										") where rownum <= 1",
								params,
								new SectionRowMapper()
						);
			} catch (EmptyResultDataAccessException ex) {
				return new SectionWithBucket(
						childrenCount * sectionSize + 1,
						(childrenCount + 1) * sectionSize,
						(short) (childrenCount + 1)
				);
			}
		}
	}

	@Override
	public SectionWithBucket getFirstEmptySection(String hierarchyCode, long sectionSize, short maxCount, HierarchyItem parent) {
		final HashMap<String, Object> params = new HashMap<>();
		params.put("hierarchyCode", hierarchyCode);
		params.put("sectionSize", sectionSize);
		params.put("level", parent.getLevel() + 1);
		params.put("parentCode", parent.getCode());
		params.put("parentLeftBound", parent.getLeftBound());
		params.put("parentRightBound", parent.getRightBound());

		final Short childrenCount = namedParameterJdbcTemplate.queryForObject(
				"select count(0) " +
						"from T_MPTT_ITEM " +
						"where \"hierarchyCode\" = :hierarchyCode " +
						"  and \"level\" = :level " +
						"  and \"leftBound\" >= :parentLeftBound" +
						"  and \"rightBound\" <= :parentRightBound",
				params,
				Short.class
		);

		if (childrenCount >= maxCount) {
			return null;
		} else if (childrenCount == 0) {
			return new SectionWithBucket(parent.getLeftBound() + 1, parent.getLeftBound() + sectionSize, (short) 1);
		} else {
			try {
				return namedParameterJdbcTemplate
						.queryForObject(
								"select * from (select t1.\"leftBound\" - :sectionSize as \"leftBound\", t1.\"leftBound\" - 1 as \"rightBound\", t1.\"bucket\" - 1 as \"bucket\" " +
										"from T_MPTT_ITEM t1 " +
										"left join T_MPTT_ITEM t2 on t2.\"leftBound\" = t1.\"leftBound\" - :sectionSize and t2.\"hierarchyCode\" = t1.\"hierarchyCode\" and t2.\"level\" = :level " +
										"where t1.\"hierarchyCode\" = :hierarchyCode " +
										"  and t1.\"level\" = :level " +
										"  and t1.\"leftBound\" - :sectionSize > :parentLeftBound " +
										"  and t1.\"rightBound\" < :parentRightBound " +
										"  and t2.\"leftBound\" is null " +
										" order by t1.\"leftBound\" asc " +
										") where rownum <= 1",
								params,
								new SectionRowMapper()
						);
			} catch (EmptyResultDataAccessException ex) {
				return new SectionWithBucket(
						parent.getLeftBound() + childrenCount * sectionSize + 1,
						parent.getLeftBound() + (childrenCount + 1) * sectionSize,
						(short)(childrenCount + 1)
				);
			}
		}
	}

}
