package one.edee.oss.pmptt.exception;

import lombok.Getter;

/**
 * Exception is thrown when there is attempt to create item in the hierarchy on level that exceeds maximal amount
 * of pre-allocated levels in the hierarchy.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public class MaxLevelExceeded extends IllegalArgumentException {
	private static final long serialVersionUID = 31075427661666761L;
	@Getter private final short level;
	@Getter private final short maxLevels;

	public MaxLevelExceeded(String message, short level, short maxLevels) {
		super(message);
		this.level = level;
		this.maxLevels = maxLevels;
	}

}
