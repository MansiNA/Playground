package de.dbuss.tefcontrol.data.modules.cobi_administration.view;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.modules.cobi_administration.entity.CurrentPeriods;
import de.dbuss.tefcontrol.data.modules.cobi_administration.entity.CurrentScenarios;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.view.GenericCommentsView;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@PageTitle("Administration")
@Route(value = "COBI_Administration/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "MAPPING", "FLIP"})
public class CobiAdminView extends VerticalLayout {
    private final ProjectConnectionService projectConnectionService;
    private String tableCurrentPeriods;
    private String tableCurrentSenarios;
    private String sqlPlanScenarios;
    private String sqlOutlookScenario;
    private String sqlQfcScenarios;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private Button startBtn;
    private int upload_id;
    private CurrentScenarios currentScenarios;
    private CurrentPeriods currentPeriods;

    public CobiAdminView(ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService) {
        this.projectConnectionService = projectConnectionService;

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
                }
            }
        }
        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";

        startBtn = new Button("Start");

        Map<String, Integer> uploadIdMap = projectConnectionService.getUploadIdMap();
        upload_id = uploadIdMap.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(1);


        System.out.println(upload_id+"mmmmmmmmmmmmmmmmmmmmmmmmm");

        H1 h1 = new H1("Cobi Administration");
        Article p1 = new Article();
        p1.setText("Auf diese Seite lässt verschiedene Einstellungen zur COBI-Beladung vornehmen.");
        add();
        add(h1, p1, getDimPeriodGrid(), getDimScenarioGrid());

        Button okBtn = new Button("OK");
        okBtn.addClickListener(e -> {

            String resultOfPeriods = projectConnectionService.saveCobiAdminCurrentPeriods(currentPeriods, dbUrl, dbUser, dbPassword, tableCurrentPeriods);
            Notification notification;
            if (resultOfPeriods.equals(Constants.OK)) {
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
        });

        add(okBtn);
    }

    private Component getDimPeriodGrid() {
        VerticalLayout content = new VerticalLayout();

        H3 p2 = new H3();
        p2.setText("Dim Period:");

        List<CurrentPeriods> periods = new ArrayList<>();
        currentPeriods = new CurrentPeriods();
        currentPeriods.setUpload_ID(upload_id);
        currentPeriods.setCurrent_month(YearMonth.now().toString().replace("-", ""));
        periods.add(currentPeriods);

        Grid<CurrentPeriods> grid_period = new Grid<>(CurrentPeriods.class, false);
        grid_period.setColumns("current_month");
        grid_period.getColumnByKey("current_month").setHeader("Current-Month").setAutoWidth(true).setFlexGrow(0).setResizable(true);
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
            cp.setPreliminary_month(monthPeriod.get(0));
            currentPeriods.setPreliminary_month(monthPeriod.get(0));
            comboBox.addValueChangeListener(event -> {
                cp.setPreliminary_month(event.getValue());
                currentPeriods.setPreliminary_month(event.getValue());
            });
            return comboBox;
        }).setHeader("Preliminary-Month").setFlexGrow(0).setAutoWidth(true);
        grid_period.setWidth("500px");
        grid_period.setHeight("110px");
        grid_period.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        grid_period.addThemeVariants(GridVariant.LUMO_COMPACT);
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

        List<CurrentScenarios> scenarios = new ArrayList<>();
        currentScenarios = new CurrentScenarios();
        currentScenarios.setUpload_ID(upload_id);

        H3 p3 = new H3();
        p3.setText("Dim Scenario:");

        Grid<CurrentScenarios> grid_scenario = new Grid<>(CurrentScenarios.class, false);
        grid_scenario.addComponentColumn(cs -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setItems(qfc);
            comboBox.setValue(qfc.get(0));
            currentScenarios.setCurrent_QFC(qfc.get(0));
            cs.setCurrent_QFC(qfc.get(0));
            comboBox.addValueChangeListener(event -> {
                cs.setCurrent_QFC(event.getValue());
                currentScenarios.setCurrent_QFC(event.getValue());
            });
            return comboBox;
        }).setHeader("Current QFC").setFlexGrow(0).setAutoWidth(true);
        grid_scenario.addComponentColumn(cs -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setItems(plan);
            comboBox.setValue(plan.get(0));
            currentScenarios.setCurrent_Plan(plan.get(0));
            cs.setCurrent_Plan(plan.get(0));
            comboBox.addValueChangeListener(event -> {
                cs.setCurrent_Plan(event.getValue());
                currentScenarios.setCurrent_Plan(event.getValue());
            });
            return comboBox;
        }).setHeader("Current Plan").setFlexGrow(0).setAutoWidth(true);
        grid_scenario.addComponentColumn(cs -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setItems(outlook);
            comboBox.setValue(outlook.get(0));
            currentScenarios.setCurrent_Outlook(outlook.get(0));
            cs.setCurrent_Outlook(outlook.get(0));
            comboBox.addValueChangeListener(event -> {
                cs.setCurrent_Outlook(event.getValue());
                currentScenarios.setCurrent_Outlook(event.getValue());
            });
            return comboBox;
        }).setHeader("Current Outlook").setFlexGrow(0).setAutoWidth(true);

        grid_scenario.setWidth("700px");
        grid_scenario.setHeight("110px");
        grid_scenario.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        grid_scenario.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid_scenario.getColumns().forEach(e -> e.setResizable(Boolean.TRUE));

        scenarios.add(currentScenarios);
        grid_scenario.setItems(scenarios);

        content.add(p3, grid_scenario);
        content.setHeightFull();
        return content;
    }

}