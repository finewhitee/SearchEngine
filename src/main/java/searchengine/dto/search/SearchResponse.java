package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponse {
    private boolean result;
    private String error;       // если result=false
    private Integer count;      // общее число найденных страниц
    private List<SearchResultItem> data;  // собственно «страницы» выдачи
}
