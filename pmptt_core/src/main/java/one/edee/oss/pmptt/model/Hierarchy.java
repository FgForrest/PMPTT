package one.edee.oss.pmptt.model;

import lombok.Data;
import lombok.Setter;
import one.edee.oss.pmptt.dao.HierarchyStorage;
import one.edee.oss.pmptt.exception.MaxLevelExceeded;
import one.edee.oss.pmptt.exception.NumericTypeExceeded;
import one.edee.oss.pmptt.exception.PivotHierarchyNodeNotFound;
import one.edee.oss.pmptt.exception.SectionExhausted;
import one.edee.oss.pmptt.util.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Central point of the MPTT algorithm. Create and use instance of the {@link Hierarchy} to perform all operations
 * connected with hierarchical tree.
 *
 * Hierarchy is not Serializable because it contains reference to the {@link HierarchyStorage} instance, but is quite
 * cheap to instantiate (only single row is fetched from the database).
 *
 * Hierarchy contains two main configuration properties - levels and sectionSize which cannot be changed once hierarchy
 * is created.
 *
 * Hierarchy represents implementation of following algorithm:
 *
 * Precalculated modified preorder tree traversal
 * ----------------------------------------------
 *
 * MPTT it quite old and very clever way for transposing hierarchical - tree structure to a two-dimensional representation
 * that is suitable for relational databases. MPTT allows to translate information about node tree position into two numbers
 * left and right bound. Two number fields can be easily indexed and look ups for records in the tree can take advantage of
 * database indexed and perform really quickly.
 *
 * I won't go in a details of the (well) documented MPTT algorithm itself, because there are a lot of sources where you can
 * go for comprehensible explanation (better than I would be able to provide). If you are not familiar with the algorithm,
 * please, go first and read these articles:
 *
 * <a href="https://gist.github.com/tmilos/f2f999b5839e2d42d751">Modified Preorder Tree Traversal</a>
 * <a href="https://www.caktusgroup.com/blog/2016/01/04/modified-preorder-tree-traversal-django/https://www.caktusgroup.com/blog/2016/01/04/modified-preorder-tree-traversal-django/">Modified Preorder Tree Traversal in Django</a>
 * <a href="https://www.zdrojak.cz/clanky/ukladame-hierarchicka-data-v-databazi-iii/">Storing hierarchical data in relational database (Czech version)</a>
 *
 * We're often working with hierarchical data in my company and so we really wanted to have this gear
 * implemented as optimally as possible. Original MPTT algorithm has one big drawback and that is costly updates when you move
 * nodes in the tree, insert new and remove old nodes. Simply said - MPTT has heavy writes and light reads.
 *
 * In our e-commerce solution we work with loads of data that are accessed primarily in a hierarchical
 * fashion. When you browse your online store you usually want to see a collection of products in certain category and all its
 * sub categories. Even if it seems as a simple operation it might translate to really complex query that might target millions
 * of records in the database. Wholesalers have complex price policies and can have for single item dozens of different prices,
 * dozens attributes that describe the products, dozens of product shapes and looks and cartesian product might grow to tens
 * of millions records. So you really want use any filtering constraint that would keep the combinations at sane levels. Using
 * MPTT bounds seems a good way to use, because that's the way user looks at the data.
 *
 * To be able to take advantage of the numeric (left/right bound) index we need to copy the bounds to all products, but also
 * other records (such as prices) that we want to finally join in a single big cartesian product (even noSQL solution would
 * benefit from indexed MPTT bounds, so there is no need to think about MPTT only in connection with relational database).
 * If we would be content with original MPTT it would lead to regular update of thousands of records in case of simple write
 * operations with the tree. So we invested some time to come up with some improvements that would tackle the heavy writes.
 *
 * We decided to limit ourselves with a few constraints that would allow us to use much more efficient form of MPTT. Let's
 * say that we can constraint ourselves with maximum level count (depth of the tree) and maximum count of children nodes of
 * each parent node. That would mean that we'll be able to compute overall numeric interval that would be required to place
 * any of the new node in advance.
 *
 * Also let's keep all these attributes per node:
 *
 * <strong>mutable attributes</strong>
 *
 * - numberOfChildren (count of intermittent children of the node)
 * - order (allows to sort child nodes in the scope of parent node)
 *
 * <strong>immutable attributes</strong>
 *
 * - level (depth of the node)
 * - left (left MPTT bound - as in original algorithm)
 * - right (right MPTT bound - as in original algorithm)
 *
 * Immutable properties are assigned at the moment of the node creation and must not be changed afterwards. As you can see
 * left and right bound are among them and that's crucial for our indexed data that are counted in millions.
 * We can say that if we prepare the tree for 10 levels and 55 children per node we would need 55^11 number interval to
 * cover the top level nodes (and that's slightly lower than 2^64 that's often represents big integers in database engines).
 * But let's use more comprehensible numbers as examples - if we are content with 3 levels and 2 children per node, we'd
 * need only span of 0 - 29. Tree leveling would look like this:
 *
 *                  0-29
 *       1-14        |         15-28
 *   2-7  |   8-13   |   16-21   |   22-27
 * 3-4|5-6|9-10|11-12|17-18|19-20|23-24|25-26
 *
 * Look at this tree as a honeycomb that is empty at the beginning and will gradually fill up as we add the contents.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@Data
