package one.edee.oss.pmptt.spi;

import one.edee.oss.pmptt.model.HierarchyItem;

/**
 * Adapter to the {@link HierarchyChangeListener} implementing all callbacks with no operation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public class HierarchyChangeListenerAdapter implements HierarchyChangeListener {

	@Override
	public void itemCreated(HierarchyItem createdItem) {
		// do nothing, let's descendants override it
	}

	@Override
	public void itemUpdated(HierarchyItem updatedItem, HierarchyItem originalItem) {
		// do nothing, let's descendants override it
	}

	@Override
	public void itemRemoved(HierarchyItem removeItem) {
		// do nothing, let's descendants override it
	}

}
