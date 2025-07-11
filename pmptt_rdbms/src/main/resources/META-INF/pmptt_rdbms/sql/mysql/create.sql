create table T_MPTT_HIERARCHY
(
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	code varchar(255) not null,
	levels smallint not null,
	sectionSize smallint not null
);

create unique index UQ_MPTT_HIERARCHY_CODE
	on T_MPTT_HIERARCHY (code);

create table T_MPTT_ITEM
(
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code varchar(255) not null,
    hierarchyCode varchar(255) not null,
    hierarchy_id INT NOT NULL,
    level smallint not null,
    leftBound bigint not null,
    rightBound bigint not null,
    numberOfChildren smallint not null,
    `order` smallint not null,
    bucket smallint not null,
    constraint FK_MPTT_ITEM_HIERARCHY_CODE
        foreign key (hierarchyCode) references T_MPTT_HIERARCHY (code)
            on update cascade on delete cascade,
    constraint FK_MPTT_ITEM_HIERARCHY_ID
        foreign key (hierarchy_id) references T_MPTT_HIERARCHY (id)
            on update cascade on delete cascade
);

create unique index UQ_MPTT_ITEM_SANITY
    on T_MPTT_ITEM (hierarchyCode, leftBound, rightBound);

create unique index UQ_MPTT_ITEM_COMPOSITE_KEY
    on T_MPTT_ITEM (hierarchyCode, code);