public class Hierarchy {
	/**
	 * Unique code of the hierarchy
	 */
	private final String code;
	/**
	 * Maximal number of nested levels in this hierarchy.
	 */
	private final short levels;
	/**
	 * Maximal number of children in each section of the tree (ie. maximal numbers of children in each node).
	 */
	private final short sectionSize;
	/**
	 * Storage implementation.
	 */
	@Setter private HierarchyStorage storage;

	/**
	 * Hierarchy constructor.
	 *
	 * @param code unique hierarchy code
	 * @param levels maximum levels preallocated in the hierarchy
	 * @param sectionSize maximum nodes in section preallocated in the hierarchy.
	 * @throws NumericTypeExceeded if combination size of the maximum nodes boundaries held in hierarchy exceeds {@link Long}
	 */
	public Hierarchy(String code, short levels, short sectionSize) throws NumericTypeExceeded {
		this.code = code;
		this.levels = (short)(levels + 1);
		this.sectionSize = (short)(sectionSize + 1);
		// check long overflow for first level
		try {
			Section.getSectionSizeForLevel(this.sectionSize, (short) 1, this.levels);
		} catch (ArithmeticException ex) {
			throw new NumericTypeExceeded(this.levels, this.sectionSize);
		}
	}

	/**
	 * Creates new item on the root level of the hierarchy and places it as last item of the root level.
	 * 
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @return created hierarchy item
	 * @throws SectionExhausted if there is no room for another item in the section
	 */
	@Nonnull
	public HierarchyItem createRootItem(@Nonnull String externalId) throws SectionExhausted {
		final HierarchyItem newItem = createRootItemInternal(externalId);
		newItem.setOrder((short)(storage.getRootItems(code).size() + 1));
		storage.createItem(newItem, null);
		return newItem;
	}

	/**
	 * Creates new item on the root level of the hierarchy and places it at the position before the requested item.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @param before code of the sibling item that would follow newly created item code of another root item, might be null and then the new item is placed as the last item
	 * @return created hierarchy item
	 * @throws PivotHierarchyNodeNotFound if beforeItem is not found in entire hierarchy
	 * @throws SectionExhausted if there is no room for another item in the section
	 */
	@Nonnull
	public HierarchyItem createRootItem(@Nonnull String externalId, String before) throws PivotHierarchyNodeNotFound, SectionExhausted {
		final HierarchyItem newItem = createRootItemInternal(externalId);
		final HierarchyItem beforeItem = getHierarchyItemWithNullabilityCheck(before, "used as pivot");
		final List<HierarchyItem> rootItems = getRootItems();
		assertItemIsPartOf(beforeItem, rootItems);
		newItem.setOrder(beforeItem.getOrder());
		storage.createItem(newItem, null);
		moveAllItemsRight(beforeItem, rootItems);
		return newItem;
	}

