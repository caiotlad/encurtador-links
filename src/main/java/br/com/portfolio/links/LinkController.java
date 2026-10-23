package br.com.portfolio.links;

import static br.com.portfolio.links.LinkDtos.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
class LinkController {
    private final LinkService service;

    LinkController(LinkService service) {
        this.service = service;
    }

    @PostMapping("/api/links")
    ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        LinkResponse created = service.create(request);
        return ResponseEntity.created(URI.create(created.shortUrl())).body(created);
    }

    @GetMapping("/r/{code}")
    ResponseEntity<Void> redirect(@PathVariable String code) {
        String destination = service.registerClickAndGetDestination(code);
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, destination).build();
    }

    @GetMapping("/api/links/{code}/stats")
    LinkStats stats(@PathVariable String code,
                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.stats(code, from, to);
    }
}
