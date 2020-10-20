package cz.fg.oss.pmptt.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Helper DTO for passing left and right bound in a single object. DTO is not used in PMPTT directly but is aimed to be
 * used by consumers of the library.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@Data
public class Bounds implements Serializable, Comparable<Bounds> {
	private final long left;
	private final long right;

	public boolean isWithin(Bounds bounds) {
		return left >= bounds.left &&
				right <= bounds.right &&
				!(left == bounds.left && right == bounds.getRight());
	}

	@Override
	public int compareTo(Bounds o) {
		final int leftBoundCompare = Long.compare(left, o.left);
		final int rightBoundCompare = Long.compare(right, o.right);
		if (leftBoundCompare != 0) {
			return leftBoundCompare;
		} else if (rightBoundCompare != 0) {
			return rightBoundCompare;
		} else {
			// equals
			return 0;
		}
	}

	@Override
	public String toString() {
		return left + "-" + right;
	}
}
