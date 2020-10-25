package one.edee.oss.pmptt.exception;

import lombok.Getter;

/**
 * Exception is thrown when there is attempt to create item in the hierarchy in the section that already has all
 * pre-allocated buckets full. Ie. when node is filled up to the maximum count of the child nodes.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public class SectionExhausted extends IllegalArgumentException {
	private static final long serialVersionUID = 5241359088446010200L;
	@Getter private final short sectionSize;

	public SectionExhausted(String message, short sectionSize) {
		super(message);
		this.sectionSize = sectionSize;
	}

}
