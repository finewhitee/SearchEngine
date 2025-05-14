package searchengine.services;
import searchengine.dto.statistics.IndexingStatusResponse;

public interface IndexingService {
    IndexingStatusResponse getIndexingStatus();
}
