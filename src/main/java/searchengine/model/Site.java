package searchengine.model;

import javax.persistence.*;
import java.text.DateFormat;
import java.util.Date;

@Entity
@Table(name = "site")
public class Site {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private int id;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    private StatusPage status;
    @Column(name = "status_time")
    private Date statusTime;


    @Column(name = "last_error")
    private String lastError;
    @Column(columnDefinition = "VARCHAR(255)")
    private String url;
    @Column(columnDefinition = "VARCHAR(255)")
    private String name;
}
