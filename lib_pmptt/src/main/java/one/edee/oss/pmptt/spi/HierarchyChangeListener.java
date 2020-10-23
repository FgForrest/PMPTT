package one.edee.oss.pmptt.spi;

import one.edee.oss.pmptt.model.HierarchyItem;

/**
 * Listener interface defines callback methods that would be called in case specific WRITE operation is executed
 * in the {@link one.edee.oss.pmptt.model.Hierarchy}. In case single operation internally translates to multiple node
 * updates several callbacks will occur inside single transaction.
 *
 * Implementation of the listener interface usually execute synchronization logic with data (bounds, order) stored
 * in external systems. This form of de-normalization provides performance gains in database implementations.
 *
 * Raising an exception within listener will stop executed operation and rolls back any changes (depends on the type
 * of {@link one.edee.oss.pmptt.dao.HierarchyStorage} - memory implementation doesn't support rollback process).
 *
 * Extending {@link HierarchyChangeListenerAdapter} is recommended if you need to listen to only single operation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public interface HierarchyChangeListener {

	/**
	 * Method is called when new {@link HierarchyItem} become attached to the {@link one.edee.oss.pmptt.model.Hierarchy}.
	 * @param createdItem hierarchy item attached to the hierarchy.
	 */
	void itemCreated(HierarchyItem createdItem);

	/**
	 * Method is called when new {@link HierarchyItem} is updated in the {@link one.edee.oss.pmptt.model.Hierarchy}.
	 * @param updatedItem hierarchy item that was updated
	 * @param originalItem original contents of the same hierarchy item BEFORE operation was executed
	 */
	void itemUpdated(HierarchyItem updatedItem, HierarchyItem originalItem);

	/**
	 * Method is called when new {@link HierarchyItem} is remoed from the {@link one.edee.oss.pmptt.model.Hierarchy}.
	 * @param removeItem hierarchy item removed from the hierarchy
	 */
	void itemRemoved(HierarchyItem removeItem);

}