	/**
	 * Creates new item on the level below parent node level and places it as last item of the parent children level.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @param withParent code of the other item in the hierarchy that would become parent item of the newly created item
	 * @return created hierarchy item
	 * @throws PivotHierarchyNodeNotFound if beforeItem is not found in entire hierarchy
	 * @throws SectionExhausted if there is no room for another item in the section
	 * @throws MaxLevelExceeded if the level is too deep for hierarchy configuration
	 */
	@Nonnull
	public HierarchyItem createItem(@Nonnull String externalId, @Nonnull String withParent) throws PivotHierarchyNodeNotFound, SectionExhausted, MaxLevelExceeded {
		final HierarchyItem parentItem = getHierarchyItemWithNullabilityCheck(withParent, "used as parent");
		if (parentItem.getLevel() + 1 > levels - 1) {
			throw new MaxLevelExceeded(
					"Cannot add item on level " + (parentItem.getLevel() + 1) + "! Maximum allowed levels is " + (levels - 1) + ".",
					(short) (parentItem.getLevel() + 1),
					(short) (levels - 1)
			);
		}
		final HierarchyItem newItem = createNewItemUnder(externalId, parentItem);
		final short newChildrenCount = (short) (parentItem.getNumberOfChildren() + 1);
		newItem.setOrder(newChildrenCount);
		storage.createItem(newItem, parentItem);

		parentItem.setNumberOfChildren(newChildrenCount);
		storage.updateItem(parentItem);

		return newItem;
	}

	/**
	 * Creates new item on the level below parent node level and places it at the position before the requested item
	 * of the parent children level.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @param withParent code of the other item in the hierarchy that would become parent item of the newly created item
	 * @param before code of the sibling item that would follow newly created item code of another root item, might be null and then the new item is placed as the last item
	 * @return created hierarchy item
	 * @throws PivotHierarchyNodeNotFound if before is not found in entire hierarchy
	 * @throws SectionExhausted if there is no room for another item in the section
	 * @throws MaxLevelExceeded if the level is too deep for hierarchy configuration
	 */
	@Nonnull
	public HierarchyItem createItem(@Nonnull String externalId, @Nonnull String withParent, String before) throws PivotHierarchyNodeNotFound, SectionExhausted, MaxLevelExceeded {
		final HierarchyItem parentItem = getHierarchyItemWithNullabilityCheck(withParent, "used as parent");
		if (parentItem.getLevel() + 1 > levels - 1) {
			throw new MaxLevelExceeded(
				"Cannot add item on level " + (parentItem.getLevel() + 1) + "! Maximum allowed levels is " + (levels - 1) + ".",
				(short) (parentItem.getLevel() + 1),
				(short) (levels - 1)
			);
		}
		final HierarchyItem beforeItem = getHierarchyItemWithNullabilityCheck(before, "used as pivot");
		final List<HierarchyItem> children = storage.getChildItems(parentItem);
		assertItemIsPartOf(beforeItem, children);

		final HierarchyItem newItem = createNewItemUnder(externalId, parentItem);
		newItem.setOrder(beforeItem.getOrder());
		storage.createItem(newItem, parentItem);

		parentItem.setNumberOfChildren((short)(parentItem.getNumberOfChildren() + 1));
		storage.updateItem(parentItem);

		moveAllItemsRight(beforeItem, children);

		return newItem;
	}

	/**
	 * Removes existing node item on any level.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @throws PivotHierarchyNodeNotFound if externalId item is not found in entire hierarchy
	 */
	public void removeItem(@Nonnull String externalId) throws PivotHierarchyNodeNotFound {
		final HierarchyItem removedItem = getHierarchyItemWithNullabilityCheck(externalId, "removed");
		final HierarchyItem parentItem = storage.getParentItem(removedItem);
		for (HierarchyItem itemToRemove : storage.getAllChildrenItems(removedItem)) {
			storage.removeItem(itemToRemove);
		}

		if (parentItem != null) {
			parentItem.setNumberOfChildren((short)(parentItem.getNumberOfChildren() - 1));
			storage.updateItem(parentItem);
		}

		moveNeighboursLeft(removedItem, parentItem);
		storage.removeItem(removedItem);
	}

	/**
	 * Moves item to different level before the requested item. This operation is quite costly and affects all inner
	 * levels of moved item.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @param withParent code of the other item in the hierarchy that would become parent item of the newly created item
	 * @param before code of the sibling item that would follow newly created item
	 */
	public void moveItemBetweenLevelsBefore(@Nonnull String externalId, @Nonnull String withParent, @Nonnull String before) {
		final HierarchyItem movedItem = getHierarchyItemWithNullabilityCheck(externalId, "moved");
		final HierarchyItem beforeItem = getHierarchyItemWithNullabilityCheck(before, "used as pivot");
		final HierarchyItem parentItem = getHierarchyItemWithNullabilityCheck(withParent, "used as new parent");

		moveItemBetweenLevels(movedItem, parentItem, (item, neighbours) -> insertIntoNeighboursBefore(item, beforeItem, neighbours));
	}

