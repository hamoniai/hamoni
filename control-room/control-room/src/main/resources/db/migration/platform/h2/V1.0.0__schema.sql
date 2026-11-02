
create table "platform"."authnprofile" (
                                           "id" uuid not null,
                                           "created" timestamp(6) with time zone not null,
                                           "email" varchar(255) not null,
                                           "enabled" boolean not null,
                                           "family_name" varchar(255) not null,
                                           "given_name" varchar(255) not null,
                                           "issuer" varchar(255) not null,
                                           "issuerSubject" varchar(255) not null,
                                           "name" varchar(255) not null,
                                           "updated" timestamp(6) with time zone not null,
                                           primary key ("id")
);

comment on table "platform"."authnprofile" is
        'Authentication profiles';

    comment on column "platform"."authnprofile"."created" is
        'Date the account was created (UTC)';

    comment on column "platform"."authnprofile"."email" is
        'Email address';

    comment on column "platform"."authnprofile"."enabled" is
        'Enabled status (for banning users)';

    comment on column "platform"."authnprofile"."family_name" is
        'Family name';

    comment on column "platform"."authnprofile"."given_name" is
        'Given name';

    comment on column "platform"."authnprofile"."issuer" is
        'OIDC issuer';

    comment on column "platform"."authnprofile"."issuerSubject" is
        'OIDC issuer subject';

    comment on column "platform"."authnprofile"."name" is
        'Display name';

    comment on column "platform"."authnprofile"."updated" is
        'Date the account was last updated (UTC)';

create table "platform"."authnprofile_organisations" (
                                                         "authnprofile_id" uuid not null,
                                                         "id" uuid not null
);

comment on column "platform"."authnprofile_organisations"."id" is
        'The organisation identifier';

create index "platform"."idx_authnprofile_id_issuer_issuersubject"
    on "platform"."authnprofile" ("id", "issuer", "issuerSubject");

create index "platform"."idx_authnprofile_issuer_issuersubject"
    on "platform"."authnprofile" ("issuer", "issuerSubject");



alter table if exists "platform"."authnprofile_organisations"
    add constraint "fk_authnprofile_organisations"
    foreign key ("authnprofile_id")
    references "platform"."authnprofile";
