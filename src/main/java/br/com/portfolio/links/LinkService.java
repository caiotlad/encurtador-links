package br.com.portfolio.links;

import static br.com.portfolio.links.LinkDtos.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class LinkService {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final String baseUrl;

    LinkService(JdbcTemplate jdbc, @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.jdbc = jdbc;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Transactional
    LinkResponse create(CreateLinkRequest request) {
        String destination = normalizeHttpUrl(request.url());
        UUID id = UUID.randomUUID();
        for (int attempt = 0; attempt < 8; attempt++) {
            String code = randomCode(8);
            int inserted = jdbc.update("""
                    INSERT INTO short_links(id, code, original_url) VALUES (?, ?, ?)
                    ON CONFLICT (code) DO NOTHING
                    """, id, code, destination);
            if (inserted == 1) {
                OffsetDateTime createdAt = jdbc.queryForObject("SELECT created_at FROM short_links WHERE id = ?",
                        (rs, row) -> rs.getObject(1, OffsetDateTime.class), id);
                return new LinkResponse(id, code, baseUrl + "/r/" + code, destination, createdAt);
            }
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível gerar um código único; tente novamente");
    }

    @Transactional
    String registerClickAndGetDestination(String code) {
        List<LinkRow> links = jdbc.query("SELECT id, original_url FROM short_links WHERE code = ?",
                (rs, row) -> new LinkRow(rs.getObject("id", UUID.class), rs.getString("original_url")), code);
        if (links.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Link não encontrado");
        LinkRow link = links.get(0);
        jdbc.update("INSERT INTO link_clicks(link_id) VALUES (?)", link.id());
        return link.originalUrl();
    }

    LinkStats stats(String code, LocalDate from, LocalDate to) {
        UUID linkId = jdbc.query("SELECT id FROM short_links WHERE code = ?", rs -> rs.next() ? rs.getObject(1, UUID.class) : null, code);
        if (linkId == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Link não encontrado");
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from não pode ser posterior a to");
        }
        LocalDate effectiveFrom = from == null ? LocalDate.of(1970, 1, 1) : from;
        LocalDate effectiveTo = to == null ? LocalDate.now(ZoneOffset.UTC) : to;
        OffsetDateTime start = effectiveFrom.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = effectiveTo.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        Long total = jdbc.queryForObject("SELECT count(*) FROM link_clicks WHERE link_id = ? AND clicked_at >= ? AND clicked_at < ?",
                Long.class, linkId, start, end);
        List<ClickDay> daily = jdbc.query("""
                SELECT (clicked_at AT TIME ZONE 'UTC')::date AS day, count(*) AS clicks
                FROM link_clicks WHERE link_id = ? AND clicked_at >= ? AND clicked_at < ?
                GROUP BY day ORDER BY day
                """, (rs, row) -> new ClickDay(rs.getObject("day", LocalDate.class), rs.getLong("clicks")), linkId, start, end);
        return new LinkStats(code, total == null ? 0 : total, from, to, daily);
    }

    private String normalizeHttpUrl(String value) {
        try {
            URI uri = new URI(value.trim()).normalize();
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if ((!scheme.equals("http") && !scheme.equals("https")) || uri.getHost() == null || uri.getRawUserInfo() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A URL precisa usar http:// ou https://, ter host e não conter credenciais");
            }
            return uri.toASCIIString();
        } catch (URISyntaxException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL inválida");
        }
    }

    private String randomCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        return code.toString();
    }

    private record LinkRow(UUID id, String originalUrl) {}
}
