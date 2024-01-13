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
import com.vaadin.flow.router.*;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.entity.ProjectUpload;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.entity.OutlookMGSR;
import de.dbuss.tefcontrol.data.service.BackendService;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
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

@Route(value = "B2P_Outlook_FIN/:project_Id", layout = MainLayout.class)
@RolesAllowed({"FLIP", "ADMIN"})
public class B2POutlookFINView extends VerticalLayout implements BeforeEnterObserver {

    private final ProjectConnectionService projectConnectionService;
    private MemoryBuffer memoryBuffer = new MemoryBuffer();
    private Upload singleFileUpload = new Upload(memoryBuffer);
    private List<List<OutlookMGSR>> listOfAllSheets = new ArrayList<>();
    private Crud<OutlookMGSR> crudMGSR;
    private Grid<OutlookMGSR> gridMGSR = new Grid<>(OutlookMGSR.class, false);
    private String tableName;

    private String agentName;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private long contentLength = 0;
    private String mimeType = "";
    private Div textArea = new Div();
    private int projectId;
    private QS_Grid qsGrid;
    private Button qsBtn;
    private Button uploadBtn;
    private String fileName;
    private int upload_id;
    ListenableFuture<String> future;

    public static Map<String, Integer> projectUploadIdMap = new HashMap<>();

    BackendService backendService;

    public B2POutlookFINView(ProjectParameterService projectParameterService, ProjectConnectionService projectConnectionService, BackendService backendService) {

        this.backendService = backendService;
        this.projectConnectionService = projectConnectionService;

        uploadBtn = new Button("Upload");
        uploadBtn.setEnabled(false);

        qsBtn = new Button("QS and Start Job");
        qsBtn.setEnabled(false);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : listOfProjectParameters) {
            if(projectParameter.getNamespace().equals(Constants.B2P_OUTLOOK_FIN)) {
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

        Text databaseDetail = new Text("Connected to: "+ dbServer+ " Database: " + dbName+ " Table: " + tableName + " AgentJob: " + agentName);

        //Componente QS-Grid:
        qsGrid = new QS_Grid(projectConnectionService, backendService);

        HorizontalLayout hl = new HorizontalLayout();
        hl.setAlignItems(Alignment.BASELINE);
        // hl.add(singleFileUpload,saveButton, databaseDetail);
        hl.add(singleFileUpload,uploadBtn, qsBtn, databaseDetail, qsGrid);
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
          //  }
            qsGrid.showDialog(true);
        });

        setupUploader();
        add(getMGSRGrid());
        setSizeFull();
        setHeightFull();

        UI.getCurrent().addShortcutListener(
                () ->  start_thread(),
                Key.KEY_V, KeyModifier.ALT);

        UI.getCurrent().addShortcutListener(
                () ->  future.cancel(true),
                Key.KEY_S, KeyModifier.ALT);


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
                Map.Entry<String, Integer> lastEntry = projectUploadIdMap.entrySet().stream()
                        .reduce((first, second) -> second)
                        .orElse(null);
                int upload_id = lastEntry.getValue();
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


