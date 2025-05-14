package searchengine.model;


import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Table(name = "page")
public class Page {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private int id;
    @Column(name = "site_id")
    private int siteId;
    @Column(columnDefinition = "TEXT")
    private String path;
    @Column(name = "code")
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;
}
