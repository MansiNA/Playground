package de.dbuss.tefcontrol.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.gridpro.GridPro;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.entity.ProjectQSEntity;
import de.dbuss.tefcontrol.data.modules.cltv_Inflow.entity.CLTVInflow;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.UnitsDeepDive;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@PageTitle("QS_View")
@Route(value = "QS_View", layout = MainLayout.class)
@RolesAllowed({"MAPPING", "ADMIN"})
public class QsView extends VerticalLayout {
    private final ProjectConnectionService projectConnectionService;
    private Crud<ProjectQSEntity> crud;
    private Grid<ProjectQSEntity> grid;
    private List<ProjectQSEntity> allProjectQSData;
    private List<ProjectQSEntity> filterProjectQsList;
    private String tableName;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    public QsView(ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService) {
        this.projectConnectionService = projectConnectionService;

        setSizeFull();

        String dbServer = "128.140.47.43";
        String dbName = "PIT";
        dbUser = "PIT";
        dbPassword = "PIT!20230904";
        tableName = "project_qs";


        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";
        Text databaseDetail = new Text("Connected to: "+ dbServer+ ", Database: " + dbName+ ", Table: " + tableName);

        allProjectQSData = projectConnectionService.getAllProjectQS(tableName, dbUrl, dbUser, dbPassword);

        filterProjectQsList = allProjectQSData.stream()
                .filter(projectQsData -> projectQsData.getQs_group() == 1)
                .collect(Collectors.toList());

        Button button = new Button("execute");
        HorizontalLayout hl = new HorizontalLayout();
        hl.add(button, databaseDetail);
        hl.setAlignItems(Alignment.BASELINE);
        add(hl, getProjectQsGrid());

        button.addClickListener(event -> {
            executeSQL();
        });
        updateGrid();
    }
    private List<ProjectQSEntity> getDataProviderAllItems() {
        DataProvider<ProjectQSEntity, Void> existDataProvider = (DataProvider<ProjectQSEntity, Void>) grid.getDataProvider();
        List<ProjectQSEntity>  listOfProjectQs = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        return listOfProjectQs;
    }
    private void executeSQL() {
        List<ProjectQSEntity> modifiedList = new ArrayList<>();
        for (ProjectQSEntity projectQSEntity : getDataProviderAllItems()) {
            String sql = projectQSEntity.getSql();

            if(sql != null && sql.contains("PIT2")) {
                try {
                    JdbcTemplate jdbcTemplate = projectConnectionService.getJdbcConnection("PIT2");
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

                    if (rows.isEmpty()) {
                        projectQSEntity.setResult("correct");
                        modifiedList.add(projectQSEntity);
                    } else {
                        projectQSEntity.setResult("cross");
                        modifiedList.add(projectQSEntity);
                    }

                } catch ( Exception e) {
                    projectQSEntity.setResult("error");
                    modifiedList.add(projectQSEntity);
                    e.printStackTrace();
                    String errormessage = projectConnectionService.handleDatabaseError(e);
                    Notification.show( "Error during execute " + errormessage,5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        }
        GenericDataProvider dataProvider = new GenericDataProvider(modifiedList, "id");
        grid.setDataProvider(dataProvider);

    }


    private final AtomicInteger activeTasks = new AtomicInteger(0);

    public void executeSqlStatements(List<String> sqlStatements, JdbcTemplate jdbcTemplate) {
        for (String sql : sqlStatements) {
            if (activeTasks.get() >= 5) {
                // Maximum 5 tasks running, wait until one finishes
                waitForTask();
            }

            activeTasks.incrementAndGet();
            new Thread(() -> {
                try {
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

                    // Process the rows as needed
                    for (Map<String, Object> row : rows) {
                        System.out.println(row + "mmmmmmmmmmmmmmm................");
                    }

                    // Update UI on the UI thread

                } catch (Exception e) {
                    e.printStackTrace();
                    // Update UI with error message
                            Notification.show("Error during execute " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                } finally {
                    activeTasks.decrementAndGet();
                }
            }).start();
        }
    }

    private synchronized void waitForTask() {
        try {
            // Wait until an active task finishes
            wait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void notifyTaskFinished() {
        // Notify waiting threads that a task has finished
        notify();
    }

    private void updateGrid() {
        GenericDataProvider dataProvider = new GenericDataProvider(filterProjectQsList, "id");
        grid.setDataProvider(dataProvider);
    }


    private Component getProjectQsGrid() {

        VerticalLayout content = new VerticalLayout();
        crud = new Crud<>(ProjectQSEntity.class, createEditor());
        configureGrid();
        crud.setToolbarVisible(false);
        crud.setSizeFull();
        content.setAlignItems(Alignment.END);
        content.add(crud);
        content.setHeightFull();
        return content;
    }

    private void configureGrid() {

        String EDIT_COLUMN = "vaadin-crud-edit-column";
        grid = crud.getGrid();
        grid.setSizeFull();
        grid.setHeightFull();
        // if setcolumn then filter not display
        grid.setColumns("name");

        grid.getColumnByKey("name").setHeader("Name").setFlexGrow(0).setResizable(true);
        //grid.getColumnByKey("result").setHeader("Result").setFlexGrow(0).setResizable(true);

        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);


        grid.addComponentColumn(projectQs -> {

            HorizontalLayout layout = new HorizontalLayout();
            Icon icon = new Icon();
            String status = projectQs.getResult();

            if ("cross".equals(status)) {
                icon = VaadinIcon.CLOSE_CIRCLE.create();
                icon.getElement().getThemeList().add("badge error");
            } else if ("correct".equals(status)) {
                icon = VaadinIcon.CHECK.create();
                icon.getElement().getThemeList().add("badge success");
            } else {
                icon = VaadinIcon.SPINNER.create();
                icon.getElement().getThemeList().add("badge spinner");
            }
            icon.getStyle().set("padding", "var(--lumo-space-xs");
            layout.add(icon);
            return layout;

        }).setHeader("Result").setFlexGrow(0).setWidth("300px").setResizable(true);

    }

    private CrudEditor<ProjectQSEntity> createEditor() {
        FormLayout editForm = new FormLayout();
        Binder<ProjectQSEntity> binder = new Binder<>(ProjectQSEntity.class);
        return new BinderCrudEditor<>(binder, editForm);
    }

}
