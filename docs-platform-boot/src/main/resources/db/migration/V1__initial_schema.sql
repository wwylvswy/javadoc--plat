-- 文档库表
CREATE TABLE IF NOT EXISTS libraries (
                                         id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                         name        VARCHAR(128) NOT NULL,
                                         created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
                                         updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_libraries_name_lower ON libraries(LOWER(name));

-- 版本表
CREATE TABLE IF NOT EXISTS versions (
                                        id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                                        library_id          INTEGER NOT NULL,
                                        name                VARCHAR(64) NOT NULL DEFAULT '',
                                        status              VARCHAR(32) NOT NULL DEFAULT 'NOT_INDEXED',
                                        progress_pages      INTEGER NOT NULL DEFAULT 0,
                                        progress_max_pages  INTEGER NOT NULL DEFAULT 0,
                                        error_message       TEXT,
                                        source_url          TEXT,
                                        scraper_options     TEXT,
                                        started_at          DATETIME,
                                        created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
                                        updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
                                        CONSTRAINT fk_versions_library FOREIGN KEY (library_id) REFERENCES libraries(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_versions_library_name_lower ON versions(library_id, LOWER(name));
CREATE INDEX IF NOT EXISTS idx_versions_status ON versions(status);
CREATE INDEX IF NOT EXISTS idx_versions_source_url ON versions(source_url);

-- 页面表
CREATE TABLE IF NOT EXISTS pages (
                                     id            INTEGER PRIMARY KEY AUTOINCREMENT,
                                     version_id    INTEGER NOT NULL,
                                     url           TEXT NOT NULL,
                                     title         TEXT,
                                     etag          TEXT,
                                     last_modified TEXT,
                                     content_type  TEXT,
                                     depth         INTEGER DEFAULT 0,
                                     created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
                                     updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
                                     CONSTRAINT fk_pages_version FOREIGN KEY (version_id) REFERENCES versions(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_pages_version_url ON pages(version_id, url);
CREATE INDEX IF NOT EXISTS idx_pages_version_id ON pages(version_id);
CREATE INDEX IF NOT EXISTS idx_pages_etag ON pages(etag);

-- 分片表
CREATE TABLE IF NOT EXISTS documents (
                                         id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                         page_id     INTEGER NOT NULL,
                                         content     TEXT,
                                         metadata    TEXT,
                                         sort_order  INTEGER NOT NULL,
                                         created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
                                         CONSTRAINT fk_documents_page FOREIGN KEY (page_id) REFERENCES pages(id)
);

CREATE INDEX IF NOT EXISTS idx_documents_page_id ON documents(page_id);
CREATE INDEX IF NOT EXISTS idx_documents_page_sort ON documents(page_id, sort_order);

-- FTS 全文索引表
CREATE VIRTUAL TABLE IF NOT EXISTS documents_fts USING fts5(
    content,
    title,
    url,
    path,
    tokenize = 'porter unicode61'
);

CREATE TRIGGER IF NOT EXISTS documents_fts_after_delete
    AFTER DELETE ON documents
BEGIN
    DELETE FROM documents_fts WHERE rowid = OLD.id;
END;

CREATE TRIGGER IF NOT EXISTS documents_fts_after_update
    AFTER UPDATE ON documents
BEGIN
    DELETE FROM documents_fts WHERE rowid = OLD.id;
    INSERT INTO documents_fts(rowid, content, title, url, path)
    SELECT NEW.id, NEW.content, p.title, p.url, json_extract(NEW.metadata, '$.path')
    FROM pages p
    WHERE p.id = NEW.page_id;
END;

CREATE TRIGGER IF NOT EXISTS documents_fts_after_insert
    AFTER INSERT ON documents
BEGIN
    INSERT INTO documents_fts(rowid, content, title, url, path)
    SELECT NEW.id, NEW.content, p.title, p.url, json_extract(NEW.metadata, '$.path')
    FROM pages p
    WHERE p.id = NEW.page_id;
END;

-- 任务表
CREATE TABLE IF NOT EXISTS jobs (
                                    id                VARCHAR(64) PRIMARY KEY,
                                    library_id        INTEGER,
                                    version_id        INTEGER,
                                    status            VARCHAR(32) NOT NULL,
                                    progress_pages    INTEGER NOT NULL DEFAULT 0,
                                    progress_max_pages INTEGER NOT NULL DEFAULT 0,
                                    error_message     TEXT,
                                    source_url        TEXT,
                                    scraper_options   TEXT,
                                    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
                                    started_at        DATETIME,
                                    finished_at       DATETIME,
                                    updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
                                    CONSTRAINT fk_jobs_library FOREIGN KEY (library_id) REFERENCES libraries(id),
                                    CONSTRAINT fk_jobs_version FOREIGN KEY (version_id) REFERENCES versions(id)
);

CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status);
CREATE INDEX IF NOT EXISTS idx_jobs_library_version ON jobs(library_id, version_id);

-- 时间戳触发器
CREATE TRIGGER IF NOT EXISTS libraries_updated_at
    AFTER UPDATE ON libraries
BEGIN
    UPDATE libraries SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

CREATE TRIGGER IF NOT EXISTS versions_updated_at
    AFTER UPDATE ON versions
BEGIN
    UPDATE versions SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

CREATE TRIGGER IF NOT EXISTS pages_updated_at
    AFTER UPDATE ON pages
BEGIN
    UPDATE pages SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

CREATE TRIGGER IF NOT EXISTS jobs_updated_at
    AFTER UPDATE ON jobs
BEGIN
    UPDATE jobs SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