	/**
	 * Moves item to root level before the requested item. This operation is quite costly and affects all inner
	 * levels of moved item.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @param before code of the sibling item that would follow newly created item
	 */
	public void moveItemBetweenLevelsBefore(@Nonnull String externalId, @Nonnull String before) {
		final HierarchyItem movedItem = getHierarchyItemWithNullabilityCheck(externalId, "moved");
		final HierarchyItem beforeItem = getHierarchyItemWithNullabilityCheck(before, "used as pivot");

		moveItemBetweenLevels(movedItem, null, (item, neighbours) -> insertIntoNeighboursBefore(item, beforeItem, neighbours));
	}

	/**
	 * Moves item to different level after the requested item. This operation is quite costly and affects all inner
	 * levels of moved item.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @param withParent code of the other item in the hierarchy that would become parent item of the newly created item
	 * @param after code of the sibling item that would precede newly created item
	 */
	public void moveItemBetweenLevelsAfter(@Nonnull String externalId, @Nonnull String withParent, @Nonnull String after) {
		final HierarchyItem movedItem = getHierarchyItemWithNullabilityCheck(externalId, "moved");
		final HierarchyItem afterItem = getHierarchyItemWithNullabilityCheck(after, "used as pivot");
		final HierarchyItem parentItem = getHierarchyItemWithNullabilityCheck(withParent, "used as new parent");

		moveItemBetweenLevels(movedItem, parentItem, (item, neighbours) -> insertIntoNeighboursAfter(item, afterItem, neighbours));
	}

	/**
	 * Moves item to root level after the requested item. This operation is quite costly and affects all inner
	 * levels of moved item.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @param after code of the sibling item that would precede newly created item
	 */
	public void moveItemBetweenLevelsAfter(@Nonnull String externalId, @Nonnull String after) {
		final HierarchyItem movedItem = getHierarchyItemWithNullabilityCheck(externalId, "moved");
		final HierarchyItem beforeItem = getHierarchyItemWithNullabilityCheck(after, "used as pivot");

		moveItemBetweenLevels(movedItem, null, (item, neighbours) -> insertIntoNeighboursAfter(item, beforeItem, neighbours));
	}

	/**
	 * Moves item to root level as a first item. This operation is quite costly and affects all inner
	 * levels of moved item.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 */
	public void moveItemBetweenLevelsFirst(@Nonnull String externalId) {
		final HierarchyItem movedItem = getHierarchyItemWithNullabilityCheck(externalId, "moved");

		moveItemBetweenLevels(movedItem, null, this::insertIntoNeighboursFirst);
	}

	/**
	 * Moves item to different level as a first item. This operation is quite costly and affects all inner
	 * levels of moved item.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @param withParent code of the other item in the hierarchy that would become parent item of the newly created item
	 */
	public void moveItemBetweenLevelsFirst(@Nonnull String externalId, @Nonnull String withParent) {
		final HierarchyItem movedItem = getHierarchyItemWithNullabilityCheck(externalId, "moved");
		final HierarchyItem parentItem = getHierarchyItemWithNullabilityCheck(withParent, "used as new parent");

		moveItemBetweenLevels(movedItem, parentItem, this::insertIntoNeighboursFirst);
	}

	/**
	 * Moves item to different level as a last item. This operation is quite costly and affects all inner
	 * levels of moved item.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @param withParent code of the other item in the hierarchy that would become parent item of the newly created item
	 */
	public void moveItemBetweenLevelsLast(@Nonnull String externalId, @Nonnull String withParent) {
		final HierarchyItem movedItem = getHierarchyItemWithNullabilityCheck(externalId, "moved");
		final HierarchyItem parentItem = getHierarchyItemWithNullabilityCheck(withParent, "used as new parent");

		moveItemBetweenLevels(movedItem, parentItem, this::insertIntoNeighboursLast);
	}

	/**
	 * Moves item to root level as a last item. This operation is quite costly and affects all inner
	 * levels of moved item.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 */
	public void moveItemBetweenLevelsLast(@Nonnull String externalId) {
		final HierarchyItem movedItem = getHierarchyItemWithNullabilityCheck(externalId, "moved");

		moveItemBetweenLevels(movedItem, null, this::insertIntoNeighboursLast);
	}

