package de.dbuss.tefcontrol.data.modules.b2pOutlook.view;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Anchor;
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
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.wontlost.ckeditor.Config;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;
import de.dbuss.tefcontrol.components.AttachmentGrid;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.Role;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.entity.OutlookMGSR;
import de.dbuss.tefcontrol.data.service.*;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.concurrent.ListenableFuture;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

@Route(value = "B2P_Outlook_FIN/:project_Id", layout = MainLayout.class)
@RolesAllowed({"FLIP", "ADMIN"})
public class B2POutlookFINView extends VerticalLayout implements BeforeEnterObserver {

    private final ProjectConnectionService projectConnectionService;
    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private MemoryBuffer memoryBuffer = new MemoryBuffer();
    private Upload singleFileUpload = new Upload(memoryBuffer);
    private List<List<OutlookMGSR>> listOfAllSheets = new ArrayList<>();
    private Crud<OutlookMGSR> crudMGSR;
    private Grid<OutlookMGSR> gridMGSR = new Grid<>(OutlookMGSR.class, false);
    private String tableName;
    int sheetNr = 0;
    private String agentName;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private long contentLength = 0;
    private String mimeType = "";
    private Div textArea = new Div();
    private int projectId;
    private Optional<Projects> projects;
    private AttachmentGrid attachmentGrid;
    private List<ProjectAttachmentsDTO> listOfProjectAttachments;
    private VaadinCKEditor editor;
    private QS_Grid qsGrid;
    private Button qsBtn;
    private Button uploadBtn;
    private String fileName;
    private int upload_id;
    ListenableFuture<String> future;

    //  public static Map<String, Integer> projectUploadIdMap = new HashMap<>();

    BackendService backendService;

