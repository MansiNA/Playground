package de.dbuss.tefcontrol.data.service;

import de.dbuss.tefcontrol.data.Role;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.repository.ProjectsRepository;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectsService {

    private final ProjectsRepository repository;
    private List<Projects> projectsList;

    private User user;
    public ProjectsService(ProjectsRepository repository) {
        this.repository = repository;
        this.projectsList = repository.findAll();


    }

    public Optional<Projects> get(Long id) {
        return repository.findById(id);
    }

    public Projects update(Projects entity) { return repository.save(entity);}

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public int count() {
        return (int) repository.count();
    }

    public List<Projects> findAll() {
        return repository.findAll();
    }

    public Projects findByName(String jobName) {
        return repository.findByName(jobName).get();
    }

    public Optional<Projects> findById(long id) {
        return repository.findById(id);
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<ProjectAttachments> getProjectAttachments(Projects projects) {
        log.info("Executing getProjectAttachments() in projectsService");
        return projects.getListOfAttachments();
    }

    public List<ProjectSql> getProjectSqls(Projects projects) {
        log.info("Executing getProjectSqls() in projectsService");
        return projects.getListOfSqls();
    }

    public List<ProjectQSEntity> getListOfProjectQs(Projects projects) {
        log.info("Executing getListOfProjectQs() in projectsService");
        return projects.getListOfProjectQs();
    }

    public List<Projects> getRootProjects() {
        log.info("Executing getRootProjects() in projectsService");
        return projectsList
                .stream()
                .filter(projects -> projects.getParent_id() == null)
                .filter(projects -> hasAccess(user.getRoles(), projects.getRole_access()))
                .collect(Collectors.toList());
    }

    public List<Projects> getChildProjects(Projects parent) {
        log.info("Executing getChildProjects() in projectsService");
        return projectsList
                .stream()
                .filter(projects -> Objects.equals(projects.getParent_id(), parent.getId()))
                .filter(projects -> hasAccess(user.getRoles(), projects.getRole_access()))
                .collect(Collectors.toList());
    }

    private boolean hasAccess(Set<Role> userRoles, String projectRoles) {
        // check if the user has at least one role that matches the project's role_access.
        if(projectRoles != null) {
            return userRoles.stream().anyMatch(role -> projectRoles.contains(role.name()));
        }
        return false;
    }

    public List<ProjectAttachmentsDTO> getProjectAttachmentsWithoutFileContent(Projects projects) {
        return repository.getProjectAttachmentsWithoutFileContent(projects);
    }
}
