package searchengine.services.impl;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.services.CrawlerService;


//import edu.uci.ics.crawlercommons.robots.BaseRobotRules;
//import crawlercommons.robots.BaseRobotRules;
//import edu.uci.ics.crawlercommons.robots.SimpleRobotRulesParser;
//import edu.uci.ics.crawlercommons.robots.RobotstxtConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
@Slf4j
public class CrawlerServiceImpl implements CrawlerService {
    public static final String USER_AGENT = "SearchEngineBot";
    public static final int TIMEOUT_MS = 5000;//todo может быть мало

    /** - Метод ничего не возвращает,
     а делегирует обработку каждой найденной страницы через колбэк./*

     * - Если нужно “вернуть” все найденные страницы, ты можешь,
     например, создать список, а в колбэке складывать туда URL */

    @Override
    public void crawlSite(String baseUrl, Consumer<String> pageCallback, Supplier<Boolean> isActive) {
        log.info("Starting crawl for site: {}", baseUrl);
        try {
            // Базовый хост для фильтрации ссылок
            URL urlObj = new URL(baseUrl);
            String hostRoot = urlObj.getProtocol() + "://" + urlObj.getHost();

            // Загрузка и разбор robots.txt
            String robotsTxtUrl = hostRoot + "/robots.txt";
            log.debug("Loading robots.txt from: {}", robotsTxtUrl);
            byte[] robotsContent = Jsoup.connect(robotsTxtUrl)
                    .ignoreContentType(true)
                    .timeout(TIMEOUT_MS)
                    .execute()
                    .bodyAsBytes();
//            RobotstxtConfig config = new RobotstxtConfig(); похоже херню заимпортил
            SimpleRobotRulesParser parser = new SimpleRobotRulesParser();
            BaseRobotRules rules = parser.parseContent(
                    robotsTxtUrl,
                    robotsContent,
                    "text/plain",
                    USER_AGENT);

            // Очередь для BFS и набор посещённых URL
            Queue<String> queue = new LinkedList<>();
            Set<String> visited = new HashSet<>();
            queue.add(baseUrl);
            visited.add(baseUrl);

            // Основной цикл краулинга
            while (!queue.isEmpty() && isActive.get()) {
                String currentUrl = queue.poll();
                // Пропустить, если запрещено robots.txt
                if (!rules.isAllowed(currentUrl)){
                    log.debug("URL disallowed by robots.txt, skipping: {}", currentUrl);
                    continue;
                }


                // Загрузка страницы
                log.info("Crawling URL: {}", currentUrl);
                Document doc = Jsoup.connect(currentUrl)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MS)
                        .get();
                // Передаём URL страницы для последующей обработки
                pageCallback.accept(currentUrl);

                // Извлекаем инкрементально новые ссылки
                doc.select("a[href]")
                        .stream()
                        .map(link -> link.attr("abs:href"))
                        .filter(link -> link.startsWith(hostRoot))
                        .filter(link -> !visited.contains(link))
                        .filter(link -> rules.isAllowed(link))
                        .forEach(link -> {
                            visited.add(link);
                            queue.add(link);
                            log.debug("Enqueued URL: {}", link);
                        });
            }

            log.info("Finished crawl for site: {}", baseUrl);
        } catch (Exception e) {
            // Здесь можно логировать ошибку: URL, текст исключения и т.п.
            log.error("Error during crawling site {}: {}", baseUrl, e.getMessage(), e);
        }

    }
}
