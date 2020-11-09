# How to propagate changes to a duplicated data

You can get a performance bonus if you break normalization of the database design and duplicate data about boundaries
to the tables with high number of rows. You can do it easily this way:

``` java
public void createHierarchy() {
	final Hierarchy categoryHierarchy = pmptt.getOrCreateHierarchy("categories", (short) 10, (short) 55);

	final HierarchyItem electronics = categoryHierarchy.createRootItem("electronics");
	final HierarchyItem televisions = categoryHierarchy.createItem("televisions", electronics.getCode());

	final HierarchyItem tube = categoryHierarchy.createItem("tube", televisions.getCode());
	productRepository.getProductsInCategory("tube").forEach(product -> {
		product.setLeftBound(tube.getLeftBound());
		product.setRightBound(tube.getRightBound());
		productRepository.store(product);
	});
}
```

The problem is that when you move category across levels of the tree, the sub-tree of the category still needs
to be fully recomputed:

``` java
categoryHierarchy.moveItemBetweenLevelsBefore("tube", "portable_electronics", "cd_players");
```

For this case we need to register so called `HierarchyChangeListener` that will be notified once any node in the
hierarchy gets modified. Using the change listener we can propagate all the necessary changes to external entities
we need:

``` java
pmptt.registerChangeListener(
    new HierarchyChangeListener() {
        @Override
        public void itemCreated(HierarchyItem createdItem) {
            
        }

        @Override
        public void itemUpdated(HierarchyItem updatedItem, HierarchyItem originalItem) {

        }

        @Override
        public void itemRemoved(HierarchyItem removeItem) {

        }
    }
);
```

***Note:** there is also `HierarchyChangeListenerAdapter` if you need to listen to only single type of the
event.*