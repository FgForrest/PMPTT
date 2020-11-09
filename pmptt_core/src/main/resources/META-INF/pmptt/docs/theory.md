# Pre-allocated Modified Preorder Tree Traversal

MPTT it quite old and very clever way for transposing hierarchical - tree structure to a two-dimensional representation
that is suitable for relational databases. MPTT allows translating information about node tree position into two numbers - 
left and right bound. Two number fields can be easily indexed and look ups for records in the tree can take advantage of
database indexes and perform really quickly.

I won't go into details of the (well) documented MPTT algorithm itself, because there are a lot of sources where you can
go for comprehensible explanation (better than I would be able to provide). If you are not familiar with the algorithm, 
please, go first and read these articles:

- [Modified Preorder Tree Traversal](https://gist.github.com/tmilos/f2f999b5839e2d42d751)
- [Modified Preorder Tree Traversal in Django](https://www.caktusgroup.com/blog/2016/01/04/modified-preorder-tree-traversal-django/https://www.caktusgroup.com/blog/2016/01/04/modified-preorder-tree-traversal-django/)
- [Storing hierarchical data in relational database (Czech version)](https://www.zdrojak.cz/clanky/ukladame-hierarchicka-data-v-databazi-iii/)

We're often working with hierarchical data in my [company](https://www.fg.cz) and so we really wanted to have this gear
implemented as optimally as possible. Original MPTT algorithm has one big drawback and that is costly updates when you move 
nodes in the tree, insert new and remove old nodes. Simply said - MPTT has heavy writes and light reads.

In our [e-commerce sollution](https://www.edee.one/) we work with loads of data that are accessed primarily in a hierarchical
fashion. When you browse your online store you usually want to see a collection of products in a certain category and all its
sub categories. Even if it seems as a simple operation it might translate to really complex query that might target millions
of records in the database. Wholesalers have complex price policies and can have dozens of different prices for single item,
dozens attributes that describe the item, dozens of product shapes and looks and cartesian product might grow to tens
of millions of records. So you really want use any filtering constraint that would keep the combination count at sane levels. Using
MPTT bounds seems to be a good way to go, because that's the way user looks at the data.

To be able to take advantage of the numeric (left/right bound) index we need to copy the bounds not only to all products, but also
to other records (such as prices) that we want to finally join in a single big cartesian product (even noSQL solution would 
benefit from indexed MPTT bounds, so there is no need to think about MPTT only in connection with a relational database).
If we would be content with original MPTT it would lead to regular update of thousands of records in case of simple write
operations with the tree. So we invested some time to come up with some improvements that would tackle the heavy writes.

We decided to limit ourselves with a few constraints that would allow us to use much more efficient form of MPTT. Let's
say that we can constraint ourselves with maximum number of levels (depth of the tree) and maximum number of children nodes of
each parent node. This way we'll be able to compute an overall numeric interval that would be required to place
any of the new node in advance.

Also let's keep all these attributes per node:

**mutable attributes**
- numberOfChildren (count of intermittent children of the node allow to easily detect leaf nodes)
- order (allows sorting child nodes in the scope of parent node without necessity to touch left/right bounds)

**immutable attributes**
- level (depth of the node)
- left (left MPTT bound - as in the original algorithm)
- right (right MPTT bound - as in the original algorithm)

Immutable properties are assigned at the moment of the node creation and must not be changed afterwards. As you can see
left and right bound are among them and that's crucial for our indexed data that are counted in millions.

We can say that if we prepare the tree for 10 levels and 55 children per node we would need 55^11 number interval to
cover the top-level nodes (and that's slightly lower than 2^64 that often represents big integers in database engines).
Let's use more comprehensible numbers as examples - if we are content with 3 levels and 2 children per node, we'd
need only span of 0 - 29. Tree leveling would look like this:

- 1 - 14
  - 2 - 7
    - 3 - 4
    - 5 - 6
  - 8 - 13
    - 9 - 10
    - 11 - 12
- 15 - 28
  - 16 - 21
    - 17 - 18
    - 19 - 20
  - 22 - 27
    - 23 - 24
    - 25 - 26
    
Look at this tree as a honeycomb that is empty at the beginning and will gradually fill up as we add the contents.
Let's go through all necessary operations one by one:

## Read operations

### Retrieve all children of the node

``` sql
select * from MPTT where left > node.left and right < node.right
```

### Retrieve all children of the node, including the node

``` sql
select * from MPTT where left >= node.left and right <= node.right
```

### Retrieve all children of the node in the next Y levels

``` sql
select * from MPTT where left > node.left and right < node.right and level > node.level and level < node.level + Y
```

### Retrieve all parents of the node

``` sql
select * from MPTT where left < node.left and right > node.right
```

### Retrieve all parents of the node, including the node

``` sql
select * from MPTT where left <= node.left and right >= node.right
```

### Retrieve all parents of the node in the upper Y levels

``` sql
select * from MPTT where left < node.left and right > node.right and level > node.level and level < node.level + Y
```

### Retrieve all leaf nodes of the parent node

``` sql
select * from MPTT a where left < node.left and right > node.right and node.numberOfChildren = 0
```

### Construct entire tree

- retrieve all nodes ordered by a left bound asc, right bound asc
- build tree adding node based on `level` attribute, traversing down and up as the `level` oscilates
- before printing the tree reorder the nodes under the same parent node by `order` attribute

## Write operations

### Add new node

- find parent node
- compute / read number interval of the parent node
- find a first unoccupied slot in the node space and assign it to the new node (ie. assign left + right bound)
- set numberOfChildren of the new node to zero
- set level of the new node to parent.level plus one
- increase numberOfChildren of the parent by one
- set the order of the new node to parent.numberOfChildren

### Remove node

- find the node
- remove all nodes that has node.leftBound >= removedNode.leftBound and node.rightBound <= removedNode.rightBound
- find all nodes that have level == removedNode.level and node.leftBound > removedNode.parentNode.leftBound and 
  node.rightBound < removedNode.parentNode.rightBound and node.order > removedNode.order and decrease their order by one
- decrease numberOfChildren of parent node by one

### Move node from position X to Y on the same level

- find node X
- find node Y
- set X.order = Y.order
- decrease order by one of all nodes where node.leftBound > X.leftBound and node.rightBound <= Y.rightBound 
  and node.level = X.level

### Move node from position X to Y on different levels

- remove node X
- add the node X after Y on different level

### Add a new node after X node

- combine operations Add new node
- and Move node from position X to Y on the same level

## Conclusion

As you can see - by constraining ourselves with limited width and depth of the tree, we're able to significantly reduce
the complexity of the updates to the tree and still keep all lookup queries really fast. Key attributes - left and right 
bound can be made immutable and never change during the lifetime of the tree. That leads to significant reduction 
of the complexity of the additional updates of records bound to the products that are attached to the category tree.
There is single write operation that remains rather costly and that is moving node to different level of the tree, 
which still requires boundary reassignment.

The main drawback in PMPTT algorithm is the situation when we run out of levels or need to store more children for the
same parent node than was accounted for in the time of tree creation. Such situations would require complete recomputation
of the entire tree.