	/**
	 * Moves item on the same level before the requested item.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @param before code of the sibling item that would follow newly created item
	 * @throws PivotHierarchyNodeNotFound if externalId or beforeItem is not found in entire hierarchy
	 */
	public void moveItemBefore(@Nonnull String externalId, @Nonnull String before) throws PivotHierarchyNodeNotFound {
		final HierarchyItem movedItem = getHierarchyItemWithNullabilityCheck(externalId, "moved");
		final HierarchyItem beforeItem = getHierarchyItemWithNullabilityCheck(before, "used as pivot");
		final HierarchyItem parentItem = storage.getParentItem(movedItem);

		insertIntoNeighboursBefore(movedItem, beforeItem, getNeighbours(parentItem));

		storage.updateItem(movedItem);
	}

	/**
	 * Moves item on the same level after the requested item.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @param after code of the sibling item that would precede newly created item
	 * @throws PivotHierarchyNodeNotFound if externalId or afterItem is not found in entire hierarchy
	 */
	public void moveItemAfter(@Nonnull String externalId, @Nonnull String after) throws PivotHierarchyNodeNotFound {
		final HierarchyItem movedItem = getHierarchyItemWithNullabilityCheck(externalId, "moved");
		final HierarchyItem afterItem = getHierarchyItemWithNullabilityCheck(after, "used as pivot");
		final HierarchyItem parentItem = storage.getParentItem(movedItem);

		final List<HierarchyItem> neighbours = getNeighbours(parentItem);
		insertIntoNeighboursAfter(movedItem, afterItem, neighbours);

		storage.updateItem(movedItem);
	}

	/**
	 * Moves item on the same level as first item of the level.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @throws PivotHierarchyNodeNotFound if externalId is not found in entire hierarchy
	 */
	public void moveItemToFirst(@Nonnull String externalId) throws PivotHierarchyNodeNotFound {
		final HierarchyItem movedItem = getHierarchyItemWithNullabilityCheck(externalId, "moved");
		final HierarchyItem parentItem = storage.getParentItem(movedItem);

		insertIntoNeighboursFirst(movedItem, getNeighbours(parentItem));
	}

	/**
	 * Moves item on the same level as last item of the level.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @throws PivotHierarchyNodeNotFound if externalId is not found in entire hierarchy
	 */
	public void moveItemToLast(@Nonnull String externalId) throws PivotHierarchyNodeNotFound {
		final HierarchyItem movedItem = getHierarchyItemWithNullabilityCheck(externalId, "moved");
		final HierarchyItem parentItem = storage.getParentItem(movedItem);

		insertIntoNeighboursLast(movedItem, getNeighbours(parentItem));
	}

	/**
	 * Returns flat ordered collection of the root level items.
	 *
	 * @return collection of all root items sorted in proper order, empty collection if there is no root node
	 */
	@Nonnull
	public List<HierarchyItem> getRootItems() {
		return storage.getRootItems(code);
	}

	/**
	 * Returns flat ordered collection of the children level items of requested parent item.
	 *
	 * @param withParent code of the other item in the hierarchy that would become parent item of the newly created item
	 * @return collection of all child items sorted in proper order, empty collection if there is no child node
	 * @throws PivotHierarchyNodeNotFound if withParent is not found in entire hierarchy
	 */
	@Nonnull
	public List<HierarchyItem> getChildItems(@Nonnull String withParent) throws PivotHierarchyNodeNotFound {
		final HierarchyItem parentItem = getHierarchyItemWithNullabilityCheck(withParent, "used as parent");
		return storage.getChildItems(parentItem);
	}

	/**
	 * Returns flat ordered collection of all the leaf items in the tree substructure. Leaf item is the item that has
	 * no other children.
	 *
	 * @param withParent code of the other item in the hierarchy that would become parent item of the newly created item - if null is passed all leaf children in entire tree are returned
	 * @return collection of all leaf items (ie. items having no other children) on any level, empty collection if there is no leaf node
	 * @throws PivotHierarchyNodeNotFound if withParent is not found in entire hierarchy
	 */
	@Nonnull
	public List<HierarchyItem> getLeafItems(String withParent) throws PivotHierarchyNodeNotFound {
		if (withParent == null) {
			return storage.getLeafItems(code);
		} else {
			final HierarchyItem parentItem = getHierarchyItemWithNullabilityCheck(withParent, "used as parent");
			return storage.getLeafItems(parentItem);
		}
	}

