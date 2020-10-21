package one.edee.oss.pmptt.model;

import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
public class SectionTest {

	@Test
	public void shouldComputeSectionSizeForLevel() {
		Assertions.assertEquals(126L, Section.getSectionSizeForLevel((short)2, (short)1, (short)6));
		Assertions.assertEquals(62L, Section.getSectionSizeForLevel((short)2, (short)2, (short)6));
		Assertions.assertEquals(30L, Section.getSectionSizeForLevel((short)2, (short)3, (short)6));
		Assertions.assertEquals(14L, Section.getSectionSizeForLevel((short)2, (short)4, (short)6));
		Assertions.assertEquals(6L, Section.getSectionSizeForLevel((short)2, (short)5, (short)6));
		Assertions.assertEquals(2L, Section.getSectionSizeForLevel((short)2, (short)6, (short)6));
	}

	@Test
	public void shouldComputeHierarchyBounds() {
		Assertions.assertEquals(new Section(1L, 126L), Section.computeHierarchyBounds((short)2, (short)6));
		Assertions.assertEquals(new Section(1L, 62L), Section.computeHierarchyBounds((short)2, (short)5));
		Assertions.assertEquals(new Section(1L, 30L), Section.computeHierarchyBounds((short)2, (short)4));
		Assertions.assertEquals(new Section(1L, 14L), Section.computeHierarchyBounds((short)2, (short)3));
		Assertions.assertEquals(new Section(1L, 6L), Section.computeHierarchyBounds((short)2, (short)2));
		Assertions.assertEquals(new Section(1L, 2L), Section.computeHierarchyBounds((short)2, (short)1));
	}

	@Test
	public void shouldComputeParentSectionBounds() {
		Assertions.assertEquals(new Section(4L, 9L), Section.computeParentSectionBounds((short)2, createItem(6, 5L, 6L, 1)));
		Assertions.assertEquals(new Section(4L, 9L), Section.computeParentSectionBounds((short)2, createItem(6, 7L, 8L, 2)));
		Assertions.assertEquals(new Section(40L, 45L), Section.computeParentSectionBounds((short)2, createItem(6, 41L, 42L, 1)));
		Assertions.assertEquals(new Section(40L, 45L), Section.computeParentSectionBounds((short)2, createItem(6, 43L, 44L, 2)));
		Assertions.assertEquals(new Section(116L, 121L), Section.computeParentSectionBounds((short)2, createItem(6, 117L, 118L, 1)));
		Assertions.assertEquals(new Section(116L, 121L), Section.computeParentSectionBounds((short)2, createItem(6, 119L, 120L, 2)));
	}

	private static HierarchyItemBase createItem(int level, long leftBound, long rightBound, int order) {
		return new HierarchyItemBase("whatever", "whatever", (short) level, leftBound, rightBound, (short)0, (short)order, (short) order);
	}

	@Test
	public void testTreeComputation() {
		computeAndVerifyTree((short)4, (short)2);
	}

	@Disabled
	@Test
	public void testBasicTrees() {
		for(short levels = 2; levels < (short)8; levels++) {
			for (short sectionSize = 2; sectionSize < (short)10; sectionSize++) {
				System.out.print("\n\n TREE " + sectionSize + " x " + levels + "\n\n");
				computeAndVerifyTree(levels, sectionSize);
			}
		}
	}

	public void computeAndVerifyTree(short levels, short sectionSize) {
		final Section section = new Section(0L, Section.getSectionSizeForLevel(sectionSize, (short)1, levels) - 1);
		final Section sectionAlternativeComputation = new Section(0L, Section.getSectionSizeForLevelAlternative(sectionSize, levels));

		assertEquals(sectionAlternativeComputation, section);

		final Item item = computeTreeLevel(section, sectionSize, (short) 1, levels, (short) 1);

		final Map<Integer, List<Section>> levelBounds = new LinkedHashMap<>();
		verifyRules(item, new HashSet<>(), levelBounds, null, 0, sectionSize);

		printHistograms(levelBounds);
		printTree(item, sectionSize, levels);
	}

