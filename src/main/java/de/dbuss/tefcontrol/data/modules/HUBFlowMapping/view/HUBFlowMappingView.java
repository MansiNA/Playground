package de.dbuss.tefcontrol.data.modules.HUBFlowMapping.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.crud.CrudVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.*;
import de.dbuss.tefcontrol.components.DefaultUtils;
import de.dbuss.tefcontrol.components.LogView;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.modules.HUBFlowMapping.entity.HUBFlowMapping;
import de.dbuss.tefcontrol.data.modules.adjustmentrefx.entity.AdjustmentsREFX;
import de.dbuss.tefcontrol.data.service.*;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@PageTitle("HUB Flow Mapping")
@Route(value = "HUB_Flow_Mapping/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN","FLIP","MAPPING"})
public class HUBFlowMappingView extends VerticalLayout implements BeforeEnterObserver {

    private final ProjectConnectionService projectConnectionService;
    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private MemoryBuffer memoryBuffer = new MemoryBuffer();
    private Upload singleFileUpload = new Upload(memoryBuffer);
    private List<HUBFlowMapping> listOfHUBFlowMapping = new ArrayList<>();
    private Crud<HUBFlowMapping> crud;
    private Grid<HUBFlowMapping> grid = new Grid<>(HUBFlowMapping.class, false);
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
    private DefaultUtils defaultUtils;
    private List<ProjectAttachmentsDTO> listOfProjectAttachments;
    private QS_Grid qsGrid;
    private Button qsBtn;
    // private Button uploadBtn;
    private String fileName;
    private int upload_id;
    ListenableFuture<String> future;
    BackendService backendService;
    private AuthenticatedUser authenticatedUser;
    private Boolean isVisible = false;
    Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);
    private LogView logView;
    private Boolean isLogsVisible = false;

    public HUBFlowMappingView(ProjectParameterService projectParameterService, ProjectConnectionService projectConnectionService, BackendService backendService, AuthenticatedUser authenticatedUser, ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService) {

        this.backendService = backendService;
        this.projectConnectionService = projectConnectionService;
        this.authenticatedUser=authenticatedUser;
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;
        logView = new LogView();
        logView.logMessage(Constants.INFO, "Starting HUBFlowMappingView");

        //uploadBtn = new Button("Upload");
        //uploadBtn.setEnabled(false);

        qsBtn = new Button("QS and Start Job");

        qsBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        qsBtn.setEnabled(false);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.HUB_FLOW_MAPPING.equals(projectParameter.getNamespace()))
                .collect(Collectors.toList());

        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : filteredProjectParameters) {
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

        if (MainLayout.isAdmin) {
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

        logView.logMessage(Constants.INFO, "Ending HUBFlowMappingView");
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
        logView.logMessage(Constants.INFO, "Staring getUpladTab() for set upload data");
        VerticalLayout content = new VerticalLayout();

        setupUploader();

        content.setSizeFull();
        content.setHeightFull();

        HorizontalLayout hl=new HorizontalLayout(singleFileUpload, qsBtn, qsGrid);
        content.add(hl);
        content.add(getAdjustmentsREFXGrid());

        //  uploadBtn.addClickListener(e ->{
        //      logView.logMessage(Constants.INFO, "Uploading in uploadBtn.addClickListener");
        //      //save2db();
        //      qsBtn.setEnabled(true);
        //  });

        qsBtn.addClickListener(e ->{
            logView.logMessage(Constants.INFO, "executing sqls in qsBtn.addClickListener");
            //   if (qsGrid.projectId != projectId) {
            save2db();
            hl.remove(qsGrid);
            qsGrid = new QS_Grid(projectConnectionService, backendService);
            hl.add(qsGrid);
            CallbackHandler callbackHandler = new CallbackHandler();
            qsGrid.createDialog(callbackHandler, projectId, upload_id);
            //  }
            qsGrid.showDialog(true);
        });
        logView.logMessage(Constants.INFO, "Ending getUpladTab() for set upload data");
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
        logView.logMessage(Constants.INFO, "Starting setProjectParameterGrid() for set database detail in Grid");
        parameterGrid = new Grid<>(ProjectParameter.class, false);
        parameterGrid.addColumn(ProjectParameter::getName).setHeader("Name").setAutoWidth(true).setResizable(true);
        parameterGrid.addColumn(ProjectParameter::getValue).setHeader("Value").setAutoWidth(true).setResizable(true);
        parameterGrid.addColumn(ProjectParameter::getDescription).setHeader("Description").setAutoWidth(true).setResizable(true);

        parameterGrid.setItems(listOfProjectParameters);
        parameterGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        parameterGrid.setHeight("200px");
        parameterGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        parameterGrid.setThemeName("dense");
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

        projectUpload.setModulName("HUB_Flow_Mapping");

        logView.logMessage(Constants.INFO, "Get file upload id from database");
        projectConnectionService.getJdbcConnection(dbUrl, dbUser, dbPassword); // Set Connection to target DB
        upload_id = projectConnectionService.saveUploadedGenericFileData(projectUpload);

        projectUpload.setUploadId(upload_id);

        System.out.println("Upload_ID: " + upload_id);
        logView.logMessage(Constants.INFO, "Upload_ID is: " +upload_id);

      /*  Map<String, Integer> uploadIdMap = projectConnectionService.getUploadIdMap();
        upload_id = uploadIdMap.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(1);*/

        System.out.println("Upload_ID for insert into " + tableName + " is " + upload_id);

        Notification notification = Notification.show("start upload to " + tableName + " with upload_id: "+ upload_id ,5000, Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        String result = projectConnectionService.saveHUBFlowMapping(listOfHUBFlowMapping, tableName, dbUrl, dbUser, dbPassword, upload_id);
        if (result.equals(Constants.OK)){
            logView.logMessage(Constants.INFO, "Saved file data in database");
            notification = Notification.show(listOfHUBFlowMapping.size() + " HUB_FlowMapping Rows Uploaded successfully",4000, Notification.Position.BOTTOM_START);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            logView.logMessage(Constants.ERROR, "Error while saving file data in database: " + result);
            notification = Notification.show("Error during HUB_FlowMapping upload: " + result ,8000, Notification.Position.BOTTOM_START);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        logView.logMessage(Constants.INFO, "Ending save2db() for saving file data in database");
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
            Notification.show(result,6000, Notification.Position.BOTTOM_START);
        });
        logView.logMessage(Constants.INFO, "Ending updateUi()");
    }

    private Component getAdjustmentsREFXGrid() {
        logView.logMessage(Constants.INFO, "Starting getAdjustmentsREFXGrid() for get AdjustmentsREFX Grid");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        crud = new Crud<>(HUBFlowMapping.class, createAdjustmentsREFXEditor());
        crud.setToolbarVisible(false);
        crud.setHeightFull();
        crud.setThemeName("dense");
        setupHUBFlowMappingGrid();
        crud.addThemeVariants(CrudVariant.NO_BORDER);
        content.add(crud);
        logView.logMessage(Constants.INFO, "Ending getAdjustmentsREFXGrid() for get AdjustmentsREFX Grid");
        return content;
    }

    private CrudEditor<HUBFlowMapping> createAdjustmentsREFXEditor() {
        logView.logMessage(Constants.INFO, "createAdjustmentsREFXEditor() for create Editor");
        FormLayout editForm = new FormLayout();
        Binder<HUBFlowMapping> binder = new Binder<>(HUBFlowMapping.class);

        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupAdjustmentsREFXGrid() {
        logView.logMessage(Constants.INFO, "Starting setupAdjustmentsREFXGrid() for setup AdjustmentsREFX Grid");

        String ID = "id";
        String SCENARIO = "scenario";
        String DATE = "date";
        String ADJUSTMENT_TYPE = "adjustmentType";
        String AUTHORIZATION_GROUP = "authorizationGroup";
        String COMPANY_CODE = "companyCode";
        String ASSET_CLASS = "assetClass";
        String VENDOR = "vendor";
        String PROFIT_CENTER = "profitCenter";
        String LEASE_PAYMENTS = "leasePayments";
        String LEASE_LIABILITY = "leaseLiability";
        String INTEREST = "interest";
        String ROU_CAPEX = "rouCapex";
        String ROU_DEPRECIATION = "rouDepreciation";
        String COMMENT = "comment";
        String LOADDATE = "loadDate";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        grid = crud.getGrid();

        //   grid.getColumnByKey(ID).setHeader("ID").setWidth("120px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(SCENARIO).setHeader("Scenario").setWidth("60px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(DATE).setHeader("Date").setWidth("80px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(ADJUSTMENT_TYPE).setHeader("Adjustment Type").setWidth("80px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(AUTHORIZATION_GROUP).setHeader("Authorization Group").setWidth("100px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(COMPANY_CODE).setHeader("Company Code").setWidth("200px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(ASSET_CLASS).setHeader("Asset Class").setWidth("200px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(VENDOR).setHeader("Vendor").setWidth("200px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(PROFIT_CENTER).setHeader("Profit Center").setWidth("80px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(LEASE_PAYMENTS).setHeader("Lease Payments").setWidth("200px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(LEASE_LIABILITY).setHeader("Lease Liability").setWidth("80px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(INTEREST).setHeader("Interest").setWidth("80px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(ROU_CAPEX).setHeader("ROU Capex").setWidth("80px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(ROU_DEPRECIATION).setHeader("ROU Depreciation").setWidth("80px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(COMMENT).setHeader("Comment").setWidth("80px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(LOADDATE).setHeader("Load Date").setWidth("80px").setFlexGrow(0).setResizable(true);

        // Remove edit column
        grid.removeColumn(grid.getColumnByKey(EDIT_COLUMN));
        grid.removeColumn(grid.getColumnByKey(LOADDATE));
        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        // Reorder the columns
        grid.setColumnOrder(
                //     grid.getColumnByKey(ID),
                grid.getColumnByKey(SCENARIO),
                grid.getColumnByKey(DATE),
                grid.getColumnByKey(ADJUSTMENT_TYPE),
                grid.getColumnByKey(AUTHORIZATION_GROUP),
                grid.getColumnByKey(COMPANY_CODE),
                grid.getColumnByKey(ASSET_CLASS),
                grid.getColumnByKey(VENDOR),
                grid.getColumnByKey(PROFIT_CENTER),
                grid.getColumnByKey(LEASE_PAYMENTS),
                grid.getColumnByKey(LEASE_LIABILITY),
                grid.getColumnByKey(INTEREST),
                grid.getColumnByKey(ROU_CAPEX),
                grid.getColumnByKey(ROU_DEPRECIATION),
                grid.getColumnByKey(COMMENT)
        );

        // Set theme variants
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        grid.setThemeName("dense");

        logView.logMessage(Constants.INFO, "Ending setupAdjustmentsREFXGrid() for setup AdjustmentsREFX Grid");
    }
    private void setupHUBFlowMappingGrid() {
        logView.logMessage(Constants.INFO, "Starting setupHUBFlowMappingGrid() for setting up HUB Flow Mapping Grid");

        // Define the column keys corresponding to the HUBFlowMapping entity fields
        String ZEILE = "zeile";
        String HUB_MOVEMENT_TYPE_DETAIL_ID = "hubMovementTypeDetailId";
        String HUB_MOVEMENT_TYPE_DETAIL_NAME = "hubMovementTypeDetailName";
        String FLOW_L1_ID = "flowL1Id";
        String FLOW_L1_NAME = "flowL1Name";
        String FLOW_L2_ID = "flowL2Id";
        String FLOW_L2_NAME = "flowL2Name";
        String FLOW_L3_ID = "flowL3Id";
        String FLOW_L3_NAME = "flowL3Name";
        String HUB_FLOW_ID = "hubFlowId";
        String HUB_FLOW_NAME = "hubFlowName";
        String SORT_HUB_FLOW_ID = "sortHubFlowId";
        String SORT_HUB_FLOW_NAME = "sortHubFlowName";
        String LOADDATE = "loadDate";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        grid = crud.getGrid();

        // Set headers and properties for each column
        grid.getColumnByKey(ZEILE).setHeader("Zeile").setWidth("60px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(HUB_MOVEMENT_TYPE_DETAIL_ID).setHeader("HUB Movement Type Detail ID").setWidth("100px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(HUB_MOVEMENT_TYPE_DETAIL_NAME).setHeader("HUB Movement Type Detail Name").setWidth("200px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(FLOW_L1_ID).setHeader("Flow L1 ID").setWidth("100px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(FLOW_L1_NAME).setHeader("Flow L1 Name").setWidth("200px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(FLOW_L2_ID).setHeader("Flow L2 ID").setWidth("100px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(FLOW_L2_NAME).setHeader("Flow L2 Name").setWidth("200px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(FLOW_L3_ID).setHeader("Flow L3 ID").setWidth("100px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(FLOW_L3_NAME).setHeader("Flow L3 Name").setWidth("200px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(HUB_FLOW_ID).setHeader("HUB Flow ID").setWidth("100px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(HUB_FLOW_NAME).setHeader("HUB Flow Name").setWidth("200px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(SORT_HUB_FLOW_ID).setHeader("Sort HUB Flow ID").setWidth("100px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(SORT_HUB_FLOW_NAME).setHeader("Sort HUB Flow Name").setWidth("200px").setFlexGrow(0).setResizable(true);
      //  grid.getColumnByKey(LOADDATE).setHeader("Load Date").setWidth("80px").setFlexGrow(0).setResizable(true);

        // Remove edit column if it exists
        grid.removeColumn(grid.getColumnByKey(EDIT_COLUMN));
        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        // Reorder the columns
        grid.setColumnOrder(
                grid.getColumnByKey(ZEILE),
                grid.getColumnByKey(HUB_MOVEMENT_TYPE_DETAIL_ID),
                grid.getColumnByKey(HUB_MOVEMENT_TYPE_DETAIL_NAME),
                grid.getColumnByKey(FLOW_L1_ID),
                grid.getColumnByKey(FLOW_L1_NAME),
                grid.getColumnByKey(FLOW_L2_ID),
                grid.getColumnByKey(FLOW_L2_NAME),
                grid.getColumnByKey(FLOW_L3_ID),
                grid.getColumnByKey(FLOW_L3_NAME),
                grid.getColumnByKey(HUB_FLOW_ID),
                grid.getColumnByKey(HUB_FLOW_NAME),
                grid.getColumnByKey(SORT_HUB_FLOW_ID),
                grid.getColumnByKey(SORT_HUB_FLOW_NAME)
             //   grid.getColumnByKey(LOADDATE)
        );

        // Set theme variants
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        grid.setThemeName("dense");

        logView.logMessage(Constants.INFO, "Ending setupHUBFlowMappingGrid() for setting up HUB Flow Mapping Grid");
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

            GenericDataProvider dataAdjustmentsREFXProvider = new GenericDataProvider(listOfHUBFlowMapping, "zeile");
            grid.setDataProvider(dataAdjustmentsREFXProvider);
            singleFileUpload.clearFileList();
            //uploadBtn.setEnabled(true);
            qsBtn.setEnabled(true);
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

            String sheetName = "HUB Flow Mapping";
            System.out.println("SheetNr: " + sheetNr + " Name: " + sheetName);
            XSSFSheet sheet = my_xls_workbook.getSheet(sheetName);
            listOfHUBFlowMapping = parseSheet(sheet, HUBFlowMapping.class);

            logView.logMessage(Constants.INFO, "Ending parseExcelFile() for parse uploaded file");
        } catch (Exception e) {
            logView.logMessage(Constants.ERROR, "Error while parse uploaded file");
            e.printStackTrace();
        }
    }

    public <T> List<T>  parseSheet(XSSFSheet my_worksheet, Class<T> targetType) {
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

}
