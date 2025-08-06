package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.IndexingStatusResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.exeption.UnknownIndexingStatusException;
import searchengine.model.Site;
import searchengine.repository.SiteRepository;
import searchengine.services.CrawlerService;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;
import searchengine.services.impl.CrawlerServiceImpl;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class ApiController {

    @Resource
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SitesList sitesList;
    private final CrawlerService crawlerService;
    private final SiteRepository siteRepository;
    private final SearchService searchService;




    /**
     * Запуск полной индексации (или переиндексации) всех сайтов.
     * Если уже идёт процесс, возвращает ошибку.
     */
    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String,Object>> startIndexing() {
        // Проверяем, не запущена ли уже индексация
        if (indexingService.isIndexing()) {
            log.warn("Попытка запустить индексирование, но оно уже выполняется");
            Map<String,Object> error = new HashMap<>();
            error.put("result", false);
            error.put("error", "Индексация уже запущена");
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)  // 409 Conflict
                    .body(error);
        }

        // Запускаем индексацию
        log.info("Запуск полной индексации сайтов по HTTP-запросу");
        indexingService.startIndexing();

        // Возвращаем успешный ответ
        Map<String,Object> success = new HashMap<>();
        success.put("result", true);
        return ResponseEntity.ok(success);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String,Object>> stopIndexing() {
        if (!indexingService.isIndexing()) {
            log.warn("StopIndexing called, but no indexing is in progress");
            Map<String,Object> error = new HashMap<>();
            error.put("result", false);
            error.put("error", "Индексация не запущена");
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(error);
        }

        log.info("StopIndexing called: stopping all crawling threads");
        indexingService.stopIndexing();

        Map<String,Object> success = new HashMap<>();
        success.put("result", true);
        return ResponseEntity.ok(success);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Map<String,Object>> indexSinglePage(
            @RequestParam("url") String pageUrl) {
        // 1. ищем подходящий префикс из конфига
        var optConfig = sitesList.getSites().stream()
                .filter(cfg -> pageUrl.startsWith(cfg.getUrl()))
                .findFirst();

        if (optConfig.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "result", false,
                            "error",
                            "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"
                    ));
        }

        // 2. подгружаем сущность Site из БД (url в ней должен совпадать с cfg.getUrl())
        String siteBaseUrl = optConfig.get().getUrl();
        var optSite = siteRepository.findByUrl(siteBaseUrl);
        if (optSite.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "result", false,
                            "error", "Конфигурированный сайт не найден в базе"
                    ));
        }
        Site site = optSite.get();

        // 3. индексируем
        try {
            indexingService.indexPage(pageUrl, site);
            return ResponseEntity.ok(Map.of("result", true));
        } catch (RuntimeException ex) {
            // здесь можно различать типы ошибок (например, Jsoup, БД и т.п.)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "result", false,
                            "error", ex.getMessage()
                    ));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "site", required = false) String site,
            @RequestParam(name = "offset", defaultValue = "0") Integer offset,
            @RequestParam(name = "limit", defaultValue = "20") Integer limit
    ) {
        // Валидация
        if (query == null || query.isBlank()) {
            SearchResponse err = new SearchResponse();
            err.setResult(false);
            err.setError("Задан пустой поисковый запрос");
            return ResponseEntity.badRequest().body(err);
        }
        // Делегируем в сервис
        SearchResponse resp = searchService.search(query.trim(), site, offset, limit);
        return resp.isResult()
                ? ResponseEntity.ok(resp)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }


}
