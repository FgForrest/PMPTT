package cz.fg.oss.pmptt.model;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a {@link HierarchyItem} with its children.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@Data
public class HierarchyLevel {
	private final HierarchyItem item;
	private final List<HierarchyItem> children = new LinkedList<>();

}
