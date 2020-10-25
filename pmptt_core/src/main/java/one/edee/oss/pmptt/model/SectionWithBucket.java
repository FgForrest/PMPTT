package one.edee.oss.pmptt.model;

import lombok.Getter;

/**
 * DTO carries Section along with assigned bucket inside it's parent section.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
public class SectionWithBucket extends Section {
	@Getter private final short bucket;

	public SectionWithBucket(Long leftBound, Long rightBound, short bucket) {
		super(leftBound, rightBound);
		this.bucket = bucket;
	}

}
