package one.edee.oss.pmptt;

import lombok.RequiredArgsConstructor;
import one.edee.oss.pmptt.dao.HierarchyStorage;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.spi.HierarchyChangeListener;
import one.edee.oss.pmptt.util.Assert;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 * * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@RequiredArgsConstructor
public class PMPTT {
	private final HierarchyStorage hierarchyStorage;

	public void registerChangeListener(HierarchyChangeListener listener) {
		this.hierarchyStorage.registerChangeListener(listener);
	}

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

}