	/**
	 * Returns single hierarchy item by its external id.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @return item found by its unique code
	 * @throws PivotHierarchyNodeNotFound if externalId is not found in entire hierarchy
	 */
	@Nonnull
	public HierarchyItem getItem(@Nonnull String externalId) throws PivotHierarchyNodeNotFound {
		return getHierarchyItemWithNullabilityCheck(externalId, "retrieved");
	}

	/**
	 * Returns parent item of the requested item.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @return parent item of the item found by its unique code, NULL if pivot item is root item
	 * @throws PivotHierarchyNodeNotFound if externalId is not found in entire hierarchy
	 */
	@Nullable
	public HierarchyItem getParentItem(@Nonnull String externalId) throws PivotHierarchyNodeNotFound {
		final HierarchyItem pivot = getHierarchyItemWithNullabilityCheck(externalId, "used as child pivot");
		return storage.getParentItem(pivot);
	}

	/**
	 * Returns entire parent chain of the requested item.
	 *
	 * @param externalId unique code of the item in the hierarchy (usually business code of some external entity)
	 * @return collection of parent items of the item found by its unique code, emtpy colletion if pivot item is root item
	 * @throws PivotHierarchyNodeNotFound if externalId is not found in entire hierarchy
	 */
	@Nonnull
	public List<HierarchyItem> getParentItems(@Nonnull String externalId) throws PivotHierarchyNodeNotFound {
		final HierarchyItem pivot = getHierarchyItemWithNullabilityCheck(externalId, "used as child pivot");
		return storage.getParentsOfItem(pivot);
	}

	/*
		PRIVATE METHODS
	 */

	private HierarchyItem createRootItemInternal(@Nonnull String externalId) {
		final SectionWithBucket section = computeBounds();
		final HierarchyItem newItem = new HierarchyItemWithHistory(code, externalId, (short) 1, section.getLeftBound(), section.getRightBound(), section.getBucket());
		newItem.setNumberOfChildren((short)0);
		return newItem;
	}

	private SectionWithBucket computeBounds() {
		final long sectionSizeForLevel = Section.getSectionSizeForLevel(sectionSize, (short) 2, levels);
		final SectionWithBucket section = storage.getFirstEmptySection(code, sectionSizeForLevel, sectionSize);
		if (section == null) {
			throw new SectionExhausted(
					"Root level is filled up with items to the maximum. Cannot add another item!",
					(short) (sectionSize - 1)
			);
		}
		Assert.isTrue(section.getLeftBound() >= 0, "Left bound must be greater or equal than zero!");
		Assert.isTrue(section.getRightBound() <= sectionSizeForLevel * sectionSize, "Right bound must be lower or equal to " + sectionSizeForLevel * sectionSize + "!");
		Assert.isTrue(sectionSizeForLevel == section.getRightBound() - section.getLeftBound() + 1, "Section size " + sectionSizeForLevel + " must be equal to " + (section.getRightBound() - section.getLeftBound() + 1) + "!");
		return section;
	}

	private HierarchyItem createNewItemUnder(@Nonnull String externalId, HierarchyItem parentItem) {
		final short targetLevel = (short) (parentItem.getLevel() + 1);
		final SectionWithBucket section = computeBounds(parentItem, targetLevel);
		final HierarchyItem newItem = new HierarchyItemWithHistory(code, externalId, targetLevel, section.getLeftBound(), section.getRightBound(), section.getBucket());
		newItem.setNumberOfChildren((short)0);
		return newItem;
	}

	private void updateMovedItemBoundsUnder(@Nonnull HierarchyItem movedItem, HierarchyItem parentItem) {
		final short targetLevel;
		final SectionWithBucket section;
		if (parentItem == null) {
			targetLevel = (short) 1;
			section = computeBounds();
		} else {
			targetLevel = (short) (parentItem.getLevel() + 1);
			section = computeBounds(parentItem, targetLevel);
		}
		movedItem.setLeftBound(section.getLeftBound());
		movedItem.setRightBound(section.getRightBound());
		movedItem.setBucket(section.getBucket());
		movedItem.setLevel(targetLevel);
	}

