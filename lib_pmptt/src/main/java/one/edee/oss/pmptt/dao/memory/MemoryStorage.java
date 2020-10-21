package one.edee.oss.pmptt.dao.memory;

import lombok.Getter;
import one.edee.oss.pmptt.dao.HierarchyStorage;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItem;
import one.edee.oss.pmptt.model.HierarchyItemWithHistory;
import one.edee.oss.pmptt.model.HierarchyLevel;
import one.edee.oss.pmptt.model.Section;
import one.edee.oss.pmptt.model.SectionWithBucket;
import one.edee.oss.pmptt.spi.HierarchyChangeListener;
import one.edee.oss.pmptt.util.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Memory implementation of the PMPTT storage. Used only in tests as data are not persistent in any way.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public class MemoryStorage implements HierarchyStorage {
	private final List<HierarchyChangeListener> changeListeners = new LinkedList<>();
	private final Map<String, HierarchyWithContents> hierarchyIndex = new HashMap<>();

	@Override
	public void registerChangeListener(HierarchyChangeListener listener) {
		this.changeListeners.add(listener);
	}

	@Override
	public void createHierarchy(Hierarchy hierarchy) {
		Assert.isTrue(!hierarchyIndex.containsKey(hierarchy.getCode()), "Hierarchy code " + hierarchy.getCode() + " is not unique!");
		hierarchyIndex.put(hierarchy.getCode(), new HierarchyWithContents(hierarchy));
		hierarchy.setStorage(this);
	}

	@Override
	public Hierarchy getHierarchy(String code) {
		final HierarchyWithContents hierarchyWithContents = hierarchyIndex.get(code);
		return hierarchyWithContents == null ? null : hierarchyWithContents.getHierarchy();
	}

	@Override
	public void createItem(HierarchyItem newItem, HierarchyItem parent) {
		final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(newItem.getHierarchyCode());

		hierarchyWithContents.addItem(newItem, parent == null ? null : parent.getCode());
		for (HierarchyChangeListener changeListener : changeListeners) {
			changeListener.itemCreated(newItem);
		}
	}

	@Override
	public void updateItem(HierarchyItem updatedItem) {
		final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(updatedItem.getHierarchyCode());
		hierarchyWithContents.updateItem(updatedItem);

		// in memory implementation instances are identities and are already updated
		for (HierarchyChangeListener changeListener : changeListeners) {
			Assert.isTrue(updatedItem instanceof HierarchyItemWithHistory, "Hierarchy item is not of type HierarchyItemWithHistory!");
			final HierarchyItemWithHistory hiwh = (HierarchyItemWithHistory) updatedItem;
			changeListener.itemUpdated(hiwh.getDelegate(), hiwh.getOriginal());
		}
	}

	@Override
	public void removeItem(HierarchyItem removedItem) {
		final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(removedItem.getHierarchyCode());
		hierarchyWithContents.removeItem(removedItem);

		for (HierarchyChangeListener changeListener : changeListeners) {
			changeListener.itemRemoved(removedItem);
		}
	}

	@Override
	public HierarchyItem getItem(String hierarchyCode, String code) {
		final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(hierarchyCode);
		return hierarchyWithContents.getItem(code);
	}

	@Override
	public HierarchyItem getParentItem(HierarchyItem pivot) {
		final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(pivot.getHierarchyCode());
		return hierarchyWithContents.getParentItem(pivot.getCode());
	}

	@Override
	public List<HierarchyItem> getParentsOfItem(HierarchyItem pivot) {
		final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(pivot.getHierarchyCode());
		return hierarchyWithContents.getParentItems(pivot.getCode());
	}

	@Override
	public List<HierarchyItem> getRootItems(String hierarchyCode) {
		final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(hierarchyCode);
		return hierarchyWithContents.getRootItems();
	}

	@Override
	public List<HierarchyItem> getChildItems(HierarchyItem parent) {
		final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(parent.getHierarchyCode());
		return hierarchyWithContents.getChildItems(parent);
	}

	@Override
	public List<HierarchyItem> getAllChildrenItems(HierarchyItem parent) {
		final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(parent.getHierarchyCode());
		return hierarchyWithContents.getAllChildItems(parent);
	}

	@Override
	public List<HierarchyItem> getLeafItems(HierarchyItem parent) {
		final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(parent.getHierarchyCode());
		return hierarchyWithContents.getAllLeafItems(parent);
	}

	@Override
	public List<HierarchyItem> getLeafItems(String hierarchyCode) {
		final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(hierarchyCode);
		return hierarchyWithContents.getAllLeafItems(null);
	}

	@Override
	public SectionWithBucket getFirstEmptySection(String hierarchyCode, long sectionSize, short maxCount) {
		final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(hierarchyCode);
		final List<HierarchyItem> rootItemsByLeftBound = hierarchyWithContents.getRootItemsByLeftBound();
		final long initialLeftBound = 1L;
		return getFirstEmptySection(sectionSize, maxCount, rootItemsByLeftBound, initialLeftBound);
	}

	@Override
	public SectionWithBucket getFirstEmptySection(String hierarchyCode, long sectionSize, short maxCount, HierarchyItem parent) {
		final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(hierarchyCode);
		final List<HierarchyItem> childItemsByLeftBound = hierarchyWithContents.getChildItemsByLeftBound(parent);
		return getFirstEmptySection(sectionSize, maxCount, childItemsByLeftBound, parent.getLeftBound() + 1L);
	}

	private HierarchyWithContents getHierarchyWithContents(String hierarchyCode) {
		final HierarchyWithContents hierarchyWithContents = hierarchyIndex.get(hierarchyCode);
		Assert.notNull(hierarchyWithContents, "Hierarchy with code " + hierarchyCode + " not found!");
		return hierarchyWithContents;
	}

	private SectionWithBucket getFirstEmptySection(long sectionSize, short maxCount, List<HierarchyItem> items, long initialLeftBound) {
		if (items.size() + 1 >= maxCount) {
			return null;
		}
		if (items.isEmpty()) {
			return new SectionWithBucket(initialLeftBound, initialLeftBound + sectionSize - 1, (short) 1);
		} else {
			Long lastLeftBound = null;
			for (int i = 0; i < items.size(); i++) {
				final HierarchyItem item = items.get(i);
				if (lastLeftBound == null) {
					if (item.getLeftBound() > initialLeftBound) {
						return new SectionWithBucket(
								initialLeftBound,
								initialLeftBound + sectionSize - 1,
								(short) 1
						);
					}
				} else {
					if (item.getLeftBound() > lastLeftBound + sectionSize) {
						return new SectionWithBucket(
								lastLeftBound + sectionSize,
								lastLeftBound + 2 * sectionSize - 1,
								(short)(i + 1)
						);
					}
				}
				lastLeftBound = item.getLeftBound();
			}
			return new SectionWithBucket(
					lastLeftBound + sectionSize,
					lastLeftBound + sectionSize * 2 - 1,
					(short)(items.size() + 1)
			);
		}
	}

	private static class HierarchyWithContents {
		private static final String ROOT_LEVEL = "__root";
		@Getter private final Hierarchy hierarchy;
		private final Map<String, HierarchyLevel> levels = new HashMap<>();
		private final Map<String, HierarchyLevel> itemParents = new HashMap<>();

		HierarchyWithContents(Hierarchy hierarchy) {
			this.hierarchy = hierarchy;
			final Section rootSection = Section.computeRootHierarchyBounds(hierarchy.getSectionSize(), hierarchy.getLevels());
			final HierarchyItem rootItem = new HierarchyItemWithHistory(hierarchy.getCode(), ROOT_LEVEL, (short) 0, rootSection.getLeftBound(), rootSection.getRightBound(), (short) 1);
			this.levels.put(ROOT_LEVEL, new HierarchyLevel(rootItem));
		}

		List<HierarchyItem> getAllLeafItems(HierarchyItem withParent) {
			final LinkedList<HierarchyItem> result = new LinkedList<>();
			final List<HierarchyItem> itemsToGoThrough = withParent == null ? levels.get(ROOT_LEVEL).getChildren() : levels.get(withParent.getCode()).getChildren();
			addLeafItems(result, itemsToGoThrough);
			return result;
		}

		private void addLeafItems(LinkedList<HierarchyItem> result, List<HierarchyItem> itemsToGoThrough) {
			for (HierarchyItem item : itemsToGoThrough) {
				if (item.getNumberOfChildren() == 0) {
					result.add(item);
				}
				final HierarchyLevel level = levels.get(item.getCode());
				if (!level.getChildren().isEmpty()) {
					addLeafItems(result, level.getChildren());
				}
			}
		}

		List<HierarchyItem> getParentItems(String code) {
			final LinkedList<HierarchyItem> result = new LinkedList<>();
			String lookUpCode = code;
			HierarchyLevel level;
			do {
				level = itemParents.get(lookUpCode);
				if (!ROOT_LEVEL.equals(level.getItem().getCode())) {
					lookUpCode = level.getItem().getCode();
					result.add(level.getItem());
				}
			} while (!ROOT_LEVEL.equals(level.getItem().getCode()));
			Collections.reverse(result);
			return result;
		}

		List<HierarchyItem> getRootItems() {
			final List<HierarchyItem> result = new ArrayList<>(levels.get(ROOT_LEVEL).getChildren());
			result.sort(HierarchyItemOrderComparator.INSTANCE);
			return result;
		}

		List<HierarchyItem> getRootItemsByLeftBound() {
			final List<HierarchyItem> result = new ArrayList<>(levels.get(ROOT_LEVEL).getChildren());
			result.sort(HierarchyItemLeftBoundComparator.INSTANCE);
			return result;
		}

		List<HierarchyItem> getChildItems(HierarchyItem parent) {
			final List<HierarchyItem> result = new ArrayList<>(levels.get(parent.getCode()).getChildren());
			result.sort(HierarchyItemOrderComparator.INSTANCE);
			return result;
		}

		List<HierarchyItem> getChildItemsByLeftBound(HierarchyItem parent) {
			final List<HierarchyItem> result = new ArrayList<>(levels.get(parent.getCode()).getChildren());
			result.removeIf(child -> !(child.getLeftBound() >= parent.getLeftBound() && child.getRightBound() <= parent.getRightBound()));
			result.sort(HierarchyItemLeftBoundComparator.INSTANCE);
			return result;
		}

		List<HierarchyItem> getAllChildItems(HierarchyItem parent) {
			final List<HierarchyItem> result = new LinkedList<>();
			addChildren(parent, result);
			return result;
		}

		HierarchyItem getParentItem(String code) {
			final HierarchyLevel hierarchyLevel = itemParents.get(code);
			return ROOT_LEVEL.equals(hierarchyLevel.getItem().getCode()) ? null : hierarchyLevel.getItem();
		}

		HierarchyItem getItem(String code) {
			final HierarchyLevel hierarchyLevel = itemParents.get(code);
			if (hierarchyLevel != null) {
				for (HierarchyItem child : hierarchyLevel.getChildren()) {
					if (Objects.equals(code, child.getCode())) {
						return child;
					}
				}
			}
			return null;
		}

		void addItem(HierarchyItem item, String parent) {
			final HierarchyLevel level = levels.get(parent == null ? ROOT_LEVEL : parent);
			level.getChildren().add(item);
			itemParents.put(item.getCode(), level);
			levels.put(item.getCode(), new HierarchyLevel(item));
		}

		void updateItem(HierarchyItem updatedItem) {
			final Section parentSection = Section.computeParentSectionBounds(hierarchy.getSectionSize(), updatedItem);
			boolean parentFound = false;
			for (HierarchyLevel level : levels.values()) {
				if (level.getItem().getLeftBound().equals(parentSection.getLeftBound()) && level.getItem().getRightBound().equals(parentSection.getRightBound())) {
					parentFound = true;
					if (!level.getChildren().contains(updatedItem)) {
						level.getChildren().add(updatedItem);
						final HierarchyLevel oldParent = itemParents.get(updatedItem.getCode());
						if (oldParent != null) {
							oldParent.getChildren().remove(updatedItem);
						}
						itemParents.put(updatedItem.getCode(), level);
					}
				}
			}
			Assert.isTrue(parentFound, "Parent with bounds " + parentSection.getLeftBound() + "-" + parentSection.getRightBound() + " was not found!");
		}

		void removeItem(HierarchyItem item) {
			final HierarchyLevel hierarchyLevel = itemParents.remove(item.getCode());
			hierarchyLevel.getChildren().remove(item);
			levels.remove(item.getCode());
		}

		private void addChildren(HierarchyItem parent, List<HierarchyItem> result) {
			final List<HierarchyItem> children = new ArrayList<>(levels.get(parent.getCode()).getChildren());
			children.sort(HierarchyItemOrderComparator.INSTANCE);
			result.addAll(children);

			for (HierarchyItem child : children) {
				addChildren(child, result);
			}
		}

	}

	private static class HierarchyItemOrderComparator implements Comparator<HierarchyItem>, Serializable {
		private static final HierarchyItemOrderComparator INSTANCE = new HierarchyItemOrderComparator();
		private static final long serialVersionUID = 3021563606387314468L;

		@Override
		public int compare(HierarchyItem o1, HierarchyItem o2) {
			return Short.compare(o1.getOrder(), o2.getOrder());
		}
	}

	private static class HierarchyItemLeftBoundComparator implements Comparator<HierarchyItem>, Serializable {
		private static final HierarchyItemLeftBoundComparator INSTANCE = new HierarchyItemLeftBoundComparator();
		private static final long serialVersionUID = -8262193400044256075L;

		@Override
		public int compare(HierarchyItem o1, HierarchyItem o2) {
			return Long.compare(o1.getLeftBound(), o2.getLeftBound());
		}
	}

}
