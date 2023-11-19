package de.dbuss.tefcontrol.helpers;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

public  class Helpers {

    private static void startAgent(int projectId) {
        System.out.println("Start Agent-Jobs...");

      /*  JdbcTemplate jdbcTemplate=new JdbcTemplate();

        jdbcTemplate = projectConnectionService.getJdbcDefaultConnection();

        String sql = "select pp.value from pit.dbo.project_parameter pp, [PIT].[dbo].[projects] p\n" +
                "  where pp.namespace=p.page_url\n" +
                "  and pp.name in ('DBJobs')\n" +
                "  and p.id=?";


        String agents = null;

        try{
            agents=jdbcTemplate.queryForObject(sql, new Object[]{projectId},String.class);
        }
        catch(Exception e)
        {
            Notification.show("Problem to find relevant Jobs in project_parameter for job_id " + projectId+ "!",10000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return;
        }


        sql = "select pp.name, pp.value from pit.dbo.project_parameter pp, [PIT].[dbo].[projects] p\n" +
                "  where pp.namespace=p.page_url\n" +
                "  and pp.name in ('DB_Server','DB_Name', 'DB_User','DB_Password')\n" +
                "  and p.id="+projectId ;

        List<ProjectParameter> resultList = jdbcTemplate.query(sql, (rs, rowNum) -> {
            ProjectParameter projectParameter = new ProjectParameter();
            projectParameter.setName(rs.getString("name"));
            projectParameter.setValue(rs.getString("value"));
            return projectParameter;
        });
        String dbName = null;
        String dbServer = null;
        for (ProjectParameter projectParameter : resultList) {
            if (Constants.DB_NAME.equals(projectParameter.getName())) {
                dbName = projectParameter.getValue();
            } else if (Constants.DB_USER.equals(projectParameter.getName())) {
                dbUser = projectParameter.getValue();
            } else if (Constants.DB_PASSWORD.equals(projectParameter.getName())) {
                dbPassword = projectParameter.getValue();
            } else if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                dbServer = projectParameter.getValue();
            }
        }
        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";

        DataSource dataSource = projectConnectionService.getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);

        jdbcTemplate.setDataSource(dataSource);

        if (agents != null) {
            String[] jobs = agents.split(";");
            for (String job : jobs) {
                System.out.println("Start job: " + job);

                try {
                    sql = "msdb.dbo.sp_start_job @job_name='" + job + "'";
                    jdbcTemplate.execute(sql);
                    Notification.show(job + " startet..." ,5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
                catch (CannotGetJdbcConnectionException connectionException) {
                    Notification.show("Error connection to DB", 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);

                } catch (Exception e) {
                    // Handle other exceptions
                    Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }

            }
        }*/
    }

}
