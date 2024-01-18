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
import de.dbuss.tefcontrol.data.modules.administration.entity.CurrentScenarios;
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
@Route(value = "COBI_Administration/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "MAPPING", "FLIP"})
public class CobiAdminView extends VerticalLayout implements BeforeEnterObserver {
    private final ProjectConnectionService projectConnectionService;
    private String tableCurrentPeriods;
    private String tableCurrentSenarios;
    private String sqlPlanScenarios;
    private String sqlOutlookScenario;
    private String sqlQfcScenarios;
    private String sqlAktCurrentPeriods;
    private String sqlAktCurrentScenarios;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private CurrentScenarios currentScenarios;
    private CurrentPeriods currentPeriods;
    private int projectId;

    private QS_Grid qsGrid;
    private Button qsBtn;

    public static Map<String, Integer> projectUploadIdMap = new HashMap<>();
    private String agentName;
    private AuthenticatedUser authenticatedUser;
    private int upload_id;

    public CobiAdminView(ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService, BackendService backendService, AuthenticatedUser authenticatedUser) {
        this.projectConnectionService = projectConnectionService;
        this.authenticatedUser=authenticatedUser;

        qsBtn = new Button("QS and Start Job");
       // qsBtn.setEnabled(false);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : listOfProjectParameters) {
            if (projectParameter.getNamespace().equals(Constants.COBI_ADMINISTRATION)) {
                if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                    dbServer = projectParameter.getValue();
                } else if (Constants.DB_NAME.equals(projectParameter.getName())) {
                    dbName = projectParameter.getValue();
                } else if (Constants.DB_USER.equals(projectParameter.getName())) {
                    dbUser = projectParameter.getValue();
                } else if (Constants.DB_PASSWORD.equals(projectParameter.getName())) {
                    dbPassword = projectParameter.getValue();
                } else if (Constants.TABLE_CURRENTPERIODS.equals(projectParameter.getName())) {
                    tableCurrentPeriods = projectParameter.getValue();
                } else if (Constants.TABLE_CURRENTSCENARIOS.equals(projectParameter.getName())) {
                    tableCurrentSenarios = projectParameter.getValue();
                } else if (Constants.SQL_Plan_Scenarios.equals(projectParameter.getName())) {
                    sqlPlanScenarios = projectParameter.getValue();
                } else if (Constants.SQL_OUTLOOK_SCENARIOS.equals(projectParameter.getName())) {
                    sqlOutlookScenario = projectParameter.getValue();
                } else if (Constants.SQL_QFC_SCENARIOS.equals(projectParameter.getName())) {
                    sqlQfcScenarios = projectParameter.getValue();
                } else if (Constants.DB_JOBS.equals(projectParameter.getName())) {
                    agentName = projectParameter.getValue();
                } else if (Constants.SQL_AKT_CURRENTPERIODS.equals(projectParameter.getName())) {
                    sqlAktCurrentPeriods = projectParameter.getValue();
                } else if (Constants.SQL_AKT_CURRENTSCENARIOS.equals(projectParameter.getName())) {
                    sqlAktCurrentScenarios = projectParameter.getValue();
                }
            }
        }
        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";

        H1 h1 = new H1("FLIP Administration");
        Article p1 = new Article();
        p1.setText("Auf diese Seite lassen sich verschiedene Einstellungen zur COBI-Beladung vornehmen.");
        add();
        add(h1, p1, getDimPeriodGrid(), getDimScenarioGrid());

        //Componente QS-Grid:
        qsGrid = new QS_Grid(projectConnectionService, backendService);

        HorizontalLayout hl = new HorizontalLayout();
        hl.setAlignItems(Alignment.BASELINE);
        hl.add( qsBtn, qsGrid);

        qsBtn.addClickListener(e ->{

            ProjectUpload projectUpload = new ProjectUpload();
            projectUpload.setFileName("");
            //  projectUpload.setUserName(MainLayout.userName);
            Optional<User> maybeUser = authenticatedUser.get();
            if (maybeUser.isPresent()) {
                User user = maybeUser.get();
                projectUpload.setUserName(user.getUsername());
            }
            projectUpload.setModulName("CobiAdmin");

            projectConnectionService.getJdbcConnection(dbUrl, dbUser, dbPassword);

            upload_id = projectConnectionService.saveUploadedGenericFileData(projectUpload);

            //String sql_rsult=projectConnectionService.saveUploadedGenericFileData(projectUpload);

            if (upload_id==-1)
            {
                Notification.show("Error in CobiAdminView saveUploadedGenericFileData! ", 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }


/*
            Map<String, Integer> uploadIdMap = projectConnectionService.getUploadIdMap(projectUpload.getModulName(), projectUpload.getUserName(), dbUrl, dbUser, dbPassword);
            int upload_id = uploadIdMap.values().stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(1);*/

            System.out.println("Upload_ID: " + upload_id);

            currentPeriods.setUpload_ID(upload_id);
            currentScenarios.setUpload_ID(upload_id);

            String resultOfPeriods = projectConnectionService.saveCobiAdminCurrentPeriods(currentPeriods, dbUrl, dbUser, dbPassword, tableCurrentPeriods);
            Notification notification;
            if (resultOfPeriods.equals(Constants.OK)) {
                // qsBtn.setEnabled(true);
                notification = Notification.show(" Current Periods Rows Uploaded successfully", 6000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                notification = Notification.show("Error during upload: " + resultOfPeriods, 15000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }

            String resultOfScenarios = projectConnectionService.saveCobiAdminCurrentScenarios(currentScenarios, dbUrl, dbUser, dbPassword, tableCurrentSenarios);
            if (resultOfScenarios.equals(Constants.OK)) {
                notification = Notification.show(" Current Scenarios Rows Uploaded successfully", 6000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                notification = Notification.show("Error during upload: " + resultOfScenarios, 15000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }

            hl.remove(qsGrid);
            qsGrid = new QS_Grid(projectConnectionService, backendService);
            hl.add(qsGrid);

/*
            Map<String, Integer> uploadIdMap = projectConnectionService.getUploadIdMap();
            int upload_id = uploadIdMap.values().stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(1);
*/

            CallbackHandler callbackHandler = new CallbackHandler();
            qsGrid.createDialog(callbackHandler, projectId, upload_id);
            qsGrid.showDialog(true);
        });

        add(hl);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        projectId = Integer.parseInt(parameters.get("project_Id").orElse(null));
    }
    public class CallbackHandler implements QS_Callback {
        // Die Methode, die aufgerufen wird, wenn die externe Methode abgeschlossen ist
        @Override
        public void onComplete(String result) {
            if(!result.equals("Cancel")) {

        /*        Map.Entry<String, Integer> lastEntry = projectUploadIdMap.entrySet().stream()
                        .reduce((first, second) -> second)
                        .orElse(null);
                int upload_id = lastEntry.getValue();*/

                System.out.println("Upload_ID:" + upload_id);
                try {
                    // String sql = "EXECUTE Core_Comment.sp_Load_Comments @p_Upload_ID="+upload_id;
                    String sql = "DECLARE @status AS INT;\n" +
                            "BEGIN TRAN\n" +
                            "   SELECT @status=[Upload_ID] FROM [Log].[Agent_Job_Uploads] WITH (UPDLOCK)\n" +
                            "   WHERE AgentJobName = '" + agentName + "';\n" +
                            "IF (@status IS NULL)\n" +
                            "BEGIN\n" +
                            "  UPDATE [Log].[Agent_Job_Uploads]\n" +
                            "  SET [Upload_ID] = "+upload_id +"\n" +
                            "   WHERE AgentJobName = '" + agentName +"' ;\n" +
                            "  COMMIT;\n" +
                            "  SELECT 'ok' AS Result\n" +
                            "END\n" +
                            "ELSE\n" +
                            "BEGIN\n" +
                            "  SELECT @status AS Result;\n" +
                            "  ROLLBACK;\n" +
                            "END";
                    DataSource dataSource = projectConnectionService.getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                    //  jdbcTemplate.execute(sql);
                    System.out.println("Execute SQL: " + sql);
                    String sqlResult = jdbcTemplate.queryForObject(sql, String.class);

                    System.out.println("SQL result for entry in Agent_Job_Uploads: (\"ok\" if no Upload_id exists)" + sqlResult);

                    if (!"ok".equals(sqlResult)) {
                        // resultMessage contains Upload_ID, so search user wo do this upload:
                        int uploadID=Integer.parseInt(sqlResult);


                        sql="select User_Name from [Log].[User_Uploads] where Upload_id=" + uploadID;
                        System.out.println("execute SQL: " + sql);
                        try {
                            sqlResult = jdbcTemplate.queryForObject(sql, String.class);
                        }
                        catch (Exception e){
                            System.out.println("User for Upload-ID " + uploadID + " not found...");
                            String AgenterrorMessage = "User for Upload-ID " + uploadID + " not found...please try again later...";
                            Notification.show(AgenterrorMessage, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                            return;
                        }

                        System.out.println("SQL result " + sqlResult);

                        String errorMessage = "ERROR: Job already executed by user " + sqlResult + " (Upload ID: " + uploadID + ") please try again later...";
                        //Notification.show(errorMessage, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);

                        Notification notification = new Notification();
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        Div statusText = new Div(new Text(errorMessage));

                        Button retryButton = new Button("Try anyway");
                        retryButton.addThemeVariants(LUMO_TERTIARY_INLINE);
                        //retryButton.getElement().getStyle().set("margin-left", "var(--lumo-space-xl)");
                        retryButton.getStyle().set("margin", "0 0 0 var(--lumo-space-l)");
                        retryButton.addClickListener(event -> {
                            notification.close();

                            //Update Agent_Job_Uploads
                            String sql1 = "UPDATE [Log].[Agent_Job_Uploads] SET [Upload_ID] = "+upload_id + " WHERE AgentJobName = '" + agentName +"' ;";
                            System.out.println("SQL executed: " + sql1);
                            jdbcTemplate.execute(sql1);
                            //End Update Agent_Job_Uploads


                            String message = projectConnectionService.startAgent(projectId);
                            if (!message.contains("Error")) {

                                Notification.show(message, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            } else {
                                String AgenterrorMessage = "ERROR: Job " + agentName + " already running please try again later...";
                                Notification.show(AgenterrorMessage, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                            }

                        });

                        //Button closeButton = new Button(new Icon("lumo", "cross"));

                        Button closeButton = new Button("OK");
                        closeButton.addThemeVariants(LUMO_TERTIARY_INLINE);
                        closeButton.getElement().setAttribute("aria-label", "Close");
                        closeButton.addClickListener(event -> {
                            notification.close();
                        });

                        HorizontalLayout layout = new HorizontalLayout(statusText, retryButton, closeButton);
                        layout.setAlignItems(Alignment.CENTER);

                        notification.add(layout);
                        notification.setPosition(Notification.Position.MIDDLE);
                        notification.open();


                    } else {
                        // Continue with startAgent
                        String message = projectConnectionService.startAgent(projectId);
                        if (!message.contains("Error")) {

                            Notification.show(message, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        } else {
                            String errorMessage = "ERROR: Job " + agentName + " already running please try again later...";
                            Notification.show(errorMessage, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                        }
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                    String errormessage = projectConnectionService.handleDatabaseError(e);
                    Notification.show(errormessage, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }



            }
        }
    }

    private Component getDimPeriodGrid() {
        VerticalLayout content = new VerticalLayout();

        H3 p2 = new H3();
        p2.setText("Period:");

        List<CurrentPeriods> periods = new ArrayList<>();
        currentPeriods = new CurrentPeriods();
        periods.add(currentPeriods);

        System.out.println("Execute SQL for sqlAktCurrentPeriods: " + sqlAktCurrentPeriods);

        List<String> aktCurrentPeriods = projectConnectionService.getCobiAdminQFCPlanOutlook(dbUrl, dbUser, dbPassword, sqlAktCurrentPeriods);
        System.out.println(aktCurrentPeriods + " aktCurrentPeriods..........");

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

            if(aktCurrentPeriods != null && !aktCurrentPeriods.isEmpty()) {
                comboBox.setValue(aktCurrentPeriods.get(0));
                cp.setCurrent_month(aktCurrentPeriods.get(0));
                currentPeriods.setCurrent_month(aktCurrentPeriods.get(0));
            } else {
                comboBox.setValue(monthPeriod.get(3));
                cp.setCurrent_month(monthPeriod.get(3));
                currentPeriods.setCurrent_month(monthPeriod.get(3));
                Notification.show("No valid sql for Current Periods in project_properties", 5000, Notification.Position.MIDDLE);
            }
            comboBox.addValueChangeListener(event -> {
                cp.setCurrent_month(event.getValue());
                currentPeriods.setCurrent_month(event.getValue());
            });
            return comboBox;
        }).setHeader("Current-Month").setFlexGrow(0).setAutoWidth(true);
        grid_period.setWidth("240px");
        grid_period.setHeight("100px");
        //grid_period.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        grid_period.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid_period.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid_period.getStyle().set( "border" , "0.5px solid black" ) ;
        //grid_period.getStyle().set( "box-shadow" , "0 10px 6px -6px black" ) ;
        grid_period.getStyle().set( "box-shadow" , "5px 6px 4px gray" ) ;
        grid_period.getStyle().set( "border-radius" , "2px" ) ;
        grid_period.getStyle().set( "padding" , "25px" ) ;

        grid_period.getElement().getStyle().set("padding", "20px");

        grid_period.getColumns().forEach(e -> e.setResizable(Boolean.TRUE));

        grid_period.setItems(periods);

        content.add(p2, grid_period);
        content.setHeightFull();
        return content;
    }

    private Component getDimScenarioGrid() {
        VerticalLayout content = new VerticalLayout();

        List<String> qfc = projectConnectionService.getCobiAdminQFCPlanOutlook(dbUrl, dbUser, dbPassword, sqlQfcScenarios);
        List<String> plan = projectConnectionService.getCobiAdminQFCPlanOutlook(dbUrl, dbUser, dbPassword, sqlPlanScenarios);
        List<String> outlook = projectConnectionService.getCobiAdminQFCPlanOutlook(dbUrl, dbUser, dbPassword, sqlOutlookScenario);

        System.out.println("execute SQL for sqlAktCurrentScenario:s " + sqlAktCurrentScenarios);

        List<String> aktCurrentScenarios = projectConnectionService.getCobiAdminQFCPlanOutlook(dbUrl, dbUser, dbPassword, sqlAktCurrentScenarios);

        System.out.println(aktCurrentScenarios + " aktCurrentScenarios..........");
        List<CurrentScenarios> scenarios = new ArrayList<>();
        currentScenarios = new CurrentScenarios();

        H3 p3 = new H3();
        p3.setText("Scenario:");

        Grid<CurrentScenarios> grid_scenario = new Grid<>(CurrentScenarios.class, false);
        grid_scenario.addClassName("grid_scenario");

        grid_scenario.addComponentColumn(cs -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setItems(qfc);
            if(aktCurrentScenarios != null && !aktCurrentScenarios.isEmpty()) {
                comboBox.setValue(aktCurrentScenarios.get(2));
                currentScenarios.setCurrent_QFC(aktCurrentScenarios.get(2));
                cs.setCurrent_QFC(aktCurrentScenarios.get(2));
            } else {
                comboBox.setValue(qfc.get(0));
                currentScenarios.setCurrent_QFC(qfc.get(0));
                cs.setCurrent_QFC(qfc.get(0));
                Notification.show("No valid sql for Current Scenarios in project_properties", 5000, Notification.Position.MIDDLE);
            }
            comboBox.addValueChangeListener(event -> {
                cs.setCurrent_QFC(event.getValue());
                currentScenarios.setCurrent_QFC(event.getValue());
            });
            return comboBox;
        }).setHeader("Current QFC").setFlexGrow(0).setAutoWidth(true);
        grid_scenario.addComponentColumn(cs -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setItems(plan);
            if(aktCurrentScenarios != null && !aktCurrentScenarios.isEmpty()) {
                comboBox.setValue(aktCurrentScenarios.get(0));
                currentScenarios.setCurrent_Plan(aktCurrentScenarios.get(0));
                cs.setCurrent_Plan(aktCurrentScenarios.get(0));
            } else {
                comboBox.setValue(plan.get(0));
                currentScenarios.setCurrent_Plan(plan.get(0));
                cs.setCurrent_Plan(plan.get(0));
              //  Notification.show("No valid sql in project_properties", 5000, Notification.Position.MIDDLE);
            }

            comboBox.addValueChangeListener(event -> {
                cs.setCurrent_Plan(event.getValue());
                currentScenarios.setCurrent_Plan(event.getValue());
            });
            return comboBox;
        }).setHeader("Current Plan").setFlexGrow(0).setAutoWidth(true);
        grid_scenario.addComponentColumn(cs -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setItems(outlook);
            if(aktCurrentScenarios != null && !aktCurrentScenarios.isEmpty()) {
                comboBox.setValue(aktCurrentScenarios.get(1));
                currentScenarios.setCurrent_Outlook(aktCurrentScenarios.get(1));
                cs.setCurrent_Outlook(aktCurrentScenarios.get(1));
            } else {
                comboBox.setValue(outlook.get(0));
                currentScenarios.setCurrent_Outlook(outlook.get(0));
                cs.setCurrent_Outlook(outlook.get(0));
                //  Notification.show("No valid sql in project_properties", 5000, Notification.Position.MIDDLE);
            }

            comboBox.addValueChangeListener(event -> {
                cs.setCurrent_Outlook(event.getValue());
                currentScenarios.setCurrent_Outlook(event.getValue());
            });
            return comboBox;
        }).setHeader("Current Outlook").setFlexGrow(0).setAutoWidth(true);

        grid_scenario.setWidth("650px");
        grid_scenario.setHeight("100px");
        //grid_scenario.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        grid_scenario.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid_scenario.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid_scenario.getColumns().forEach(e -> e.setResizable(Boolean.TRUE));
        grid_scenario.getStyle().set( "border" , "0.5px solid black" ) ;
        //grid_scenario.getStyle().set( "box-shadow" , "0 10px 6px -6px black" ) ;
        grid_scenario.getStyle().set( "box-shadow" , "5px 6px 4px gray" ) ;
        grid_scenario.getStyle().set( "border-radius" , "1px" ) ;

        scenarios.add(currentScenarios);
        grid_scenario.setItems(scenarios);

        content.add(p3, grid_scenario);
        content.setHeightFull();
        return content;
    }

}