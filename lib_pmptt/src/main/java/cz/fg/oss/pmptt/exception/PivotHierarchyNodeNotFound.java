package cz.fg.oss.pmptt.exception;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public class PivotHierarchyNodeNotFound extends IllegalArgumentException {

	public PivotHierarchyNodeNotFound(String s) {
		super(s);
	}

}
