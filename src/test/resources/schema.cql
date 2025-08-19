
-- Keyspace, RF=1 is fine for local dev

CREATE KEYSPACE IF NOT EXISTS spring_ai
WITH REPLICATION = { 'class': 'SimpleStrategy', 'replication_factor': 1 };

USE spring_ai;

-- chat_tables.cql

CREATE TABLE IF NOT EXISTS ai_chat_message (
      session_id         text,        -- chat id
      msg_timestamp  timestamp,
      msg_type           text,
      msg_content        text,
      PRIMARY KEY ((session_id), msg_timestamp)
  ) WITH CLUSTERING ORDER BY (msg_timestamp DESC);


CREATE TABLE IF NOT EXISTS ai_chat_memory (
    session_id   text PRIMARY KEY,
    session_name text,
    created_at   timestamp
);

-- used for ordering chats and fetching them in DESC order

CREATE TABLE IF NOT EXISTS chats_by_created (
    bucket        text,        -- constant (e.g. 'all') or sharded bucket
    created_at    timeuuid,    -- time-based UUID (Uuids.timeBased()), supports ordering + pagination
    session_id    text,        -- chat id
    session_name  text,
    PRIMARY KEY ((bucket), created_at, session_id)
) WITH CLUSTERING ORDER BY (created_at DESC);