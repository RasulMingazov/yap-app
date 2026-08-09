-- Authentication schema: challenges, accounts, provider identities, and sessions.
-- Every timestamp is stored in UTC.

CREATE TABLE auth_challenge (
    id uuid PRIMARY KEY,
    provider text NOT NULL,
    -- SHA-256 of the issued nonce; the raw value is disclosed to the client exactly once.
    nonce_hash text,
    -- The base64url S256 code challenge supplied verbatim by a PKCE client.
    proof text,
    created_at timestamp NOT NULL,
    expires_at timestamp NOT NULL
);

CREATE INDEX auth_challenge_expires_at_idx ON auth_challenge (expires_at);

-- Product-owned fields only: everything a provider owns lives in auth_provider_identity.
CREATE TABLE auth_account (
    id uuid PRIMARY KEY,
    created_at timestamp NOT NULL
);

CREATE TABLE auth_provider_identity (
    id uuid PRIMARY KEY,
    account_id uuid NOT NULL REFERENCES auth_account (id),
    provider text NOT NULL,
    subject text NOT NULL,
    email text,
    is_email_verified boolean,
    created_at timestamp NOT NULL,
    last_login_at timestamp NOT NULL,
    -- Provider plus stable subject is the only identity lookup key; email never resolves an account.
    CONSTRAINT auth_provider_identity_provider_subject_key UNIQUE (provider, subject)
);

CREATE TABLE auth_session (
    id uuid PRIMARY KEY,
    account_id uuid NOT NULL REFERENCES auth_account (id),
    -- Hashes only: a refresh credential is never stored in a form that could be presented.
    refresh_token_hash text NOT NULL,
    previous_token_hash text,
    created_at timestamp NOT NULL,
    last_used_at timestamp NOT NULL,
    absolute_expires_at timestamp NOT NULL,
    revoked_at timestamp
);

CREATE INDEX auth_session_account_id_idx ON auth_session (account_id);
