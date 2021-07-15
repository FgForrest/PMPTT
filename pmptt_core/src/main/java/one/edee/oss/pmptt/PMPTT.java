package one.edee.oss.pmptt;

import lombok.RequiredArgsConstructor;
import one.edee.oss.pmptt.dao.HierarchyStorage;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.spi.HierarchyChangeListener;
import one.edee.oss.pmptt.util.Assert;

/**
 * Base class of the Pre-allocated Modified Preorder Tree Traversal algorithm. For algorithm description see documentation
 * for {@link Hierarchy}. This class servers to maintain list of multiple hierarchies distinguished by their code and
 * registering {@link HierarchyChangeListener}.
 *
 * @see Hierarchy
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@RequiredArgsConstructor
public class PMPTT {
	private final HierarchyStorage hierarchyStorage;

	/**
	 * Registers new callback listeners to be called back in case modification event occurs in the hierarchy.
	 * Listeners are shared for all hierarchies of this PMPTT instance.
	 *
	 * @param listener implementation to be called back
	 */
	public void registerChangeListener(HierarchyChangeListener listener) {
		this.hierarchyStorage.registerChangeListener(listener);
	}

	/**
	 * Returns or creates new hierarchy of certain (unique) code.
	 *
	 * @param code unique code of the hierarchy
	 * @param levels maximum levels of the hierarchy
	 * @param sectionSize maximum items inside the section
	 * @return hierarchy looked up
	 * @throws IllegalArgumentException when dimensions in the arguments don't match dimensions of already created hierarchy
	 */
	public Hierarchy getOrCreateHierarchy(String code, short levels, short sectionSize) {
		final Hierarchy hierarchy = hierarchyStorage.getHierarchy(code);
		if (hierarchy == null) {
			final Hierarchy newHierarchy = new Hierarchy(code, levels, sectionSize);
			hierarchyStorage.createHierarchy(newHierarchy);
			return newHierarchy;
		} else {
			Assert.isTrue(
					hierarchy.getLevels() - 1 == levels,
					"Incompatible level size - existing " + hierarchy.getLevels() + " wanted " + levels + "!"
			);
			Assert.isTrue(
					hierarchy.getSectionSize() - 1 == sectionSize,
					"Incompatible section size - existing " + hierarchy.getSectionSize() + " wanted " + sectionSize + "!"
			);
			return hierarchy;
		}
	}

	/**
	 * Removes existing hierarchy of certain code. Returns true if hierarchy was found and removed.
	 *
	 * @param code unique code of the hierarchy
	 */
	public boolean removeHierarchy(String code) {
		return hierarchyStorage.removeHierarchy(code);
	}

}