    private AuthenticatedUser authenticatedUser;
    private Boolean isVisible = false;
    Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);

    public B2POutlookFINView(ProjectParameterService projectParameterService, ProjectConnectionService projectConnectionService, BackendService backendService,  AuthenticatedUser authenticatedUser, ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService) {

        this.backendService = backendService;
        this.projectConnectionService = projectConnectionService;
        this.authenticatedUser=authenticatedUser;
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;

        uploadBtn = new Button("Upload");
        uploadBtn.setEnabled(false);

        qsBtn = new Button("QS and Start Job");
        qsBtn.setEnabled(false);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.B2P_OUTLOOK_FIN.equals(projectParameter.getNamespace()))
                .collect(Collectors.toList());

        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : filteredProjectParameters) {
            //  if(projectParameter.getNamespace().equals(Constants.B2P_OUTLOOK_FIN)) {
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
            // }
        }

        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";

        setProjectParameterGrid(filteredProjectParameters);

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
        System.out.println("Is admin: " + checkAdminRole());

        if(MainLayout.isAdmin) {
            UI.getCurrent().addShortcutListener(
                    () -> start_thread(),
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
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        projectId = Integer.parseInt(parameters.get("project_Id").orElse(null));

        projects = projectsService.findById(projectId);

        projects.ifPresent(value -> listOfProjectAttachments = projectsService.getProjectAttachmentsWithoutFileContent(value));

        updateDescription();
        updateAttachmentGrid(listOfProjectAttachments);
    }

    private TabSheet getTabsheet() {

        //log.info("Starting getTabsheet() for Tabsheet");
        TabSheet tabSheet = new TabSheet();

        tabSheet.add("Upload", getUpladTab());
        tabSheet.add("Description", getDescriptionTab());
        tabSheet.add("Attachments", getAttachmentTab());

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();
        //log.info("Ending getTabsheet() for Tabsheet");

        return tabSheet;
    }

    private Component getAttachmentTab() {
        attachmentGrid = new AttachmentGrid(projectsService, projectAttachmentsService);
        return attachmentGrid.getProjectAttachements();
    }

    private Component getDescriptionTab() {
        Button saveBtn = new Button("save");
        Button editBtn = new Button("edit");
        saveBtn.setVisible(false);
        editBtn.setVisible(true);
        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            System.out.println("User: " + user.getName());
            Set<Role> roles = user.getRoles();
            boolean isAdmin = roles.stream()
                    .anyMatch(role -> role == Role.ADMIN);
            editBtn.setVisible(isAdmin);
        }
        VerticalLayout content = new VerticalLayout();

        Config config = new Config();
        config.setBalloonToolBar(com.wontlost.ckeditor.Constants.Toolbar.values());
        config.setImage(new String[][]{},
                "", new String[]{"full", "alignLeft", "alignCenter", "alignRight"},
                new String[]{"imageTextAlternative", "|",
                        "imageStyle:alignLeft",
                        "imageStyle:full",
                        "imageStyle:alignCenter",
                        "imageStyle:alignRight"}, new String[]{});

        editor = new VaadinCKEditorBuilder().with(builder -> {

            builder.editorType = com.wontlost.ckeditor.Constants.EditorType.CLASSIC;
            builder.width = "95%";
            builder.readOnly = true;
            builder.hideToolbar=true;
            builder.config = config;
        }).createVaadinCKEditor();

        editor.setReadOnly(true);

        saveBtn.addClickListener((event -> {
            projects.get().setDescription(editor.getValue());
            projectsService.update(projects.get());

            editBtn.setVisible(true);
            saveBtn.setVisible(false);
            //editor.setReadOnly(true);
            editor.setReadOnlyWithToolbarAction(!editor.isReadOnly());

        }));

        editBtn.addClickListener(e->{
            editor.setReadOnlyWithToolbarAction(!editor.isReadOnly());
            editBtn.setVisible(false);
            saveBtn.setVisible(true);
            //editor.setReadOnly(false);
        });

        content.add(editor,editBtn,saveBtn);

        if(projects != null && projects.isPresent()) {
            editor.setValue(projects.map(Projects::getDescription).orElse(""));
        }
        return content;

    }
    private void updateDescription() {
        editor.setValue(projects.map(Projects::getDescription).orElse(""));
    }
    private void updateAttachmentGrid(List<ProjectAttachmentsDTO> projectAttachmentsDTOS) {
        attachmentGrid.setItems(projectAttachmentsDTOS);
        attachmentGrid.setProjectId(projectId);
    }
    private Component getUpladTab() {
        VerticalLayout content = new VerticalLayout();

        setupUploader();

        content.setSizeFull();
        content.setHeightFull();

        HorizontalLayout hl=new HorizontalLayout(singleFileUpload,uploadBtn, qsBtn, qsGrid);
        content.add(hl);
        content.add(getMGSRGrid());

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

        return content;
    }


    private boolean checkAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication !=  null  && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                return userDetails.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            }
        }
        return false;
    }

    private void setProjectParameterGrid(List<ProjectParameter> listOfProjectParameters) {
        parameterGrid = new Grid<>(ProjectParameter.class, false);
        parameterGrid.addColumn(ProjectParameter::getName).setHeader("Name").setAutoWidth(true).setResizable(true);
        parameterGrid.addColumn(ProjectParameter::getValue).setHeader("Value").setAutoWidth(true).setResizable(true);
        parameterGrid.addColumn(ProjectParameter::getDescription).setHeader("Description").setAutoWidth(true).setResizable(true);

        parameterGrid.setItems(listOfProjectParameters);
        parameterGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        parameterGrid.setHeight("200px");
        parameterGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    }

    public class CallbackHandler implements QS_Callback {
        // Die Methode, die aufgerufen wird, wenn die externe Methode abgeschlossen ist
        @Override
        public void onComplete(String result) {
            if(!result.equals("Cancel")) {
                qsGrid.executeStartJobSteps(upload_id, agentName);
            }
        }
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

        projectUpload.setModulName("B2POutlookFIN");

        projectConnectionService.getJdbcConnection(dbUrl, dbUser, dbPassword); // Set Connection to target DB
        upload_id = projectConnectionService.saveUploadedGenericFileData(projectUpload);

        projectUpload.setUploadId(upload_id);

        System.out.println("Upload_ID: " + upload_id);

      /*  Map<String, Integer> uploadIdMap = projectConnectionService.getUploadIdMap();
        upload_id = uploadIdMap.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(1);*/

        System.out.println("Upload_ID for insert into " + tableName + " is " + upload_id);

        Notification notification = Notification.show("start upload to " + tableName + " with upload_id: "+ upload_id ,2000, Notification.Position.MIDDLE);
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
                System.out.println("SheetNr: " + sheetNr + " Name: " + sheetName);
                if (sheetName != null && !firstSheet.equals(sheetName) && sheetNr>1) {
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
            System.out.println(sheet.getPhysicalNumberOfRows()+"$$$$$$$$$" + sheet.getSheetName());

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
