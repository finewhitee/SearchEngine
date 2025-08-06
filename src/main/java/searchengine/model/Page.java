package searchengine.model;


import lombok.*;
import org.jsoup.nodes.Document;

import javax.persistence.*;


@Entity
@Table(name = "page",
        indexes = @javax.persistence.Index(name = "idx_page_path", columnList = "path"))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    /** Путь страницы (начинается со "/") */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String path;

    /** HTTP-код ответа */
    @Column(name = "code", nullable = false)
    private Integer code;

    /** Контент страницы (HTML) */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;


}