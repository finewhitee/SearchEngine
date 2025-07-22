package searchengine.services;


import searchengine.dto.search.SearchResponse;

public interface SearchService {
    /**
     * @param query  поисковый запрос
     * @param site   URL сайта для поиска (null — искать по всем проиндексированным)
     * @param offset сдвиг для пагинации
     * @param limit  максимум результатов
     * @return DTO-ответ с полями result, count, data или error
     */
    SearchResponse search(String query, String site, Integer offset, Integer limit);
}
