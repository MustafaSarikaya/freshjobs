-- Saved views and seen listings now live in the visitor's HTTP session, so the
-- per-user tables go away. auth_user stays: it holds the admin accounts.
DROP TABLE IF EXISTS core_seenlisting;
DROP TABLE IF EXISTS core_savedview;
