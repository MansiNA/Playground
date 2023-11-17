package de.dbuss.tefcontrol.data.service;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import de.dbuss.tefcontrol.data.entity.ProjectQSEntity;
import de.dbuss.tefcontrol.data.entity.ProjectSql;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.repository.ProjectQsRepository;
import de.dbuss.tefcontrol.data.repository.ProjectSqlRepository;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProjectQsService {
    private final ProjectQsRepository repository;
    private final ProjectsService projectsService;
    private final ProjectConnectionService projectConnectionService;

    public ProjectQsService(ProjectQsRepository repository, ProjectsService projectsService, ProjectConnectionService projectConnectionService) {
        this.repository = repository;
        this.projectsService = projectsService;
        this.projectConnectionService = projectConnectionService;
    }

    public List<ProjectQSEntity> findAll() {
        return repository.findAll();
    }
    public Optional<ProjectQSEntity> findById(int id) {
        return repository.findById(id);
    }
    public void delete (ProjectQSEntity rojectQSEntity) {
        repository.deleteById(rojectQSEntity.getId());
    }

    public List<ProjectQSEntity> getListOfProjectQs(int projectId) {
        Projects projects = projectsService.findById(projectId).get();
        return projectsService.getListOfProjectQs(projects);
    }

    public List<ProjectQSEntity> executeSQL(String dbUrl, String dbUser, String dbPassword, List<ProjectQSEntity> listOfProjectQs) {
        for (ProjectQSEntity projectQSEntity : listOfProjectQs) {
            String sql = projectQSEntity.getSql();

            if(sql != null ) {
                try {

                    DataSource dataSource = projectConnectionService.getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

                    if (rows.isEmpty()) {
                        projectQSEntity.setResult("Ok");
                    } else {
                        projectQSEntity.setResult("Failed");
                    }

                } catch ( Exception e) {

                 //   e.printStackTrace();
                    String errormessage = projectConnectionService.handleDatabaseError(e);
                    projectQSEntity.setResult(errormessage);
                  //  Notification.show( "Error during execute " + errormessage,5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        }
        return listOfProjectQs;
    }


}
