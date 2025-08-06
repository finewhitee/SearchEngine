package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jsoup.Connection;

import javax.persistence.*;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "site")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SiteStatus status;

    /** Дата и время обновления статуса */
    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    /** Последняя ошибка индексации, если была */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /** Главная страница сайта */
    @Column(nullable = false, length = 255)
    private String url;

    /** Имя сайта */
    @Column(nullable = false, length = 255)
    private String name;

}
