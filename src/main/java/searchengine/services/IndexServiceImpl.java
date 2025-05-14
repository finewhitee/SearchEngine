package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexingStatusResponse;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexingService {

    private final Random random = new Random();
    private final SitesList sites;

    @Override
    public IndexingStatusResponse getIndexingStatus() {
        String[] statuses = {"INDEXED", "FAILED", "INDEXING"};
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        List<SiteConfig> sitesList = sites.getSites();
        int randomIndex = random.nextInt(sitesList.size());
        SiteConfig site = sitesList.get(randomIndex);

        boolean indexingResult = random.nextBoolean();
        String error = indexingResult ? "" : errors[random.nextInt(errors.length)];

        return new IndexingStatusResponse(indexingResult, error);
    }
}