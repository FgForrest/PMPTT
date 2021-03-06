package one.edee.oss.pmptt;

import one.edee.oss.pmptt.dao.HierarchyStorage;
import one.edee.oss.pmptt.dao.memory.MemoryStorage;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@Transactional
public abstract class AbstractPMPTTTest {
	@Autowired(required = false)
	private HierarchyStorage hierarchyStorage;
	private PMPTT tested;

	@BeforeEach
	public void setUp() {
		tested = hierarchyStorage == null ? new PMPTT(new MemoryStorage()) : new PMPTT(hierarchyStorage);
	}

	@Test
	public void shouldCreateNewHierarchy() {
		final Hierarchy test = tested.getOrCreateHierarchy("test", (short) 5, (short) 10);
		assertNotNull(test);
		final Collection<String> existingHierarchyCodes = tested.getExistingHierarchyCodes();
		assertEquals(1, existingHierarchyCodes.size());
		assertEquals("test", existingHierarchyCodes.iterator().next());
	}

	@Test
	public void shouldRemoveExistingHierarchy() {
		assertFalse(tested.removeHierarchy("test"));
		final Hierarchy test = tested.getOrCreateHierarchy("test", (short) 5, (short) 10);
		assertNotNull(test);
		assertNotNull(tested.getOrCreateHierarchy("test", (short) 5, (short) 10));
		assertTrue(tested.removeHierarchy("test"));
	}

	@Test
	public void shouldGetExistingHierarchy() {
		final Hierarchy test = tested.getOrCreateHierarchy("test", (short) 4, (short) 9);
		test.createRootItem("1");

		final Hierarchy loadedHierarchy = tested.getOrCreateHierarchy("test", (short) 4, (short) 9);
		final List<HierarchyItem> rootItems = loadedHierarchy.getRootItems();
		assertEquals(1, rootItems.size());
		final HierarchyItem firstItem = rootItems.get(0);
		Assertions.assertEquals("1", firstItem.getCode());
		Assertions.assertEquals(Short.valueOf((short)1), firstItem.getLevel());
		Assertions.assertEquals(Long.valueOf(1), firstItem.getLeftBound());
		Assertions.assertEquals(Long.valueOf(10222), firstItem.getRightBound());
	}

	@Test
	public void shouldFailToGetHierarchyWithDifferentLevelCount() {
		assertThrows(IllegalArgumentException.class, () -> {
			tested.getOrCreateHierarchy("test", (short) 5, (short) 10);
			tested.getOrCreateHierarchy("test", (short) 6, (short) 10);
		});
	}

	@Test
	public void shouldFailToGetHierarchyWithDifferentSectionSize() {
		assertThrows(IllegalArgumentException.class, () -> {
			tested.getOrCreateHierarchy("test", (short) 5, (short) 10);
			tested.getOrCreateHierarchy("test", (short) 5, (short) 20);
		});
	}
}
