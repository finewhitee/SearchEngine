package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResultItem;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.LemmaService;
import searchengine.services.SearchService;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final double MAX_LEMMA_FREQUENCY_PERCENT = 1.0;

    private final LemmaService lemmaService;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;

    @Override
    public SearchResponse search(String query, String siteUrl, Integer offset, Integer limit) {
        SearchResponse response = new SearchResponse();

        // 1) Валидация
        if (query == null || query.isBlank()) {
            response.setResult(false);
            response.setError("Задан пустой поисковый запрос");
            return response;
        }

        // 2) Определяем список сайтов для поиска
        List<Site> sites = new ArrayList<>();
        if (siteUrl != null) {
            Site site = siteRepository.findByUrl(siteUrl)
                    .orElse(null);
            if (site == null || pageRepository.countBySite(site) == 0) {
                response.setResult(false);
                response.setError("Сайт не проиндексирован: " + siteUrl);
                return response;
            }
            sites.add(site);
        } else {
            // все проиндексированные
            for (Site s : siteRepository.findAll()) {
                if (pageRepository.countBySite(s) > 0) {
                    sites.add(s);
                }
            }
            if (sites.isEmpty()) {
                response.setResult(false);
                response.setError("Нет проиндексированных сайтов для поиска");
                return response;
            }
        }

        // 3) Лемматизация запроса
        Map<String, Integer> queryLemmas = lemmaService.extractLemmas(query);
        log.info("Query lemmas: {}", queryLemmas.keySet());
        if (queryLemmas.isEmpty()) {
            // нечего искать
            response.setResult(true);
            response.setCount(0);
            response.setData(Collections.emptyList());
            return response;
        }

        // 4) Для каждого сайта собираем результаты
        List<SearchResultItem> allItems = new ArrayList<>();
        for (Site site : sites) {
            long totalPages = pageRepository.countBySite(site);

            // 4.1) Фильтрация «частых» лемм
            log.info("Site: {}, total pages: {}", site.getUrl(), totalPages);
            List<Lemma> filtered = queryLemmas.keySet().stream()
                    .map(lemmaStr -> {
                        var lemma = lemmaRepository.findByLemmaAndSite(lemmaStr, site).orElse(null);
                        log.info("Lemma '{}' for site {}: {}", lemmaStr, site.getUrl(), lemma);
                        return lemma;
                    })
                    .filter(Objects::nonNull)
                    .filter(l -> {
                        double freqPercent = (double) l.getFrequency() / totalPages;
                        log.info("Lemma '{}' frequency: {}, percent: {}, max: {}", l.getLemma(), l.getFrequency(), freqPercent, MAX_LEMMA_FREQUENCY_PERCENT);
                        // Временно отключаем фильтрацию для отладки
                        return true; // freqPercent <= MAX_LEMMA_FREQUENCY_PERCENT;
                    })
                    .sorted(Comparator.comparingInt(Lemma::getFrequency))
                    .toList();

            if (filtered.isEmpty()) {
                continue;
            }

            // 4.2) Поиск страниц: сначала по самой «редкой» лемме
            List<Page> pages = indexRepository.findByLemma(filtered.get(0)).stream()
                    .map(Index::getPage)
                    .filter(p -> p.getSite().equals(site))
                    .distinct()
                    .collect(Collectors.toList());

            // затем по каждой следующей — пересечение
            for (int i = 1; i < filtered.size() && !pages.isEmpty(); i++) {
                Lemma lem = filtered.get(i);
                Set<Page> pagesWithThis = indexRepository.findByLemma(lem).stream()
                        .map(Index::getPage)
                        .filter(p -> p.getSite().equals(site))
                        .collect(Collectors.toSet());
                pages.retainAll(pagesWithThis);
            }

            if (pages.isEmpty()) {
                continue;
            }

            // 4.3) Рассчитываем абсолютную релевантность
            Map<Page, Double> absRelMap = new HashMap<>();
            double maxAbs = 0;
            for (Page page : pages) {
                double sumRank = 0;
                for (Lemma lem : filtered) {
                    for (Index idx : indexRepository.findByLemmaAndPage(lem, page)) {
                        sumRank += idx.getRank();
                    }
                }
                absRelMap.put(page, sumRank);
                if (sumRank > maxAbs) {
                    maxAbs = sumRank;
                }
            }

            // 4.4) Формируем SearchResultItem
            for (Page page : pages) {
                double absRel = absRelMap.get(page);
                float rel = maxAbs > 0 ? (float)(absRel / maxAbs) : 0f;

                // извлекаем title
                Document doc = Jsoup.parse(page.getContent());
                String title = doc.title();

                // формируем сниппет
                String snippet = generateSnippet(page, new ArrayList<>(queryLemmas.keySet()), 150);

                allItems.add(new SearchResultItem(
                        site.getUrl(),
                        site.getName(),
                        page.getPath(),
                        title,
                        snippet,
                        rel
                ));
            }
        }

        // 5) Сортировка по убыванию релевантности
        allItems.sort(Comparator.comparingDouble(SearchResultItem::getRelevance).reversed());

        // 6) Постраничная выдача
        int total = allItems.size();
        int from = Math.min(Math.max(offset, 0), total);
        int to = Math.min(from + (limit != null ? limit : 20), total);
        List<SearchResultItem> pageItems = allItems.subList(from, to);

        response.setResult(true);
        response.setCount(total);
        response.setData(pageItems);
        return response;
    }

    /**
     * Генерирует сниппет длиной ~snippetLen символов, выделяя <b>жирным</b> все вхождения queryWords.
     */
    private String generateSnippet(Page page, List<String> queryWords, int snippetLen) {
        String text = Jsoup.parse(page.getContent()).text();
        String lower = text.toLowerCase();
        int idx = -1;
        for (String w : queryWords) {
            int p = lower.indexOf(w.toLowerCase());
            if (p >= 0 && (idx < 0 || p < idx)) {
                idx = p;
            }
        }
        if (idx < 0) {
            // нет прямых вхождений — берём начало
            idx = 0;
        }
        int start = Math.max(0, idx - snippetLen / 2);
        int end = Math.min(text.length(), start + snippetLen);
        String snippet = text.substring(start, end).trim();

        // завершаем обрезку на слове
        if (end < text.length() && Character.isLetterOrDigit(text.charAt(end))) {
            int cut = snippet.lastIndexOf(' ');
            if (cut > 0) snippet = snippet.substring(0, cut);
        }

        // отладочная информация
        log.debug("Original snippet: {}", snippet);
        log.debug("Query words: {}", queryWords);

        // выделяем теги <b>
        for (String w : queryWords) {
            if (w.isBlank()) continue;
            String before = snippet;
            snippet = snippet.replaceAll("(?i)\\b" + Pattern.quote(w) + "\\b", "<b>$0</b>");
            log.debug("Word '{}': before='{}', after='{}'", w, before, snippet);
        }
        return snippet;
    }
}
