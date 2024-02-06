package de.dbuss.tefcontrol.data.service;

import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.ProjectAttachments;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.repository.ProjectAttachmentsRepository;
import de.dbuss.tefcontrol.data.repository.ProjectsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProjectAttachmentsService {
    private final ProjectAttachmentsRepository repository;
    private final ProjectsService projectsService; // Inject ProjectsService

    public ProjectAttachmentsService(ProjectAttachmentsRepository repository, ProjectsService projectsService) {
        this.repository = repository;
        this.projectsService = projectsService; // Initialize ProjectsService
    }
    public Optional<ProjectAttachmentsDTO> get(Long id) {
        return repository.findById(id).map(this::mapToProjectAttachmentsDTO);
    }

    public ProjectAttachmentsDTO update(ProjectAttachmentsDTO dto) {
        // Convert ProjectAttachmentsDTO to ProjectAttachments entity
        ProjectAttachments entity = mapToProjectAttachmentsEntity(dto);
        // Save the entity and then convert it back to DTO
        return mapToProjectAttachmentsDTO(repository.save(entity));
    }

    @Transactional
    public void updateGridValues(ProjectAttachmentsDTO dto) {
        repository.updateFilenameAndDescription(dto.getId(), dto.getFilename(), dto.getDescription());
    }
    public ProjectAttachmentsDTO updateGridValuesOld(ProjectAttachmentsDTO dto) {
        // get ProjectAttachmentsDTO to ProjectAttachments entity
        ProjectAttachments entity = repository.findById(dto.getId()).get();
        entity.setFilename(dto.getFilename());
        entity.setDescription(dto.getDescription());
        // Save the entity and then convert it back to DTO
        return mapToProjectAttachmentsDTO(repository.save(entity));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<ProjectAttachmentsDTO> findAll() {
        List<ProjectAttachments> attachments = repository.findAll();
        return attachments.stream()
                .map(this::mapToProjectAttachmentsDTO)
                .collect(Collectors.toList());
    }

    public Optional<ProjectAttachmentsDTO> projectAttachmentsDTOfindById(long id) {
        return repository.findById(id).map(this::mapToProjectAttachmentsDTO);
    }


    public ProjectAttachments findById(Long id) {
        return repository.findById(id).get();
    }
    private ProjectAttachmentsDTO mapToProjectAttachmentsDTO(ProjectAttachments entity) {
        ProjectAttachmentsDTO dto = new ProjectAttachmentsDTO();
        dto.setId(entity.getId());
        dto.setDescription(entity.getDescription());
        dto.setFilename(entity.getFilename());
        dto.setUploadDate(entity.getUpload_date());
        dto.setFilesizeKb(entity.getFilesizekb());
        dto.setProjectId(entity.getProject().getId());
        dto.setFileContent(entity.getFilecontent());
        return dto;
    }

    private ProjectAttachments mapToProjectAttachmentsEntity(ProjectAttachmentsDTO dto) {
        ProjectAttachments entity = new ProjectAttachments();
        entity.setId(dto.getId());
        entity.setDescription(dto.getDescription());
        entity.setFilename(dto.getFilename());
        entity.setUpload_date(dto.getUploadDate());
        entity.setFilesizekb(dto.getFilesizeKb());
        entity.setFilecontent(dto.getFileContent());
        if (dto.getProjectId() != null) {
            Projects project = projectsService.findById(dto.getProjectId()).orElse(null);
            entity.setProject(project);
        }
        return entity;
    }

}
