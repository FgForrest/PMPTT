package one.edee.oss.pmptt.model;

/**
 * Represents a single node in the {@link Hierarchy}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public interface HierarchyItem {

	/**
	 * Unique code of the hierarchy.
	 * @return unique code of the hierarchy
	 */
	String getHierarchyCode();

	/**
	 * Unique code of the node in the hierarchy.
	 * @return unique code of the item
	 */
	String getCode();

	/**
	 * Level of nesting of the node in the hierarchy.
	 * Root level is level = 1, additional layers have level = level + 1
	 *
	 * @return level of the item
	 */
	Short getLevel();
	void setLevel(Short level);

	/**
	 * Left bound of the item {@link Section}. All child nodes have its leftBound &gt;= parent.leftBound.
	 *
	 * @return left bound of the item
	 */
	Long getLeftBound();
	void setLeftBound(Long leftBound);

	/**
	 * Right bound of the item {@link Section}. All child nodes have its rightBound &lt;= parent.rightBound.
	 * @return right bound of the item
	 */
	Long getRightBound();
	void setRightBound(Long rightBound);

	/**
	 * Count of the immediate children of the node.
	 * @return number of immediate children of this item, if zero item is leaf
	 */
	Short getNumberOfChildren();
	void setNumberOfChildren(Short numberOfChildren);

	/**
	 * Order of the node among neighbouring items on the same level.
	 * Order = 1 means first node, order = parent.numberOfChildren means last node. Order increases by one between
	 * neighbouring nodes.
	 *
	 * @return order of the node among siblings
	 */
	Short getOrder();
	void setOrder(Short order);

	/**
	 * Order of the bucket occupied by this node under the parent node.
	 * Bucket = 1 means first bucket. Two nodes under the same parent must have different buckets. Bucket is usually
	 * different from order because gaps after removed nodes are filled by new nodes and buckets are reused.
	 *
	 * @return number of occupied bucket in the section
	 */
	Short getBucket();
	void setBucket(Short bucket);

}
