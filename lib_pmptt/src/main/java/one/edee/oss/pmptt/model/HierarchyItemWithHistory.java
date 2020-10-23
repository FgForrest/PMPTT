package one.edee.oss.pmptt.model;

import lombok.Getter;

/**
 * Special form of the item, that holds information about previous form of the item before it has been updated.
 * This information is precious in cases when node is moved in the hierarchy and we want to know its previous and current
 * location in order to execute updates in external systems.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public class HierarchyItemWithHistory implements HierarchyItem {
	@Getter private final HierarchyItemBase original;
	@Getter private final HierarchyItemBase delegate;

	public HierarchyItemWithHistory(String hierarchyCode, String code, Short level, Long leftBound, Long rightBound, Short bucket) {
		this.original = new HierarchyItemBase(hierarchyCode, code, level, leftBound, rightBound, bucket);
		this.delegate = new HierarchyItemBase(hierarchyCode, code, level, leftBound, rightBound, bucket);
	}

	public HierarchyItemWithHistory(String hierarchyCode, String code, Short level, Long leftBound, Long rightBound, Short numberOfChildren, Short order, Short bucket) {
		this.original = new HierarchyItemBase(hierarchyCode, code, level, leftBound, rightBound, numberOfChildren, order, bucket);
		this.delegate = new HierarchyItemBase(hierarchyCode, code, level, leftBound, rightBound, numberOfChildren, order, bucket);
	}

	@Override
	public String getHierarchyCode() {
		return delegate.getHierarchyCode();
	}

	@Override
	public String getCode() {
		return delegate.getCode();
	}

	@Override
	public Short getLevel() {
		return delegate.getLevel();
	}

	@Override
	public Long getLeftBound() {
		return delegate.getLeftBound();
	}

	@Override
	public Long getRightBound() {
		return delegate.getRightBound();
	}

	@Override
	public Short getNumberOfChildren() {
		return delegate.getNumberOfChildren();
	}

	@Override
	public Short getOrder() {
		return delegate.getOrder();
	}

	@Override
	public Short getBucket() {
		return delegate.getBucket();
	}

	@Override
	public void setLevel(Short level) {
		delegate.setLevel(level);
	}

	@Override
	public void setLeftBound(Long leftBound) {
		delegate.setLeftBound(leftBound);
	}

	@Override
	public void setRightBound(Long rightBound) {
		delegate.setRightBound(rightBound);
	}

	@Override
	public void setNumberOfChildren(Short numberOfChildren) {
		delegate.setNumberOfChildren(numberOfChildren);
	}

	@Override
	public void setOrder(Short order) {
		delegate.setOrder(order);
	}

	@Override
	public void setBucket(Short bucket) {
		delegate.setBucket(bucket);
	}

	@Override
	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

}
