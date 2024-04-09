package de.dbuss.tefcontrol.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.service.BackendService;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import lombok.Getter;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.concurrent.ListenableFuture;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

public class QS_Grid extends Composite<Div> {

    private List<ProjectQSEntity> listOfProjectQs;
    private Grid<ProjectQSEntity> grid;
    private JdbcTemplate jdbcTemplate;
    private ProjectConnectionService projectConnectionService;
    private BackendService backendService;
    Dialog qsDialog = new Dialog();
    private Dialog contextDialog;
    private Button cancelContextButton;
    QS_Callback qs_callback;
    @Getter
    public int projectId;
    private int uploadId;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    //private ProgressBar progressBar = new ProgressBar();
    private AtomicInteger threadCount = new AtomicInteger(0);
    //private TextField threadCountField;
    private Button okButton;
    private Map<Integer, List<Map<String, Object>>> rowsMap = new HashMap<>();

    public QS_Grid(ProjectConnectionService projectConnectionService, BackendService backendService) {
        this.projectConnectionService = projectConnectionService;
        this.backendService = backendService;
       // this.jdbcTemplate = projectConnectionService.getJdbcDefaultConnection();
    }

    public void createDialog(QS_Callback callback, int projectId) {
        this.qs_callback=callback;
        this.projectId=projectId;

    //    progressBar.setWidth("15em");
    //    progressBar.setIndeterminate(true);
    //    progressBar.setVisible(false);

        VerticalLayout dialogLayout = createDialogLayout();
        qsDialog.add(dialogLayout);
        qsDialog.setDraggable(true);
        qsDialog.setResizable(true);
        qsDialog.setVisible(false);
        qsDialog.setHeaderTitle("Execute QS-Statements");
        getContent().add(qsDialog);

        // createContextMenu();
    }
    public void createDialog(QS_Callback callback, int projectId, int uploadId) {
        this.qs_callback = callback;
        this.projectId = projectId;
        this.uploadId = uploadId;

        VerticalLayout dialogLayout = createDialogLayout();
        qsDialog.add(dialogLayout);
        qsDialog.setDraggable(true);
        qsDialog.setResizable(true);
        qsDialog.setVisible(false);
        qsDialog.setHeaderTitle("Execute QS-Statements (upload-id " + uploadId + ")" );
        getContent().add(qsDialog);

        // createContextMenu();
    }
    public void showDialog(boolean show)
    {
        if (show){
            qsDialog.open();
            qsDialog.setVisible(true);
           // executeSQL(listOfProjectQs);

        }
        else
        {
            qsDialog.close();
            qsDialog.setVisible(false);
        }
    }

    private VerticalLayout createDialogLayout() {
        getListOfProjectQsWithResult();

        String SHOWROWS = "showRows";
        String SHOWDESRIPTION = "showdescription";
        grid = new Grid<>(ProjectQSEntity.class, false);
        grid.addColumn(ProjectQSEntity::getName).setHeader("QS-Name").setResizable(true).setAutoWidth(true);
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
                System.out.println("icon changed.......");
                icon.getElement().getThemeList().add("badge spinner");
                if(status == null) {
                    status = "";
                    layout.add(status);
                } else if (status.equals("running")){
                    layout.add(createIcon());
                } else {
                    layout.add(status);
                }
            }
            icon.getStyle().set("padding", "var(--lumo-space-xs");

