package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.StatisticsService;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        // Получаем все сайты из БД
        List<Site> sites = siteRepository.findAll();

        // Формируем общую статистику
        int totalSites = sites.size();
        int totalPages = (int) pageRepository.count();
        int totalLemmas = (int) lemmaRepository.count();
        boolean indexingInProgress = sites.stream()
                .anyMatch(site -> site.getStatus() == SiteStatus.INDEXING);

        TotalStatistics total = new TotalStatistics();
        total.setSites(totalSites);
        total.setPages(totalPages);
        total.setLemmas(totalLemmas);
        total.setIndexing(indexingInProgress);

        // Формируем детальную статистику по каждому сайту
        List<DetailedStatisticsItem> detailed = sites.stream().map(site -> {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setUrl(site.getUrl());
            item.setName(site.getName());
            item.setStatus(site.getStatus().name());
            // Переводим LocalDateTime в UNIX-время (секунды с 1970-01-01 UTC)
            item.setStatusTime(site.getStatusTime().toInstant(ZoneOffset.UTC).getEpochSecond());

            // Строку error добавляем только если есть текст ошибки
            if (site.getLastError() != null && !site.getLastError().isBlank()) {
                item.setError(site.getLastError());
            }

            // Считаем количество страниц и лемм для данного сайта
            item.setPages(pageRepository.countBySiteId(site.getId()));
            item.setLemmas(lemmaRepository.countBySiteId(site.getId()));
            return item;
        }).collect(Collectors.toList());

        // Собираем ответ
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);

        StatisticsResponse response = new StatisticsResponse();
        response.setResult(true);
        response.setStatistics(data);

        return response;
    }
}