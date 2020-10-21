package one.edee.oss.pmptt.dao;

import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItem;
import one.edee.oss.pmptt.model.SectionWithBucket;
import one.edee.oss.pmptt.spi.HierarchyChangeListener;

import java.util.List;

/**
 * Interface that has to be implemented in order to {@link Hierarchy} and PMPTT algorithm can work.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public interface HierarchyStorage {

	/**
	 * Registers new {@link HierarchyChangeListener} that will be called in case any {@link HierarchyItem} is modified.
	 * @param listener
	 */
	void registerChangeListener(HierarchyChangeListener listener);

	/**
	 * Created new MPTT hierarchy with unique code and configuration.
	 * @param hierarchy
	 */
	void createHierarchy(Hierarchy hierarchy);

	/**
	 * Returns MPTT hierarchy by code. Hierarchy has initialized reference to the storage implementation that was used
	 * to fetch it.
	 * @param code
	 * @return
	 */
	Hierarchy getHierarchy(String code);

	/**
	 * Creates new hierarchy item under the passed parent item. Parent might be null and in such case root item is created.
	 * Upon creation {@link HierarchyChangeListener} is called.
	 *
	 * @param newItem
	 * @param parent
	 */
	void createItem(HierarchyItem newItem, HierarchyItem parent);

	/**
	 * Updated existing hierarchy item.
	 * Upon update {@link HierarchyChangeListener} is called.
	 * @param updatedItem
	 */
	void updateItem(HierarchyItem updatedItem);

	/**
	 * Removes existing hierarchy item.
	 * Upon removal {@link HierarchyChangeListener} is called.
	 * @param removedItem
	 */
	void removeItem(HierarchyItem removedItem);

	/**
	 * Returns existing item in hierarchy by its code.
	 * @param hierarchyCode
	 * @param code
	 * @return
	 */
	HierarchyItem getItem(String hierarchyCode, String code);

	/**
	 * Returns existing item that is parent item for the passed pivot item.
	 * @param pivot
	 * @return
	 */
	HierarchyItem getParentItem(HierarchyItem pivot);

	/**
	 * Returns all parent items for passed pivot item from root to the pivot (pivot item is excluded).
	 * @param pivot
	 * @return
	 */
	List<HierarchyItem> getParentsOfItem(HierarchyItem pivot);

	/**
	 * Returns collection of all root items of passed hierarchy.
	 * @param hierarchyCode
	 * @return
	 */
	List<HierarchyItem> getRootItems(String hierarchyCode);

	/**
	 * Returns collection of immediate child items of passed parent item in certain hierarchy.
	 * @param parent
	 * @return
	 */
	List<HierarchyItem> getChildItems(HierarchyItem parent);

	/**
	 * Returns collection of all child items (deep wise) of passed parent item in certain hierarchy.
	 * @param parent
	 * @return
	 */
	List<HierarchyItem> getAllChildrenItems(HierarchyItem parent);

	/**
	 * Returns collection of all child items (deep wise) that contain no children themselves of passed parent item
	 * in certain hierarchy.
	 * @param parent
	 * @return
	 */
	List<HierarchyItem> getLeafItems(HierarchyItem parent);

	/**
	 * Returns collection of all child items (deep wise) that contain no children themselves in certain hierarchy.
	 * @param hierarchyCode
	 * @return
	 */
	List<HierarchyItem> getLeafItems(String hierarchyCode);

	/**
	 * Returns information of first empty section on root level considering size of the section and maximum items in
	 * root level.
	 *
	 * @param hierarchyCode
	 * @param sectionSize
	 * @param maxCount
	 * @return
	 */
	SectionWithBucket getFirstEmptySection(String hierarchyCode, long sectionSize, short maxCount);

	/**
	 * Returns information of first empty sub-section on level of the parent item in section of the parent item considering
	 * size of the child sub-section and maximum allowed children in parent.
	 *
	 * @param hierarchyCode
	 * @param sectionSize
	 * @param maxCount
	 * @param parent
	 * @return
	 */
	SectionWithBucket getFirstEmptySection(String hierarchyCode, long sectionSize, short maxCount, HierarchyItem parent);

}
