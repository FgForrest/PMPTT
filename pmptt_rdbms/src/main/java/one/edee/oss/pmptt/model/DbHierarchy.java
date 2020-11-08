package one.edee.oss.pmptt.model;

import lombok.NonNull;
import one.edee.oss.pmptt.dao.DbHierarchyStorage;
import one.edee.oss.pmptt.dao.HierarchyStorage;
import one.edee.oss.pmptt.exception.MaxLevelExceeded;
import one.edee.oss.pmptt.exception.PivotHierarchyNodeNotFound;
import one.edee.oss.pmptt.exception.SectionExhausted;
import one.edee.oss.pmptt.util.Assert;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
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

	@Override
	public HierarchyItem createRootItem(@NonNull String externalId) throws SectionExhausted {
		return txTemplate.execute(transactionStatus -> super.createRootItem(externalId));
	}

	@Override
	public HierarchyItem createRootItem(@NonNull String externalId, String before) throws PivotHierarchyNodeNotFound, SectionExhausted {
		return txTemplate.execute(transactionStatus -> super.createRootItem(externalId, before));
	}

	@Override
	public HierarchyItem createItem(@NonNull String externalId, @NonNull String withParent) throws PivotHierarchyNodeNotFound, SectionExhausted, MaxLevelExceeded {
		return txTemplate.execute(transactionStatus -> super.createItem(externalId, withParent));
	}
	
	@Override
	public HierarchyItem createItem(@NonNull String externalId, @NonNull String withParent, String before) throws PivotHierarchyNodeNotFound, SectionExhausted, MaxLevelExceeded {
		return txTemplate.execute(transactionStatus -> super.createItem(externalId, withParent, before));
	}

	@Override
	public void removeItem(@NonNull String externalId) throws PivotHierarchyNodeNotFound {
		txTemplate.execute(transactionStatus -> {
			super.removeItem(externalId);
			return null;
		});
	}
	
	@Override
	public void moveItemBetweenLevelsBefore(@NonNull String externalId, @NonNull String withParent, @NonNull String before) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsBefore(externalId, withParent, before);
			return null;
		});
	}
	
	@Override
	public void moveItemBetweenLevelsBefore(@NonNull String externalId, @NonNull String before) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsBefore(externalId, before);
			return null;
		});
	}

	@Override
	public void moveItemBetweenLevelsAfter(@NonNull String externalId, @NonNull String withParent, @NonNull String after) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsAfter(externalId, withParent, after);
			return null;
		});
	}

	@Override
	public void moveItemBetweenLevelsAfter(@NonNull String externalId, @NonNull String after) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsAfter(externalId, after);
			return null;
		});
	}

	@Override
	public void moveItemBetweenLevelsFirst(@NonNull String externalId) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsFirst(externalId);
			return null;
		});
	}

	@Override
	public void moveItemBetweenLevelsFirst(@NonNull String externalId, @NonNull String withParent) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsFirst(externalId, withParent);
			return null;
		});
	}
	
	@Override
	public void moveItemBetweenLevelsLast(@NonNull String externalId, @NonNull String withParent) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsLast(externalId, withParent);
			return null;
		});
	}
	
	@Override
	public void moveItemBetweenLevelsLast(@NonNull String externalId) {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBetweenLevelsLast(externalId);
			return null;
		});
	}

	@Override
	public void moveItemBefore(@NonNull String externalId, @NonNull String before) throws PivotHierarchyNodeNotFound {
		txTemplate.execute(transactionStatus -> {
			super.moveItemBefore(externalId, before);
			return null;
		});
	}
	
	@Override
	public void moveItemAfter(@NonNull String externalId, @NonNull String after) throws PivotHierarchyNodeNotFound {
		txTemplate.execute(transactionStatus -> {
			super.moveItemAfter(externalId, after);
			return null;
		});
	}
	
	@Override
	public void moveItemToFirst(@NonNull String externalId) throws PivotHierarchyNodeNotFound {
		txTemplate.execute(transactionStatus -> {
			super.moveItemToFirst(externalId);
			return null;
		});
	}
	
	@Override
	public void moveItemToLast(@NonNull String externalId) throws PivotHierarchyNodeNotFound {
		txTemplate.execute(transactionStatus -> {
			super.moveItemToLast(externalId);
			return null;
		});
	}

}
