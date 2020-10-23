package one.edee.oss.pmptt.dao;

import jdk.internal.jline.internal.Nullable;
import lombok.NonNull;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItem;
import one.edee.oss.pmptt.model.SectionWithBucket;
import one.edee.oss.pmptt.spi.HierarchyChangeListener;

import java.util.List;

/**
 * Interface that has to be implemented in order to {@link Hierarchy} and PMPTT algorithm can work.
 * Storage provides read/write operations upon tree structure model. Interface doesn't represent public interface of the
 * PMPTT - use {@link one.edee.oss.pmptt.PMPTT} or {@link Hierarchy} classes instead.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public interface HierarchyStorage {

	/**
	 * Registers new {@link HierarchyChangeListener} that will be called in case any {@link HierarchyItem} is modified.
	 *
	 * @param listener implementation that will be called back in observed situations
	 */
	void registerChangeListener(HierarchyChangeListener listener);

	/**
	 * Created new MPTT hierarchy with unique code and configuration.
	 *
	 * @param hierarchy definition
	 */
	void createHierarchy(Hierarchy hierarchy);

	/**
	 * Returns MPTT hierarchy by code. Hierarchy has initialized reference to the storage implementation that was used
	 * to fetch it.
	 *
	 * @param code of the hierarchy
	 * @return looked up hierarchy if found, otherwise NULL
	 */
	@Nullable
	Hierarchy getHierarchy(String code);

	/**
	 * Creates new hierarchy item under the passed parent item. Parent might be null and in such case root item is created.
	 * Upon creation {@link HierarchyChangeListener} is called.
	 *
	 * @param newItem to be added under parent item in hierarchy
	 * @param parent item of the newly created item
	 */
	void createItem(HierarchyItem newItem, HierarchyItem parent);

	/**
	 * Updated existing hierarchy item.
	 * Upon update {@link HierarchyChangeListener} is called.
	 *
	 * @param updatedItem item that should be updated
	 */
	void updateItem(HierarchyItem updatedItem);

	/**
	 * Removes existing hierarchy item.
	 * Upon removal {@link HierarchyChangeListener} is called.
	 *
	 * @param removedItem item that should be removed
	 */
	void removeItem(HierarchyItem removedItem);

	/**
	 * Returns existing item in hierarchy by its code.
	 *
	 * @param hierarchyCode to look up proper hierarchy
	 * @param code to lookup proper item in the hierarchy
	 * @return
	 */
	@Nullable
	HierarchyItem getItem(String hierarchyCode, String code);

	/**
	 * Returns existing item that is parent item for the passed pivot item.
	 *
	 * @param pivot item whose parent is looked up for
	 * @return immediate parent of the item or NULL if item is root item
	 */
	@Nullable
	HierarchyItem getParentItem(HierarchyItem pivot);

	/**
	 * Returns all parent items for passed pivot item from root to the pivot (pivot item is excluded).
	 *
	 * @param pivot item whose parents is looked up for
	 * @return all parents of the item or empty collection
	 */
	@NonNull
	List<HierarchyItem> getParentsOfItem(HierarchyItem pivot);

	/**
	 * Returns collection of all root items of passed hierarchy.
	 *
	 * @param hierarchyCode code of the hierarchy to look up
	 * @return all root items of the hierarchy or empty collection
	 */
	@NonNull
	List<HierarchyItem> getRootItems(String hierarchyCode);

	/**
	 * Returns collection of immediate child items of passed parent item in certain hierarchy.
	 *
	 * @param parent item which children should be returned
	 * @return collection of immediate child items of the parent item in the argument or empty collection
	 */
	@NonNull
	List<HierarchyItem> getChildItems(HierarchyItem parent);

	/**
	 * Returns collection of all child items (deep wise) of passed parent item in certain hierarchy.
	 *
	 * @param parent item which children should be returned
	 * @return collection of all child items of the parent item in the argument or empty collection
	 */
	@NonNull
	List<HierarchyItem> getAllChildrenItems(HierarchyItem parent);

	/**
	 * Returns collection of all child items (deep wise) that contain no children themselves of passed parent item
	 * in certain hierarchy.
	 *
	 * @param parent item which children should be returned
	 * @return collection of all child items that have no children of the parent item in the argument or empty collection
	 */
	@NonNull
	List<HierarchyItem> getLeafItems(HierarchyItem parent);

	/**
	 * Returns collection of all child items (deep wise) that contain no children themselves in certain hierarchy.
	 *
	 * @param hierarchyCode code of the hierarchy to look up for
	 * @return collection of all child items that have no children of the hierarchy in the argument or empty collection
	 */
	@NonNull
	List<HierarchyItem> getLeafItems(String hierarchyCode);

	/**
	 * Returns information of first empty section on root level considering size of the section and maximum items in
	 * root level.
	 *
	 * @param hierarchyCode code of the hierarchy to look up for
	 * @param sectionSize size of the section on particular level (number of nodes in the section)
	 * @param maxCount size of the section (number of nodes in the section)
	 * @return information about first empty bucket in the section
	 */
	@NonNull
	SectionWithBucket getFirstEmptySection(String hierarchyCode, long sectionSize, short maxCount);

	/**
	 * Returns information of first empty sub-section on level of the parent item in section of the parent item considering
	 * size of the child sub-section and maximum allowed children in parent.
	 *
	 * @param hierarchyCode code of the hierarchy to look up for
	 * @param sectionSize size of the section on particular level (number of nodes in the section)
	 * @param maxCount size of the section (number of nodes in the section)
	 * @param parent item that will become the parent of the section bucket
	 * @return information about first empty bucket in the section
	 */
	SectionWithBucket getFirstEmptySection(String hierarchyCode, long sectionSize, short maxCount, HierarchyItem parent);

}
