package one.edee.oss.pmptt.exception;

/**
 * Exception is thrown when the item is already part of the hierarchy. Such item cannot be added and must be either
 * removed first or moved.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ItemAlreadyPresent extends IllegalArgumentException {
	private static final long serialVersionUID = -6149802963462178358L;

	public ItemAlreadyPresent(String hierarchyCode, String itemCode) {
		super("Item " + itemCode + " is already part of the " + hierarchyCode + " hierarchy. Remove first or use move operation.");
	}
}
