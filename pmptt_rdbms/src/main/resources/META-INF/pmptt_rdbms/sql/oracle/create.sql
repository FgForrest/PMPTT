create table T_MPTT_HIERARCHY
(
	"code" varchar2(255) not null,
    "levels" smallint not null,
    "sectionSize" smallint not null
);

create unique index UQ_MPTT_HIERARCHY_CODE
	on T_MPTT_HIERARCHY ("code");

alter table T_MPTT_HIERARCHY
	add constraint PK_MPTT_HIERARCHY_CODE
		primary key ("code");

create table T_MPTT_ITEM
(
    "code" varchar2(255) not null,
    "hierarchyCode" varchar2(255) not null,
    "level" smallint not null,
    "leftBound" number(19) not null,
    "rightBound" number(19) not null,
    "numberOfChildren" smallint not null,
    "order" smallint not null,
    "bucket" smallint not null,
    constraint FK_MPTT_ITEM_HIERARCHY_CODE
        foreign key ("hierarchyCode") references T_MPTT_HIERARCHY ("code")
            on delete cascade
);

alter table T_MPTT_ITEM
    add constraint PK_MPTT_ITEM_CODE
        primary key ("hierarchyCode", "code");

create unique index UQ_MPTT_ITEM_SANITY
    on T_MPTT_ITEM ("hierarchyCode", "leftBound", "rightBound");