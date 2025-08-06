package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Index;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;


import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис запускает индексацию сайтов из конфига или по одному URL
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexingService {
    private final CrawlerServiceImpl crawlerService;         // отвечает за обход ссылок
    private final LemmaServiceImpl lemmaService;             // отвечает за извлечение лемм
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;                   // настройки из application.yml

    // Флаг, разрешающий остановку индексации
    private volatile boolean indexing = false;

    /**
     * Запускает полную индексацию всех сайтов из конфига
     */
    @Override
    @Transactional
    public void startIndexing() {
        indexing = true;
        log.info("Start indexing for all configured sites (count={})", sitesList.getSites().size());

        for (SiteConfig cfg : sitesList.getSites()) {
            String url = cfg.getUrl();
            String name = cfg.getName();
            Site site = null;
            try {
                log.info("Preparing to index site '{}' with name '{}'", url, name);

                // 1) Удаляем из БД все данные по сайту (pages, lemmas, индексы и сам site)
                siteRepository.findByUrl(url).ifPresent(existing -> {
                    log.debug("Clearing existing data for site id={}, url={}", existing.getId(), url);
                    pageRepository.deleteAllBySite(existing);
                    lemmaRepository.deleteAllBySite(existing);
                    indexRepository.deleteAllByPage_Site(existing);
                    siteRepository.delete(existing);
                });

                // 2) Создаём новую запись в таблице site со статусом INDEXING
                site = Site.builder()
                        .url(url)
                        .name(name)
                        .status(SiteStatus.INDEXING)
                        .statusTime(LocalDateTime.now())
                        .lastError(null)
                        .build();
                site = siteRepository.save(site);
                log.info("Site record created (id={}), status INDEXING", site.getId());

                // 3) Краулим все страницы: для каждой вызываем indexPage(...)
                Site finalSite = site;
                crawlerService.crawlSite(url,
                        pageUrl -> {
                            // индексируем каждую страницу
                            indexPage(pageUrl, finalSite);
                        },
                        () -> indexing  // проверка флага на остановку
                );

                // 4) После обхода обновляем статус на INDEXED
                site.setStatus(SiteStatus.INDEXED);
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
                log.info("Indexing completed successfully for site id={}", site.getId());

            } catch (Exception ex) {
                // При любой ошибке меняем статус на FAILED и сохраняем текст ошибки
                log.error("Indexing failed for site '{}': {}", url, ex.getMessage(), ex);
                if (site != null) {
                    site.setStatus(SiteStatus.FAILED);
                    site.setLastError(ex.getMessage());
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                }
            }
        }

        indexing = false;
        log.info("Finished indexing all sites");
    }

    /**
     * Останавливает текущую индексацию (прерывает crawlSite через флаг)
     */
    @Override
    @Transactional
    public void stopIndexing() {
        indexing = false;
        log.info("Indexing stop requested");

        List<Site> running = siteRepository.findByStatus(SiteStatus.INDEXING);
        for (Site site : running) {
            site.setStatus(SiteStatus.FAILED);
            site.setLastError("Индексация остановлена пользователем");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
            log.info("Site id={} marked FAILED because indexing was stopped", site.getId());
        }
    }

    /**
     * Возвращает, идёт ли сейчас индексирование
     */
    @Override
    public boolean isIndexing() {
        return indexing;
    }

    /**
     * Индексирует один сайт по его URL (берем конфиг, затем почти тот же процесс что в startIndexing)
     */
    @Override
    public void indexSite(String url) {
        // Находим SiteConfig по URL в списке настроек
        SiteConfig cfg = sitesList.getSites().stream()
                .filter(s -> s.getUrl().equals(url))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown site: " + url));

        log.info("Indexing single site: {}", url);
        // Вызов общей логики, но только для одного сайта
        startIndexing();  // можно вынести в приватный метод doIndexSingleSite(cfg)
    }

    /**
     * Индексирует одну страницу:
     * - сохраняет Page,
     * - извлекает леммы и увеличивает их частоту,
     * - сохраняет связку Page↔Lemma в Index,
     * - обновляет status_time в таблице site.
     */
    @Override
    @Transactional
    public void indexPage(String pageUrl, Site site) {
        log.debug("Indexing page '{}' for site id={}", pageUrl, site.getId());
        try {
            // Загружаем документ через Jsoup
            Document doc = Jsoup.connect(pageUrl)
                    .userAgent(CrawlerServiceImpl.USER_AGENT)
                    .timeout(CrawlerServiceImpl.TIMEOUT_MS)
                    .get();

            // Сохраняем или обновляем запись Page
            Page page = Page.builder()
                    .site(site)
                    .path(new URL(pageUrl).getPath())
                    .code(doc.connection().response().statusCode())
                    .content(doc.html())
                    .build();
            page = pageRepository.save(page);
            log.trace("Saved Page id={} path={}", page.getId(), page.getPath());

            // Извлекаем леммы и сохраняем их частоты
            Page finalPage = page;
            String pageText = doc.text();
            lemmaService.extractLemmas(pageText).forEach((lemmaStr, freq) -> {
                // 1) Находим или создаём Lemma
                Lemma lemma = lemmaRepository
                        .findByLemmaAndSite(lemmaStr, site)
                        .orElseGet(() -> {
                            log.trace("Creating new Lemma '{}' for site id={}", lemmaStr, site.getId());
                            return lemmaRepository.save(
                                    Lemma.builder()
                                            .site(site)
                                            .lemma(lemmaStr)
                                            .frequency(0)
                                            .build()
                            );
                        });

                // 2) Обновляем общую частоту леммы по сайту
                lemma.setFrequency(lemma.getFrequency() + 1);
                lemmaRepository.save(lemma);
                log.trace("Updated Lemma id={} freq={}", lemma.getId(), lemma.getFrequency());

                // 3) Сохраняем запись в индекс (связь page–lemma)
                indexRepository.save(Index.builder()
                        .page(finalPage)
                        .lemma(lemma)
                        .rank((float) freq)
                        .build());
                log.trace("Indexed Lemma id={} in Page id={} with rank={}", lemma.getId(), finalPage.getId(), freq);
            });

            // Обновляем время последней активности индексации для сайта
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

        } catch (Exception ex) {
            log.error("Failed to index page '{}' for site '{}': {}", pageUrl, site.getUrl(), ex.getMessage(), ex);
            // пробрасываем, чтобы внешняя логика выставила статус FAILED
            throw new RuntimeException("Error indexing page " + pageUrl, ex);
        }
    }
}
