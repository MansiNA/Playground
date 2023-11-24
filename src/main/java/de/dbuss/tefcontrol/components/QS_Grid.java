package de.dbuss.tefcontrol.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import lombok.Getter;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.util.*;

public class QS_Grid extends Composite<Div> {

    private List<ProjectQSEntity> listOfProjectQs;
    private Grid<ProjectQSEntity> grid;
    private JdbcTemplate jdbcTemplate;
    private ProjectConnectionService projectConnectionService;
    Dialog qsDialog = new Dialog();
    private Dialog contextDialog;
    private Button cancelContextButton;
    QS_Callback qs_callback;
    @Getter
    public int projectId;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private Map<Integer, List<Map<String, Object>>> rowsMap = new HashMap<>();

    public QS_Grid(ProjectConnectionService projectConnectionService) {
        this.projectConnectionService = projectConnectionService;
        this.jdbcTemplate = projectConnectionService.getJdbcDefaultConnection();
    }

    public void createDialog(QS_Callback callback, int projectId) {
        this.qs_callback=callback;
        this.projectId=projectId;

        VerticalLayout dialogLayout = createDialogLayout();
        qsDialog.add(dialogLayout);
        qsDialog.setDraggable(true);
        qsDialog.setResizable(true);
        qsDialog.setVisible(false);
        qsDialog.setHeaderTitle("QS for Project-ID " + projectId );
        getContent().add(qsDialog);

        createContextMenu();
    }

    public void showDialog(boolean show)
    {
        if (show){
            qsDialog.open();
            qsDialog.setVisible(true);

        }
        else
        {
            qsDialog.close();
            qsDialog.setVisible(false);
        }
    }

    private VerticalLayout createDialogLayout() {
        getListOfProjectQsWithResult();

        grid = new Grid<>(ProjectQSEntity.class, false);
        grid.addColumn(ProjectQSEntity::getName).setHeader("QS-Name");
        // grid.addColumn(ProjectQSEntity::getResult).setHeader("Result");

        grid.addComponentColumn(projectQs -> {

            HorizontalLayout layout = new HorizontalLayout();
            Icon icon = new Icon();
            String status = projectQs.getResult();

            if (Constants.FAILED.equals(status)) {
                icon = VaadinIcon.CLOSE_CIRCLE.create();
                icon.getElement().getThemeList().add("badge error");
                layout.add(icon);
            } else if (Constants.OK.equals(status)) {
                icon = VaadinIcon.CHECK.create();
                icon.getElement().getThemeList().add("badge success");
                layout.add(icon);
            } else {
                icon = VaadinIcon.SPINNER.create();
                icon.getElement().getThemeList().add("badge spinner");
                if(status == null) {
                    status = "before execute...";
                }
                layout.add(status);
                System.out.println(status);
            }
            icon.getStyle().set("padding", "var(--lumo-space-xs");

            return layout;

        }).setHeader("Result").setFlexGrow(0).setWidth("300px").setResizable(true);


        grid.setItems(listOfProjectQs);

        // updateListOfProjectQs();

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("min-width", "300px")
                .set("max-width", "100%").set("height", "100%");

        Paragraph paragraph = new Paragraph(
                "Please check failed Checks. Only when all tests are ok further processing can startet");

        Button okButton = new Button("Start Job");
        okButton.setEnabled(false);
        okButton.addClickListener(e -> {
            qsDialog.close();
            qs_callback.onComplete("Start further prcessing...");
        });

        // if all result Ok then okbutton enable

        Button executeButton = new Button("execute");
        executeButton.addClickListener(e -> {
            listOfProjectQs = getResultExecuteSQL(dbUrl, dbUser, dbPassword, listOfProjectQs);
            grid.setItems(listOfProjectQs);
            boolean allCorrect = listOfProjectQs.stream()
                    .allMatch(projectQs -> Constants.OK.equals(projectQs.getResult()));
            okButton.setEnabled(allCorrect);
        });
        HorizontalLayout hlexecute = new HorizontalLayout(executeButton);

        if (listOfProjectQs.isEmpty()) {
            executeButton.setEnabled(false);
            okButton.setEnabled(true);
        }
        Button closeButton = new Button("Close");
        closeButton.addClickListener(e -> {
            qsDialog.close();
            qs_callback.onComplete("Cancel");
        });

        HorizontalLayout hl = new HorizontalLayout(closeButton,okButton);

        dialogLayout.add(paragraph, hlexecute, grid,hl);

        return dialogLayout;
    }

    private void getListOfProjectQsWithResult() {
        String tableName = "project_qs";
        listOfProjectQs = getProjectQSList(tableName);

        String sql = "select pp.name, pp.value from pit.dbo.project_parameter pp, [PIT].[dbo].[projects] p\n" +
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

    }

    public  void setListOfProjectQs(List<ProjectQSEntity> listOfProjectQs) {
        this.listOfProjectQs = listOfProjectQs;
    }

