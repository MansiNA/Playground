package de.dbuss.tefcontrol.data.repository;

import de.dbuss.tefcontrol.data.entity.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectsRepository extends JpaRepository<Projects, Long> {

    @Query("select p from Projects p where lower(p.name) = lower(:searchTerm)")
    Projects search(@Param("searchTerm") String searchTerm);
}
