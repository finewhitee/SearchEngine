package searchengine.services;
import org.springframework.scheduling.annotation.Async;
import searchengine.dto.statistics.IndexingStatusResponse;
import searchengine.model.Site;

public interface IndexingService {
//    IndexingStatusResponse getIndexingStatus();
    /**
     * Запускает полную индексацию всех сайтов из конфигурации.
     */
    void startIndexing();

    /**
     * Останавливает текущий процесс индексации.
     */
    void stopIndexing();

    /**
     * Проверяет, идёт ли сейчас процесс индексации.
     *
     * @return true, если индексация активна
     */
    boolean isIndexing();

    /**
     * Индексирует все страницы указанного сайта (домен).
     *
     * @param url корневой URL сайта
     */
    void indexSite(String url);

    /**
     * Индексирует одну конкретную страницу по URL.
     *
     * @param url адрес страницы для индексации
     */
    void indexPage(String pageUrl, Site site);
}


