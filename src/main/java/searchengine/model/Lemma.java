package searchengine.model;

import lombok.*;

import javax.persistence.*;
@Entity
@Table(name = "lemma",
        uniqueConstraints = @UniqueConstraint(columnNames = {"lemma", "site_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Ссылка на сайт */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    /** Текст леммы */
    @Column(name = "lemma", nullable = false, length = 255)
    private String lemma;

    /** Количество страниц, где встречается лемма */
    @Column(name = "frequency", nullable = false)
    private Integer frequency;
}
