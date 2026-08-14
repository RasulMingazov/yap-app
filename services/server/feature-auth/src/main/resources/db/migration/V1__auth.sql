create table users (
    id uuid primary key,
    created_at timestamptz not null default now()
);

create table provider_identities (
    id uuid primary key,
    user_id uuid not null references users (id) on delete cascade,
    provider text not null,
    provider_user_id text not null,
    email text,
    display_name text,
    avatar_url text,
    created_at timestamptz not null default now(),
    -- This constraint, not application code, is what makes one provider account resolve to one Yap
    -- account: two concurrent first logins race on the insert and the loser retries the lookup.
    constraint provider_identities_provider_user_unique unique (provider, provider_user_id)
);

-- email, display_name, and avatar_url carry no unique constraint and no index on purpose: nothing
-- ever queries by them, which is what keeps two providers reporting the same address independent.
create index provider_identities_user_id_idx on provider_identities (user_id);

create table sessions (
    id uuid primary key,
    user_id uuid not null references users (id) on delete cascade,
    refresh_token_hash text not null,
    expires_at timestamptz not null,
    created_at timestamptz not null default now(),
    rotated_at timestamptz
);

create index sessions_user_id_idx on sessions (user_id);
