package de.dbuss.tefcontrol.data.modules.administration.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.entity.ProjectUpload;
import de.dbuss.tefcontrol.data.entity.User;
import de.dbuss.tefcontrol.data.modules.administration.entity.CurrentPeriods;
import de.dbuss.tefcontrol.data.service.BackendService;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.YearMonth;
import java.util.*;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

@PageTitle("Administration")
@Route(value = "REPORT_Administration/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "MAPPING", "USER"})
public class ReportAdminView extends VerticalLayout implements BeforeEnterObserver {
    private final ProjectConnectionService projectConnectionService;
    private String tableReportingConfig;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String agentName;
    private CurrentPeriods currentPeriods;
    private int projectId;

    public ReportAdminView(ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService, BackendService backendService, AuthenticatedUser authenticatedUser) {
        this.projectConnectionService = projectConnectionService;

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : listOfProjectParameters) {
            if (projectParameter.getNamespace().equals(Constants.REPORT_ADMINISTRATION)) {
                if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                    dbServer = projectParameter.getValue();
                } else if (Constants.DB_NAME.equals(projectParameter.getName())) {
                    dbName = projectParameter.getValue();
                } else if (Constants.DB_USER.equals(projectParameter.getName())) {
                    dbUser = projectParameter.getValue();
                } else if (Constants.DB_PASSWORD.equals(projectParameter.getName())) {
                    dbPassword = projectParameter.getValue();
                } else if (Constants.TABLE_REPORTINGCONFIG.equals(projectParameter.getName())) {
                    tableReportingConfig = projectParameter.getValue();
                }  else if (Constants.DB_JOBS.equals(projectParameter.getName())) {
                    agentName = projectParameter.getValue();
                }
            }
        }

        H1 h1 = new H1("Report Administration");
        Article p1 = new Article();
        p1.setText("Auf diese Seite lassen sich verschiedene Einstellungen zur Report-Beladung vornehmen.");

        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";



        H2 h2 = new H2("Rohdatenreporting:");
        Text databaseDetail = new Text("Connected to: "+ dbServer+ ", Database: " + dbName + ", Target Table: " +tableReportingConfig + ", SQL-Job: "+ agentName);

        HorizontalLayout header = new HorizontalLayout(h2,databaseDetail);
        header.setAlignItems(Alignment.BASELINE);

        add(h1, p1, header);

        Button startRohdatenReportBtn = new Button("Start Report");

        startRohdatenReportBtn.addClickListener(e -> {
            startRohdatenReportBtn.setEnabled(false);
            String resultOfPeriods = projectConnectionService.saveReportAdmintPeriods(currentPeriods, dbUrl, dbUser, dbPassword, tableReportingConfig);
            Notification notification;
            if (resultOfPeriods.equals(Constants.OK)) {
                notification = Notification.show(" Report updated successfully", 2000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                notification = Notification.show("Error during upload: " + resultOfPeriods, 15000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }

            String message = projectConnectionService.startAgent(projectId);
            if (!message.contains("Error")) {
                Notification.show(message, 6000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                String AgenterrorMessage = "ERROR: Job " + agentName + " already running please try again later...";
                Notification.show(AgenterrorMessage, 6000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }

        });

        HorizontalLayout hl = new HorizontalLayout();
        hl.setAlignItems(Alignment.BASELINE);
        hl.add(getDimPeriodGrid(), startRohdatenReportBtn);
        hl.setAlignItems(Alignment.CENTER);

        add(hl);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        projectId = Integer.parseInt(parameters.get("project_Id").orElse(null));
    }
    private Component getDimPeriodGrid() {
        VerticalLayout content = new VerticalLayout();

        H3 p2 = new H3();
        p2.setText("Period:");

        List<CurrentPeriods> periods = new ArrayList<>();
        currentPeriods = new CurrentPeriods();
        periods.add(currentPeriods);

        Grid<CurrentPeriods> grid_period = new Grid<>(CurrentPeriods.class, false);
        grid_period.addComponentColumn(cp -> {
            ComboBox<String> comboBox = new ComboBox<>();
            List<String> monthPeriod = new ArrayList<>();

            // Get the current YearMonth
            YearMonth currentYearMonth = YearMonth.now();

            for (int i = -3; i <= 3; i++) {
                monthPeriod.add(currentYearMonth.plusMonths(i).toString().replace("-", ""));
            }

            comboBox.setItems(monthPeriod);
            comboBox.setValue(monthPeriod.get(0));
            cp.setCurrent_month(monthPeriod.get(0));
            currentPeriods.setCurrent_month(monthPeriod.get(0));
            comboBox.addValueChangeListener(event -> {
                cp.setCurrent_month(event.getValue());
                currentPeriods.setCurrent_month(event.getValue());
            });
            return comboBox;
        }).setHeader("Current-Month").setFlexGrow(0).setAutoWidth(true);
        grid_period.setWidth("240px");
        grid_period.setHeight("100px");

        grid_period.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid_period.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid_period.getStyle().set( "border" , "0.5px solid black" ) ;
        grid_period.getStyle().set( "box-shadow" , "5px 6px 4px gray" ) ;
        grid_period.getStyle().set( "border-radius" , "2px" ) ;
        grid_period.getStyle().set( "padding" , "25px" ) ;

        grid_period.getElement().getStyle().set("padding", "20px");

        grid_period.getColumns().forEach(e -> e.setResizable(Boolean.TRUE));

        grid_period.setItems(periods);

     //   content.add(p2, grid_period);
        content.add(grid_period);
        content.setHeightFull();
        return content;
    }

}