# Pre-allocated Modified Preorder Tree Traversal

[MPTT](https://en.wikipedia.org/wiki/Tree_traversal) approach to tree structures simplifies the evaluation of parent 
and child relationship to two numbers comparation. One of the [Forrest guys](https://www.fg.cz/cs/forresti/tym/honza-10233)
has a lecture on this topic at [JavaDays 2020](https://javadays.cz/cs/) conference. Although it is a relatively old 
algorithm, it has one key disadvantage and that is the high cost of writing a new item or moving it in the structure. 
These operations typically involve the recalculation of a large part of the tree, and if these boundaries are stored 
elsewhere due to the greater performance of SQL queries, this disadvantage is further multiplied.

For this reason, we have come up with a small mutation in this algorithm (called Pre-allocated Modified Preorder Tree 
Traversal), which, while accepting certain simplifications, allows you to define an equation that determines boundaries 
for each tree node so that in write operations such as adding, removing or moving a node within the same superior node, 
which doesn't imply any boundary recalculation requirements at all.

The simplification to be undertaken is to define in advance the maximum number of levels of immersion in the tree, 
and the maximum number of children per node. At the same time, it is necessary to combine the data type long, which 
represents about 10 levels of the tree in depth with 55 items in one node.

The library also greatly simplifies the operations of moving nodes between different levels of the tree (including 
child nodes), although these operations remain just as write-intensive as in the original algorithm.

Implications of the PMPTT agorithms are summarized in this [presentation](http://bit.ly/javadays2020).

For more information see section [How to use](#how-to-use)

## Prerequisites

- JDK 1.8+
- Commons Logging (1.2.0+)

**RDBMS version:**

- Spring framework (5.3.0+), works also on 4.3.X older version although not compilable due to JUnit tests

## Supported databases

- MySQL
- Oracle

Do you missing one? Fill a pull request!

## How to compile

Use standard Maven 3 command:

```
mvn clean install
```

## How to run tests

Start databases:

```
docker-compose -f docker/docker-compose.yml up 
```

Run your tests in IDE or run:

```
mvn clean test
```

Help us maintain at least 80% code coverage!

## How to use

See separate chapters for details:

- [Theory behind](pmptt_core/src/main/resources/META-INF/pmptt/docs/theory.md)
- [How to integrate to your application](pmptt_core/src/main/resources/META-INF/pmptt/docs/how-to-integrate.md)
- [How to create hierarchy](pmptt_core/src/main/resources/META-INF/pmptt/docs/how-to-create-hierarchy.md)
- [How to query hierarchy](pmptt_core/src/main/resources/META-INF/pmptt/docs/how-to-query-hierarchy.md)
- [How to propagate changes do duplicated data](pmptt_core/src/main/resources/META-INF/pmptt/docs/how-to-propagate-changes.md)