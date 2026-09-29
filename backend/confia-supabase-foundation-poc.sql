-- ============================================================================
-- ConfIA.vc — Fundação Supabase para POC de extensão + UsageStats
-- Projeto: minhAi / qyonozbroekuqlotqcbm
-- Produto: ConfIA.vc (schema SQL: confia)
-- Data: 2026-09-27
--
-- SEGURANÇA:
--   - cria SOMENTE o schema confia;
--   - não altera public.companies, créditos, pagamentos ou apps existentes;
--   - anon/authenticated não recebem acesso direto;
--   - service_role é o único papel de API com acesso às tabelas;
--   - nenhuma coluna aceita URL completa, path, query, HTML, senha ou texto.
--
-- Depois de aplicar:
--   Dashboard Supabase > Data API > Exposed schemas
--   adicionar: confia
--
-- Isso NÃO torna o schema público: os GRANTs abaixo continuam bloqueando
-- anon/authenticated. A exposição é necessária apenas para que Edge Functions
-- server-side usando service_role possam acessar .schema('confia').
-- ============================================================================

begin;

create schema if not exists confia;

comment on schema confia is
  'ConfIA.vc — telemetria mínima de navegador/dispositivo. Host-only; sem URL completa, conteúdo, senha ou texto digitado.';

revoke all on schema confia from public;
revoke all on schema confia from anon;
revoke all on schema confia from authenticated;
grant usage on schema confia to service_role;

create or replace function confia.touch_updated_at()
returns trigger
language plpgsql
security invoker
set search_path = pg_catalog, confia
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

