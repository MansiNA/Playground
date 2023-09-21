package de.dbuss.tefcontrol.data.repository;

import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectsRepository extends JpaRepository<Projects, Long> {
    Optional<Projects> findByName(String name);

    @Query("SELECT new de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO(pa.id, pa.description, pa.filename, pa.upload_date, pa.filesizekb) FROM ProjectAttachments pa WHERE pa.project = :project")
    List<ProjectAttachmentsDTO> getProjectAttachmentsWithoutFileContent(@Param("project") Projects project);

}
