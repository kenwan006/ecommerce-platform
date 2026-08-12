ALTER TABLE users
  ADD COLUMN oauth_provider VARCHAR(50) NULL,
  ADD COLUMN oauth_provider_subject VARCHAR(255) NULL,
  ADD CONSTRAINT uk_users_oauth_identity UNIQUE (oauth_provider, oauth_provider_subject);
