-- RF=1 is fine for local dev
CREATE KEYSPACE IF NOT EXISTS spring_ai
  WITH REPLICATION = { 'class': 'SimpleStrategy', 'replication_factor': 1 };