	private void printHistograms(Map<Integer, List<Section>> levelBounds) {
		int cnt;
		Map<Integer, Integer> histogram = new TreeMap<>((o1, o2) -> Integer.compare(o2, o1));
		final StringBuilder sb = new StringBuilder();
		for (Entry<Integer, List<Section>> entry : levelBounds.entrySet()) {
			final Integer level = entry.getKey();
			cnt = 0;
			histogram.clear();
			final List<Section> bounds = entry.getValue();
			sb.append(level).append(": ");
			int lastSpace = 0;
			int sameSpace = 0;
			for (int i = 0; i < bounds.size() - 1; i++) {
				final Section section = bounds.get(i);
				final Section nextSection = bounds.get(i + 1);
				final long rb = section.getRightBound();
				final long nextLb = nextSection.getLeftBound();
				final int space;
				if (rb + 1 != nextLb) {
					space = (int) (nextLb - rb - 1);
					cnt += space;
					final Integer hCnt = histogram.get(space);
					histogram.put(space, (hCnt == null ? 1 : hCnt + 1));
				} else {
					space = 1;
				}
				if (space != 1) {
					if (lastSpace != 0 && space != lastSpace) {
						sb.append(sameSpace).append("x").append(lastSpace).append("|");
						sameSpace = 1;
					} else {
						sameSpace++;
					}
					lastSpace = space;
				}
			}
			if (sameSpace > 0) {
				sb.append(sameSpace).append("x").append(lastSpace).append("|");
			}
			sb.append("\n");
			sb.append("(").append(cnt).append(") count ").append(bounds.size()).append(", histogram: ");
			for (Entry<Integer, Integer> hEntry : histogram.entrySet()) {
				sb.append(hEntry.getKey()).append("->").append(hEntry.getValue()).append(", ");
			}
			sb.append("\n");
		}
		System.out.println(sb);
	}

	private void verifyRules(Item item, Set<Long> bounds, Map<Integer, List<Section>> levelBounds, Section parentSection, int level, short sectionSize) {
		assertTrue(item.section.getLeftBound() < item.section.getRightBound());

		List<Section> sections = levelBounds.computeIfAbsent(level, k -> new LinkedList<>());
		sections.add(item.getSection());

		if (!item.getSubItems().isEmpty()) {
			assertEquals(Long.valueOf(item.section.getLeftBound() + 1), item.subItems.get(0).getSection().getLeftBound());
			assertEquals(Long.valueOf(item.section.getRightBound() - 1), item.subItems.get(item.subItems.size() - 1).getSection().getRightBound());
			assertFalse(bounds.contains(item.section.getLeftBound()));
			assertFalse(bounds.contains(item.section.getRightBound()));
			if (parentSection != null) {
				Assertions.assertEquals(parentSection, Section.computeParentSectionBounds(sectionSize, item.createItem()));
			}
			bounds.add(item.section.getLeftBound());
			bounds.add(item.section.getRightBound());
			long spanSize = item.getSubItems().get(0).getSection().getSpanSize();
			for (Item subItem : item.getSubItems()) {
				assertEquals(spanSize, subItem.getSection().getSpanSize());
				verifyRules(subItem, bounds, levelBounds, item.section, level + 1, sectionSize);
			}
		} else {
			assertEquals(sectionSize, item.section.getRightBound() - item.section.getLeftBound() + 1);
		}
	}

	private void printTree(Item item, short sectionSize, short maxLevels) {
		final HashMap<Integer, String> result = new HashMap<>();
		printTree(item, result, sectionSize, 1, maxLevels);
		for (int i = 1; i <= result.size(); i++) {
			System.out.println(result.get(i));
		}
	}

	private void printTree(Item item, Map<Integer, String> output, short sectionSize, int level, short maxLevels) {
		for (Item subItem : item.getSubItems()) {
			printTree(subItem, output, sectionSize, level + 1, maxLevels);
		}
		final String existingValue = output.get(level);
		output.put(level, (existingValue == null ? "span size " + (item.getSection().getSpanSize()) + " vs. " + Section.getSectionSizeForLevel(sectionSize, (short)level, maxLevels) + ">>" : existingValue) + item.getSection().getLeftBound() + "-" + item.getSection().getRightBound() + "|");
	}

	private Item computeTreeLevel(Section section, short sectionSize, short level, short maxLevels, short order) {
		final Item result = new Item(section, level, order);
		long innerLeftBound = section.getLeftBound() + 1;
		for(int i = 0; i < sectionSize; i++) {
			final long innerRightBound = innerLeftBound + (section.getRightBound() - section.getLeftBound() - 1) / sectionSize - 1;
			if (level < maxLevels) {
				result.addItem(
						computeTreeLevel(
								new Section(innerLeftBound, innerRightBound),
								sectionSize,
								(short) (level + 1),
								maxLevels,
								(short)(i + 1)
						)
				);
			}
			innerLeftBound = innerRightBound + 1;
		}
		return result;
	}

	@Data
	private static class Item {
		private final Section section;
		private final short level;
		private final short order;
		private final List<Item> subItems = new ArrayList<>();

		public void addItem(Item item) {
			subItems.add(item);
		}

		public HierarchyItem createItem() {
			return SectionTest.createItem(level, section.getLeftBound(), section.getRightBound(), order);
		}
	}

}