package cz.fg.oss.pmptt.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

/**
 * Represents a single node in the {@link Hierarchy}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@Data
@AllArgsConstructor
public class HierarchyItemBase implements HierarchyItem {
	/**
	 * Unique code of the hierarchy.
	 */
	private final String hierarchyCode;
	/**
	 * Unique code of the node in the hierarchy.
	 */
	private final String code;
	/**
	 * Level of nesting of the node in the hierarchy.
	 * Root level is level = 1, additional layers have level = level + 1
	 */
	private Short level;
	/**
	 * Left bound of the item {@link Section}. All child nodes have its leftBound >= parent.leftBound.
	 */
	private Long leftBound;
	/**
	 * Right bound of the item {@link Section}. All child nodes have its rightBound <= parent.rightBound.
	 */
	private Long rightBound;
	/**
	 * Count of the immediate children of the node.
	 */
	private Short numberOfChildren;
	/**
	 * Order of the node among neighbouring items on the same level.
	 * Order = 1 means first node, order = parent.numberOfChildren means last node. Order increases by one between
	 * neighbouring nodes.
	 */
	private Short order;
	/**
	 * Order of the bucket occupied by this node under the parent node.
	 * Bucket = 1 means first bucket. Two nodes under the same parent must have different buckets. Bucket is usually
	 * different from order because gaps after removed nodes are filled by new nodes and buckets are reused.
	 */
	private Short bucket;

	public HierarchyItemBase(String hierarchyCode, String code, Short level, Long leftBound, Long rightBound, Short bucket) {
		this.hierarchyCode = hierarchyCode;
		this.code = code;
		this.level = level;
		this.leftBound = leftBound;
		this.rightBound = rightBound;
		this.bucket = bucket;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof HierarchyItem)) return false;
		HierarchyItem that = (HierarchyItem) o;
		return hierarchyCode.equals(that.getHierarchyCode()) &&
				code.equals(that.getCode());
	}

	@Override
	public int hashCode() {
		return Objects.hash(hierarchyCode, code);
	}

}
