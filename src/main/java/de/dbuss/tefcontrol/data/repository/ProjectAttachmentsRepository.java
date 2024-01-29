package de.dbuss.tefcontrol.data.repository;

import de.dbuss.tefcontrol.data.entity.ProjectAttachments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectAttachmentsRepository extends JpaRepository<ProjectAttachments, Long> {
    @Modifying
    @Query("UPDATE ProjectAttachments p SET p.filename = :filename, p.description = :description WHERE p.id = :id")
    void updateFilenameAndDescription(@Param("id") Long id, @Param("filename") String filename, @Param("description") String description);
}
