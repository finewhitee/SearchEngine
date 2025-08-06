package searchengine.model;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "index")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Страница */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    /** Лемма */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    /** Количество вхождений данной леммы на странице */
    @Column(name = "rank", nullable = false)
    private Float rank;
}