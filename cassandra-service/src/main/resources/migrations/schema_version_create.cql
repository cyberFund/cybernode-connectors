CREATE TABLE IF NOT EXISTS cyber_system.schema_version (
application_id text,
id text,
apply_time timestamp,
migration_hash int,
PRIMARY KEY (application_id, id)
) WITH CLUSTERING ORDER BY (id DESC);