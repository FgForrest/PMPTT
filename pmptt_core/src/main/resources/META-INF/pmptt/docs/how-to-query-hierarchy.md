# How to query hierarchy

You can query an existing hierarchy in following way:

``` java
public void query() {
	final Hierarchy categoryHierarchy = pmptt.getOrCreateHierarchy("categories", (short) 10, (short) 55);

	final HierarchyItem lcd = categoryHierarchy.getItem("lcd");
	final Long leftBound = lcd.getLeftBound();
	final Long rightBound = lcd.getRightBound();
	final Short level = lcd.getLevel();
	final Short numberOfChildren = lcd.getNumberOfChildren();

	final List<HierarchyItem> tvChildren = categoryHierarchy.getChildItems("televisions");
	final List<HierarchyItem> plasmaParents = categoryHierarchy.getParentItems("plasma");
	final List<HierarchyItem> tvLeafNodes = categoryHierarchy.getLeafItems("televisions");
}
```