package searchengine.repository;

import com.sun.istack.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface IndexRepository extends JpaRepository<Index, Long> {
    List<Index> findByLemmaAndPage(Lemma lemma, Page page);
    List<Index> findByPage(Page page);
    List<Index> findByLemma(Lemma lemma);

    void deleteAllByPage_Site(@NotNull Site existing);//todo


}

