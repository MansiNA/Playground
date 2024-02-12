package de.dbuss.tefcontrol.data.modules.administration.view;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.*;
import de.dbuss.tefcontrol.components.DefaultUtils;
import de.dbuss.tefcontrol.components.LogView;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.modules.administration.entity.CurrentPeriods;
import de.dbuss.tefcontrol.data.service.*;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Administration")
@Route(value = "REPORT_Administration/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "MAPPING", "USER"})
public class ReportAdminView extends VerticalLayout implements BeforeEnterObserver {
    private final ProjectConnectionService projectConnectionService;
    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private String tableReportingConfig;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String agentName;
    private CurrentPeriods currentPeriods;
    private int projectId;
    private Optional<Projects> projects;
    Boolean isVisible = false;
    Boolean isLogsVisible = false;

    Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);
    private static final Logger logger = LoggerFactory.getLogger(ReportAdminView.class);
    private LogView logView;
    private DefaultUtils defaultUtils;
    private List<ProjectAttachmentsDTO> listOfProjectAttachments;

    public ReportAdminView(ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService, BackendService backendService, AuthenticatedUser authenticatedUser, ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService) {
        this.projectConnectionService = projectConnectionService;
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;

        logView = new LogView();
        logView.logMessage(Constants.INFO, "Starting ReportAdminView");
        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.REPORT_ADMINISTRATION.equals(projectParameter.getNamespace()))
                .collect(Collectors.toList());
        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : filteredProjectParameters) {
          //  if (projectParameter.getNamespace().equals(Constants.REPORT_ADMINISTRATION)) {
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
        //    }
        }


        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";

        setProjectParameterGrid(filteredProjectParameters);
        defaultUtils = new DefaultUtils(projectsService, projectAttachmentsService);

        HorizontalLayout vl = new HorizontalLayout();
        vl.add(getTabsheet());

        vl.setHeightFull();
        vl.setSizeFull();

        setHeightFull();
        setSizeFull();

        add(vl,parameterGrid);


        parameterGrid.setVisible(false);
        logView.setVisible(false);

        if (MainLayout.isAdmin) {
            UI.getCurrent().addShortcutListener(
                    () -> {

                        isVisible = !isVisible;
                        parameterGrid.setVisible(isVisible);
                    },
                    Key.KEY_I, KeyModifier.ALT);

            UI.getCurrent().addShortcutListener(
                    () -> {

                        isLogsVisible = !isLogsVisible;
                        logView.setVisible(isLogsVisible);
                    },
                    Key.KEY_V, KeyModifier.ALT);

        }
        add(logView);
        logView.logMessage(Constants.INFO, "Ending ReportAdminView....");
    }

    private void setProjectParameterGrid(List<ProjectParameter> listOfProjectParameters) {
        logView.logMessage(Constants.INFO, "Starting setProjectParameterGrid() for database detail grid");
        parameterGrid = new Grid<>(ProjectParameter.class, false);
        parameterGrid.addColumn(ProjectParameter::getName).setHeader("Name").setAutoWidth(true).setResizable(true);
        parameterGrid.addColumn(ProjectParameter::getValue).setHeader("Value").setAutoWidth(true).setResizable(true);
        parameterGrid.addColumn(ProjectParameter::getDescription).setHeader("Description").setAutoWidth(true).setResizable(true);

        parameterGrid.setItems(listOfProjectParameters);
        parameterGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        parameterGrid.setHeight("200px");
        parameterGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        logView.logMessage(Constants.INFO, "Ending setProjectParameterGrid() for database detail grid");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        logView.logMessage(Constants.INFO, "Starting beforeEnter() for update");
        RouteParameters parameters = event.getRouteParameters();
        projectId = Integer.parseInt(parameters.get("project_Id").orElse(null));
        projects = projectsService.findById(projectId);

        projects.ifPresent(value -> listOfProjectAttachments = projectsService.getProjectAttachmentsWithoutFileContent(value));

        updateDescription();
        updateAttachmentGrid(listOfProjectAttachments);
        logView.logMessage(Constants.INFO, "Ending beforeEnter for update");
        logView.logMessage(Constants.INFO, "Starting beforeEnter() for update");
    }

    private TabSheet getTabsheet() {
        logView.logMessage(Constants.INFO, "Starting getTabsheet() for Tabs");
        //log.info("Starting getTabsheet() for Tabsheet");
        TabSheet tabSheet = new TabSheet();

        tabSheet.add("Upload", getUpladTab());
        tabSheet.add("Description", getDescriptionTab());
        tabSheet.add("Attachments", getAttachmentTab());

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();
        //log.info("Ending getTabsheet() for Tabsheet");
        logView.logMessage(Constants.INFO, "Ending getTabsheet() for Tabs");
        return tabSheet;
    }


    private Component getAttachmentTab() {
        logView.logMessage(Constants.INFO, "Set Attachment in getAttachmentTab()");
        return defaultUtils.getProjectAttachements();
    }

    private Component getDescriptionTab() {
        logView.logMessage(Constants.INFO, "Set Description in getDescriptionTab()");
        return defaultUtils.getProjectDescription();
    }
    private void updateDescription() {
        logView.logMessage(Constants.INFO, "Update Attachment in updateDescription()");
        defaultUtils.setProjectId(projectId);
        defaultUtils.setDescription();
    }
    private void updateAttachmentGrid(List<ProjectAttachmentsDTO> projectAttachmentsDTOS) {
        logView.logMessage(Constants.INFO, "Update Description in updateAttachmentGrid()");
        defaultUtils.setProjectId(projectId);
        defaultUtils.setAttachmentGridItems(projectAttachmentsDTOS);
    }
    private Component getUpladTab() {
        logView.logMessage(Constants.INFO, "Sarting getUpladTab() for set upload data");
        VerticalLayout content = new VerticalLayout();

        content.setSizeFull();
        content.setHeightFull();

        H1 h1 = new H1("Report Administration");
        H2 h2 = new H2("Rohdatenreporting:");

        HorizontalLayout header = new HorizontalLayout(h2);
        header.setAlignItems(Alignment.BASELINE);

        Article p1 = new Article();
        p1.setText("Erstellung des \"Rohdatenreports\" und Ablage unter \\\\dewsttwak11\\Ablagen\\Rohdatenauswertung.");

        content.add(h1, h2, p1);

        Button startRohdatenReportBtn = new Button("Start Report");
        startRohdatenReportBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        startRohdatenReportBtn.addClickListener(e -> {
            logView.logMessage(Constants.INFO, "startRohdatenReportBtn.addClickListener for Start Reporting");
            startRohdatenReportBtn.setEnabled(false);
            startRohdatenReportBtn.setText("running");
            String resultOfPeriods = projectConnectionService.executeReportAdminPeriods(currentPeriods, dbUrl, dbUser, dbPassword);
            System.out.println(resultOfPeriods+"......................................");
            System.err.println(resultOfPeriods);
            Notification notification;
            if (resultOfPeriods.equals(Constants.OK)) {
                notification = Notification.show("Reporting Monat " + currentPeriods.getCurrent_month() + " updated successfully", 5000, Notification.Position.MIDDLE);
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

        HorizontalLayout hlayout = new HorizontalLayout();

        hlayout.add(getDimPeriodGrid(), startRohdatenReportBtn);
        hlayout.setAlignItems(Alignment.BASELINE);

        content.add(hlayout);

        logView.logMessage(Constants.INFO, "Ending getUpladTab() for set upload data");
        return content;
    }

    private Component getDimPeriodGrid() {
        logView.logMessage(Constants.INFO, "Starting getDimPeriodGrid() for current period grid");
        VerticalLayout content = new VerticalLayout();

        List<CurrentPeriods> periods = new ArrayList<>();
        currentPeriods = new CurrentPeriods();
        periods.add(currentPeriods);

        Grid<CurrentPeriods> grid_period = new Grid<>(CurrentPeriods.class, false);
        grid_period.addComponentColumn(cp -> {
            ComboBox<String> comboBox = new ComboBox<>();
            List<String> monthPeriod = new ArrayList<>();

            // Get the current YearMonth
            YearMonth currentYearMonth = YearMonth.now();

            for (int i = -12; i <= 0; i++) {
                monthPeriod.add(currentYearMonth.plusMonths(i).toString().replace("-", ""));
            }

            comboBox.setItems(monthPeriod);
            String selectedValue = currentYearMonth.plusMonths(-1).toString().replace("-", "");
            comboBox.setValue(selectedValue);
            cp.setCurrent_month(selectedValue);
            currentPeriods.setCurrent_month(selectedValue);
            comboBox.addValueChangeListener(event -> {
                logView.logMessage(Constants.INFO, "Selecting current period value");
                cp.setCurrent_month(event.getValue());
                currentPeriods.setCurrent_month(event.getValue());
            });
            return comboBox;
       // }).setHeader("Reporting Monat").setFlexGrow(0).setAutoWidth(true);
        }).setFlexGrow(0).setAutoWidth(true);

        grid_period.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid_period.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid_period.setWidth("240px");
        grid_period.setHeight("140px");
      //  grid_period.getStyle().set( "border" , "0.5px solid black" ) ;
        grid_period.getStyle().set( "border" , "none" ) ;
        //grid_period.getStyle().set( "box-shadow" , "5px 6px 4px gray" ) ;
        //grid_period.getStyle().set( "border-radius" , "2px" ) ;
        //grid_period.getStyle().set( "padding" , "25px" ) ;

        //grid_period.getElement().getStyle().set("padding", "20px");

        grid_period.getColumns().forEach(e -> e.setResizable(Boolean.TRUE));

        grid_period.setItems(periods);

     //   content.add(p2, grid_period);
        content.add(grid_period);
        content.setHeightFull();
        content.setWidth("250px");
        content.setHeight("150px");
        content.setAlignItems(Alignment.BASELINE);
        logView.logMessage(Constants.INFO, "Ending getDimPeriodGrid() for current period grid");
        return content;

    }

}