	private SectionWithBucket computeBounds(HierarchyItem parentItem, short targetLevel) {
		final long sectionSizeForLevel = Section.getSectionSizeForLevel(sectionSize, (short) (targetLevel + 1), levels);
		final SectionWithBucket section = storage.getFirstEmptySection(
				code,
				sectionSizeForLevel,
				sectionSize,
				parentItem
		);
		if (section == null) {
			throw new SectionExhausted(
					"Children section of item " + parentItem.getCode() + " is filled up with items to the maximum. Cannot add another item!",
					(short) (sectionSize - 1)
			);
		}
		Assert.isTrue(section.getLeftBound() > parentItem.getLeftBound(), "Sub section left bound must be after parent left bound!");
		Assert.isTrue(section.getRightBound() < parentItem.getRightBound(), "Sub section right bound must be before parent right bound!");
		Assert.isTrue(sectionSizeForLevel == section.getRightBound() - section.getLeftBound() + 1, "Section size " + sectionSizeForLevel + " must be equal to " + (section.getRightBound() - section.getLeftBound() + 1) + "!");
		return section;
	}

	private HierarchyItem getHierarchyItemWithNullabilityCheck(@Nonnull String externalId, final String reasonToUse) {
		final HierarchyItem movedItem = storage.getItem(code, externalId);
		if (movedItem == null) {
			throw new PivotHierarchyNodeNotFound(
					"Item to be " + reasonToUse + " with code " + externalId + " not found!",
					externalId
			);
		}
		return movedItem;
	}

	private void assertItemIsPartOf(HierarchyItem item, List<HierarchyItem> items) {
		for (HierarchyItem examinedItem : items) {
			if (Objects.equals(item.getCode(), examinedItem.getCode())) {
				return;
			}
		}
		throw new PivotHierarchyNodeNotFound(
				"Pivot item " + item.getCode() + " is present on different level (" + item.getLevel() + ")!",
				item.getCode()
		);
	}

	private void moveAllItemsRight(HierarchyItem beforeItem, List<HierarchyItem> items) {
		for (HierarchyItem item : items) {
			if (item.getOrder() >= beforeItem.getOrder()) {
				item.setOrder((short)(item.getOrder() + 1));
				storage.updateItem(item);
			}
		}
	}

	private List<HierarchyItem> getNeighbours(HierarchyItem parentItem) {
		return parentItem == null ? storage.getRootItems(code) : storage.getChildItems(parentItem);
	}

	private Map<String, String> createChildToParentIndex(List<HierarchyItem> childItems, HierarchyItem parentItem) {
		final List<HierarchyItem> collectedItems = new ArrayList<>(childItems.size() + 1);
		collectedItems.add(parentItem);
		collectedItems.addAll(childItems);

		final Map<Section, HierarchyItem> sectionIndex = new HashMap<>(collectedItems.size());
		for (HierarchyItem item : collectedItems) {
			sectionIndex.put(new Section(item.getLeftBound(), item.getRightBound()), item);
		}

		final Map<String, String> result = new HashMap<>(collectedItems.size());
		for (HierarchyItem item : collectedItems) {
			if (item != parentItem) {
				final HierarchyItem childParent = sectionIndex.get(Section.computeParentSectionBounds(sectionSize, item));
				Assert.notNull(childParent, "Child parent must be not null!");
				result.put(item.getCode(), childParent.getCode());
			}
		}

		return result;
	}

	private void insertIntoNeighboursBefore(HierarchyItem movedItem, HierarchyItem beforeItem, List<HierarchyItem> neighbours) {
		assertItemIsPartOf(movedItem, neighbours);
		assertItemIsPartOf(beforeItem, neighbours);

		final Short beforeItemOrder = beforeItem.getOrder();
		HierarchyItem beforeItemInList = null;
		for (HierarchyItem child : neighbours) {
			if (beforeItem.getCode().equals(child.getCode())) {
				beforeItemInList = child;
			}
			if (child.getOrder() > movedItem.getOrder() && child.getOrder() < beforeItemOrder) {
				child.setOrder((short) (child.getOrder() - 1));
				storage.updateItem(child);
			}
			if (child.getOrder() < movedItem.getOrder() && child.getOrder() >= beforeItemOrder) {
				child.setOrder((short) (child.getOrder() + 1));
				storage.updateItem(child);
			}
		}

		Assert.notNull(beforeItemInList, "Pivot item must be not null!");
		movedItem.setOrder((short)(beforeItemInList.getOrder() - 1));
	}

