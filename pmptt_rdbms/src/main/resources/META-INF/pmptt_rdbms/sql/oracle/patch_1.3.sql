-- Patch 1.3: Add autoincremental primary key 'id' to all tables
-- This patch handles existing data by creating nullable columns first,
-- then generating IDs, making them NOT NULL, and finally setting up constraints

-- Step 1: Add nullable id columns to both tables
ALTER TABLE T_MPTT_HIERARCHY ADD "id" NUMBER(10) NULL;
ALTER TABLE T_MPTT_ITEM ADD "id" NUMBER(10) NULL;

-- Step 2: Generate unique IDs for existing data in T_MPTT_HIERARCHY
UPDATE T_MPTT_HIERARCHY SET "id" = ROWNUM;

-- Step 3: Generate unique IDs for existing data in T_MPTT_ITEM
UPDATE T_MPTT_ITEM SET "id" = ROWNUM;

-- Step 4: Make id columns NOT NULL
ALTER TABLE T_MPTT_HIERARCHY MODIFY "id" NUMBER(10) NOT NULL;
ALTER TABLE T_MPTT_ITEM MODIFY "id" NUMBER(10) NOT NULL;

-- Step 5: Drop existing foreign key constraints that reference primary keys we need to drop
ALTER TABLE T_MPTT_ITEM DROP CONSTRAINT FK_MPTT_ITEM_HIERARCHY_CODE;

-- Step 6: Drop existing primary key constraints
ALTER TABLE T_MPTT_HIERARCHY DROP CONSTRAINT PK_MPTT_HIERARCHY_CODE;
ALTER TABLE T_MPTT_ITEM DROP CONSTRAINT PK_MPTT_ITEM_CODE;

-- Step 7: Add new primary key constraints using id columns
ALTER TABLE T_MPTT_HIERARCHY ADD CONSTRAINT PK_MPTT_HIERARCHY_ID PRIMARY KEY ("id");
ALTER TABLE T_MPTT_ITEM ADD CONSTRAINT PK_MPTT_ITEM_ID PRIMARY KEY ("id");

-- Step 8: Create sequences for auto-increment functionality
CREATE SEQUENCE SEQ_MPTT_HIERARCHY_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_MPTT_ITEM_ID START WITH 1 INCREMENT BY 1;

-- Step 9: Add hierarchy_id column to T_MPTT_ITEM for foreign key relationship
ALTER TABLE T_MPTT_ITEM ADD "hierarchy_id" NUMBER(10) NULL;

-- Step 10: Populate hierarchy_id with corresponding id from T_MPTT_HIERARCHY
UPDATE T_MPTT_ITEM ti
SET ti."hierarchy_id" = (
    SELECT th."id"
    FROM T_MPTT_HIERARCHY th
    WHERE ti."hierarchyCode" = th."code"
);

-- Step 11: Make hierarchy_id NOT NULL
ALTER TABLE T_MPTT_ITEM MODIFY "hierarchy_id" NUMBER(10) NOT NULL;

-- Step 12: Add new foreign key constraint using id columns
ALTER TABLE T_MPTT_ITEM
ADD CONSTRAINT FK_MPTT_ITEM_HIERARCHY_ID
FOREIGN KEY ("hierarchy_id") REFERENCES T_MPTT_HIERARCHY ("id")
ON DELETE CASCADE;

-- Step 13: Initialize sequences to ensure unique future IDs
-- Set sequence start values based on maximum existing IDs
DECLARE
    max_hierarchy_id NUMBER;;
    max_item_id NUMBER;;
    sql_stmt VARCHAR2(200);;
BEGIN
    -- Get maximum id from T_MPTT_HIERARCHY
    SELECT NVL(MAX("id"), 0) + 1 INTO max_hierarchy_id FROM T_MPTT_HIERARCHY;;

    -- Drop and recreate sequence with correct start value
    EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_MPTT_HIERARCHY_ID';;
    sql_stmt := 'CREATE SEQUENCE SEQ_MPTT_HIERARCHY_ID START WITH ' || max_hierarchy_id || ' INCREMENT BY 1';;
    EXECUTE IMMEDIATE sql_stmt;;

    -- Get maximum id from T_MPTT_ITEM
    SELECT NVL(MAX("id"), 0) + 1 INTO max_item_id FROM T_MPTT_ITEM;;

    -- Drop and recreate sequence with correct start value
    EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_MPTT_ITEM_ID';;
    sql_stmt := 'CREATE SEQUENCE SEQ_MPTT_ITEM_ID START WITH ' || max_item_id || ' INCREMENT BY 1';;
    EXECUTE IMMEDIATE sql_stmt;;
END;;;

-- Step 14: Ensure unique indexes on the original key columns for data integrity
-- Note: UQ_MPTT_HIERARCHY_CODE already exists from create.sql, so we don't recreate it
-- Note: UQ_MPTT_ITEM_SANITY already exists but covers different columns, so we add one for the original composite key
CREATE UNIQUE INDEX UQ_MPTT_ITEM_COMPOSITE_KEY ON T_MPTT_ITEM ("hierarchyCode", "code");
