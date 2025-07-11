-- Patch 1.3: Add autoincremental primary key 'id' to all tables
-- This patch handles existing data by creating nullable columns first,
-- then generating IDs, making them NOT NULL, and finally setting up constraints

-- Step 1: Add nullable id columns to both tables
ALTER TABLE T_MPTT_HIERARCHY ADD COLUMN id INT NULL;
ALTER TABLE T_MPTT_ITEM ADD COLUMN id INT NULL;

-- Step 2: Generate unique IDs for existing data in T_MPTT_HIERARCHY
SET @row_number = 0;
UPDATE T_MPTT_HIERARCHY SET id = (@row_number := @row_number + 1);

-- Step 3: Generate unique IDs for existing data in T_MPTT_ITEM
SET @row_number = 0;
UPDATE T_MPTT_ITEM SET id = (@row_number := @row_number + 1);

-- Step 4: Make id columns NOT NULL
ALTER TABLE T_MPTT_HIERARCHY MODIFY COLUMN id INT NOT NULL;
ALTER TABLE T_MPTT_ITEM MODIFY COLUMN id INT NOT NULL;

-- Step 5: Drop existing primary key constraints
ALTER TABLE T_MPTT_HIERARCHY DROP PRIMARY KEY;
ALTER TABLE T_MPTT_ITEM DROP PRIMARY KEY;

-- Step 6: Add AUTO_INCREMENT to id columns and set as primary keys
ALTER TABLE T_MPTT_HIERARCHY MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT PRIMARY KEY;
ALTER TABLE T_MPTT_ITEM MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT PRIMARY KEY;

-- Step 7: Add hierarchy_id column to T_MPTT_ITEM for foreign key relationship
ALTER TABLE T_MPTT_ITEM ADD COLUMN hierarchy_id INT NULL;

-- Step 8: Populate hierarchy_id with corresponding id from T_MPTT_HIERARCHY
UPDATE T_MPTT_ITEM ti
JOIN T_MPTT_HIERARCHY th ON ti.hierarchyCode = th.code
SET ti.hierarchy_id = th.id;

-- Step 9: Make hierarchy_id NOT NULL
ALTER TABLE T_MPTT_ITEM MODIFY COLUMN hierarchy_id INT NOT NULL;

-- Step 10: Add new foreign key constraint using id columns
ALTER TABLE T_MPTT_ITEM
ADD CONSTRAINT FK_MPTT_ITEM_HIERARCHY_ID
FOREIGN KEY (hierarchy_id) REFERENCES T_MPTT_HIERARCHY (id)
ON UPDATE CASCADE ON DELETE CASCADE;

-- Step 11: Initialize AUTO_INCREMENT sequences to ensure unique future IDs
-- Get the maximum id from T_MPTT_HIERARCHY and set AUTO_INCREMENT
SET @max_hierarchy_id = (SELECT COALESCE(MAX(id), 0) + 1 FROM T_MPTT_HIERARCHY);
SET @sql = CONCAT('ALTER TABLE T_MPTT_HIERARCHY AUTO_INCREMENT = ', @max_hierarchy_id);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Get the maximum id from T_MPTT_ITEM and set AUTO_INCREMENT
SET @max_item_id = (SELECT COALESCE(MAX(id), 0) + 1 FROM T_MPTT_ITEM);
SET @sql = CONCAT('ALTER TABLE T_MPTT_ITEM AUTO_INCREMENT = ', @max_item_id);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 12: Ensure unique indexes on the original key columns for data integrity
-- Note: UQ_MPTT_HIERARCHY_CODE already exists from create.sql, so we don't recreate it
-- Note: UQ_MPTT_ITEM_SANITY already exists but covers different columns, so we add one for the original composite key
CREATE UNIQUE INDEX UQ_MPTT_ITEM_COMPOSITE_KEY ON T_MPTT_ITEM (hierarchyCode, code);
