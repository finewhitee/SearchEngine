package searchengine.repository;

import com.sun.istack.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Optional;
import java.util.Set;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);
    long countBySite(Site site);

    void deleteAllBySite(@NotNull Site existing);

    int countBySiteId(Integer id);







}
