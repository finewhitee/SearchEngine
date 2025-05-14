package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.IndexingStatusResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.exeption.UnknownIndexingStatusException;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import javax.annotation.Resource;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Resource
    private final StatisticsService statisticsService;

    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingStatusResponse> startIndexing() throws IOException {
        IndexingStatusResponse status = indexingService.getIndexingStatus();
        if (status.getResult()) {
            return ResponseEntity.ok(status);
        }
        throw new UnknownIndexingStatusException("Неизвестная ошибка индексирования");
    }
}
