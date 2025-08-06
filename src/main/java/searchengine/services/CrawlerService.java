package searchengine.services;

import org.springframework.stereotype.Service;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface CrawlerService {
    /**
     * Обходит все страницы заданного домена в ширину (BFS), передавая каждую найденную ссылку в callback.
     * Останавливается, если isActive.get() вернёт false.
     *
     * @param baseUrl корневой URL сайта (например, https://example.com)
     * @param pageCallback вызывается для каждой корректно загруженной страницы с её URL
     * @param isActive флаг продолжения работы (возвращает false, когда нужно остановить краулинг)
     */
    void crawlSite(String baseUrl, Consumer<String> pageCallback, Supplier<Boolean> isActive);
}
