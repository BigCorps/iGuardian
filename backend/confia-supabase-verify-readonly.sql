-- ConfIA.vc — verificação SOMENTE LEITURA após aplicar a fundação.

select table_schema, table_name
from information_schema.tables
where table_schema = 'confia'
order by table_name;

select n.nspname as schema_name, c.relname as table_name, c.relrowsecurity as rls_enabled
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'confia' and c.relkind = 'r'
order by c.relname;

select grantee, table_schema, table_name, privilege_type
from information_schema.role_table_grants
where table_schema = 'confia'
  and grantee in ('anon','authenticated')
order by grantee, table_name, privilege_type;
-- Esperado: ZERO LINHAS.

select grantee, table_name, privilege_type
from information_schema.role_table_grants
where table_schema = 'confia'
  and grantee = 'service_role'
order by table_name, privilege_type;

select table_name, column_name
from information_schema.columns
where table_schema = 'confia'
  and lower(column_name) in (
    'url','full_url','path','query','fragment','title',
    'html','content','password','text_input'
  );
-- Esperado: ZERO LINHAS.

select
  (select count(*) from confia.accounts) as accounts,
  (select count(*) from confia.installations) as installations,
  (select count(*) from confia.browser_sources) as browser_sources,
  (select count(*) from confia.browser_events) as browser_events,
  (select count(*) from confia.foreground_intervals) as foreground_intervals,
  (select count(*) from confia.domain_sessions) as domain_sessions;
