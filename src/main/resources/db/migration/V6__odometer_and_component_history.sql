-- ODOMETER záznamy nájezdu
create table if not exists odometer_entry (
                                              id bigserial primary key,
                                              bike_id bigint not null references bike(id) on delete cascade,
    at_date date not null,
    km numeric(10,1) not null check (km >= 0),
    created_at timestamptz not null default now(),
    unique (bike_id, at_date)
    );
create index if not exists idx_odo_bike_date_desc on odometer_entry (bike_id, at_date desc);

-- HISTORIE komponent
create table if not exists component_event (
                                               id bigserial primary key,
                                               bike_id bigint not null references bike(id) on delete cascade,
    component_id bigint not null references bike_component(id) on delete cascade,
    event_type text not null check (event_type in ('INSTALLED','REMOVED','RESTORED','HARD_DELETED','UPDATED')),
    at_time timestamptz not null default now(),
    note text,
    odometer_km numeric(10,1)
    );
create index if not exists idx_comp_event_comp_time on component_event (component_id, at_time desc);
create index if not exists idx_comp_event_bike_time on component_event (bike_id, at_time desc);

-- ===== Triggery pro automatické logování historie komponent =====

-- 1) Při INSERT do bike_component zaloguj INSTALLED
create or replace function trg_bc_after_insert()
returns trigger language plpgsql as $$
begin
insert into component_event (bike_id, component_id, event_type, at_time, odometer_km, note)
values (new.bike_id, new.id, 'INSTALLED', coalesce(new.installed_at, now()), new.installed_odometer_km, null);
return new;
end $$;

drop trigger if exists trg_bc_ai on bike_component;
create trigger trg_bc_ai
    after insert on bike_component
    for each row execute function trg_bc_after_insert();

-- 2) Při UPDATE removed_at loguj REMOVED/RESTORED
create or replace function trg_bc_after_update_removed()
returns trigger language plpgsql as $$
begin
    if (old.removed_at is null and new.removed_at is not null) then
        insert into component_event (bike_id, component_id, event_type, at_time, note)
        values (new.bike_id, new.id, 'REMOVED', new.removed_at, null);
    elsif (old.removed_at is not null and new.removed_at is null) then
        insert into component_event (bike_id, component_id, event_type, at_time, note)
        values (new.bike_id, new.id, 'RESTORED', now(), null);
end if;
return new;
end $$;

drop trigger if exists trg_bc_au_removed on bike_component;
create trigger trg_bc_au_removed
    after update of removed_at on bike_component
    for each row execute function trg_bc_after_update_removed();

-- 3) Před DELETE, pokud je to hard-delete po softu, zaloguj HARD_DELETED
create or replace function trg_bc_before_delete()
returns trigger language plpgsql as $$
begin
    if old.removed_at is not null then
        insert into component_event (bike_id, component_id, event_type, at_time, note)
        values (old.bike_id, old.id, 'HARD_DELETED', now(), null);
end if;
return old;
end $$;

drop trigger if exists trg_bc_bd on bike_component;
create trigger trg_bc_bd
    before delete on bike_component
    for each row execute function trg_bc_before_delete();