   /*     public void onComplete(String result) {
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
        projectUpload.setUserName(MainLayout.userName);
        projectConnectionService.saveUploadedGenericFileData(projectUpload);

        Map<String, Integer> uploadIdMap = projectConnectionService.getUploadIdMap();
        upload_id = uploadIdMap.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(1);

        Notification notification = Notification.show(" Rows Uploaded start",2000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        List<OutlookMGSR> listOfAllData = new ArrayList<>();

        for (List<OutlookMGSR> sheetData : listOfAllSheets) {
            listOfAllData.addAll(sheetData);
        }
        String resultFinancial = projectConnectionService.saveOutlookMGSR(listOfAllData, tableName, dbUrl, dbUser, dbPassword, upload_id);
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

    private Component getMGSRGrid() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        crudMGSR = new Crud<>(OutlookMGSR.class, createMGSREditor());
        crudMGSR.setToolbarVisible(false);
        crudMGSR.setHeightFull();
        crudMGSR.setSizeFull();
        setupMGSRGrid();
        content.add(crudMGSR);

        return content;
    }

    private CrudEditor<OutlookMGSR> createMGSREditor() {

        FormLayout editForm = new FormLayout();
        Binder<OutlookMGSR> binder = new Binder<>(OutlookMGSR.class);
        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupMGSRGrid() {

        String ZEILE = "zeile";
        String MONTH = "month";
        String PLLINE = "pl_Line";
        String PROFITCENTER = "profitCenter";
        String SCENARIO = "scenario";
        String BLOCK = "block";
        String SEGMENT = "segment";
        String PAYMENTTYPE = "paymentType";
        String TYPEOFDATA = "typeOfData";
        String VALUE = "value";
        String BLATT = "blatt";
        String LOADDATE = "loadDate";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        gridMGSR = crudMGSR.getGrid();

        gridMGSR.getColumnByKey(BLATT).setHeader("Blatt").setWidth("120px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(ZEILE).setHeader("Zeile").setWidth("60px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(MONTH).setHeader("Month").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(PLLINE).setHeader("PL_Line").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(PROFITCENTER).setHeader("ProfitCenter").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(SCENARIO).setHeader("Scenario").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(BLOCK).setHeader("Block").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(SEGMENT).setHeader("Segment").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(PAYMENTTYPE).setHeader("PaymentType").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(TYPEOFDATA).setHeader("TypeOfData").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(VALUE).setHeader("Value").setWidth("80px").setFlexGrow(0).setResizable(true);

        gridMGSR.getColumnByKey(LOADDATE).setHeader("LoadDate").setWidth("80px").setFlexGrow(0).setResizable(true);

        gridMGSR.getColumns().forEach(col -> col.setAutoWidth(true));
        // gridMGSR.setHeightFull();
        // gridMGSR.setSizeFull();

        gridMGSR.removeColumn(gridMGSR.getColumnByKey(EDIT_COLUMN));
        gridMGSR.removeColumn(gridMGSR.getColumnByKey(LOADDATE));

        // Reorder the columns (alphabetical by default)
        gridMGSR.setColumnOrder(gridMGSR.getColumnByKey(BLATT)
                , gridMGSR.getColumnByKey(ZEILE)
                , gridMGSR.getColumnByKey(MONTH)
                , gridMGSR.getColumnByKey(PLLINE)
                , gridMGSR.getColumnByKey(PROFITCENTER)
                , gridMGSR.getColumnByKey(SCENARIO)
                , gridMGSR.getColumnByKey(BLOCK)
                , gridMGSR.getColumnByKey(SEGMENT)
                , gridMGSR.getColumnByKey(PAYMENTTYPE)
                , gridMGSR.getColumnByKey(TYPEOFDATA)
                , gridMGSR.getColumnByKey(VALUE)
                );
        //    , gridFinancials.getColumnByKey(EDIT_COLUMN));



        gridMGSR.addThemeVariants(GridVariant.LUMO_COMPACT);

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
            List<OutlookMGSR> listOfAllData = new ArrayList<>();

            for (List<OutlookMGSR> sheetData : listOfAllSheets) {
                listOfAllData.addAll(sheetData);
            }
            GenericDataProvider dataFinancialsProvider = new GenericDataProvider(listOfAllData, "Zeile");
            gridMGSR.setDataProvider(dataFinancialsProvider);
            singleFileUpload.clearFileList();
            uploadBtn.setEnabled(true);
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

            my_xls_workbook.forEach(sheet -> {
                String sheetName = sheet.getSheetName();
                if (sheetName != null) {
                    List<OutlookMGSR> sheetData = parseSheet(sheet, OutlookMGSR.class);
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

                                if(cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
                                    String value = cell.getNumericCellValue() + "";
                                    field.set(entity, value);
                                } else {
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
                    Field blattField = fields[fields.length - 2];
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
