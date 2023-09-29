package de.dbuss.tefcontrol.data.repository;

import de.dbuss.tefcontrol.data.entity.ProjectSql;
import de.dbuss.tefcontrol.data.entity.Projects;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectSqlRepository extends JpaRepository<ProjectSql, Long> {
    Optional<ProjectSql> findByName(String name);
}