    public  void updateListOfProjectQs() {
        listOfProjectQs = getResultExecuteSQL(dbUrl, dbUser, dbPassword, listOfProjectQs);
        grid.setItems(listOfProjectQs);
    }
    public List<ProjectQSEntity> getProjectQSList(String tableName) {
        try {

            String sqlQuery = "SELECT * FROM " + tableName + " WHERE [project_id] =" + projectId;

            // Create a RowMapper to map the query result to a ProjectQSEntity object
            RowMapper<ProjectQSEntity> rowMapper = (rs, rowNum) -> {
                ProjectQSEntity projectQSEntity = new ProjectQSEntity();
                projectQSEntity.setId(rs.getInt("id"));
                projectQSEntity.setName(rs.getString("name"));
                projectQSEntity.setSql(rs.getString("sql"));
                projectQSEntity.setDescription(rs.getString("description"));
                Projects projects = new Projects();
                projects.setId(rs.getLong("project_id"));
                projectQSEntity.setProject(projects);
                projectQSEntity.setCreate_date(rs.getDate("create_date"));
                return projectQSEntity;
            };

            List<ProjectQSEntity> fetchedData = jdbcTemplate.query(sqlQuery, rowMapper);

            return fetchedData;
        } catch (Exception ex) {
            ex.printStackTrace();
            String errorMessage = handleDatabaseError(ex);
            return Collections.emptyList();
        }
    }
    public List<ProjectQSEntity> getResultExecuteSQL(String dbUrl, String dbUser, String dbPassword, List<ProjectQSEntity> listOfProjectQs) {
        for (ProjectQSEntity projectQSEntity : listOfProjectQs) {
            String sql = projectQSEntity.getSql();

            if(sql != null ) {
                try {

                    DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
                    jdbcTemplate = new JdbcTemplate(dataSource);
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

                    if (rows.isEmpty()) {
                        projectQSEntity.setResult(Constants.OK);
                    } else {
                        rowsMap.put(projectQSEntity.getId(), rows);
                        projectQSEntity.setResult(Constants.FAILED);
                    }

                } catch ( Exception e) {

                    //   e.printStackTrace();
                    String errormessage = handleDatabaseError(e);
                    projectQSEntity.setResult(errormessage);
                    //  Notification.show( "Error during execute " + errormessage,5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        }
        return listOfProjectQs;
    }
    private void createContextMenu() {
        // Add a context menu to the grid
        GridContextMenu<ProjectQSEntity> contextMenu = new GridContextMenu<>(grid);

        // Add a menu item for "Show rows"
        contextMenu.addItem("Show rows", this::showRows);

        // Add a menu item for "Show Description"
        contextMenu.addItem("Show Description", this::showDescription);

        contextDialog = new Dialog();
        contextDialog.setDraggable(true);
        contextDialog.setResizable(true);
        cancelContextButton = new Button("Cancel");
        cancelContextButton.addClickListener(e -> contextDialog.close());
    }

    private void showRows(GridContextMenu.GridContextMenuItemClickEvent<ProjectQSEntity> event) {
        // Get the selected ProjectQSEntity
        ProjectQSEntity selectedProjectQS = event.getItem().orElse(null);

        // Check if a row is selected
        if (selectedProjectQS != null) {
            contextDialog.removeAll();
            contextDialog.setWidth("800px"); // Set the width as per your requirement

            // Create a grid for rows

            Grid<Map<String, Object>> rowsGrid = new Grid<>();
            // Set the data for the grid
            List<Map<String, Object>> rowsData = rowsMap.get(selectedProjectQS.getId());

            if (rowsData != null) {

                rowsGrid.setItems(rowsData);

                // Add columns dynamically based on the keys in the first row (assuming all rows have the same keys)
                Set<String> columns = rowsData.isEmpty() ?
                        Collections.emptySet() : rowsData.get(0).keySet();

                for (String column : columns) {
                    rowsGrid.addColumn(row -> row.get(column)).setHeader(column).setAutoWidth(true);
                }
                // Add components to the rowsDialog layout
                VerticalLayout rowsDialogContent = new VerticalLayout();
                rowsDialogContent.add(rowsGrid, cancelContextButton);
                contextDialog.add(rowsDialogContent);
                contextDialog.open();
                // Notification.show("Show rows for QS ID: " + selectedProjectQS.getId());
            }

            contextDialog.addResizeListener(e -> {
                // Adjust the grid's width when the dialog is resized
                rowsGrid.setWidth(e.getWidth());
                rowsGrid.setHeight(e.getHeight());
            });

        }

    }

    private void showDescription(GridContextMenu.GridContextMenuItemClickEvent<ProjectQSEntity> event) {
        // Get the selected ProjectQSEntity
        ProjectQSEntity selectedProjectQS = event.getItem().orElse(null);

        // Check if a row is selected
        if (selectedProjectQS != null && (selectedProjectQS.getProject().getId() == projectId)) {
            contextDialog.removeAll();
            contextDialog.add(new Paragraph(selectedProjectQS.getDescription()));
            contextDialog.add(cancelContextButton);
            contextDialog.open();
            Notification.show("Show Description for QS ID: " + selectedProjectQS.getId());
        }
    }

    public DataSource getDataSourceUsingParameter(String dbUrl, String dbUser, String dbPassword) {

        if(dbUser != null) {
            System.out.println(dbUrl);
            System.out.println("Username = " + dbUser + " Password = " + dbPassword);
            DataSource dataSource = DataSourceBuilder
                    .create()
                    .url(dbUrl)
                    .username(dbUser)
                    .password(dbPassword)
                    .build();
            return dataSource;
        }

        throw new RuntimeException("Database connection not found: " + dbUser);
    }
    public String handleDatabaseError(Exception e) {

        if (e instanceof DataAccessException) {
            Throwable rootCause = getRootCause(e);
            if (rootCause instanceof org.springframework.jdbc.CannotGetJdbcConnectionException) {
                return "Error: Cannot connect to the database. Check database configuration.";
            } else if (rootCause instanceof org.springframework.jdbc.BadSqlGrammarException) {
                return "Error: Table does not exist or SQL syntax error.";
            } else {
                e.printStackTrace();
                if(e.getMessage().contains(";")) {
                    String [] errorMessage = e.getMessage().split(";");
                    return errorMessage[errorMessage.length - 1];
                }
                return "Database error: " + e.getMessage();
            }
        } else {
            e.printStackTrace();
            return "Unknown error: " + e.getMessage();
        }
    }
    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause == null) {
            return throwable;
        }
        return getRootCause(cause);
    }
}