package searchengine.dto.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultItem {
    private String site;      // http://www.site.com
    private String siteName;  // «Имя сайта»
    private String uri;       // «/path/to/page/6784»
    private String title;     // заголовок страницы
    private String snippet;   // HTML-отметка совпадений
    private float relevance;  // относительная релевантность
}