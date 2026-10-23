CREATE TABLE short_links (
    id UUID PRIMARY KEY,
    code VARCHAR(12) NOT NULL UNIQUE,
    original_url TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE link_clicks (
    id BIGSERIAL PRIMARY KEY,
    link_id UUID NOT NULL REFERENCES short_links(id) ON DELETE CASCADE,
    clicked_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_clicks_link_time ON link_clicks(link_id, clicked_at DESC);
