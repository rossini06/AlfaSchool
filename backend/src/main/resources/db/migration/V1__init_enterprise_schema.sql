CREATE TABLE IF NOT EXISTS tenants (
    id           CHAR(36)     NOT NULL,
    tenant_id    CHAR(36)     NOT NULL,
    name         VARCHAR(160) NOT NULL,
    document     VARCHAR(40)  NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    created_by   CHAR(36),
    updated_by   CHAR(36),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_tenants_tenant_id UNIQUE (tenant_id),
    CONSTRAINT uk_tenants_document  UNIQUE (document)
);

CREATE TABLE IF NOT EXISTS units (
    id         CHAR(36)     NOT NULL,
    tenant_id  CHAR(36)     NOT NULL,
    name       VARCHAR(160) NOT NULL,
    address    VARCHAR(255),
    city       VARCHAR(120),
    state      VARCHAR(2),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    created_by CHAR(36),
    updated_by CHAR(36),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_units_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id)
);

CREATE TABLE IF NOT EXISTS users (
    id                   CHAR(36)     NOT NULL,
    tenant_id            CHAR(36)     NOT NULL,
    unit_id              CHAR(36),
    name                 VARCHAR(120) NOT NULL,
    email                VARCHAR(160) NOT NULL,
    password             VARCHAR(255) NOT NULL,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    locked               BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_attempts      INT          NOT NULL DEFAULT 0,
    last_login           DATETIME(6),
    must_change_password BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    created_by           CHAR(36),
    updated_by           CHAR(36),
    deleted              BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_users_unit   FOREIGN KEY (unit_id)   REFERENCES units (id),
    CONSTRAINT uk_users_tenant_email UNIQUE (tenant_id, email)
);

CREATE TABLE IF NOT EXISTS roles (
    id          CHAR(36)     NOT NULL,
    tenant_id   CHAR(36)     NOT NULL,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255),
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  CHAR(36),
    updated_by  CHAR(36),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id)
);

CREATE TABLE IF NOT EXISTS permissions (
    id          CHAR(36)     NOT NULL,
    tenant_id   CHAR(36)     NOT NULL,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255),
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  CHAR(36),
    updated_by  CHAR(36),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_permissions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       CHAR(36) NOT NULL,
    permission_id CHAR(36) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role       FOREIGN KEY (role_id)       REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id         CHAR(36)     NOT NULL,
    tenant_id  CHAR(36)     NOT NULL,
    user_id    CHAR(36),
    action     VARCHAR(120) NOT NULL,
    entity     VARCHAR(120) NOT NULL,
    entity_id  CHAR(36),
    timestamp  DATETIME(6)  NOT NULL,
    ip_address VARCHAR(64),
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    created_by CHAR(36),
    updated_by CHAR(36),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id)
);

-- Indexes
CREATE INDEX idx_tenants_tenant_id  ON tenants (tenant_id);
CREATE INDEX idx_tenants_created_at ON tenants (created_at);

CREATE INDEX idx_units_tenant_id  ON units (tenant_id);
CREATE INDEX idx_units_created_at ON units (created_at);

CREATE INDEX idx_users_tenant_id  ON users (tenant_id);
CREATE INDEX idx_users_unit_id    ON users (unit_id);
CREATE INDEX idx_users_email      ON users (email);
CREATE INDEX idx_users_created_at ON users (created_at);

CREATE INDEX idx_roles_tenant_id  ON roles (tenant_id);
CREATE INDEX idx_roles_created_at ON roles (created_at);

CREATE INDEX idx_permissions_tenant_id  ON permissions (tenant_id);
CREATE INDEX idx_permissions_created_at ON permissions (created_at);

CREATE INDEX idx_role_permissions_role_id       ON role_permissions (role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions (permission_id);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

CREATE INDEX idx_audit_logs_tenant_id  ON audit_logs (tenant_id);
CREATE INDEX idx_audit_logs_user_id    ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
