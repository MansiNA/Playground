package de.dbuss.tefcontrol.data.modules.inputpbicomments.view;

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
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.*;
import com.vaadin.flow.router.*;
import de.dbuss.tefcontrol.components.DefaultUtils;
import de.dbuss.tefcontrol.components.LogView;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.GenericComments;
import de.dbuss.tefcontrol.data.service.*;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@PageTitle("Generic Comments")
@Route(value = "Generic_Comments/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "MAPPING", "FLIP"})
public class GenericCommentsView extends VerticalLayout implements BeforeEnterObserver {
    private final ProjectConnectionService projectConnectionService;
    private final BackendService backendService;
    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private Optional<Projects> projects;
    private DefaultUtils defaultUtils;
    private List<ProjectAttachmentsDTO> listOfProjectAttachments;

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

    String fileName;

    private int upload_id;
    long contentLength = 0;
    String mimeType = "";
    Div textArea = new Div();
    private int projectId;
    private QS_Grid qsGrid;
    private Button uploadBtn;
    private Button qsBtn;
    private int id;
    private AuthenticatedUser authenticatedUser;
    public static Map<String, Integer> projectUploadIdMap = new HashMap<>();
    private Boolean isVisible = false;
    Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);
    private LogView logView;
    private Boolean isLogsVisible = false;

    public GenericCommentsView(AuthenticatedUser authenticatedUser, ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService, BackendService backendService, ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService) {

        this.projectConnectionService = projectConnectionService;
        this.authenticatedUser = authenticatedUser;
        this.backendService = backendService;
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;
        logView = new LogView();
        logView.logMessage(Constants.INFO, "Starting GenericCommentsView");

        uploadBtn = new Button("Upload");
        uploadBtn.setEnabled(false);

        qsBtn = new Button("QS and Start Job");
        qsBtn.setEnabled(false);
        progressBar.setVisible(false);

        Div htmlDiv = new Div();
        htmlDiv.getElement().setProperty("innerHTML", "<h2>Input Frontend for Generic Comments");
        add(htmlDiv);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.GENERIC_COMMENTS.equals(projectParameter.getNamespace()))
                .collect(Collectors.toList());

        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : filteredProjectParameters) {
          //  if(projectParameter.getNamespace().equals(Constants.GENERIC_COMMENTS)) {
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
          //  }
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
        logView.logMessage(Constants.INFO, "Ending GenericCommentsView");
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
        logView.logMessage(Constants.INFO, "Sarting getUpladTab() for set upload data");
        VerticalLayout content = new VerticalLayout();

        content.add(textArea);
        setupUploader();

        content.setSizeFull();
        content.setHeightFull();

        HorizontalLayout hl=new HorizontalLayout(singleFileUpload, uploadBtn, qsBtn, qsGrid);
        content.add(hl, progressBar);
        content.add(getGenericCommentsGrid());

        uploadBtn.addClickListener(e ->{
            logView.logMessage(Constants.INFO, "Uploading in uploadBtn.addClickListener");
            save2db();
            //qsBtn.setEnabled(true);
        });

        qsBtn.addClickListener(e ->{
            logView.logMessage(Constants.INFO, "executing sqls in qsBtn.addClickListener");
            // if (qsGrid.projectId != projectId) {
            hl.remove(qsGrid);
            qsGrid = new QS_Grid(projectConnectionService, backendService);
            hl.add(qsGrid);
            CallbackHandler callbackHandler = new CallbackHandler();
            qsGrid.createDialog(callbackHandler, projectId, upload_id);

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

               /* Map.Entry<String, Integer> lastEntry = projectUploadIdMap.entrySet().stream()
                        .reduce((first, second) -> second)
                        .orElse(null);
                int upload_id = lastEntry.getValue();
               */
                qsGrid.executeStartJobSteps(upload_id, agentName);
            }
            logView.logMessage(Constants.INFO, "Ending CallbackHandler onComplete for execute Start Job");
        }
    }
    private void save2db() {
        logView.logMessage(Constants.INFO, "Starting save2db() for saving file data in database");
        List<GenericComments> allGenericCommentsItems = getGenericCommentsDataProviderAllItems();

        ProjectUpload projectUpload = new ProjectUpload();
        projectUpload.setFileName(fileName);
        //projectUpload.setUserName(MainLayout.userName);

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            projectUpload.setUserName(user.getUsername());
        }
        else {
            //No Username found break...
            Notification.show("Username not found, data can not upload to DB", 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }


        projectUpload.setModulName("GenericComments");

        logView.logMessage(Constants.INFO, "Get file upload id from database");
        projectConnectionService.getJdbcConnection(dbUrl, dbUser, dbPassword); // Set Connection to target DB
        upload_id = projectConnectionService.saveUploadedGenericFileData(projectUpload);

        projectUpload.setUploadId(upload_id);

        System.out.println("Upload_ID: " + upload_id);


        ui.setPollInterval(500);
        saveAllCommentsdata(allGenericCommentsItems);
    }

    private void saveAllCommentsdata(List<GenericComments> allGenericCommentsItems) {
        logView.logMessage(Constants.INFO, "Starting saveAllCommentsdata() for save all comments");
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
                int batchSize = 100; // Die Anzahl der Zeilen, die auf einmal verarbeitet werden sollen

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

                    String resultComments = projectConnectionService.saveGenericComments(batchData, tableName, dbUrl, dbUser, dbPassword, upload_id);
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
           //     logView.logMessage(Constants.INFO, "Saved file data in database");
            } catch (Exception e) {
                ui.access(() -> {
               //     logView.logMessage(Constants.ERROR, "Error while saving file data in database");
                    Notification.show("Error during Comments upload! ", 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);

                });
                e.printStackTrace();
            }
            ui.access(() -> {
                progressBar.setVisible(false);
                projectConnectionService.connectionClose(projectConnectionService.getTemplate());
                if (returnStatus.toString().equals(Constants.OK))
                {
                    logView.logMessage(Constants.INFO, "Saved file data in database");
                    Notification.show("Comments saved " + totalRows + " rows.",5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    qsBtn.setEnabled(true);
                }
                else
                {
                    logView.logMessage(Constants.ERROR, "Error while saving file data in database");
                    Notification.show("Error during Comments upload! " + returnStatus.toString(), 15000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }

                ui.setPollInterval(-1);
            });

        }).start();
        logView.logMessage(Constants.INFO, "Ending saveAllCommentsdata() for save all comments");
    }

    private Component getGenericCommentsGrid() {
        logView.logMessage(Constants.INFO, "Starting getGenericCommentsGrid() for get GenericCommentsGrid");
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
        content.setHeight("340px");
        logView.logMessage(Constants.INFO, "Ending getGenericCommentsGrid() for get GenericCommentsGrid");
        return content;
    }

    private CrudEditor<GenericComments> createGenericCommentsEditor() {
        logView.logMessage(Constants.INFO, "createGenericCommentsEditor() for create Editor");
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
        logView.logMessage(Constants.INFO, "Starting setupUploader() for setup file uploader");
        singleFileUpload.setWidth("600px");

        singleFileUpload.addSucceededListener(event -> {
            logView.logMessage(Constants.INFO, "FIle Upload in Fileuploader");
            // Get information about the uploaded file
            InputStream fileData = memoryBuffer.getInputStream();
            fileName = event.getFileName();
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
            qsBtn.setEnabled(false);

            logView.logMessage(Constants.INFO, "Ending setupUploader() for setup file uploader");
        });
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

            my_xls_workbook.forEach(sheet -> {
                String sheetName = sheet.getSheetName();
                if (sheetName != null) {
                    List<GenericComments> sheetData = parseSheet(fileName, sheet, GenericComments.class);
                    listOfAllSheets.add(sheetData);
                }
            });

            logView.logMessage(Constants.INFO, "Ending parseExcelFile() for parse uploaded file");
        } catch (Exception e) {
            logView.logMessage(Constants.ERROR, "Error while parse uploaded file");
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
        logView.logMessage(Constants.INFO, "Starting setupGenericCommentsGrid() for setup GenericCommentsGrid");
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

        logView.logMessage(Constants.INFO, "Ending setupGenericCommentsGrid() for setup GenericCommentsGrid");
    }

    public <T> List<T>  parseSheet(String fileName, XSSFSheet my_worksheet, Class<T> targetType) {
        logView.logMessage(Constants.INFO, "Starting parseSheet() for parse sheet of file");
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
            logView.logMessage(Constants.INFO, "Ending parseSheet() for parse sheet of file");
            return resultList;
        } catch (Exception e) {
            logView.logMessage(Constants.ERROR, "Error while parse sheet of file");
            e.printStackTrace();
            return null;
        }

    }

    private void setupDataProviderEvent() {
        logView.logMessage(Constants.INFO, "Starting setupDataProviderEvent() for setup dataProvider");
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
        logView.logMessage(Constants.INFO, "Ending setupDataProviderEvent() for setup dataProvider");
    }

    private List<GenericComments> getGenericCommentsDataProviderAllItems() {
        logView.logMessage(Constants.INFO, "getGenericCommentsDataProviderAllItems() for get dataProvider all item");
        DataProvider<GenericComments, Void> existDataProvider = (DataProvider<GenericComments, Void>) gridGenericComments.getDataProvider();
        List<GenericComments> listOfGenericComments = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        return listOfGenericComments;
    }

}
