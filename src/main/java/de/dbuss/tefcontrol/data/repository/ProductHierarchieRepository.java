package de.dbuss.tefcontrol.data.repository;

import de.dbuss.tefcontrol.data.modules.pfgproductmapping.entity.ProductHierarchie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductHierarchieRepository extends JpaRepository<ProductHierarchie, Long> {

    @Query("select c from ProductHierarchie c " +
            "where lower(c.node) like lower(concat('%', :searchTerm, '%')) " +
            "or lower(c.product_name) like lower(concat('%', :searchTerm, '%'))")
    List<ProductHierarchie> search(@Param("searchTerm") String searchTerm);

}
