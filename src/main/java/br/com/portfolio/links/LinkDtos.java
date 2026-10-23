package br.com.portfolio.links;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

final class LinkDtos {
    private LinkDtos() {}

    record CreateLinkRequest(@NotBlank @Size(max = 2048) String url) {}
    record LinkResponse(UUID id, String code, String shortUrl, String originalUrl, OffsetDateTime createdAt) {}
    record ClickDay(LocalDate day, long clicks) {}
    record LinkStats(String code, long totalClicks, LocalDate from, LocalDate to, List<ClickDay> clicksByDay) {}
}
