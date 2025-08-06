package searchengine.repository;

import com.sun.istack.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    long countBySite(Site site);
    Optional<Page> findByPath(String path);

    void deleteAllBySite(@NotNull Site existing); // todo

    int countBySiteId(Integer id);

    long countBySiteUrl(String siteUrl);

}
