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
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.dbuss.tefcontrol.components.DefaultUtils;
import de.dbuss.tefcontrol.components.LogView;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.entity.B2pOutlookSub;
import de.dbuss.tefcontrol.data.service.*;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "B2P_Outlook_Sub/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "FLIP"})
public class B2POutlookSUBView extends VerticalLayout implements BeforeEnterObserver {

    private final ProjectConnectionService projectConnectionService;
    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private Optional<Projects> projects;
    private DefaultUtils defaultUtils;
    private List<ProjectAttachmentsDTO> listOfProjectAttachments;
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
    private Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);
    private LogView logView;
    private Boolean isLogsVisible = false;

    public B2POutlookSUBView(ProjectParameterService projectParameterService, ProjectConnectionService projectConnectionService, BackendService backendService, AuthenticatedUser authenticatedUser,  ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService) {

        this.backendService = backendService;
        this.projectConnectionService = projectConnectionService;
        this.authenticatedUser=authenticatedUser;
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;

        logView = new LogView();
        logView.logMessage(Constants.INFO, "Starting B2POutlookSUBView");

        uploadBtn = new Button("Upload");
        uploadBtn.setEnabled(false);

        qsBtn = new Button("QS and Start Job");
        qsBtn.setEnabled(false);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.B2P_OUTLOOK_SUB.equals(projectParameter.getNamespace()))
                .collect(Collectors.toList());

        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : filteredProjectParameters) {
          //  if(projectParameter.getNamespace().equals(Constants.B2P_OUTLOOK_SUB)) {
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
          //  }
            }
        }

        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";
        setProjectParameterGrid(filteredProjectParameters);
        defaultUtils = new DefaultUtils(projectsService, projectAttachmentsService);

        //Componente QS-Grid:
        qsGrid = new QS_Grid(projectConnectionService, backendService);

        HorizontalLayout vl = new HorizontalLayout();
        vl.add(getTabsheet());

        vl.setHeightFull();
        vl.setSizeFull();

        setHeightFull();
        setSizeFull();

        add(vl,parameterGrid);

        parameterGrid.setVisible(false);
        logView.setVisible(false);
        add(logView);

        if(MainLayout.isAdmin) {
            UI.getCurrent().addShortcutListener(
                    () -> {
                        start_thread();
                        isLogsVisible = !isLogsVisible;
                        logView.setVisible(isLogsVisible);
                    },
                    Key.KEY_V, KeyModifier.ALT);

            UI.getCurrent().addShortcutListener(
                    () -> future.cancel(true),
                    Key.KEY_S, KeyModifier.ALT);

            UI.getCurrent().addShortcutListener(
                    () -> {
                        isVisible = !isVisible;
                        parameterGrid.setVisible(isVisible);
                    },
                    Key.KEY_I, KeyModifier.ALT);
        }
        logView.logMessage(Constants.INFO, "Ending B2POutlookSUBView");
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
        logView.logMessage(Constants.INFO, "Ending beforeEnter() for update");
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
        logView.logMessage(Constants.INFO, "Starting getUpladTab() for set upload data");
        VerticalLayout content = new VerticalLayout();

        setupUploader();

        content.setSizeFull();
        content.setHeightFull();

        HorizontalLayout hl=new HorizontalLayout(singleFileUpload,uploadBtn, qsBtn, qsGrid);
        content.add(hl);
        content.add(getOutlookSubGrid());

        uploadBtn.addClickListener(e ->{
            logView.logMessage(Constants.INFO, "Uploading in uploadBtn.addClickListener");
            save2db();
            qsBtn.setEnabled(true);
        });

        qsBtn.addClickListener(e ->{
            logView.logMessage(Constants.INFO, "executing sqls in qsBtn.addClickListener");
            //   if (qsGrid.projectId != projectId) {
            hl.remove(qsGrid);
            qsGrid = new QS_Grid(projectConnectionService, backendService);
            hl.add(qsGrid);
            CallbackHandler callbackHandler = new CallbackHandler();
            qsGrid.createDialog(callbackHandler, projectId, upload_id);
            //   }
            qsGrid.showDialog(true);
        });
        logView.logMessage(Constants.INFO, "Ending getUpladTab() for set upload data");
        return content;
    }

    private void setProjectParameterGrid(List<ProjectParameter> listOfProjectParameters) {
        logView.logMessage(Constants.INFO, "Starting setProjectParameterGrid() for set database detail in Grid");
        parameterGrid = new Grid<>(ProjectParameter.class, false);
        parameterGrid.addColumn(ProjectParameter::getName).setHeader("Name").setAutoWidth(true).setResizable(true);
        parameterGrid.addColumn(ProjectParameter::getValue).setHeader("Value").setAutoWidth(true).setResizable(true);
        parameterGrid.addColumn(ProjectParameter::getDescription).setHeader("Description").setAutoWidth(true).setResizable(true);

        parameterGrid.setItems(listOfProjectParameters);
        parameterGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        parameterGrid.setHeight("200px");
        parameterGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        logView.logMessage(Constants.INFO, "Ending setProjectParameterGrid() for set database detail in Grid");

    }

    public class CallbackHandler implements QS_Callback {
        // Die Methode, die aufgerufen wird, wenn die externe Methode abgeschlossen ist
        @Override
        public void onComplete(String result) {
            logView.logMessage(Constants.INFO, "Starting CallbackHandler onComplete for execute Start Job");
            if(!result.equals("Cancel")) {
                qsGrid.executeStartJobSteps(upload_id, agentName);
            }
            logView.logMessage(Constants.INFO, "Ending CallbackHandler onComplete for execute Start Job");
        }
    }

    private void save2db(){
        logView.logMessage(Constants.INFO, "Starting save2db() for saving file data in database");
        ProjectUpload projectUpload = new ProjectUpload();
        projectUpload.setFileName(fileName);
        //projectUpload.setUserName(MainLayout.userName);

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            projectUpload.setUserName(user.getUsername());
        }

        projectUpload.setModulName("B2POutlookSUB");

        logView.logMessage(Constants.INFO, "Get file upload id from database");
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
            logView.logMessage(Constants.INFO, "Saved file data in database");
            notification = Notification.show(listOfAllData.size() + " B2P_Outlook Rows Uploaded successfully",5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            logView.logMessage(Constants.ERROR, "Error while saving file data in database");
            notification = Notification.show("Error during B2P_Outlook upload: " + resultFinancial ,5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void start_thread() {
        logView.logMessage(Constants.INFO, "Starting start_thread() for Ui task");
        Notification.show("starte Thread");

        UI ui = getUI().orElseThrow();

        future = backendService.longRunningTask();
        future.addCallback(
                successResult -> updateUi(ui, "Task finished: " + successResult),
                failureException -> updateUi(ui, "Task failed: " + failureException.getMessage())

        );
        logView.logMessage(Constants.INFO, "Ending start_thread() for Ui task");
    }

    private void updateUi(UI ui, String result) {

        logView.logMessage(Constants.INFO, "Starting updateUi()");
        ui.access(() -> {
            Notification.show(result,6000, Notification.Position.MIDDLE);
        });
        logView.logMessage(Constants.INFO, "Ending updateUi()");
    }

    private Component getOutlookSubGrid() {
        logView.logMessage(Constants.INFO, "Starting getOutlookSubGrid() for get OutlookSubGrid");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        crudOutlookSub = new Crud<>(B2pOutlookSub.class, createOutlookSubEditor());
        crudOutlookSub.setToolbarVisible(false);
        crudOutlookSub.setHeightFull();
        crudOutlookSub.setSizeFull();
        setupOutlookSubGrid();
        content.add(crudOutlookSub);
        logView.logMessage(Constants.INFO, "Ending getOutlookSubGrid() for get OutlookSubGrid");
        return content;
    }

    private CrudEditor<B2pOutlookSub> createOutlookSubEditor() {
        logView.logMessage(Constants.INFO, "createOutlookSubEditor() for create Editor");
        FormLayout editForm = new FormLayout();
        Binder<B2pOutlookSub> binder = new Binder<>(B2pOutlookSub.class);
        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupOutlookSubGrid() {
        logView.logMessage(Constants.INFO, "Starting setupOutlookSubGrid() for setup OutlookSubGrid");
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
        logView.logMessage(Constants.INFO, "Ending setupOutlookSubGrid() for setup OutlookSubGrid");
    }
    private void setupUploader() {
        logView.logMessage(Constants.INFO, "Starting setupUploader() for setup file uploader");
        singleFileUpload.setWidth("600px");

        singleFileUpload.addSucceededListener(event -> {
            logView.logMessage(Constants.INFO, "FIle Upload in Fileuploader");
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
        logView.logMessage(Constants.INFO, "Ending setupUploader() for setup file uploader");
    }

    private void parseExcelFile(InputStream fileData, String fileName) {
        logView.logMessage(Constants.INFO, "Starting parseExcelFile() for parse uploaded file");
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
            logView.logMessage(Constants.INFO, "Ending parseExcelFile() for parse uploaded file");
        } catch (Exception e) {
            logView.logMessage(Constants.ERROR, "Error while parse uploaded file");
            e.printStackTrace();
        }
    }

    public <T> List<T>  parseSheet(XSSFSheet sheet, Class<T> targetType) {
        logView.logMessage(Constants.INFO, "Starting parseSheet() for parse sheet of file");
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
            logView.logMessage(Constants.INFO, "Ending parseSheet() for parse sheet of file");
            return resultList;
        } catch (Exception e) {
            logView.logMessage(Constants.ERROR, "Error while parse sheet of file");
            e.printStackTrace();
            Notification.show(sheet.getSheetName() +" sheet having a parsing problem", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return resultList;
        }

    }
}
