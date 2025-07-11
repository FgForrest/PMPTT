-- Create sequences for auto-increment functionality
CREATE SEQUENCE SEQ_MPTT_HIERARCHY_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_MPTT_ITEM_ID START WITH 1 INCREMENT BY 1;

create table T_MPTT_HIERARCHY
(
	"id" number(10) not null,
	"code" varchar2(255) not null,
    "levels" smallint not null,
    "sectionSize" smallint not null,
    constraint PK_MPTT_HIERARCHY_ID primary key ("id")
);

create unique index UQ_MPTT_HIERARCHY_CODE
	on T_MPTT_HIERARCHY ("code");

create table T_MPTT_ITEM
(
    "id" number(10) not null,
    "code" varchar2(255) not null,
    "hierarchyCode" varchar2(255) not null,
    "hierarchy_id" number(10) not null,
    "level" smallint not null,
    "leftBound" number(19) not null,
    "rightBound" number(19) not null,
    "numberOfChildren" smallint not null,
    "order" smallint not null,
    "bucket" smallint not null,
    constraint PK_MPTT_ITEM_ID primary key ("id"),
    constraint FK_MPTT_ITEM_HIERARCHY_CODE
        foreign key ("hierarchyCode") references T_MPTT_HIERARCHY ("code")
            on delete cascade,
    constraint FK_MPTT_ITEM_HIERARCHY_ID
        foreign key ("hierarchy_id") references T_MPTT_HIERARCHY ("id")
            on delete cascade
);

create unique index UQ_MPTT_ITEM_SANITY
    on T_MPTT_ITEM ("hierarchyCode", "leftBound", "rightBound");

create unique index UQ_MPTT_ITEM_COMPOSITE_KEY
    on T_MPTT_ITEM ("hierarchyCode", "code");