create table if not exists confia.accounts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid unique references auth.users(id) on delete cascade,
  status text not null default 'test'
    check (status in ('test','active','suspended','closed')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

comment on table confia.accounts is
  'Conta lógica ConfIA.vc. user_id é opcional no POC e futuramente referencia o login compartilhado da plataforma.';

create table if not exists confia.installations (
  id uuid primary key default gen_random_uuid(),
  account_id uuid references confia.accounts(id) on delete cascade,
  platform text not null default 'android'
    check (platform in ('android','windows','other')),
  device_role text not null default 'self'
    check (device_role in ('self','child','employee','other')),
  device_label text,
  app_version text,
  environment text not null default 'test'
    check (environment in ('test','production')),
  android_ingest_token_hash text unique,
  last_seen_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (device_label is null or char_length(device_label) between 1 and 120),
  check (
    android_ingest_token_hash is null
    or android_ingest_token_hash ~ '^[0-9a-f]{64}$'
  )
);

create index if not exists confia_installations_account_idx
  on confia.installations(account_id, created_at);

create index if not exists confia_installations_last_seen_idx
  on confia.installations(last_seen_at desc)
  where last_seen_at is not null;

create table if not exists confia.browser_sources (
  id uuid primary key default gen_random_uuid(),
  installation_id uuid not null
    references confia.installations(id) on delete cascade,
  browser_key text not null,
  browser_family text not null
    check (browser_family in (
      'firefox','edge','chrome','brave','samsung','opera','mi','other'
    )),
  android_package text,
  source_type text not null default 'extension'
    check (source_type in ('extension','autofill','doh')),
  source_label text,
  ingest_token_hash text not null unique,
  status text not null default 'active'
    check (status in ('active','revoked','disabled')),
  paired_at timestamptz not null default now(),
  revoked_at timestamptz,
  last_seen_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (installation_id, browser_key, source_type),
  check (char_length(browser_key) between 2 and 80),
  check (android_package is null or char_length(android_package) between 3 and 160),
  check (source_label is null or char_length(source_label) between 1 and 120),
  check (ingest_token_hash ~ '^[0-9a-f]{64}$')
);

create index if not exists confia_browser_sources_installation_idx
  on confia.browser_sources(installation_id, status, browser_family);

create index if not exists confia_browser_sources_last_seen_idx
  on confia.browser_sources(last_seen_at desc)
  where last_seen_at is not null;

create table if not exists confia.browser_events (
  id uuid primary key default gen_random_uuid(),
  source_id uuid not null
    references confia.browser_sources(id) on delete cascade,
  client_event_id uuid not null,
  event_type text not null
    check (event_type in (
      'tab_active',
      'navigation_committed',
      'tab_updated',
      'tab_closed',
      'context_lost',
      'heartbeat'
    )),
  host text,
  scheme text
    check (scheme is null or scheme in ('http','https')),
  private_state text not null default 'unknown'
    check (private_state in ('normal','private','unknown')),
  event_at timestamptz not null,
  received_at timestamptz not null default now(),
  expires_at timestamptz not null default (now() + interval '30 days'),
  unique (source_id, client_event_id),
  check (
    host is null
    or (
      host = lower(host)
      and char_length(host) between 3 and 253
      and host like '%.%'
      and position('/' in host) = 0
      and position('?' in host) = 0
      and position('#' in host) = 0
      and position('@' in host) = 0
      and host !~ '[[:space:]]'
      and host ~ '^[a-z0-9][a-z0-9.-]*[a-z0-9]$'
      and position('..' in host) = 0
    )
  )
);

create index if not exists confia_browser_events_source_time_idx
  on confia.browser_events(source_id, event_at desc);

create index if not exists confia_browser_events_host_time_idx
  on confia.browser_events(host, event_at desc)
  where host is not null;

create index if not exists confia_browser_events_expiry_idx
  on confia.browser_events(expires_at);

create table if not exists confia.foreground_intervals (
  id uuid primary key default gen_random_uuid(),
  installation_id uuid not null
    references confia.installations(id) on delete cascade,
  client_interval_id uuid not null,
  browser_package text not null,
  started_at timestamptz not null,
  ended_at timestamptz not null,
  collected_at timestamptz not null default now(),
  expires_at timestamptz not null default (now() + interval '30 days'),
  unique (installation_id, client_interval_id),
  check (char_length(browser_package) between 3 and 160),
  check (ended_at > started_at)
);

create index if not exists confia_foreground_installation_time_idx
  on confia.foreground_intervals(
    installation_id,
    browser_package,
    started_at desc
  );

create index if not exists confia_foreground_expiry_idx
  on confia.foreground_intervals(expires_at);

create table if not exists confia.domain_sessions (
  id uuid primary key default gen_random_uuid(),
  installation_id uuid not null
    references confia.installations(id) on delete cascade,
  source_id uuid not null
    references confia.browser_sources(id) on delete cascade,
  browser_package text,
  host text not null,
  started_at timestamptz not null,
  ended_at timestamptz not null,
  duration_ms bigint not null check (duration_ms >= 0),
  private_state text not null default 'unknown'
    check (private_state in ('normal','private','unknown')),
  derivation text not null default 'extension_usage_intersection'
    check (derivation in (
      'extension_usage_intersection',
      'extension_only',
      'autofill_context',
      'doh_observed'
    )),
  created_at timestamptz not null default now(),
  expires_at timestamptz not null default (now() + interval '30 days'),
  check (ended_at >= started_at),
  check (
    host = lower(host)
    and char_length(host) between 3 and 253
    and host like '%.%'
    and position('/' in host) = 0
    and position('?' in host) = 0
    and position('#' in host) = 0
    and position('@' in host) = 0
    and host !~ '[[:space:]]'
    and host ~ '^[a-z0-9][a-z0-9.-]*[a-z0-9]$'
    and position('..' in host) = 0
  )
);

create index if not exists confia_domain_sessions_installation_time_idx
  on confia.domain_sessions(installation_id, started_at desc);

create index if not exists confia_domain_sessions_host_time_idx
  on confia.domain_sessions(host, started_at desc);

create index if not exists confia_domain_sessions_expiry_idx
  on confia.domain_sessions(expires_at);

drop trigger if exists confia_accounts_touch_updated_at on confia.accounts;
create trigger confia_accounts_touch_updated_at
before update on confia.accounts
for each row execute function confia.touch_updated_at();

drop trigger if exists confia_installations_touch_updated_at on confia.installations;
create trigger confia_installations_touch_updated_at
before update on confia.installations
for each row execute function confia.touch_updated_at();

drop trigger if exists confia_browser_sources_touch_updated_at on confia.browser_sources;
create trigger confia_browser_sources_touch_updated_at
before update on confia.browser_sources
for each row execute function confia.touch_updated_at();

create or replace function confia.bootstrap_test_pair(
  p_browser_key text,
  p_browser_family text,
  p_android_package text default null,
  p_device_label text default 'POC ConfIA.vc'
)
returns table (
  installation_id uuid,
  browser_source_id uuid,
  android_ingest_token text,
  browser_ingest_token text
)
language plpgsql
security definer
set search_path = pg_catalog, confia, extensions
as $$
declare
  v_installation_id uuid;
  v_source_id uuid;
  v_android_token text;
  v_browser_token text;
begin
  if p_browser_key is null or char_length(trim(p_browser_key)) < 2 then
    raise exception 'invalid_browser_key';
  end if;

  if p_browser_family not in (
    'firefox','edge','chrome','brave','samsung','opera','mi','other'
  ) then
    raise exception 'invalid_browser_family';
  end if;

  v_android_token :=
    'ig_a_' || encode(extensions.gen_random_bytes(32), 'hex');

  v_browser_token :=
    'ig_b_' || encode(extensions.gen_random_bytes(32), 'hex');

  insert into confia.installations (
    platform,
    device_role,
    device_label,
    environment,
    android_ingest_token_hash
  )
  values (
    'android',
    'self',
    nullif(trim(p_device_label), ''),
    'test',
    encode(extensions.digest(v_android_token, 'sha256'), 'hex')
  )
  returning id into v_installation_id;

  insert into confia.browser_sources (
    installation_id,
    browser_key,
    browser_family,
    android_package,
    source_type,
    source_label,
    ingest_token_hash,
    status
  )
  values (
    v_installation_id,
    lower(trim(p_browser_key)),
    p_browser_family,
    nullif(trim(p_android_package), ''),
    'extension',
    'POC extension',
    encode(extensions.digest(v_browser_token, 'sha256'), 'hex'),
    'active'
  )
  returning id into v_source_id;

  return query
  select
    v_installation_id,
    v_source_id,
    v_android_token,
    v_browser_token;
end;
$$;

create or replace function confia.purge_expired_telemetry()
returns table (
  browser_events_deleted bigint,
  foreground_intervals_deleted bigint,
  domain_sessions_deleted bigint
)
language plpgsql
security definer
set search_path = pg_catalog, confia
as $$
declare
  v_events bigint;
  v_intervals bigint;
  v_sessions bigint;
begin
  delete from confia.browser_events
  where expires_at < now();
  get diagnostics v_events = row_count;

  delete from confia.foreground_intervals
  where expires_at < now();
  get diagnostics v_intervals = row_count;

  delete from confia.domain_sessions
  where expires_at < now();
  get diagnostics v_sessions = row_count;

  return query
  select v_events, v_intervals, v_sessions;
end;
$$;

alter table confia.accounts enable row level security;
alter table confia.installations enable row level security;
alter table confia.browser_sources enable row level security;
alter table confia.browser_events enable row level security;
alter table confia.foreground_intervals enable row level security;
alter table confia.domain_sessions enable row level security;

revoke all on all tables in schema confia from public, anon, authenticated;
revoke all on all sequences in schema confia from public, anon, authenticated;
revoke execute on all functions in schema confia from public, anon, authenticated;

grant all on all tables in schema confia to service_role;
grant all on all sequences in schema confia to service_role;
grant execute on all functions in schema confia to service_role;

alter default privileges in schema confia
  revoke all on tables from public, anon, authenticated;
alter default privileges in schema confia
  revoke all on sequences from public, anon, authenticated;
alter default privileges in schema confia
  revoke execute on functions from public, anon, authenticated;

alter default privileges in schema confia
  grant all on tables to service_role;
alter default privileges in schema confia
  grant all on sequences to service_role;
alter default privileges in schema confia
  grant execute on functions to service_role;

commit;

notify pgrst, 'reload schema';

-- Depois de adicionar `confia` aos Exposed schemas, para gerar um par POC:
--
-- select *
-- from confia.bootstrap_test_pair(
--   'firefox_android',
--   'firefox',
--   'org.mozilla.firefox',
--   'Redmi POC'
-- );
--
-- O resultado devolve os dois tokens em texto puro UMA VEZ.
-- Não grave esses tokens em código-fonte/GitHub.
