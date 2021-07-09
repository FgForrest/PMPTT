package one.edee.oss.pmptt.model;

import one.edee.oss.pmptt.dao.DbHierarchyStorage;
import one.edee.oss.pmptt.dao.HierarchyStorage;
import one.edee.oss.pmptt.exception.MaxLevelExceeded;
import one.edee.oss.pmptt.exception.PivotHierarchyNodeNotFound;
import one.edee.oss.pmptt.exception.SectionExhausted;
import one.edee.oss.pmptt.util.Assert;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Nonnull;

/**
 * This implementation stores the hierarchy into the relational database.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
public class DbHierarchy extends Hierarchy {
	private TransactionTemplate txTemplate;

	public DbHierarchy(String code, short levels, short sectionSize) {
		super(code, levels, sectionSize);
	}

	public DbHierarchy(String code, short levels, short sectionSize, DbHierarchyStorage storage) {
		super(code, levels, sectionSize);
		setStorage(storage);
	}

	@Override
	public void setStorage(HierarchyStorage storage) {
		super.setStorage(storage);
		Assert.isTrue(storage instanceof DbHierarchyStorage, "Storage is expected to be of type DbHierarchyStorage.");
		this.txTemplate = new TransactionTemplate(((DbHierarchyStorage)storage).getTransactionManager());
	}

	@Nonnull
	@Override
	public HierarchyItem createRootItem(@Nonnull String externalId) throws SectionExhausted {
		return txTemplate.execute(transactionStatus -> super.createRootItem(externalId));
	}

	@Nonnull
	@Override
	public HierarchyItem createRootItem(@Nonnull String externalId, String before) throws PivotHierarchyNodeNotFound, SectionExhausted {
		return txTemplate.execute(transactionStatus -> super.createRootItem(externalId, before));
	}

	@Nonnull
	@Override
	public HierarchyItem createItem(@Nonnull String externalId, @Nonnull String withParent) throws PivotHierarchyNodeNotFound, SectionExhausted, MaxLevelExceeded {
		return txTemplate.execute(transactionStatus -> super.createItem(externalId, withParent));
	}

	@Nonnull
	@Override
	public HierarchyItem createItem(@Nonnull String externalId, @Nonnull String withParent, String before) throws PivotHierarchyNodeNotFound, SectionExhausted, MaxLevelExceeded {
		return txTemplate.execute(transactionStatus -> super.createItem(externalId, withParent, before));
	}

	@Override
	public void removeItem(@Nonnull String externalId) throws PivotHierarchyNodeNotFound {
		txTemplate.execute(transactionStatus -> {
			super.removeItem(externalId);
			return null;
		});
	}
	
	@Override
	public void moveItemBetweenLevelsBefore(@Nonnull String externalId, @Nonnull String withParent, @Nonnull String before) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsBefore(externalId, withParent, before);
			return null;
		});
	}
	
	@Override
	public void moveItemBetweenLevelsBefore(@Nonnull String externalId, @Nonnull String before) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsBefore(externalId, before);
			return null;
		});
	}

	@Override
	public void moveItemBetweenLevelsAfter(@Nonnull String externalId, @Nonnull String withParent, @Nonnull String after) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsAfter(externalId, withParent, after);
			return null;
		});
	}

	@Override
	public void moveItemBetweenLevelsAfter(@Nonnull String externalId, @Nonnull String after) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsAfter(externalId, after);
			return null;
		});
	}

	@Override
	public void moveItemBetweenLevelsFirst(@Nonnull String externalId) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsFirst(externalId);
			return null;
		});
	}

	@Override
	public void moveItemBetweenLevelsFirst(@Nonnull String externalId, @Nonnull String withParent) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsFirst(externalId, withParent);
			return null;
		});
	}
	
	@Override
	public void moveItemBetweenLevelsLast(@Nonnull String externalId, @Nonnull String withParent) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsLast(externalId, withParent);
			return null;
		});
	}
	
	@Override
	public void moveItemBetweenLevelsLast(@Nonnull String externalId) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsLast(externalId);
			return null;
		});
	}

	@Override
	public void moveItemBefore(@Nonnull String externalId, @Nonnull String before) throws PivotHierarchyNodeNotFound {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBefore(externalId, before);
			return null;
		});
	}
	
	@Override
	public void moveItemAfter(@Nonnull String externalId, @Nonnull String after) throws PivotHierarchyNodeNotFound {
		txTemplate.execute(transactionStatus -> {
			super.moveItemAfter(externalId, after);
			return null;
		});
	}
	
	@Override
	public void moveItemToFirst(@Nonnull String externalId) throws PivotHierarchyNodeNotFound {
		txTemplate.execute(transactionStatus -> {
			super.moveItemToFirst(externalId);
			return null;
		});
	}
	
	@Override
	public void moveItemToLast(@Nonnull String externalId) throws PivotHierarchyNodeNotFound {
		txTemplate.execute(transactionStatus -> {
			super.moveItemToLast(externalId);
			return null;
		});
	}

}
