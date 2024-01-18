package de.dbuss.tefcontrol.data.modules.b2pOutlook.view;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.entity.ProjectUpload;
import de.dbuss.tefcontrol.data.entity.User;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.entity.B2pOutlookSub;
import de.dbuss.tefcontrol.data.service.BackendService;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.concurrent.ListenableFuture;

import javax.sql.DataSource;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

@Route(value = "B2P_Outlook_Sub/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "FLIP"})
public class B2POutlookSUBView extends VerticalLayout implements BeforeEnterObserver {

    private final ProjectConnectionService projectConnectionService;
    private MemoryBuffer memoryBuffer = new MemoryBuffer();
    private Upload singleFileUpload = new Upload(memoryBuffer);
    private List<List<B2pOutlookSub>> listOfAllSheets = new ArrayList<>();
    private Crud<B2pOutlookSub> crudOutlookSub;
    private Grid<B2pOutlookSub> gridOutlookSub = new Grid<>(B2pOutlookSub.class, false);
    private String tableName;
    private String agentName;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private long contentLength = 0;
    private String mimeType = "";
    private Div textArea = new Div();
    private int projectId;
    int sheetNr = 0;
    private QS_Grid qsGrid;
    private Button qsBtn;
    private Button uploadBtn;
    private String fileName;
    private int upload_id;

    ListenableFuture<String> future;

    BackendService backendService;
    private Boolean isVisible = false;
    private AuthenticatedUser authenticatedUser;
    //public static Map<String, Integer> projectUploadIdMap = new HashMap<>();

    public B2POutlookSUBView(ProjectParameterService projectParameterService, ProjectConnectionService projectConnectionService, BackendService backendService, AuthenticatedUser authenticatedUser) {

        this.backendService = backendService;
        this.projectConnectionService = projectConnectionService;
        this.authenticatedUser=authenticatedUser;

        uploadBtn = new Button("Upload");
        uploadBtn.setEnabled(false);

        qsBtn = new Button("QS and Start Job");
        qsBtn.setEnabled(false);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : listOfProjectParameters) {
            if(projectParameter.getNamespace().equals(Constants.B2P_OUTLOOK_SUB)) {
                if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                    dbServer = projectParameter.getValue();
                } else if (Constants.DB_NAME.equals(projectParameter.getName())) {
                    dbName = projectParameter.getValue();
                } else if (Constants.DB_USER.equals(projectParameter.getName())) {
                    dbUser = projectParameter.getValue();
                } else if (Constants.DB_PASSWORD.equals(projectParameter.getName())) {
                    dbPassword = projectParameter.getValue();
                }else if (Constants.TABLE.equals(projectParameter.getName())) {
                    tableName = projectParameter.getValue();
                } else if (Constants.DB_JOBS.equals(projectParameter.getName())){
                agentName = projectParameter.getValue();
            }
            }
        }

        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";

        Div text = new Div();
        text.setText("Connected to: "+ dbServer+ " Database: " + dbName+ " Table: " + tableName + " AgentJob: " + agentName);

        //Componente QS-Grid:
        qsGrid = new QS_Grid(projectConnectionService, backendService);

        HorizontalLayout hl = new HorizontalLayout();
        hl.setAlignItems(Alignment.BASELINE);
        // hl.add(singleFileUpload,saveButton, databaseDetail);
        hl.add(singleFileUpload,uploadBtn, qsBtn, text, qsGrid);
        add(hl);

        uploadBtn.addClickListener(e ->{
            save2db();
            qsBtn.setEnabled(true);
        });

        qsBtn.addClickListener(e ->{
         //   if (qsGrid.projectId != projectId) {
            hl.remove(qsGrid);
            qsGrid = new QS_Grid(projectConnectionService, backendService);
            hl.add(qsGrid);
            CallbackHandler callbackHandler = new CallbackHandler();
            qsGrid.createDialog(callbackHandler, projectId, upload_id);
         //   }
            qsGrid.showDialog(true);
        });

        setupUploader();
        add(getOutlookSubGrid());
        setSizeFull();
        setHeightFull();

        UI.getCurrent().addShortcutListener(
                () ->  start_thread(),
                Key.KEY_V, KeyModifier.ALT);

        UI.getCurrent().addShortcutListener(
                () ->  future.cancel(true),
                Key.KEY_S, KeyModifier.ALT);

        text.setVisible(false);

        UI.getCurrent().addShortcutListener(
                () -> {
                    isVisible=!isVisible;
                    text.setVisible(isVisible);
                },
                Key.KEY_I, KeyModifier.ALT);

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
              /*  Map.Entry<String, Integer> lastEntry = projectUploadIdMap.entrySet().stream()
                        .reduce((first, second) -> second)
                        .orElse(null);
                int upload_id = lastEntry.getValue();
              */

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
                    System.out.println("SQL executed: " + sql);
                    String sqlResult = jdbcTemplate.queryForObject(sql, String.class);

                    System.out.println("SQL result: " + sqlResult);

                    if (!"ok".equals(sqlResult)) {
                        // resultMessage contains Upload_ID, so search user wo do this upload:
                        int uploadID=Integer.parseInt(sqlResult);

                        sql="select User_Name from [Log].[User_Uploads] where Upload_id=" + uploadID;

                        sqlResult = jdbcTemplate.queryForObject(sql, String.class);
                        System.out.println("SQL executed: " + sql);
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
       /* public void onComplete(String result) {
            if(!result.equals("Cancel")) {
                String message = projectConnectionService.startAgent(projectId);
                if (!message.contains("Error")) {
                    Notification.show(message, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show(message, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        }*/
    }

    private void save2db(){

        ProjectUpload projectUpload = new ProjectUpload();
        projectUpload.setFileName(fileName);
        //projectUpload.setUserName(MainLayout.userName);

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            projectUpload.setUserName(user.getUsername());
        }

        projectUpload.setModulName("B2POutlookSUB");

        projectConnectionService.getJdbcConnection(dbUrl, dbUser, dbPassword); // Set Connection to target DB
        upload_id = projectConnectionService.saveUploadedGenericFileData(projectUpload);

        projectUpload.setUploadId(upload_id);

        System.out.println("Upload_ID: " + upload_id);

        Notification notification = Notification.show(" Rows Uploaded start",2000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        List<B2pOutlookSub> listOfAllData = new ArrayList<>();

        for (List<B2pOutlookSub> sheetData : listOfAllSheets) {
            listOfAllData.addAll(sheetData);
        }
        String resultFinancial = projectConnectionService.saveB2POutlookSub(listOfAllData, tableName, dbUrl, dbUser, dbPassword, upload_id);
        if (resultFinancial.equals(Constants.OK)){
            notification = Notification.show(listOfAllData.size() + " B2P_Outlook Rows Uploaded successfully",5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            notification = Notification.show("Error during B2P_Outlook upload: " + resultFinancial ,5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void start_thread() {

        Notification.show("starte Thread");

        UI ui = getUI().orElseThrow();

        future = backendService.longRunningTask();
        future.addCallback(
                successResult -> updateUi(ui, "Task finished: " + successResult),
                failureException -> updateUi(ui, "Task failed: " + failureException.getMessage())

        );

    }

    private void updateUi(UI ui, String result) {


        ui.access(() -> {
            Notification.show(result,6000, Notification.Position.MIDDLE);
        });
    }

    private Component getOutlookSubGrid() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        crudOutlookSub = new Crud<>(B2pOutlookSub.class, createOutlookSubEditor());
        crudOutlookSub.setToolbarVisible(false);
        crudOutlookSub.setHeightFull();
        crudOutlookSub.setSizeFull();
        setupOutlookSubGrid();
        content.add(crudOutlookSub);

        return content;
    }

    private CrudEditor<B2pOutlookSub> createOutlookSubEditor() {

        FormLayout editForm = new FormLayout();
        Binder<B2pOutlookSub> binder = new Binder<>(B2pOutlookSub.class);
        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupOutlookSubGrid() {

        String ZEILE = "zeile";
        String MONTH = "month";
        String SCENARIO = "scenario";
        String MEASURE = "measure";
        String PHYSICALSLINE = "physicalsLine";
        String SEGMENT = "segment";
        String PAYMENTTYPE = "paymentType";
        String CONTRACTTYPE = "contractType";
        String VALUE = "value";
        String BLATT = "blatt";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        gridOutlookSub = crudOutlookSub.getGrid();

        gridOutlookSub.getColumnByKey(BLATT).setHeader("Blatt").setWidth("120px").setFlexGrow(0).setResizable(true);
        gridOutlookSub.getColumnByKey(ZEILE).setHeader("Zeile").setWidth("60px").setFlexGrow(0).setResizable(true);
        gridOutlookSub.getColumnByKey(MONTH).setHeader("Month").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridOutlookSub.getColumnByKey(SCENARIO).setHeader("Scenario").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridOutlookSub.getColumnByKey(MEASURE).setHeader("Measure").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridOutlookSub.getColumnByKey(PHYSICALSLINE).setHeader("PhysicalsLine").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridOutlookSub.getColumnByKey(SEGMENT).setHeader("Segment").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridOutlookSub.getColumnByKey(PAYMENTTYPE).setHeader("PaymentType").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridOutlookSub.getColumnByKey(CONTRACTTYPE).setHeader("Contract_Type").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridOutlookSub.getColumnByKey(VALUE).setHeader("Value").setWidth("80px").setFlexGrow(0).setResizable(true);


        gridOutlookSub.getColumns().forEach(col -> col.setAutoWidth(true));

        gridOutlookSub.removeColumn(gridOutlookSub.getColumnByKey(EDIT_COLUMN));

        // Reorder the columns (alphabetical by default)
        gridOutlookSub.setColumnOrder(gridOutlookSub.getColumnByKey(BLATT)
                , gridOutlookSub.getColumnByKey(ZEILE)
                , gridOutlookSub.getColumnByKey(MONTH)
                , gridOutlookSub.getColumnByKey(SCENARIO)
                , gridOutlookSub.getColumnByKey(MEASURE)
                , gridOutlookSub.getColumnByKey(PHYSICALSLINE)
                , gridOutlookSub.getColumnByKey(SEGMENT)
                , gridOutlookSub.getColumnByKey(PAYMENTTYPE)
                , gridOutlookSub.getColumnByKey(CONTRACTTYPE)
                , gridOutlookSub.getColumnByKey(VALUE)
                );
        //    , gridFinancials.getColumnByKey(EDIT_COLUMN));

        gridOutlookSub.addThemeVariants(GridVariant.LUMO_COMPACT);

    }
    private void setupUploader() {
        singleFileUpload.setWidth("600px");

        singleFileUpload.addSucceededListener(event -> {
            // Get information about the uploaded file
            InputStream fileData = memoryBuffer.getInputStream();
            fileName = event.getFileName();
            contentLength = event.getContentLength();
            mimeType = event.getMIMEType();

            parseExcelFile(fileData,fileName);
            List<B2pOutlookSub> listOfAllData = new ArrayList<>();

            for (List<B2pOutlookSub> sheetData : listOfAllSheets) {
                listOfAllData.addAll(sheetData);
            }
            GenericDataProvider dataFinancialsProvider = new GenericDataProvider(listOfAllData, "Zeile");
            gridOutlookSub.setDataProvider(dataFinancialsProvider);
            singleFileUpload.clearFileList();
            uploadBtn.setEnabled(true);
            qsBtn.setEnabled(false);
        });
    }

    private void parseExcelFile(InputStream fileData, String fileName) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        Article article = new Article();

        try {
            if(fileName.isEmpty() || fileName.length()==0)
            {
                article=new Article();
                article.setText(LocalDateTime.now().format(formatter) + ": Error: Keine Datei angegeben!");
                textArea.add(article);
            }

            if(!mimeType.contains("openxmlformats-officedocument"))
            {
                article=new Article();
                article.setText(LocalDateTime.now().format(formatter) + ": Error: ungültiges Dateiformat!");
                textArea.add(article);
            }
            textArea.setText(LocalDateTime.now().format(formatter) + ": Info: Verarbeite Datei: " + fileName + " (" + contentLength + " Byte)");

            XSSFWorkbook my_xls_workbook = new XSSFWorkbook(fileData);

            String firstSheet = "Mapping  - OL";
            my_xls_workbook.forEach(sheet -> {
                sheetNr++;
                String sheetName = sheet.getSheetName();
                System.out.println("SheetNr: " + sheetNr + " Name: " + sheetName );
                if (sheetName != null && !firstSheet.equals(sheetName) && sheetNr>1) {
                    List<B2pOutlookSub> sheetData = parseSheet(sheet, B2pOutlookSub.class);
                    listOfAllSheets.add(sheetData);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T> List<T>  parseSheet(XSSFSheet sheet, Class<T> targetType) {
        List<T> resultList = new ArrayList<>();
        try {
            // List<T> resultList = new ArrayList<>();
            Iterator<Row> rowIterator = sheet.iterator();

            int rowNumber=0;
            Integer Error_count=0;
            System.out.println(sheet.getPhysicalNumberOfRows()+"$$$$$$$$$");

            while (rowIterator.hasNext() ) {
                Row row = rowIterator.next();
                T entity = targetType.newInstance();
                rowNumber++;

                if (rowNumber == 1) {
                    continue;
                }
                if (row.getCell(0) != null && row.getCell(0).toString().isEmpty()) {
                    break;
                }

                Field[] fields = targetType.getDeclaredFields();
                for (int index = 0; index < fields.length; index++) {
                    Cell cell = null;
                    if (index != 0) {
                        cell = row.getCell(index - 1);
                    } else {
                        cell = row.getCell(index);
                    }
                    if (cell != null && !cell.toString().isEmpty()) {
                        Field field = fields[index];
                        field.setAccessible(true);
                        if (index == 0) {
                            field.set(entity, rowNumber);
                        } else {
                            if (field.getType() == int.class || field.getType() == Integer.class) {
                                field.set(entity, (int) cell.getNumericCellValue());
                            } else if (field.getType() == long.class || field.getType() == Long.class) {
                                field.set(entity, (long) cell.getNumericCellValue());
                            } else if (field.getType() == double.class || field.getType() == Double.class) {
                                field.set(entity, (double) cell.getNumericCellValue());
                            } else if (field.getType() == String.class) {
                                if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
                                    // System.out.println(cell.getCachedFormulaResultType() + "............" + sheet.getSheetName());
                                    if (cell.getCachedFormulaResultType() == Cell.CELL_TYPE_NUMERIC) {
                                        // Formula result is numeric, get the numeric value
                                        double numericValue = cell.getNumericCellValue();
                                        String value = String.valueOf(numericValue);
                                        field.set(entity, value);
                                    } else if (cell.getCachedFormulaResultType() == Cell.CELL_TYPE_STRING) {
                                        // Formula result is string, get the string value
                                        String value = cell.getRichStringCellValue().getString();
                                        field.set(entity, value);
                                    } else {
                                        // Handle other formula result types if needed
                                    }
                                } else {
                                    // Handle regular string cell
                                    field.set(entity, cell.getStringCellValue());
                                }
                            }
                        }
                    }
                }

                Field zeilField = fields[0];// entity.getClass().getDeclaredField("zeile");
                zeilField.setAccessible(true);
                int zeilValue = zeilField.getInt(entity);

                if(zeilValue != 0 ) {
                    Field blattField = fields[fields.length - 1];
                    blattField.setAccessible(true);
                    blattField.set(entity, sheet.getSheetName());
                    resultList.add(entity);
                } else {
                    break;
                }
            }
            return resultList;
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show(sheet.getSheetName() +" sheet having a parsing problem", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return resultList;
        }

    }
}
