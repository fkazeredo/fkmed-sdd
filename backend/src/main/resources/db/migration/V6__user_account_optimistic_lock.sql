-- Débito técnico A (SPEC-0002 BR8 under concurrency): optimistic lock on user_account.
-- Concurrent failed-login increments were lost-updating each other (a stale read overwriting a
-- committed one), so N simultaneous wrong passwords could leave failed_attempts below the lock
-- threshold and the account never locked. @Version turns the collision into an optimistic-lock
-- failure that the application retries on a fresh read (DL-0005), preserving every increment.
alter table user_account add column version bigint not null default 0;
