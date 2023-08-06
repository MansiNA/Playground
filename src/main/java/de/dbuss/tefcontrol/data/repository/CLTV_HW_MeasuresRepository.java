package de.dbuss.tefcontrol.data.repository;

import de.dbuss.tefcontrol.data.entity.CLTV_HW_Measures;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CLTV_HW_MeasuresRepository extends JpaRepository<CLTV_HW_Measures, Long> {


    @Query("select c from CLTV_HW_Measures c " +
            "where lower(c.device) like lower(concat('%', :searchTerm, '%')) " +
            "or lower(c.device) like lower(concat('%', :searchTerm, '%'))" +
            "or lower(c.channel) like lower(concat('%', :searchTerm, '%'))" +
            "or lower(c.measure_Name) like lower(concat('%', :searchTerm, '%'))")
    List<CLTV_HW_Measures> search(@Param("searchTerm") String searchTerm);

    @Query("select c from CLTV_HW_Measures c " +
            "where c.monat_ID = :searchTerm ")

    List<CLTV_HW_Measures> searchMonat(@Param("searchTerm") Integer searchTerm);

}
