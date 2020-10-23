package one.edee.oss.pmptt.exception;

import lombok.Getter;

/**
 * Exception is thrown when there is attempt to use non-existing node as pivot for requested operation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public class PivotHierarchyNodeNotFound extends IllegalArgumentException {
	private static final long serialVersionUID = -514854517544794926L;
	@Getter private final String nodeId;

	public PivotHierarchyNodeNotFound(String message, String nodeId) {
		super(message);
		this.nodeId = nodeId;
	}

}
