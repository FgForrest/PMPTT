package cz.fg.oss.pmptt.model;

/**
 * Represents a single node in the {@link Hierarchy}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public interface HierarchyItem {

	/**
	 * Unique code of the hierarchy.
	 * @return
	 */
	String getHierarchyCode();

	/**
	 * Unique code of the node in the hierarchy.
	 * @return
	 */
	String getCode();

	/**
	 * Level of nesting of the node in the hierarchy.
	 * Root level is level = 1, additional layers have level = level + 1
	 * @return
	 */
	Short getLevel();
	void setLevel(Short level);

	/**
	 * Left bound of the item {@link Section}. All child nodes have its leftBound >= parent.leftBound.
	 * @return
	 */
	Long getLeftBound();
	void setLeftBound(Long leftBound);

	/**
	 * Right bound of the item {@link Section}. All child nodes have its rightBound <= parent.rightBound.
	 * @return
	 */
	Long getRightBound();
	void setRightBound(Long rightBound);

	/**
	 * Count of the immediate children of the node.
	 * @return
	 */
	Short getNumberOfChildren();
	void setNumberOfChildren(Short numberOfChildren);

	/**
	 * Order of the node among neighbouring items on the same level.
	 * Order = 1 means first node, order = parent.numberOfChildren means last node. Order increases by one between
	 * neighbouring nodes.
	 * @return
	 */
	Short getOrder();
	void setOrder(Short order);

	/**
	 * Order of the bucket occupied by this node under the parent node.
	 * Bucket = 1 means first bucket. Two nodes under the same parent must have different buckets. Bucket is usually
	 * different from order because gaps after removed nodes are filled by new nodes and buckets are reused.
	 * @return
	 */
	Short getBucket();
	void setBucket(Short bucket);

}
