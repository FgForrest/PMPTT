/* all previously generated data must be recomputed */
delete from T_MPTT_HIERARCHY;

alter table T_MPTT_ITEM
	add "bucket" smallint not null;

drop index UQ_MPTT_ITEM_SANITY;

create unique index UQ_MPTT_ITEM_SANITY
    on T_MPTT_ITEM ("hierarchyCode", "leftBound", "rightBound");