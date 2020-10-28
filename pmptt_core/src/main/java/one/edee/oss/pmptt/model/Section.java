package one.edee.oss.pmptt.model;

import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;

/**
 * Represents section reserved for the hierarchy item.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@Data
@CommonsLog
public class Section {
	private final Long leftBound;
	private final Long rightBound;

	/**
	 * Hard invented algorithm that computes tree section size on any of the tree level in such way, that section fully
	 * envelopes sections subtree and don't share outer bounds with first and last sub item. For visualisation see
	 * SectionTest#testTreeComputation() test.
	 *
	 * @param sectionSize maximal number of nodes in the section
	 * @param level for which section size is computed
	 * @param maxLevels maximal number of levels in hierarchy
	 * @return section size for the particular level
	 * @throws ArithmeticException when span exceeds limits of long type
	 */
	public static long getSectionSizeForLevel(short sectionSize, short level, short maxLevels) {
		long spanSize = 0;
		// bottom level introduced first unused space that is square of section size - one section + 2 (external sides)
		long lastSpan = Math.addExact(Math.multiplyExact((long)sectionSize, (long)sectionSize), (long)2) - sectionSize;
		// bottom level unused space is not used for bottom level
		for (int i = 0; i < (maxLevels - level); i++) {
			if (i == 0) {
				// bottom level unused space use used on one level above bottom level
				spanSize = lastSpan;
			} else {
				// additional levels multiply firstly introduced unused space by section size and sum
				final long newSpan = Math.multiplyExact((long)lastSpan, (long)sectionSize);
				spanSize = Math.addExact(spanSize, newSpan);
				lastSpan = newSpan;
			}
		}
		// result space on desired level is section size + reverse factorial of unused space
		return Math.addExact((long)sectionSize, (long)spanSize);
	}

	/**
	 * This is original computation that led to {@link #getSectionSizeForLevel(short, short, short)} computation.
	 * It's left in the code so that validation tests can be executed upon it and results of above mentioned method can
	 * be cross verified.
	 *
	 * BEWARE: THIS METHOD IS NOT INTENDED TO BE USED IN PRODUCTION LOGIC
	 *
	 * @param sectionSize maximal number of nodes in the section
	 * @param maxLevels maximal number of levels in hierarchy
	 * @return section size
	 */
	public static long getSectionSizeForLevelAlternative(short sectionSize, short maxLevels) {
		final long usedNumbers = Math.round(Math.pow(sectionSize, maxLevels));
		long unusedNumbers = 0L;
		for (int i = 0; i < maxLevels - 1; i++) {
			final int maxSpace = i * 2;
			int space = maxSpace;
			unusedNumbers = 0L;
			int multiplier = sectionSize - 1;
			final StringBuilder logInfo = new StringBuilder();
			for(int j = 1; j <= i; j++) {
				logInfo.append(space).append("->").append(multiplier).append(", ");
				unusedNumbers += space * multiplier;
				multiplier *= sectionSize;
				space -= 2;
			}
			if (log.isDebugEnabled()) {
			    log.debug("Max space " + maxSpace + ", computed " + unusedNumbers + ", histogram: " + logInfo.toString());
			}
		}
		unusedNumbers += (maxLevels - 1) * 2 - 1;
		if (log.isDebugEnabled()) {
		    log.debug("Estimated " + (usedNumbers + unusedNumbers) + ", used numbers " + usedNumbers + ", unused numbers " + unusedNumbers);
		}
		return usedNumbers + unusedNumbers;
	}

	/**
	 * Computes bounds for the entire hierarchy.
	 *
	 * @param sectionSize maximal number of nodes in the section
	 * @param maxLevels maximal number of levels in hierarchy
	 * @return left and right bounds for the root item
	 */
	public static Section computeEntireHierarchyBounds(short sectionSize, short maxLevels) {
		final long rootSection = Section.getSectionSizeForLevel(sectionSize, (short) 1, maxLevels);
		return new Section(0L, rootSection - 1);
	}

	/**
	 * Computes bounds for the root item of the hierarchy.
	 *
	 * @param sectionSize maximal number of nodes in the section
	 * @param maxLevels maximal number of levels in hierarchy
	 * @return left and right bounds for the root item
	 */
	public static Section computeRootHierarchyBounds(short sectionSize, short maxLevels) {
		final long rootSection = Section.getSectionSizeForLevel(sectionSize, (short) 2, (short)(maxLevels + 1));
		return new Section(1L, rootSection);
	}

	/**
	 * Computes bounds for the item that is parent of the passed hierarchy item.
	 *
	 * @param sectionSize maximal number of nodes in the section
	 * @param item whose parent bounds needs to be computed
	 * @return left and right bounds for the parent item of the passed item
	 */
	public static Section computeParentSectionBounds(short sectionSize, HierarchyItem item) {
		final long itemSize = item.getRightBound() - item.getLeftBound() + 1;
		return new Section(
				item.getLeftBound() - (item.getBucket() - 1) * itemSize - 1,
				item.getLeftBound() + (sectionSize - item.getBucket() + 1) * itemSize
		);
	}

	/**
	 * Computes span size (ie. the space between left and right bound of the section).
	 *
	 * @return section size in terms of bounds
	 */
	public long getBoundSpan() {
		return rightBound - leftBound + 1;
	}

}
