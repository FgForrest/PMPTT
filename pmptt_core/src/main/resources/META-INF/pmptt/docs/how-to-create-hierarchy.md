# How to create hierarchy

Simply instantiate by passing memory storage implementation and create the tree:

``` java
public class ExampleClass {
	private final PMPTT pmptt = new PMPTT(new MemoryStorage());

	public ExampleClass() {
		final Hierarchy categoryHierarchy = pmptt.getOrCreateHierarchy("categories", (short) 10, (short) 55);

		final HierarchyItem electronics = categoryHierarchy.createRootItem("electronics");
		final HierarchyItem televisions = categoryHierarchy.createItem("televisions", electronics.getCode());
		final HierarchyItem portableElectronics = categoryHierarchy.createItem("portable_electronics", electronics.getCode());

		categoryHierarchy.createItem("tube", televisions.getCode());
		categoryHierarchy.createItem("lcd", televisions.getCode());
		categoryHierarchy.createItem("plasma", televisions.getCode());

		categoryHierarchy.createItem("mp3_players", portableElectronics.getCode());
		categoryHierarchy.createItem("cd_players", portableElectronics.getCode());
		categoryHierarchy.createItem("3_way_radios", portableElectronics.getCode());
	}
}
```

You can manage multiple hierarchies inside single PMPTT instance.

## Alter hierarchy

You can create, move or delete nodes in the hierarchy:

``` java
public void doSomeChanges() {
	final Hierarchy categoryHierarchy = pmptt.getOrCreateHierarchy("categories", (short) 10, (short) 55);
	
	categoryHierarchy.moveItemAfter("tube", "plasma");
	categoryHierarchy.moveItemBefore("tube", "plasma");
	categoryHierarchy.moveItemToFirst("2_way_radios");
	categoryHierarchy.moveItemToLast("2_way_radios");
	
	categoryHierarchy.moveItemBetweenLevelsFirst("lcd", "portable_electronics");
	
	// this removes also nodes in its sub-tree
	categoryHierarchy.removeItem("televisions");
}
```