	private void insertIntoNeighboursAfter(HierarchyItem movedItem, HierarchyItem afterItem, List<HierarchyItem> neighbours) {
		assertItemIsPartOf(movedItem, neighbours);
		assertItemIsPartOf(afterItem, neighbours);

		final Short afterItemOrder = afterItem.getOrder();
		HierarchyItem afterItemInList = null;
		for (HierarchyItem child : neighbours) {
			if (afterItem.getCode().equals(child.getCode())) {
				afterItemInList = child;
			}
			if (child.getOrder() > movedItem.getOrder() && child.getOrder() <= afterItemOrder) {
				child.setOrder((short) (child.getOrder() - 1));
				storage.updateItem(child);
			}
			if (child.getOrder() < movedItem.getOrder() && child.getOrder() > afterItemOrder) {
				child.setOrder((short) (child.getOrder() + 1));
				storage.updateItem(child);
			}
		}

		Assert.notNull(afterItemInList, "Pivot item must be not null!");
		movedItem.setOrder((short)(afterItemInList.getOrder() + 1));
	}

	private void insertIntoNeighboursFirst(HierarchyItem movedItem, List<HierarchyItem> neighbours) {
		for (HierarchyItem child : neighbours) {
			if (child.getOrder() < movedItem.getOrder()) {
				child.setOrder((short)(child.getOrder() + 1));
				storage.updateItem(child);
			}
		}
		movedItem.setOrder((short)1);
		storage.updateItem(movedItem);
	}

	private void insertIntoNeighboursLast(HierarchyItem movedItem,  List<HierarchyItem> neighbours) {
		for (HierarchyItem child : neighbours) {
			if (child.getOrder() > movedItem.getOrder()) {
				child.setOrder((short)(child.getOrder() - 1));
				storage.updateItem(child);
			}
		}

		movedItem.setOrder((short) neighbours.size());
		storage.updateItem(movedItem);
	}

	private void moveNeighboursLeft(HierarchyItem movedItem, HierarchyItem parentItem) {
		final List<HierarchyItem> neighbours = getNeighbours(parentItem);
		for (HierarchyItem child : neighbours) {
			if (child.getOrder() > movedItem.getOrder()) {
				child.setOrder((short)(child.getOrder() - 1));
				storage.updateItem(child);
			}
		}
	}

	private void moveItemBetweenLevels(HierarchyItem movedItem, HierarchyItem parentItem, PositioningLogic positioningLogic) {
		if (parentItem != null && parentItem.getLevel() + 1 > levels - 1) {
			throw new MaxLevelExceeded(
					"Cannot add item on level " + (parentItem.getLevel() + 1) + "! Maximum allowed levels is " + (levels - 1) + ".",
					(short) (parentItem.getLevel() + 1),
					(short) (levels - 1)
			);
		}

		final List<HierarchyItem> allChildrenItems = storage.getAllChildrenItems(movedItem);
		final Map<String, String> childToParentIndex = createChildToParentIndex(allChildrenItems, movedItem);
		final HierarchyItem movedItemParent = storage.getParentItem(movedItem);
		moveNeighboursLeft(movedItem, movedItemParent);

		if (movedItemParent != null) {
			movedItemParent.setNumberOfChildren((short) (movedItemParent.getNumberOfChildren() - 1));
			storage.updateItem(movedItemParent);
		}

		final List<HierarchyItem> newParentChildren = getNeighbours(parentItem);
		final short newChildrenCount = (short) (newParentChildren.size() + 1);

		if (parentItem != null) {
			parentItem.setNumberOfChildren(newChildrenCount);
			storage.updateItem(parentItem);
		}

		movedItem.setOrder(newChildrenCount);
		final List<HierarchyItem> neighbours = new ArrayList<>(newParentChildren.size() + 1);
		neighbours.addAll(newParentChildren);
		neighbours.add(movedItem);
		positioningLogic.positionItem(movedItem, neighbours);

		updateMovedItemBoundsUnder(movedItem, parentItem);
		storage.updateItem(movedItem);

		for (HierarchyItem movedItemChild : allChildrenItems) {
			final HierarchyItem currentParent = storage.getItem(code, childToParentIndex.get(movedItemChild.getCode()));
			updateMovedItemBoundsUnder(movedItemChild, currentParent);
			storage.updateItem(movedItemChild);
		}
	}

	private interface PositioningLogic {

		void positionItem(HierarchyItem movedItem, List<HierarchyItem> neighbours);

	}
}
