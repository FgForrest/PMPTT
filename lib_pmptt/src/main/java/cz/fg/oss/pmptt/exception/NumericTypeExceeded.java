package cz.fg.oss.pmptt.exception;

import cz.fg.oss.pmptt.model.Section;
import lombok.Getter;

/**
 * This exception is thrown when long type is exhausted for the requested level depth and section size.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
public class NumericTypeExceeded extends ArithmeticException {
	private static final long serialVersionUID = -5174191353850685224L;
	@Getter private final String message;
	@Getter private final short maxLevels;
	@Getter private final short sectionSize;

	public NumericTypeExceeded(short requestedLevels, short sectionSize) {
		this.sectionSize = (short) (sectionSize - 1);
		this.maxLevels = (short) (getMaxLevel(requestedLevels, sectionSize) - 1);
		this.message = "Maximum of " + this.maxLevels + " levels is allowed when section size is " + sectionSize + "!";
	}

	private short getMaxLevel(short requestedLevels, short sectionSize) {
		short reachedLevel = -1;
		for (short maxLevels = requestedLevels; maxLevels > 0; maxLevels--) {
			try {
				Section.getSectionSizeForLevel(sectionSize, (short) 1, maxLevels);
				reachedLevel = maxLevels;
				break;
			} catch (ArithmeticException ex) {
				// ignored
			}
		}
		return reachedLevel;
	}
}