            return layout;

        }).setHeader("Result").setFlexGrow(0).setWidth("100px").setResizable(true);

        contextDialog = new Dialog();
        contextDialog.setDraggable(true);
        contextDialog.setResizable(true);
        cancelContextButton = new Button("Cancel");
        cancelContextButton.addClickListener(e -> contextDialog.close());

        grid.addComponentColumn(user -> {
            Button rowsBtn = new Button("Show Rows");
            rowsBtn.addClickListener(event -> {
                showRows(user);
            });
            return rowsBtn;
        }).setKey(SHOWROWS).setFlexGrow(0).setWidth("140px");

        grid.addComponentColumn(user -> {
            Button descriptionBtn = new Button("Show Description");
            descriptionBtn.addClickListener(event -> {
                showDescription(user);
            });
            return descriptionBtn;
        }).setKey(SHOWDESRIPTION).setFlexGrow(0).setWidth("140px");

        grid.setItems(listOfProjectQs);
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        // updateListOfProjectQs();

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.removeAll();
        dialogLayout.setPadding(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("min-width", "300px")
                .set("max-width", "100%").set("height", "100%");

        Paragraph paragraph = new Paragraph(
                "Please check failed Checks. Only when all tests are ok further processing can startet");

        okButton = new Button("Start Job");
        okButton.setEnabled(false);
        okButton.addClickListener(e -> {
            qsDialog.close();
            qs_callback.onComplete("Start further prcessing...");
        });


        //Button isBlockedButton = new Button("Is UI blocked?", clickEvent -> {
        //    Notification.show("UI isn't blocked!");
        //});

//        Button executeButton = new Button("execute");
//        executeButton.addClickListener(e -> {
            executeSQL(listOfProjectQs);
//       });

        //threadCountField = new TextField("Anzahl der Threads");
        //threadCountField.setReadOnly(true); // Textfeld schreibgeschützt machen, um es nur lesbar zu machen
        //updateThreadCountField();
  //      HorizontalLayout hlexecute = new HorizontalLayout();
  //      hlexecute.add(executeButton);

        if (listOfProjectQs.isEmpty()) {
    //        executeButton.setEnabled(false);
            okButton.setEnabled(true);
        }
        Button closeButton = new Button("Close");
        closeButton.addClickListener(e -> {
            qsDialog.close();
            qs_callback.onComplete("Cancel");
        });

        HorizontalLayout hl = new HorizontalLayout(closeButton,okButton);

        //dialogLayout.add(paragraph, hlexecute, grid,hl);
        dialogLayout.add(paragraph, grid,hl);

        return dialogLayout;
    }

    private Component createIcon() {
        String imageUrl = "icons/spinner.gif";
        Image image = new Image(imageUrl, "GIF Icon");
        image.setWidth("20px");
        image.setHeight("20px");

        // Das Image-Objekt zurückgeben
        return image;
    }

    private void executeSQL(List<ProjectQSEntity> projectSqls) {

        UI ui = getUI().orElseThrow();

        for (ProjectQSEntity projectQS:projectSqls) {
            projectQS.setResult("running");
        }
        grid.getDataProvider().refreshAll();
        System.out.println("running changed.......");
        DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
        jdbcTemplate = new JdbcTemplate(dataSource);

        for (ProjectQSEntity projectQS:projectSqls) {
            System.out.println("Ausführen SQL: " + projectQS.getSql() );
            try {
                increaseThreadCount();

                //ListenableFuture<String> future = backendService.longRunningTask();

                String sql = projectQS.getSql();
                if (sql.contains("UPLOAD_ID")) {
                    sql = sql.replace("UPLOAD_ID", uploadId + "");
                    projectQS.setSql(sql);
                }

                ListenableFuture<ProjectQSEntity> future = backendService.getQsResult(jdbcTemplate, projectQS, rowsMap);
                future.addCallback(
                        successResult -> {
                            decreaseThreadCount();
                            updateUi(ui, "Task finished: SQL: " + successResult.getId() + " Ergebnis: " + successResult.getResult());

                        },

                        failureException -> {
                            decreaseThreadCount();
                            projectQS.setResult("Error in SQL");
                            updateUi(ui, "Task failed: " + failureException.getMessage());
                        //    projectConnectionService.connectionClose(jdbcTemplate);
                        }

                );

            } catch (Exception e){
                String errormessage = handleDatabaseError(e);
                System.out.println("error changed......."+projectQS.getName());
                projectQS.setResult(errormessage);
            }
        }
    }

    private void getListOfProjectQsWithResult() {
        String tableName = "project_qs";
        jdbcTemplate = projectConnectionService.getJdbcDefaultConnection();
        listOfProjectQs = getProjectQSList(tableName);

        String sql = "select pp.name, pp.value from project_parameter pp, projects p\n" +
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
        projectConnectionService.connectionClose(jdbcTemplate);
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
                    if(sql.contains("UPLOAD_ID")) {
                        sql = sql.replace("UPLOAD_ID", uploadId + "");
                        System.out.println(sql+"++++++++++++++++++++++++++++++++++++++");
                    }

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
                }finally {
                    projectConnectionService.connectionClose(jdbcTemplate);
                }
            }
        }
        return listOfProjectQs;
    }
    private void createContextMenu() {
        // Add a context menu to the grid
        GridContextMenu<ProjectQSEntity> contextMenu = new GridContextMenu<>(grid);

        // Add a menu item for "Show rows"
       // contextMenu.addItem("Show rows", this::showRows);

        // Add a menu item for "Show Description"
      //  contextMenu.addItem("Show Description", this::showDescription);

        contextDialog = new Dialog();
        contextDialog.setDraggable(true);
        contextDialog.setResizable(true);
        cancelContextButton = new Button("Cancel");
        cancelContextButton.addClickListener(e -> contextDialog.close());
    }

    private void showRows(ProjectQSEntity selectedProjectQS) {

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
                    rowsGrid.addColumn(row -> row.get(column)).setHeader(column).setAutoWidth(true).setResizable(true);
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

    private void showDescription(ProjectQSEntity selectedProjectQS) {

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

/*    private void updateThreadCountField() {
        int count = threadCount.get();
        // Setze die Thread-Anzahl im Textfeld
        System.out.println(count + "--------------------------------");
        threadCountField.setValue(String.valueOf(count));
    }
*/
    private void updateUi(UI ui, String result) {
        ui.access(() -> {
            Notification.show(result);

          //  updateThreadCountField();
            int count = threadCount.get();
            System.out.println("Anzahl Threads jetzt: " + count);

            grid.getDataProvider().refreshAll();

            if (count == 0)
            {
                //progressBar.setVisible(false);
                boolean allCorrect = listOfProjectQs.stream()
                        .allMatch(projectQs -> Constants.OK.equals(projectQs.getResult()));
                okButton.setEnabled(allCorrect);
                projectConnectionService.connectionClose(jdbcTemplate);
            }

        });

    }

    private void increaseThreadCount() {
        int count = threadCount.incrementAndGet();
     //   updateThreadCountField();
    }

    private void decreaseThreadCount() {
        int count = threadCount.decrementAndGet();
    }

    public void executeStartJobSteps(int upload_id, String agentName){
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
            jdbcTemplate = new JdbcTemplate(dataSource);
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

                    try {
                        jdbcTemplate.execute(sql1);
                    }
                    catch (Exception e)
                    {
                        Notification.show("Update Agent_Job_Uploads failed: " + e.getMessage(), 8000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }


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
                layout.setAlignItems(FlexComponent.Alignment.CENTER);

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
        } finally {
            projectConnectionService.connectionClose(jdbcTemplate);
        }

    }
}