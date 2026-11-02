
create table "organisation" (
                                "id" uuid not null,
                                "initialised" boolean not null,
                                "name" varchar(255) not null,
                                primary key ("id")
);

comment on table "organisation" is
        'Organisations';

    comment on column "organisation"."initialised" is
        'Initialised status';

    comment on column "organisation"."name" is
        'Organisation name';

create table "organisation_authconfiguration" (
                                                  "id" uuid not null,
                                                  "client_id" varchar(255) not null unique,
                                                  "client_secret_ref" varchar(255) not null unique,
                                                  "created" timestamp(6) with time zone not null,
                                                  "enabled" boolean not null,
                                                  "name" varchar(255) not null unique,
                                                  "updated" timestamp(6) with time zone not null,
                                                  "url" varchar(255) not null unique,
                                                  "organisation_id" uuid,
                                                  primary key ("id")
);

comment on table "organisation_authconfiguration" is
        'Organisation Authentication Configuration (OIDC)';

    comment on column "organisation_authconfiguration"."client_id" is
        'The client id';

    comment on column "organisation_authconfiguration"."client_secret_ref" is
        'The client secret reference, the actual value is in the credential vault. Null means public client.';

    comment on column "organisation_authconfiguration"."created" is
        'Date the account was created (UTC)';

    comment on column "organisation_authconfiguration"."enabled" is
        'Enabled status';

    comment on column "organisation_authconfiguration"."name" is
        'The display name for the OIDC provider';

    comment on column "organisation_authconfiguration"."updated" is
        'Date the account was last updated (UTC)';

    comment on column "organisation_authconfiguration"."url" is
        'URL for the OIDC provider';

create table "organisation_configuration" (
                                              "organisation_id" uuid not null,
                                              "removeMe" varchar(255) not null unique
);

comment on column "organisation_configuration"."removeMe" is
        'REMOVE ME';

alter table if exists "organisation_authconfiguration"
    add constraint "fk_organisation_authconfiguration"
    foreign key ("organisation_id")
    references "organisation";

alter table if exists "organisation_configuration"
    add constraint "fk_organisation_configuration"
    foreign key ("organisation_id")
    references "organisation";
