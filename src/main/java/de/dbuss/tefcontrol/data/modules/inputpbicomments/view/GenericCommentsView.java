package de.dbuss.tefcontrol.data.modules.inputpbicomments.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.*;
import com.vaadin.flow.router.*;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.Role;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.entity.OutlookMGSR;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.Financials;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.GenericComments;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.Subscriber;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.UnitsDeepDive;
import de.dbuss.tefcontrol.data.modules.techkpi.view.Tech_KPIView;
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

import javax.sql.DataSource;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;


@PageTitle("Generic Comments")
@Route(value = "Generic_Comments/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "MAPPING", "FLIP"})
public class GenericCommentsView extends VerticalLayout implements BeforeEnterObserver {
    private final ProjectConnectionService projectConnectionService;
    MemoryBuffer memoryBuffer = new MemoryBuffer();
    Upload singleFileUpload = new Upload(memoryBuffer);
    private List<List<GenericComments>> listOfAllSheets = new ArrayList<>();
    private Crud<GenericComments> crudGenericComments;
    private Grid<GenericComments> gridGenericComments = new Grid<>(GenericComments.class, false);
    private ProgressBar progressBar = new ProgressBar();
    private UI ui=UI.getCurrent();
    private String tableName;
    private String agentName;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    long contentLength = 0;
    String mimeType = "";
    Div textArea = new Div();
    private int projectId;
    private QS_Grid qsGrid;
    private Button uploadBtn;
    private Button qsBtn;
    private int id;
    public static Map<String, Integer> projectUploadIdMap = new HashMap<>();
    public GenericCommentsView(AuthenticatedUser authenticatedUser, ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService, BackendService backendService) {

        this.projectConnectionService = projectConnectionService;

        uploadBtn = new Button("Upload");
        uploadBtn.setEnabled(false);

        qsBtn = new Button("QS and Start Job");
        qsBtn.setEnabled(false);
        progressBar.setVisible(false);

        Div htmlDiv = new Div();
        htmlDiv.getElement().setProperty("innerHTML", "<h2>Input Frontend for Generic Comments");
        add(htmlDiv);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : listOfProjectParameters) {
            if(projectParameter.getNamespace().equals(Constants.GENERIC_COMMENTS)) {
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
                } else if (Constants.DB_JOBS.equals(projectParameter.getName())) {
                    agentName = projectParameter.getValue();
                }
            }
        }
        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";
        Text databaseDetail = new Text("Connected to: "+ dbServer+ ", Database: " + dbName + " Table: " + tableName + " AgentJob: " + agentName);

        //Componente QS-Grid:
        qsGrid = new QS_Grid(projectConnectionService, backendService);

        HorizontalLayout hl = new HorizontalLayout();
        hl.setAlignItems(Alignment.BASELINE);
        // hl.add(singleFileUpload, saveButton, databaseDetail, progressBar);
        hl.add(singleFileUpload, uploadBtn, qsBtn, databaseDetail, qsGrid);
        add(hl);
        add(progressBar);
        uploadBtn.addClickListener(e ->{
            save2db();
            //qsBtn.setEnabled(true);
        });

        qsBtn.addClickListener(e ->{
            // if (qsGrid.projectId != projectId) {
            hl.remove(qsGrid);
            qsGrid = new QS_Grid(projectConnectionService, backendService);
            hl.add(qsGrid);
            CallbackHandler callbackHandler = new CallbackHandler();
            Map.Entry<String, Integer> lastEntry = projectUploadIdMap.entrySet().stream()
                    .reduce((first, second) -> second)
                    .orElse(null);
            int upload_id = lastEntry.getValue();
            qsGrid.createDialog(callbackHandler, projectId, upload_id);
            //   projectUploadIdMap = new HashMap<>();
            //    }
            qsGrid.showDialog(true);
        });

        add(textArea);
        setupUploader();
        add(getGenericCommentsGrid());
        setHeightFull();
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
    }
    private void save2db() {
        List<GenericComments> allGenericCommentsItems = getGenericCommentsDataProviderAllItems();

        List<String> allFileNames = allGenericCommentsItems.stream()
                .map(GenericComments::getFileName)
                .distinct()
                .collect(Collectors.toList());

        for (String fileName : allFileNames) {
            ProjectUpload projectUpload = new ProjectUpload();
            projectUpload.setFileName(fileName);
            projectUpload.setUserName(MainLayout.userName);
            projectConnectionService.saveUploadedGenericFileData(projectUpload);
        }

        ui.setPollInterval(500);
        saveAllCommentsdata(allGenericCommentsItems);
    }

    private void saveAllCommentsdata(List<GenericComments> allGenericCommentsItems) {
        AtomicReference<String> returnStatus = new AtomicReference<>("false");
        int totalRows = allGenericCommentsItems.size();
        progressBar.setVisible(true);
        progressBar.setMin(0);
        progressBar.setMax(totalRows);
        progressBar.setValue(0);

        new Thread(() -> {
            projectConnectionService.getJdbcConnection(dbUrl, dbUser, dbPassword);
            // Do some long running task
            try {
                int batchSize = 10; // Die Anzahl der Zeilen, die auf einmal verarbeitet werden sollen

                for (int i = 0; i < totalRows; i += batchSize) {

                    if (Thread.interrupted()) {
                        System.out.println("Thread hat interrupt bekommen");
                        // Hier könntest du aufräumen oder andere Aktionen ausführen, bevor der Thread beendet wird
                        return; // Verlässt den Thread
                    }
                    else {
                        // System.out.println("Thread läuft noch...");
                    }

                    int endIndex = Math.min(i + batchSize, totalRows);

                    List<GenericComments> batchData = allGenericCommentsItems.subList(i, endIndex);

                    System.out.println("Verarbeitete Zeilen: " + endIndex + " von " + totalRows);

                    String resultComments = projectConnectionService.saveGenericComments(batchData, tableName, dbUrl, dbUser, dbPassword);
                    returnStatus.set(resultComments);

                    System.out.println("ResultComment: " + returnStatus.toString());

                    if (returnStatus.toString().equals(Constants.OK)){
                        //System.out.println("Alles in Butter...");
                    }
                    else{
                        //System.out.println("Fehler aufgetreten...");
                        Thread.currentThread().interrupt(); // Interrupt-Signal setzen

                        ui.access(() -> {
                            progressBar.setVisible(false);
                            Notification.show("Error during Comments upload! " + returnStatus.toString(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                            ui.setPollInterval(-1);
                        });


                        return;

                    }

                    int finalI = i;
                    ui.access(() -> {
                        progressBar.setValue((double) finalI);
                        // System.out.println("Fortschritt aktualisiert auf: " + finalI);
                        //         message.setText(LocalDateTime.now().format(formatter) + ": Info: saving to database (" + endIndex + "/" + totalRows +")");
                    });

                }
            } catch (Exception e) {
                ui.access(() -> {

                    Notification.show("Error during Comments upload! ", 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);

                });
                e.printStackTrace();
            }
            ui.access(() -> {
                progressBar.setVisible(false);

                if (returnStatus.toString().equals(Constants.OK))
                {
                    Notification.show("Comments saved " + totalRows + " rows.",5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    qsBtn.setEnabled(true);
                }
                else
                {
                    Notification.show("Error during Comments upload! " + returnStatus.toString(), 15000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }

                ui.setPollInterval(-1);
            });

        }).start();

    }

    private Component getGenericCommentsGrid() {
        VerticalLayout content = new VerticalLayout();
        crudGenericComments = new Crud<>(GenericComments.class, createGenericCommentsEditor());
        setupGenericCommentsGrid();
        content.add(crudGenericComments);

        crudGenericComments.setToolbarVisible(false);

        gridGenericComments.addItemDoubleClickListener(event -> {
            GenericComments selectedEntity = event.getItem();
            crudGenericComments.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            crudGenericComments.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        crudGenericComments.setHeightFull();
        content.setHeightFull();
        return content;
    }

    private CrudEditor<GenericComments> createGenericCommentsEditor() {

        TextArea comment = new TextArea("Comment");

        comment.setHeight("250px");
        comment.setWidth("1200px");
        FormLayout editForm = new FormLayout(comment);
        editForm.setColspan(comment, 2);

        editForm.setHeight("250px");
        editForm.setWidth("1200px");

        Binder<GenericComments> binder = new Binder<>(GenericComments.class);
        binder.forField(comment).asRequired().bind(GenericComments:: getComment, GenericComments::setComment);

        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupUploader() {
        singleFileUpload.setWidth("600px");

        singleFileUpload.addSucceededListener(event -> {
            // Get information about the uploaded file
            InputStream fileData = memoryBuffer.getInputStream();
            String fileName = event.getFileName();
            contentLength = event.getContentLength();
            mimeType = event.getMIMEType();

            GenericDataProvider emptyDataProvider = new GenericDataProvider<>(Collections.emptyList());
            gridGenericComments.setDataProvider(emptyDataProvider);
            // Refresh the grid to reflect the changes
            gridGenericComments.getDataProvider().refreshAll();
            listOfAllSheets.clear();

            parseExcelFile(fileData, fileName);
            List<GenericComments> listOfAllData = new ArrayList<>();

            for (List<GenericComments> sheetData : listOfAllSheets) {
                listOfAllData.addAll(sheetData);
            }
            GenericDataProvider dataGenericCommentsProvider = new GenericDataProvider(listOfAllData);
            gridGenericComments.setDataProvider(dataGenericCommentsProvider);
            setupDataProviderEvent();

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
                    List<GenericComments> sheetData = parseSheet(fileName, sheet, GenericComments.class);
                    listOfAllSheets.add(sheetData);
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isFileNameAvailable(String fileName) {
        // Use stream to check if the fileName is present in any list within listOfAllSheets
        return listOfAllSheets.stream()
                .anyMatch(sheet -> sheet.stream()
                        .anyMatch(comment -> comment.getFileName().equals(fileName)));
    }
    private void setupGenericCommentsGrid() {

        String FILENAME = "fileName";
        String REGISTERNAME = "registerName";
        String LINENUMBER = "lineNumber";
        String RESPONSIBLE = "responsible";
        String TOPIC = "topic";
        String MONTH = "month";
        String CATEGORY_1 = "category1";
        String CATEGORY_2 = "category2";
        String SCENARIO = "scenario";
        String XTD = "xtd";
        String SEGMENT = "segment";
        String PAYMENTTYPE = "paymentType";
        String COMMENT = "comment";
        String ID = "id";
        String UPLOADID = "uploadId";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        gridGenericComments = crudGenericComments.getGrid();

        gridGenericComments.removeColumn(gridGenericComments.getColumnByKey(EDIT_COLUMN));
        gridGenericComments.removeColumn(gridGenericComments.getColumnByKey(ID));
        gridGenericComments.removeColumn(gridGenericComments.getColumnByKey(UPLOADID));

        gridGenericComments.getColumnByKey(FILENAME).setHeader("File_Name").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(REGISTERNAME).setHeader("Register_Name").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(LINENUMBER).setHeader("Line_Number").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(RESPONSIBLE).setHeader("Responsible").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(TOPIC).setHeader("Topic").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(MONTH).setHeader("Month").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(CATEGORY_1).setHeader("Category_1").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(CATEGORY_2).setHeader("Category_2").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(SCENARIO).setHeader("Scenario").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(XTD).setHeader("XTD").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(SEGMENT).setHeader("Segment").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(PAYMENTTYPE).setHeader("Payment_Type").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(COMMENT).setHeader("Comment").setWidth("200px").setFlexGrow(0).setResizable(true);

        gridGenericComments.getColumns().forEach(col -> col.setAutoWidth(true));
        // Reorder the columns (alphabetical by default)
        gridGenericComments.setColumnOrder( gridGenericComments.getColumnByKey(FILENAME)
                , gridGenericComments.getColumnByKey(REGISTERNAME)
                , gridGenericComments.getColumnByKey(LINENUMBER)
                , gridGenericComments.getColumnByKey(RESPONSIBLE)
                , gridGenericComments.getColumnByKey(TOPIC)
                , gridGenericComments.getColumnByKey(MONTH)
                , gridGenericComments.getColumnByKey(CATEGORY_1)
                , gridGenericComments.getColumnByKey(CATEGORY_2)
                , gridGenericComments.getColumnByKey(SCENARIO)
                , gridGenericComments.getColumnByKey(XTD)
                , gridGenericComments.getColumnByKey(SEGMENT)
                , gridGenericComments.getColumnByKey(PAYMENTTYPE)
                , gridGenericComments.getColumnByKey(COMMENT));
        //    , gridFinancials.getColumnByKey(EDIT_COLUMN));
    }

    public <T> List<T>  parseSheet(String fileName, XSSFSheet my_worksheet, Class<T> targetType) {

        try {
            List<T> resultList = new ArrayList<>();
            Iterator<Row> rowIterator = my_worksheet.iterator();

            int rowNumber=0;
            Integer Error_count=0;
            System.out.println(my_worksheet.getPhysicalNumberOfRows()+"$$$$$$$$$");

            while (rowIterator.hasNext() ) {
                Row row = rowIterator.next();
                T entity = targetType.newInstance();
                rowNumber++;

                if (rowNumber == 1 || row.getCell(0) == null ) {
                    continue;
                }

                if(row.getCell(0) != null && row.getCell(0).toString().isEmpty()) {
                    break;
                }

                Field[] fields = targetType.getDeclaredFields();
                for (int index = 0; index < fields.length; index++) {
                    Cell cell = null;
                    if(index != 0) {
                        cell = row.getCell(index -1);
                    } else {
                        cell = row.getCell(index);
                    }

                    Field field = fields[index];
                    field.setAccessible(true);
                    if (cell != null && !cell.toString().isEmpty()) {
                        if (index == 0) {
                            field.set(entity, rowNumber);
                        } else {
                            if (field.getType() == int.class || field.getType() == Integer.class) {
                                if (cell.getCellType() == cell.CELL_TYPE_NUMERIC) {
                                    field.set(entity, (int) cell.getNumericCellValue());
                                } else if (cell.getCellType() == cell.CELL_TYPE_STRING) {
                                    String cellText = cell.getStringCellValue();
                                    field.set(entity, Integer.parseInt(cellText));
                                }
                            } else if (field.getType() == double.class || field.getType() == Double.class) {
                                field.set(entity, cell.getNumericCellValue());
                            } else if (field.getType() == String.class) {
                                field.set(entity, cell.getStringCellValue());
                            }
                        }
                    }
                    if (field.getName().equals("fileName")) {
                        field.set(entity, fileName);
                    } else if (field.getName().equals("registerName")) {
                        field.set(entity, my_worksheet.getSheetName());
                    } else if (field.getName().equals("id")) {
                        field.set(entity, id++);
                    }
                }
                resultList.add(entity);
            }
            return resultList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private void setupDataProviderEvent() {
        GenericDataProvider financialsdataProvider = new GenericDataProvider(getGenericCommentsDataProviderAllItems());

        crudGenericComments.addDeleteListener(
                deleteEvent -> {financialsdataProvider.delete(deleteEvent.getItem());
                    crudGenericComments.setDataProvider(financialsdataProvider);

                });
        crudGenericComments.addSaveListener(
                saveEvent -> {
                    financialsdataProvider.persist(saveEvent.getItem());
                    crudGenericComments.setDataProvider(financialsdataProvider);
                });
    }

    private List<GenericComments> getGenericCommentsDataProviderAllItems() {
        DataProvider<GenericComments, Void> existDataProvider = (DataProvider<GenericComments, Void>) gridGenericComments.getDataProvider();
        List<GenericComments> listOfGenericComments = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        return listOfGenericComments;
    }

}
