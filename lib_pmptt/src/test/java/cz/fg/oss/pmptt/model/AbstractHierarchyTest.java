package cz.fg.oss.pmptt.model;

import cz.fg.oss.pmptt.dao.HierarchyStorage;
import cz.fg.oss.pmptt.dao.memory.MemoryStorage;
import cz.fg.oss.pmptt.exception.NumericTypeExceeded;
import cz.fg.oss.pmptt.exception.SectionExhausted;
import cz.fg.oss.pmptt.spi.HierarchyChangeListener;
import cz.fg.oss.pmptt.util.StructureLoader;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public abstract class AbstractHierarchyTest {
	private static final ClassPathResource TREE_5_2 = new ClassPathResource("META-INF/lib_pmptt/data/structure-5-2.txt");
	private static final ClassPathResource TREE_5_4 = new ClassPathResource("META-INF/lib_pmptt/data/structure-5-4.txt");
	private Hierarchy tested;
	@Autowired(required = false)
	private HierarchyStorage hierarchyStorage;
	private PuppetHierarchyChangeListener puppetListener = new PuppetHierarchyChangeListener();

	@BeforeEach
	public void setUp() {
		tested = new Hierarchy("test", (short)4, (short)9);
		final HierarchyStorage hierarchyStorage = this.hierarchyStorage == null ? new MemoryStorage() : this.hierarchyStorage;
		tested.setStorage(hierarchyStorage);
		hierarchyStorage.createHierarchy(tested);
		hierarchyStorage.registerChangeListener(puppetListener);
		puppetListener.clear();
	}

	@Test
	void shouldFailToCreateHierarchyExceedingLong() {
		try {
			new Hierarchy("test", (short) 55, (short) 50);
		} catch (NumericTypeExceeded ex) {
			assertEquals(10, ex.getMaxLevels());
			assertEquals(50, ex.getSectionSize());
		}
	}

	@Test
	public void shouldAddItem() {
		for (int i = 0; i < 5; i++) {
			tested.createRootItem(String.valueOf(i));
		}

		final List<HierarchyItem> rootItems = tested.getRootItems();
		assertEquals(5, rootItems.size());
		assertItem(rootItems.get(0), 1, 1, 0);
		assertEquals(rootItems.get(0).getRightBound() + 1, (long) rootItems.get(1).getLeftBound());
		assertItem(rootItems.get(1), 1, 2, 0);
		assertEquals(rootItems.get(1).getRightBound() + 1, (long) rootItems.get(2).getLeftBound());
		assertItem(rootItems.get(2), 1, 3, 0);
		assertEquals(rootItems.get(2).getRightBound() + 1, (long) rootItems.get(3).getLeftBound());
		assertItem(rootItems.get(3), 1, 4, 0);
		assertEquals(rootItems.get(3).getRightBound() + 1, (long) rootItems.get(4).getLeftBound());
		assertItem(rootItems.get(4), 1, 5, 0);

		assertItemsCreated("0", "1", "2", "3", "4");
		assertItemsTouched("0", "1", "2", "3", "4");
	}

	@Test
	public void shouldAddAllItemsOnSameLevel() {
		for (int i = 0; i < 9; i++) {
			tested.createRootItem(String.valueOf(i));
		}
	}

	@Test
	public void shouldAddItemsOnAllLevels() {
		HierarchyItem parentItem = tested.createRootItem(String.valueOf(0));
		for (int i = 1; i < 4; i++) {
			parentItem = tested.createItem(String.valueOf(i), parentItem.getCode());
		}
	}

	@Test
	public void shouldFailTooAddToManyItemsOnSameLevel() {
		assertThrows(SectionExhausted.class, () -> {
			for (int i = 0; i < 10; i++) {
				tested.createRootItem(String.valueOf(i));
			}
		});
	}

	@Test
	public void shouldAddItemOnThirdLevel() {
		StructureLoader.loadHierarchy(TREE_5_2, tested);

		puppetListener.clear();

		final HierarchyItem item1 = tested.createItem("Dvounohé", "Stoly");
		final HierarchyItem item2 = tested.createItem("Třínohé", "Stoly");
		final HierarchyItem item3 = tested.createItem("Čtyřnohé", "Stoly");
		final HierarchyItem item4 = tested.createItem("Židle", "Jídelna");

		assertItem(item1, 3, 1, 0);
		assertItem(item2, 3, 2, 0);
		assertItem(item3, 3, 3, 0);

		final HierarchyItem diningRoom = tested.getItem("Jídelna");
		assertItem(diningRoom, 1, 1, 3);

		final HierarchyItem tables = tested.getItem("Stoly");
		assertItem(tables, 2, 2, 3);

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-2-testAdding.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-2-testAdding_withBounds.txt", tested);

		assertItemsCreated("Dvounohé", "Třínohé", "Čtyřnohé", "Židle");
		assertItemsUpdated("Stoly", "Stoly", "Stoly", "Jídelna");
		assertItemsTouched("Dvounohé", "Stoly", "Třínohé", "Stoly", "Čtyřnohé", "Stoly", "Židle", "Jídelna");
	}

	@Test
	public void shouldAddItemBefore() {
		StructureLoader.loadHierarchy(TREE_5_2, tested);

		puppetListener.clear();

		final HierarchyItem shortTable = tested.createItem("Krátké", "Obývací pokoj", "Konferenční stoly");
		final HierarchyItem first = tested.getItem("Komody a regály");
		final HierarchyItem second = tested.getItem("Konferenční stoly");

		tested.createRootItem("Dlažební kostky", "Ložnice");
		tested.createItem("Žulové", "Dlažební kostky");
		tested.createItem("Papírové", "Dlažební kostky", "Žulové");
		tested.createItem("Měděné", "Dlažební kostky", "Žulové");
		tested.createItem("S vyřezáváním", "Měděné");
		tested.createItem("Prosté", "Měděné", "S vyřezáváním");

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-2-testAddingBefore.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-2-testAddingBefore_withBounds.txt", tested);

		assertItem(first, 2, 1, 0);
		assertItem(shortTable, 2, 2, 0);
		assertItem(second, 2, 3, 0);

		assertItem(tested.getItem("Měděné"), 2, 2, 2);
		assertItem(tested.getItem("S vyřezáváním"), 3, 2, 0);
		assertItem(tested.getItem("Prosté"), 3, 1, 0);

		assertItemsCreated("Krátké", "Dlažební kostky", "Žulové", "Papírové", "Měděné", "S vyřezáváním", "Prosté");
		assertItemsUpdated("Obývací pokoj", "Konferenční stoly", "Ložnice", "Obývací pokoj", "Předsíň", "Dlažební kostky", "Dlažební kostky", "Žulové", "Dlažební kostky", "Žulové", "Měděné", "Měděné", "S vyřezáváním");
		assertItemsTouched("Krátké", "Obývací pokoj", "Konferenční stoly", "Dlažební kostky", "Ložnice", "Obývací pokoj", "Předsíň", "Žulové", "Dlažební kostky", "Papírové", "Dlažební kostky", "Žulové", "Měděné", "Dlažební kostky", "Žulové", "S vyřezáváním", "Měděné", "Prosté", "Měděné", "S vyřezáváním");
	}

	@Test
	public void shouldRemoveItem() {
		StructureLoader.loadHierarchy(TREE_5_2, tested);

		puppetListener.clear();

		tested.removeItem("Kancelářské kontejnery");
		tested.removeItem("Ložnice");

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-2-testRemoval.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-2-testRemoval_withBounds.txt", tested);

		assertItem(tested.getItem("Jídelna"), 1, 1, 2);
		assertItem(tested.getItem("Kancelář"), 1, 2, 1);
		assertItem(tested.getItem("Obývací pokoj"), 1, 3, 2);

		assertItem(tested.getItem("Kancelářské stoly"), 2, 1, 0);

		assertItemsRemoved("Kancelářské kontejnery", "Komody", "Noční stolky", "Ložnice");
		assertItemsUpdated("Kancelář", "Kancelářské stoly", "Obývací pokoj", "Předsíň");
		assertItemsTouched("Kancelář", "Kancelářské stoly", "Kancelářské kontejnery", "Komody", "Noční stolky", "Obývací pokoj", "Předsíň", "Ložnice");
	}

	@Test
	public void shouldFillGapAfterItemRemoval() {
		StructureLoader.loadHierarchy(TREE_5_2, tested);

		final HierarchyItem removedItem = tested.getItem("Kancelářské kontejnery");
		tested.removeItem("Kancelářské kontejnery");
		final HierarchyItem removedRootItem = tested.getItem("Ložnice");
		tested.removeItem("Ložnice");

		tested.createRootItem("Přístřešky");
		tested.createItem("Podtácky", "Kancelář");

		final HierarchyItem addedRootItem = tested.getItem("Přístřešky");
		final HierarchyItem addedItem = tested.getItem("Podtácky");

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-2-testRemovalAndAdd.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-2-testRemovalAndAdd_withBounds.txt", tested);

		assertEquals(removedRootItem.getLeftBound(), addedRootItem.getLeftBound());
		assertEquals(removedRootItem.getRightBound(), addedRootItem.getRightBound());
		assertEquals(removedRootItem.getBucket(), addedRootItem.getBucket());
		assertEquals(removedItem.getLeftBound(), addedItem.getLeftBound());
		assertEquals(removedItem.getRightBound(), addedItem.getRightBound());
		assertEquals(removedItem.getBucket(), addedItem.getBucket());

		assertItem(tested.getItem("Jídelna"), 1, 1, 2);
		assertItem(tested.getItem("Kancelář"), 1, 2, 2);
		assertItem(tested.getItem("Obývací pokoj"), 1, 3, 2);
		assertItem(tested.getItem("Přístřešky"), 1, 5, 0);

		assertItem(tested.getItem("Kancelářské stoly"), 2, 1, 0);
		assertItem(tested.getItem("Podtácky"), 2, 2, 0);
	}

	@Test
	public void shouldMoveItemLast() {
		StructureLoader.loadHierarchy(TREE_5_2, tested);

		puppetListener.clear();

		tested.moveItemToLast("Jídelna");
		tested.moveItemToLast("Ložnice");

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-2-testMoveLast.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-2-testMoveLast_withBounds.txt", tested);

		assertItem(tested.getItem("Kancelář"), 1, 1, 2);
		assertItem(tested.getItem("Obývací pokoj"), 1, 2, 2);
		assertItem(tested.getItem("Předsíň"), 1, 3, 2);
		assertItem(tested.getItem("Jídelna"), 1, 4, 2);
		assertItem(tested.getItem("Ložnice"), 1, 5, 2);

		assertItemsUpdated("Kancelář", "Ložnice", "Obývací pokoj", "Předsíň", "Jídelna", "Obývací pokoj", "Předsíň", "Jídelna", "Ložnice");
		assertItemsTouched("Kancelář", "Ložnice", "Obývací pokoj", "Předsíň", "Jídelna", "Obývací pokoj", "Předsíň", "Jídelna", "Ložnice");
	}

	@Test
	public void shouldMoveItemFirst() {
		StructureLoader.loadHierarchy(TREE_5_2, tested);

		puppetListener.clear();

		tested.moveItemToFirst("Obývací pokoj");
		tested.moveItemToFirst("Kancelář");
		tested.moveItemToFirst("Konferenční stoly");

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-2-testMoveFirst.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-2-testMoveFirst_withBounds.txt", tested);

		assertItem(tested.getItem("Kancelář"), 1, 1, 2);
		assertItem(tested.getItem("Obývací pokoj"), 1, 2, 2);
		assertItem(tested.getItem("Jídelna"), 1, 3, 2);
		assertItem(tested.getItem("Ložnice"), 1, 4, 2);
		assertItem(tested.getItem("Předsíň"), 1, 5, 2);

		assertItem(tested.getItem("Konferenční stoly"), 2, 1, 0);
		assertItem(tested.getItem("Komody a regály"), 2, 2, 0);

		assertItemsUpdated("Jídelna", "Kancelář", "Ložnice", "Obývací pokoj", "Obývací pokoj", "Jídelna", "Kancelář", "Komody a regály", "Konferenční stoly");
		assertItemsTouched("Jídelna", "Kancelář", "Ložnice", "Obývací pokoj", "Obývací pokoj", "Jídelna", "Kancelář", "Komody a regály", "Konferenční stoly");
	}

	@Test
	public void shouldMoveBefore() {
		StructureLoader.loadHierarchy(TREE_5_2, tested);

		puppetListener.clear();

		tested.moveItemBefore("Jídelna", "Obývací pokoj");
		tested.moveItemBefore("Předsíň", "Kancelář");
		tested.moveItemBefore("Noční stolky", "Komody");

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-2-testMoveBefore.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-2-testMoveBefore_withBounds.txt", tested);

		assertItem(tested.getItem("Předsíň"), 1, 1, 2);
		assertItem(tested.getItem("Kancelář"), 1, 2, 2);
		assertItem(tested.getItem("Ložnice"), 1, 3, 2);
		assertItem(tested.getItem("Jídelna"), 1, 4, 2);
		assertItem(tested.getItem("Obývací pokoj"), 1, 5, 2);

		assertItem(tested.getItem("Noční stolky"), 2, 1, 0);
		assertItem(tested.getItem("Komody"), 2, 2, 0);

		assertItemsUpdated("Kancelář", "Ložnice", "Jídelna", "Kancelář", "Ložnice", "Jídelna", "Obývací pokoj", "Předsíň", "Komody", "Noční stolky");
		assertItemsTouched("Kancelář", "Ložnice", "Jídelna", "Kancelář", "Ložnice", "Jídelna", "Obývací pokoj", "Předsíň", "Komody", "Noční stolky");
	}

	@Test
	public void shouldMoveAfter() {
		StructureLoader.loadHierarchy(TREE_5_2, tested);

		puppetListener.clear();

		tested.moveItemAfter("Jídelna", "Obývací pokoj");
		tested.moveItemAfter("Předsíň", "Kancelář");
		tested.moveItemAfter("Noční stolky", "Komody");
		tested.moveItemAfter("Komody a regály", "Konferenční stoly");

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-2-testMoveAfter.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-2-testMoveAfter_withBounds.txt", tested);

		assertItem(tested.getItem("Kancelář"), 1, 1, 2);
		assertItem(tested.getItem("Předsíň"), 1, 2, 2);
		assertItem(tested.getItem("Ložnice"), 1, 3, 2);
		assertItem(tested.getItem("Obývací pokoj"), 1, 4, 2);
		assertItem(tested.getItem("Jídelna"), 1, 5, 2);

		assertItem(tested.getItem("Komody"), 2, 1, 0);
		assertItem(tested.getItem("Noční stolky"), 2, 2, 0);

		assertItem(tested.getItem("Konferenční stoly"), 2, 1, 0);
		assertItem(tested.getItem("Komody a regály"), 2, 2, 0);

		assertItemsUpdated("Kancelář", "Ložnice", "Obývací pokoj", "Jídelna", "Ložnice", "Obývací pokoj", "Jídelna", "Předsíň", "Noční stolky", "Konferenční stoly", "Komody a regály");
		assertItemsTouched("Kancelář", "Ložnice", "Obývací pokoj", "Jídelna", "Ložnice", "Obývací pokoj", "Jídelna", "Předsíň", "Noční stolky", "Konferenční stoly", "Komody a regály");
	}

	@Test
	public void shouldMoveBetweenLevelsBefore() {
		StructureLoader.loadHierarchy(TREE_5_4, tested);

		puppetListener.clear();

		tested.moveItemBetweenLevelsBefore("Čalouněné postele", "Ložnice", "Postele");

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevels.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevels_withBounds.txt", tested);

		assertItem(tested.getItem("Ložnice"), 1, 3, 4);

		assertItem(tested.getItem("Komody"), 2, 1, 0);
		assertItem(tested.getItem("Noční stolky"), 2, 2, 0);
		assertItem(tested.getItem("Čalouněné postele"), 2, 3, 2);
		assertItem(tested.getItem("Postele"), 2, 4, 2);

		assertItem(tested.getItem("Rozměr 140x200 cm"), 3, 1, 0);
		assertItem(tested.getItem("Rozměr 160x200 cm"), 3, 2, 0);

		assertItemsUpdated("Postele", "Ložnice", "Postele", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
		assertItemsTouched("Postele", "Ložnice", "Postele", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
	}

	@Test
	public void shouldMoveBetweenLevelsBeforeTop() {
		StructureLoader.loadHierarchy(TREE_5_4, tested);

		puppetListener.clear();

		tested.moveItemBetweenLevelsBefore("Čalouněné postele", "Ložnice");

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevelsTop.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevelsTop_withBounds.txt", tested);

		assertItem(tested.getItem("Jídelna"), 1, 1, 3);
		assertItem(tested.getItem("Kancelář"), 1, 2, 2);
		assertItem(tested.getItem("Čalouněné postele"), 1, 3, 2);
		assertItem(tested.getItem("Ložnice"), 1, 4, 3);
		assertItem(tested.getItem("Obývací pokoj"), 1, 5, 2);
		assertItem(tested.getItem("Předsíň"), 1, 6, 2);

		assertItem(tested.getItem("Rozměr 140x200 cm"), 2, 1, 0);
		assertItem(tested.getItem("Rozměr 160x200 cm"), 2, 2, 0);

		assertItemsUpdated("Postele", "Ložnice", "Obývací pokoj", "Předsíň", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
		assertItemsTouched("Postele", "Ložnice", "Obývací pokoj", "Předsíň", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
	}

	@Test
	public void shouldMoveBetweenLevelsAfter() {
		StructureLoader.loadHierarchy(TREE_5_4, tested);

		puppetListener.clear();

		tested.moveItemBetweenLevelsAfter("Čalouněné postele", "Ložnice", "Noční stolky");

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevels.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevels_withBounds.txt", tested);

		assertItem(tested.getItem("Ložnice"), 1, 3, 4);

		assertItem(tested.getItem("Komody"), 2, 1, 0);
		assertItem(tested.getItem("Noční stolky"), 2, 2, 0);
		assertItem(tested.getItem("Čalouněné postele"), 2, 3, 2);
		assertItem(tested.getItem("Postele"), 2, 4, 2);

		assertItem(tested.getItem("Rozměr 140x200 cm"), 3, 1, 0);
		assertItem(tested.getItem("Rozměr 160x200 cm"), 3, 2, 0);

		assertItemsUpdated("Postele", "Ložnice", "Postele", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
		assertItemsTouched("Postele", "Ložnice", "Postele", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
	}

	@Test
	public void shouldMoveBetweenLevelsAfterTop() {
		StructureLoader.loadHierarchy(TREE_5_4, tested);

		puppetListener.clear();

		tested.moveItemBetweenLevelsAfter("Čalouněné postele", "Kancelář");

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevelsTop.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevelsTop_withBounds.txt", tested);

		assertItem(tested.getItem("Jídelna"), 1, 1, 3);
		assertItem(tested.getItem("Kancelář"), 1, 2, 2);
		assertItem(tested.getItem("Čalouněné postele"), 1, 3, 2);
		assertItem(tested.getItem("Ložnice"), 1, 4, 3);
		assertItem(tested.getItem("Obývací pokoj"), 1, 5, 2);
		assertItem(tested.getItem("Předsíň"), 1, 6, 2);

		assertItem(tested.getItem("Rozměr 140x200 cm"), 2, 1, 0);
		assertItem(tested.getItem("Rozměr 160x200 cm"), 2, 2, 0);

		assertItemsUpdated("Postele", "Ložnice", "Obývací pokoj", "Předsíň", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
		assertItemsTouched("Postele", "Ložnice", "Obývací pokoj", "Předsíň", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
	}

	@Test
	public void shouldMoveBetweenLevelsFirst() {
		StructureLoader.loadHierarchy(TREE_5_4, tested);

		puppetListener.clear();

		tested.moveItemBetweenLevelsFirst("Čalouněné postele", "Ložnice");

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevelsFirst.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevelsFirst_withBounds.txt", tested);

		assertItem(tested.getItem("Ložnice"), 1, 3, 4);

		assertItem(tested.getItem("Čalouněné postele"), 2, 1, 2);
		assertItem(tested.getItem("Komody"), 2, 2, 0);
		assertItem(tested.getItem("Noční stolky"), 2, 3, 0);
		assertItem(tested.getItem("Postele"), 2, 4, 2);

		assertItem(tested.getItem("Rozměr 140x200 cm"), 3, 1, 0);
		assertItem(tested.getItem("Rozměr 160x200 cm"), 3, 2, 0);

		assertItemsUpdated("Postele", "Ložnice", "Komody", "Noční stolky", "Postele", "Čalouněné postele", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
		assertItemsTouched("Postele", "Ložnice", "Komody", "Noční stolky", "Postele", "Čalouněné postele", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
	}

	@Test
	public void shouldMoveBetweenLevelsTopFirst() {
		StructureLoader.loadHierarchy(TREE_5_4, tested);

		puppetListener.clear();

		tested.moveItemBetweenLevelsFirst("Čalouněné postele");

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevelsTopFirst.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevelsTopFirst_withBounds.txt", tested);

		assertItem(tested.getItem("Čalouněné postele"), 1, 1, 2);
		assertItem(tested.getItem("Jídelna"), 1, 2, 3);
		assertItem(tested.getItem("Kancelář"), 1, 3, 2);
		assertItem(tested.getItem("Ložnice"), 1, 4, 3);
		assertItem(tested.getItem("Obývací pokoj"), 1, 5, 2);
		assertItem(tested.getItem("Předsíň"), 1, 6, 2);

		assertItem(tested.getItem("Rozměr 140x200 cm"), 2, 1, 0);
		assertItem(tested.getItem("Rozměr 160x200 cm"), 2, 2, 0);

		assertItemsUpdated("Postele", "Jídelna", "Kancelář", "Ložnice", "Obývací pokoj", "Předsíň", "Čalouněné postele", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
		assertItemsTouched("Postele", "Jídelna", "Kancelář", "Ložnice", "Obývací pokoj", "Předsíň", "Čalouněné postele", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
	}

	@Test
	public void shouldMoveBetweenLevelsLast() {
		StructureLoader.loadHierarchy(TREE_5_4, tested);

		puppetListener.clear();

		tested.moveItemBetweenLevelsLast("Čalouněné postele", "Ložnice");

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevelsLast.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevelsLast_withBounds.txt", tested);

		assertItem(tested.getItem("Ložnice"), 1, 3, 4);

		assertItem(tested.getItem("Komody"), 2, 1, 0);
		assertItem(tested.getItem("Noční stolky"), 2, 2, 0);
		assertItem(tested.getItem("Postele"), 2, 3, 2);
		assertItem(tested.getItem("Čalouněné postele"), 2, 4, 2);

		assertItem(tested.getItem("Rozměr 140x200 cm"), 3, 1, 0);
		assertItem(tested.getItem("Rozměr 160x200 cm"), 3, 2, 0);

		assertItemsUpdated("Postele", "Ložnice", "Čalouněné postele", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
		assertItemsTouched("Postele", "Ložnice", "Čalouněné postele", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
	}

	@Test
	public void shouldMoveBetweenLevelsTopLast() {
		StructureLoader.loadHierarchy(TREE_5_4, tested);

		puppetListener.clear();

		tested.moveItemBetweenLevelsLast("Čalouněné postele");

		assertTreeLike("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevelsTopLast.txt", tested);
		assertTreeLikeWithBounds("META-INF/lib_pmptt/data/structure-5-4-testMoveBetweenLevelsTopLast_withBounds.txt", tested);

		assertItem(tested.getItem("Jídelna"), 1, 1, 3);
		assertItem(tested.getItem("Kancelář"), 1, 2, 2);
		assertItem(tested.getItem("Ložnice"), 1, 3, 3);
		assertItem(tested.getItem("Obývací pokoj"), 1, 4, 2);
		assertItem(tested.getItem("Předsíň"), 1, 5, 2);
		assertItem(tested.getItem("Čalouněné postele"), 1, 6, 2);

		assertItem(tested.getItem("Rozměr 140x200 cm"), 2, 1, 0);
		assertItem(tested.getItem("Rozměr 160x200 cm"), 2, 2, 0);

		assertItemsUpdated("Postele", "Čalouněné postele", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
		assertItemsTouched("Postele", "Čalouněné postele", "Čalouněné postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
	}

	@Test
	public void shouldGetItem() {
		StructureLoader.loadHierarchy(TREE_5_4, tested);

		assertNotNull(tested.getItem("Jídelna"));
		assertNotNull(tested.getItem("Obývací pokoj"));
		assertNotNull(tested.getItem("Rozměr 160x200 cm"));
	}

	@Test
	public void shouldGetRootItems() {
		StructureLoader.loadHierarchy(TREE_5_4, tested);

		assertItems(tested.getRootItems(), "Jídelna", "Kancelář", "Ložnice", "Obývací pokoj", "Předsíň");
	}

	@Test
	public void shouldGetChildItems() {
		StructureLoader.loadHierarchy(TREE_5_4, tested);

		assertItems(tested.getChildItems("Jídelna"), "Barové židle", "Stoly", "Židle");
	}

	@Test
	public void shouldGetChildItemsLowLevel() {
		StructureLoader.loadHierarchy(TREE_5_4, tested);

		assertItems(tested.getLeafItems("Postele"), "Dřevěné postele", "Kovové postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm");
		assertItems(tested.getLeafItems(null), "Barové židle", "Barové stoly", "Dřevěné stoly", "Jidelní sestavy", "Dřevěné židle", "Kovové židle", "Kancelářské kontejnery", "Kancelářské stoly", "Komody", "Noční stolky", "Dřevěné postele", "Kovové postele", "Rozměr 140x200 cm", "Rozměr 160x200 cm", "Komody obývákové", "Regály", "Dřevěné", "Skleněné", "Věšáky", "Botníky");
	}

	@Test
	public void shouldGetParent() {
		StructureLoader.loadHierarchy(TREE_5_4, tested);

		assertNull(tested.getParentItem("Jídelna"));
		assertNull(tested.getParentItem("Obývací pokoj"));
		assertEquals("Postele", tested.getParentItem("Dřevěné postele").getCode());
		assertEquals("Čalouněné postele", tested.getParentItem("Rozměr 160x200 cm").getCode());
		assertEquals("Konferenční stoly", tested.getParentItem("Dřevěné").getCode());
	}

	@Test
	public void shouldGetParents() {
		StructureLoader.loadHierarchy(TREE_5_4, tested);

		assertParents(tested.getParentItems("Rozměr 140x200 cm"), "Ložnice", "Postele", "Čalouněné postele");
		assertParents(tested.getParentItems("Kovové židle"), "Jídelna", "Židle");
		assertParents(tested.getParentItems("Jídelna"));
	}

	private void assertParents(List<HierarchyItem> parentItems, String... parentCodes) {
		assertEquals(parentCodes.length, parentItems.size());
		for (int i = 0; i < parentCodes.length; i++) {
			final String expected = parentCodes[i];
			assertEquals(expected, parentItems.get(i).getCode());
		}
	}

	private void assertItems(List<HierarchyItem> items, String... codes) {
		assertEquals(codes.length, items.size());
		for (int i = 0; i < codes.length; i++) {
			final String expected = codes[i];
			assertEquals(expected, items.get(i).getCode());
		}
	}

	private void assertTreeLike(String expectedResource, Hierarchy hierarchy) {
		String result = StructureLoader.storeHierarchy(hierarchy);
		try (final InputStream is = new ClassPathResource(expectedResource).getInputStream()) {
			assertEquals(IOUtils.toString(is, StandardCharsets.UTF_8).trim(), result.trim());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void assertTreeLikeWithBounds(String expectedResource, Hierarchy hierarchy) {
		String result = StructureLoader.storeHierarchyAndPrintWithBounds(hierarchy);
		try (final InputStream is = new ClassPathResource(expectedResource).getInputStream()) {
			assertEquals(IOUtils.toString(is, StandardCharsets.UTF_8).trim(), result.trim());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void assertItem(HierarchyItem item, Integer level, Integer order, Integer numberOfChildren) {
		assertEquals(Short.valueOf(level.shortValue()), item.getLevel(), "Level doesn't match!");
		assertEquals(Short.valueOf(order.shortValue()), item.getOrder(), "Order doesn't match!");
		assertEquals(Short.valueOf(numberOfChildren.shortValue()), item.getNumberOfChildren(), "Number of children doesn't match!");
	}

	private void assertItemsCreated(String... expected) {
		assertEquals(
				StringUtils.arrayToCommaDelimitedString(expected),
				StringUtils.collectionToCommaDelimitedString(puppetListener.getCreated())
		);
	}

	private void assertItemsUpdated(String... expected) {
		assertEquals(
				StringUtils.arrayToCommaDelimitedString(expected),
				StringUtils.collectionToCommaDelimitedString(puppetListener.getUpdated())
		);
	}

	private void assertItemsRemoved(String... expected) {
		assertEquals(
				StringUtils.arrayToCommaDelimitedString(expected),
				StringUtils.collectionToCommaDelimitedString(puppetListener.getRemoved())
		);
	}

	private void assertItemsTouched(String... expected) {
		assertEquals(
			StringUtils.arrayToCommaDelimitedString(expected),
			StringUtils.collectionToCommaDelimitedString(puppetListener.getTouched())
		);
	}

	private static class PuppetHierarchyChangeListener implements HierarchyChangeListener {
		@Getter private final List<String> created = new LinkedList<>();
		@Getter private final List<String> updated = new LinkedList<>();
		@Getter private final List<String> removed = new LinkedList<>();
		@Getter private final List<String> touched = new LinkedList<>();

		@Override
		public void itemCreated(HierarchyItem createdItem) {
			this.created.add(createdItem.getCode());
			this.touched.add(createdItem.getCode());
		}

		@Override
		public void itemUpdated(HierarchyItem updatedItem, HierarchyItem originalItem) {
			this.updated.add(updatedItem.getCode());
			this.touched.add(updatedItem.getCode());
		}

		@Override
		public void itemRemoved(HierarchyItem removeItem) {
			this.removed.add(removeItem.getCode());
			this.touched.add(removeItem.getCode());
		}

		void clear() {
			this.created.clear();
			this.updated.clear();
			this.removed.clear();
			this.touched.clear();
		}

	}
}
