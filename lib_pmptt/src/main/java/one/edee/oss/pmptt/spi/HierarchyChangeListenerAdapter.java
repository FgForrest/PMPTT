package one.edee.oss.pmptt.spi;

import one.edee.oss.pmptt.model.HierarchyItem;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
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
