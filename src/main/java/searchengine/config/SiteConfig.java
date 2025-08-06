package searchengine.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@NoArgsConstructor        // нужен JPA / Spring для биндинга
@AllArgsConstructor
public class SiteConfig {
    private String url;
    private String name;
}
