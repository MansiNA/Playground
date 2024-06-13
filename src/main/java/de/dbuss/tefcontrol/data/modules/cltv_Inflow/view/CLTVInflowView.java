package de.dbuss.tefcontrol.data.modules.cltv_Inflow.view;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.crud.*;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.gridpro.GridPro;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import de.dbuss.tefcontrol.components.DefaultUtils;
import de.dbuss.tefcontrol.components.LogView;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.modules.adjustmentrefx.entity.AdjustmentsREFX;
import de.dbuss.tefcontrol.data.modules.adjustmentrefx.view.AdjustmentsREFXView;
import de.dbuss.tefcontrol.data.modules.cltv_Inflow.entity.CLTVInflow;
import de.dbuss.tefcontrol.data.modules.cltv_Inflow.entity.CasaTerm;
import de.dbuss.tefcontrol.data.service.*;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


import java.io.*;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("CLTV Inflow-Mapping")
@Route(value = "CLTV_Inflow/:project_Id", layout = MainLayout.class)
@CssImport(
        themeFor = "vaadin-grid",
        value = "./styles/styles.css"
)
@RolesAllowed({"MAPPING", "ADMIN"})
public class CLTVInflowView extends VerticalLayout implements BeforeEnterObserver {
    private final ProjectConnectionService projectConnectionService;
    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private Crud<CLTVInflow> crud;
    private Grid<CLTVInflow> grid;

    private Crud<CasaTerm> uploadCASACrud;
    private Grid<CasaTerm> uploadCASAGrid;

    List<CLTVInflow> allCLTVInflowData;
    private Grid<CLTVInflow> missingGrid = new Grid(CLTVInflow.class);
    private Grid<CasaTerm> casaGrid = new Grid(CasaTerm.class);

    //private GridPro<CLTVInflow> missingGrid = new GridPro<>(CLTVInflow.class);
    private List<CLTVInflow> modifiedCLTVInflow = new ArrayList<>();
    private List<CasaTerm> modifiedCasa = new ArrayList<>();
    private List<CasaTerm> listOfUploadCASA = new ArrayList<>();
    private String tableName;
    private String casaTableName;
    private String casaDbUrl;

    private String casaDbUser;

    private String casaDbPassword;
    private String casaQuery;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    String missing_keyword= "nicht definiert";

    Grid.Column cltvCategoryNameColumn;
    Grid.Column controllingBrandingColumn;
    Grid.Column cltvChargeNameColumn;


    private Button missingShowHidebtn = new Button("Show/Hide Columns");
    private Button casaShowHidebtn = new Button("Show/Hide Columns");
    private Button allEntriesShowHidebtn = new Button("Show/Hide Columns");
    private Button inflowExportButton = new Button("Export");
    private Button casaExportButton = new Button("Export");
    private Button casaUploadButton = new Button("Upload");
    private int projectId;
    List<String> listOfCLTVCategoryName;
    //List<String> listOfControllingBrandingDetailed;
    List<String> listOfControllingBranding;
    List<String> listOfCLTVChargeName;
    private LogView logView;
    private Boolean isLogsVisible = false;
    private Boolean isVisible = false;
    Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);
    private Optional<Projects> projects;
    private DefaultUtils defaultUtils;
    private List<ProjectAttachmentsDTO> listOfProjectAttachments;
    private MemoryBuffer memoryBuffer = new MemoryBuffer();
    private Upload singleFileUpload = new Upload(memoryBuffer);
    private String fileName;
    private long contentLength = 0;
    private String mimeType = "";
    private Div textArea = new Div();

    public CLTVInflowView(ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService, BackendService backendService, ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService) {
        this.projectConnectionService = projectConnectionService;
        this.projectAttachmentsService = projectAttachmentsService;
        this.projectsService = projectsService;

        casaExportButton.setVisible(false);
        missingShowHidebtn.setVisible(false);
        allEntriesShowHidebtn.setVisible(true);
        casaShowHidebtn.setVisible(false);
        logView = new LogView();
        logView.logMessage(Constants.INFO, "Starting CLTVInflowView");

        addClassName("list-view");
        setSizeFull();
        // configureSaveBtn();

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.CLTV_INFLOW.equals(projectParameter.getNamespace()))
                .collect(Collectors.toList());

        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : filteredProjectParameters) {
            //  if(projectParameter.getNamespace().equals(Constants.CLTV_INFLOW)) {
            if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                dbServer = projectParameter.getValue();
            } else if (Constants.DB_NAME.equals(projectParameter.getName())) {
                dbName = projectParameter.getValue();
            } else if (Constants.DB_USER.equals(projectParameter.getName())) {
                dbUser = projectParameter.getValue();
            } else if (Constants.DB_PASSWORD.equals(projectParameter.getName())) {
                dbPassword = projectParameter.getValue();
            } else if (Constants.TABLE.equals(projectParameter.getName())) {
                tableName = projectParameter.getValue();
            } else if (Constants.CASA_TABLE.equals(projectParameter.getName())) {
                casaTableName = projectParameter.getValue();
            } else if (Constants.CASA_DB_URL.equals(projectParameter.getName())) {
                casaDbUrl = projectParameter.getValue();
            } else if (Constants.CASA_DB_USER.equals(projectParameter.getName())) {
                casaDbUser = projectParameter.getValue();
            } else if (Constants.CASA_DB_PASSWORD.equals(projectParameter.getName())) {
                casaDbPassword = projectParameter.getValue();
            } else if (Constants.CASA_QUERY.equals(projectParameter.getName())) {
                casaQuery = projectParameter.getValue();
            }
            //  }
        }

        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";


     //   Text databaseDetail = new Text("Connected to: "+ dbServer+ ", Database: " + dbName+ ", Table: " + tableName);
        setProjectParameterGrid(filteredProjectParameters);
        defaultUtils = new DefaultUtils(projectsService, projectAttachmentsService);
        //Componente QS-Grid:
        //qsGrid = new QS_Grid(projectConnectionService, backendService);

        add(getTabsheet(),parameterGrid);

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
        logView.logMessage(Constants.INFO, "Ending CLTVInflowView");
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

        tabSheet.add("Mapping", getMappingTab());
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
    private Component getMappingTab() {
        logView.logMessage(Constants.INFO, "Sarting getUpladTab() for set upload data");
        VerticalLayout content = new VerticalLayout();

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Inflow-Mapping",getCLTV_InflowGrid());
        tabSheet.add("Missing CLTV Entries", getMissingCLTV_InflowGrid());
        tabSheet.add("Missing CASA Entries", getCASA_Grid());
        tabSheet.add("Upload CASA Mapping", getUploadCASAMapping());

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();
        tabSheet.addThemeVariants(TabSheetVariant.MATERIAL_BORDERED);

        tabSheet.addSelectedChangeListener(event -> {
            //   System.out.println("Tab: " + event.getSelectedTab().toString());
            switch(event.getSelectedTab().toString()){
                case "Tab{Missing CLTV Entries}":
                    missingShowHidebtn.setVisible(true);
                    casaShowHidebtn.setVisible(false);
                    allEntriesShowHidebtn.setVisible(false);
                    inflowExportButton.setVisible(false);
                    casaExportButton.setVisible(false);
                    break;

                case "Tab{Inflow-Mapping}":
                    missingShowHidebtn.setVisible(false);
                    casaShowHidebtn.setVisible(false);
                    allEntriesShowHidebtn.setVisible(true);
                    inflowExportButton.setVisible(true);
                    casaExportButton.setVisible(false);
                    break;

                case "Tab{Missing CASA Entries}":
                    missingShowHidebtn.setVisible(false);
                    casaShowHidebtn.setVisible(true);
                    allEntriesShowHidebtn.setVisible(false);
                    inflowExportButton.setVisible(false);
                    casaExportButton.setVisible(true);
                    casaGrid.setItems(Collections.emptyList());
                    updateCasaGrid();
                    break;
                case "Tab{Upload CASA Mapping}":
                    missingShowHidebtn.setVisible(false);
                    casaShowHidebtn.setVisible(false);
                    allEntriesShowHidebtn.setVisible(false);
                    inflowExportButton.setVisible(false);
                    casaExportButton.setVisible(false);
                    listOfUploadCASA.clear();
                    break;
            }

        });



        HorizontalLayout hl = new HorizontalLayout();
        // hl.add(saveButton,databaseDetail);
        hl.add(missingShowHidebtn,casaShowHidebtn,allEntriesShowHidebtn, inflowExportButton, casaExportButton);

        hl.setAlignItems(Alignment.BASELINE);
        content.add(hl, parameterGrid, tabSheet );
        content.setHeightFull();
        content.setWidthFull();
        updateGrid();
        updateMissingGrid();

        //     allCLTVInflowData = projectConnectionService.getAllCLTVInflow(tableName, dbUrl, dbUser, dbPassword);

        //    updateCasaGrid();

        return content;
    }

    private Component getCASA_Grid() {
        logView.logMessage(Constants.INFO, "Staring getCASA_Grid for get CASA Grid");
        VerticalLayout vl = new VerticalLayout();
        configureCASAGrid();
        setUpCASAExportButton();
        vl.setAlignItems(Alignment.END);
        vl.add(casaGrid);
        vl.setSizeFull();
        vl.setHeightFull();
        logView.logMessage(Constants.INFO, "Ending getCASA_Grid for get CASA Grid");
        return vl;
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
            if(!result.equals("Cancel")) {
                logView.logMessage(Constants.INFO, "Starting CallbackHandler onComplete for update grid");
                if (modifiedCLTVInflow != null && !modifiedCLTVInflow.isEmpty()) {
                    String resultString = projectConnectionService.updateListOfCLTVInflow(modifiedCLTVInflow, tableName, dbUrl, dbUser, dbPassword);
                    if (resultString.equals(Constants.OK)){
                        Notification.show(modifiedCLTVInflow.size()+" Uploaded successfully",2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        modifiedCLTVInflow.clear();
                    } else {
                        Notification.show( "Error during upload: "+ result,3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                    updateMissingGrid();
                    updateGrid();
                } else {
                    Notification.show( "Not any changes",3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
            logView.logMessage(Constants.INFO, "Ending CallbackHandler onComplete for update grid");
        }
    }

    private void updateGrid() {
        logView.logMessage(Constants.INFO, "Starting updateGrid for update allCLTVInflow grid");
        allCLTVInflowData = projectConnectionService.getAllCLTVInflow(tableName, dbUrl, dbUser, dbPassword);
        GenericDataProvider dataProvider = new GenericDataProvider(allCLTVInflowData);


        if (cltvCategoryNameColumn != null && controllingBrandingColumn != null && cltvChargeNameColumn != null) {
            dataProvider.addDataProviderListener(changeEvent -> {


                long categoryCount = allCLTVInflowData.stream()
                        .filter(person -> "nicht definiert".equals(person.getCltvCategoryName()))
                        .count();


                long brandingCount = allCLTVInflowData.stream()
                        .filter(person -> "nicht definiert".equals(person.getControllingBranding()))
                        .count();

                long chargeNameCount = allCLTVInflowData.stream()
                        .filter(person -> "nicht definiert".equals(person.getCltvChargeName()))
                        .count();

                cltvCategoryNameColumn.setFooter("Count-Missing: " + categoryCount);
                controllingBrandingColumn.setFooter("Count-Missing: " + brandingCount);
                cltvChargeNameColumn.setFooter("Count-Missing: " + chargeNameCount);


            });

        }

      //  grid.setItems(allCLTVInflowData);
        grid.setDataProvider(dataProvider);
        logView.logMessage(Constants.INFO, "Ending updateGrid for update allCLTVInflow grid");
    }


    private void updateCasaGrid() {
        logView.logMessage(Constants.INFO, "Starting updateCasaGrid for update allCasaData grid");
        System.out.println("Update Casa-GRID");
        List<CasaTerm> allCasaData = projectConnectionService.getAllCASATerms(casaQuery, casaDbUrl, casaDbUser, casaDbPassword);
        allCLTVInflowData = projectConnectionService.getAllCLTVInflow(tableName, dbUrl, dbUser, dbPassword);
        List<CasaTerm> existingEntries=new ArrayList<>();

        if(allCLTVInflowData!=null) {
            System.out.println("Count All CASA-Entries: " + allCasaData.size());
            for (CasaTerm employee : allCasaData) {
                String employeeKey = employee.getContractFeatureId()+"_"+employee.getAttributeClassesId()+"_"+employee.getConnectType();
                for (CLTVInflow secondEmployee : allCLTVInflowData) {
                    String secondEmployeeKey = secondEmployee.getContractFeatureId()+"_"+secondEmployee.getAttributeClassesId()+"_"+secondEmployee.getConnectType();
                    if (employeeKey.equals(secondEmployeeKey)) {
                        System.out.println("Key bereits vorhanden: " + employeeKey);
                        existingEntries.add(employee);
                        break;
                    }
                }
            }
            if (allCasaData.size() == 0) {
                Notification.show( projectConnectionService.getErrorCause(),5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
        else {
            System.out.println("allCLTVInflowData is null!!");
        }

     //   System.out.println(allCasaData.size() + "..."+ allCLTVInflowData.size() +"..."+existingEntries.size());
        allCasaData.removeAll(existingEntries);

        System.out.println("Count CASA-Entries after remove existing entries: " + allCasaData.size());


       // GenericDataProvider dataProvider = new GenericDataProvider(allCasaData, "ContractFeature_id");
       // casaGrid.setDataProvider(dataProvider);
        casaGrid.setItems(allCasaData);
        logView.logMessage(Constants.INFO, "Ending updateCasaGrid for update allCasaData grid");
    }

    private void updateMissingGrid() {
        logView.logMessage(Constants.INFO, "Starting updateMissingGrid for update allCLTVInflowData missing grid");
        List<CLTVInflow> allCLTVInflowData = projectConnectionService.getAllCLTVInflow(tableName, dbUrl, dbUser, dbPassword);
        List<CLTVInflow> missingList = allCLTVInflowData.stream()
                .filter(item -> missing_keyword.equals(item.getCltvCategoryName()) ||
             //           missing_keyword.equals(item.getControllingBrandingDetailed()) ||
                        missing_keyword.equals(item.getControllingBranding()) ||
                        missing_keyword.equals(item.getCltvChargeName()))
                .collect(Collectors.toList());
        missingGrid.setItems(missingList);
        logView.logMessage(Constants.INFO, "Ending updateMissingGrid for update allCLTVInflowData missing grid");
    }

    private Component getCLTV_InflowGrid() {
        logView.logMessage(Constants.INFO, "Starting getCLTV_InflowGrid for get CLTVInflow grid");
        VerticalLayout content = new VerticalLayout();
       // content.add(exportButton);
        setUpInflowExportButton();
        crud = new Crud<>(CLTVInflow.class, createEditor());
        configureGrid();
        //content.setFlexGrow(2,crud);
        crud.setToolbarVisible(false);
        crud.setSizeFull();
        content.setAlignItems(Alignment.END);
        content.add(crud);
        content.setHeightFull();
        logView.logMessage(Constants.INFO, "Ending getCLTV_InflowGrid for get CLTVInflow grid");
        return content;
    }

    private Component getMissingCLTV_InflowGrid() {
        logView.logMessage(Constants.INFO, "Starting getMissingCLTV_InflowGrid for get missing CLTVInflow grid");
        VerticalLayout vl = new VerticalLayout();
        configureMissingGrid();
        vl.setAlignItems(Alignment.END);
        vl.add(missingGrid);
        vl.setSizeFull();
        vl.setHeightFull();
        logView.logMessage(Constants.INFO, "Ending getMissingCLTV_InflowGrid for get missing CLTVInflow grid");
        return vl;
    }

    private void configureGrid() {
        logView.logMessage(Constants.INFO, "Starting configureGrid() for configure CLTVInflow grid");
        String EDIT_COLUMN = "vaadin-crud-edit-column";
        String ID = "id";
        grid = crud.getGrid();
        grid.setSizeFull();
        grid.setHeightFull();

    //    updateGrid();

        // if setcolumn then filter not display
        // grid.setColumns("contractFeatureId", "attributeClassesId", "cfTypeClassName", "attributeClassesName", "contractFeatureSubCategoryName","contractFeatureName","cfTypeName","cfDurationInMonth","connectType", "cltvCategoryName","controllingBrandingDetailed", "controllingBranding", "user", "cltvChargeName");

        Grid.Column contractFeatureIdColumn = grid.getColumnByKey("contractFeatureId").setHeader("CF_ID").setFlexGrow(0).setResizable(true);
        Grid.Column contractFeatureNameColumn = grid.getColumnByKey("contractFeatureName").setHeader("CF_Name").setFlexGrow(0).setResizable(true);
        Grid.Column cfTypeNameColumn = grid.getColumnByKey("cfTypeName").setHeader("CF_TYPE_NAME").setFlexGrow(0).setResizable(true);
        Grid.Column contractFeatureSubCategoryNameColumn = grid.getColumnByKey("contractFeatureSubCategoryName").setHeader("CF_SubCategory_Name").setFlexGrow(0).setResizable(true);
        Grid.Column cfTypeClassNameColumn = grid.getColumnByKey("cfTypeClassName").setHeader("CF_TYPE_CLASS_NAME").setFlexGrow(0).setResizable(true);
        Grid.Column attributeClassesIdColumn = grid.getColumnByKey("attributeClassesId").setHeader("AttributeClasses_ID").setFlexGrow(0).setResizable(true);
        Grid.Column attributeClassesNameColumn = grid.getColumnByKey("attributeClassesName").setHeader("AttributeClasses_NAME").setFlexGrow(0).setResizable(true);
        Grid.Column cfDurationInMonthColumn = grid.getColumnByKey("cfDurationInMonth").setHeader("CF_Duration_in_Month").setFlexGrow(0).setResizable(true);
        Grid.Column connectTypeColumn = grid.getColumnByKey("connectType").setHeader("Connect_Type").setFlexGrow(0).setResizable(true);
        //Grid.Column cltvCategoryNameColumn = grid.getColumnByKey("cltvCategoryName").setHeader("CLTV_Category_Name").setFlexGrow(0).setResizable(true);
        cltvCategoryNameColumn = grid.getColumnByKey("cltvCategoryName").setHeader("CLTV_Category").setFlexGrow(0).setResizable(true);
      //  grid.getColumnByKey("controllingBrandingDetailed").setHeader("Controlling_Branding_Detailed").setFlexGrow(0).setResizable(true);
        controllingBrandingColumn = grid.getColumnByKey("controllingBranding").setHeader("Controlling_Branding").setFlexGrow(0).setResizable(true);
        cltvChargeNameColumn = grid.getColumnByKey("cltvChargeName").setHeader("CLTV_Charge_Name").setFlexGrow(0).setResizable(true);
        Grid.Column userColumn = grid.getColumnByKey("user").setHeader("User").setFlexGrow(0).setResizable(true);

        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        grid.removeColumn(grid.getColumnByKey(EDIT_COLUMN));
        grid.removeColumn(grid.getColumnByKey(ID));
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid.setThemeName("dense");
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);




        // set column order
        List<Grid.Column<CLTVInflow>> columnOrder = Arrays.asList(
                contractFeatureIdColumn,contractFeatureNameColumn,cfTypeNameColumn,contractFeatureSubCategoryNameColumn,cfTypeClassNameColumn,attributeClassesIdColumn,attributeClassesNameColumn,cfDurationInMonthColumn,connectTypeColumn,
                cltvCategoryNameColumn,
        //        grid.getColumnByKey("controllingBrandingDetailed"),
                controllingBrandingColumn,
                cltvChargeNameColumn,
                userColumn
        );

        grid.setColumnOrder(columnOrder);

        // default hide
        contractFeatureIdColumn.setVisible(false);
        attributeClassesIdColumn.setVisible(false);
      //  contractFeatureNameColumn.setVisible(false);
        contractFeatureSubCategoryNameColumn.setVisible(false);
        cfTypeClassNameColumn.setVisible(false);
        cfTypeNameColumn.setVisible(false);
      //  attributeClassesNameColumn.setVisible(false);
        cfDurationInMonthColumn.setVisible(false);
        userColumn.setVisible(false);
      //  connectTypeColumn.setVisible(false);

        grid.addItemDoubleClickListener(event -> {
            crud.edit(event.getItem(), Crud.EditMode.EXISTING_ITEM);
            crud.getDeleteButton().getElement().getStyle().set("display", "none");
            crud.setToolbarVisible(false);
         //   crud.getGrid().getElement().getStyle().set("display", "none");
            crud.getNewButton().getElement().getStyle().set("display", "none");
        });
        crud.addSaveListener(event -> {
            logView.logMessage(Constants.INFO, "executing crud.addSaveListener for save editedAttachment in Attachment grid");
            CLTVInflow editedCltvInflow = event.getItem();
            String resultString = projectConnectionService.updateCLTVInflow(editedCltvInflow, tableName, dbUrl, dbUser, dbPassword);
            if (resultString.equals(Constants.OK)) {
                logView.logMessage(Constants.INFO, "update modified CLTVInflow data");
                Notification.show(" Update successfully", 2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                modifiedCLTVInflow.clear();
                updateGrid();
                updateMissingGrid();
            } else {
                logView.logMessage(Constants.ERROR, "Error while updating modified CLTVInflow data");
                Notification.show("Error during upload: " + resultString, 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        allEntriesShowHidebtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        ColumnToggleContextMenu columnToggleContextMenu = new ColumnToggleContextMenu(allEntriesShowHidebtn);

        columnToggleContextMenu.addColumnToggleItem("CF_ID", contractFeatureIdColumn);
        columnToggleContextMenu.addColumnToggleItem("AttributeClasses_ID", attributeClassesIdColumn);
        columnToggleContextMenu.addColumnToggleItem("AttributeClasses_Name", attributeClassesNameColumn);
        columnToggleContextMenu.addColumnToggleItem("CF_SubCategory_Name", contractFeatureSubCategoryNameColumn);
        columnToggleContextMenu.addColumnToggleItem("CF_Name", contractFeatureNameColumn);
        columnToggleContextMenu.addColumnToggleItem("CF_Type_Name", cfTypeNameColumn);
        columnToggleContextMenu.addColumnToggleItem("Connect_Type", connectTypeColumn);
        columnToggleContextMenu.addColumnToggleItem("CF_Type_Class_Name", cfTypeClassNameColumn);
        columnToggleContextMenu.addColumnToggleItem("CF_Duration_in_Month", cfDurationInMonthColumn);




    //    columnToggleContextMenu.addColumnToggleItem("User", userColumn);
        logView.logMessage(Constants.INFO, "Ending configureGrid() for configure CLTVInflow grid");
    }




    private CrudEditor<CLTVInflow> createEditor() {
        logView.logMessage(Constants.INFO, "Starting createEditor() for CrudEditor");

        allCLTVInflowData = projectConnectionService.getAllCLTVInflow(tableName, dbUrl, dbUser, dbPassword);
        List<String> cltvCategoryNames = extractUniqueCltvCategoryNames(allCLTVInflowData);
        List<String> controllingBrandingOptions = extractUniqueControllingBranding(allCLTVInflowData);
        List<String> cltvChargeNames = extractUniqueCltvChargeNames(allCLTVInflowData);

        TextField contractFeature_idField = new TextField("ContractFeature_id");
        contractFeature_idField.setReadOnly(true);
        TextField attributeClasses_IDField = new TextField("AttributeClasses_ID");
        attributeClasses_IDField.setReadOnly(true);
        TextField connect_TypeField = new TextField("Connect_Type");
        connect_TypeField.setReadOnly(true);

        // Create ComboBox fields
        ComboBox<String> cltvCategoryNameField = new ComboBox<>("CLTV_Category_Name");
        ComboBox<String> controllingBrandingField = new ComboBox<>("Controlling_Branding");
        ComboBox<String> cltvChargeNameField = new ComboBox<>("CLTV_Charge_Name");

        // Populate ComboBox with items
        cltvCategoryNameField.setItems(cltvCategoryNames);
        controllingBrandingField.setItems(controllingBrandingOptions);
        cltvChargeNameField.setItems(cltvChargeNames);

        enableCustomValueSupport(cltvCategoryNameField, cltvCategoryNames);
        enableCustomValueSupport(controllingBrandingField, controllingBrandingOptions);
        enableCustomValueSupport(cltvChargeNameField, cltvChargeNames);

        FormLayout editForm = new FormLayout(contractFeature_idField, attributeClasses_IDField, connect_TypeField, cltvCategoryNameField, controllingBrandingField, cltvChargeNameField);

        Binder<CLTVInflow> binder = new Binder<>(CLTVInflow.class);
        binder.forField(contractFeature_idField)
                .asRequired()
                .bind(cltvInflow -> String.valueOf(cltvInflow.getContractFeatureId()),
                        (cltvInflow, value) -> cltvInflow.setContractFeatureId(Long.parseLong(value)));
        binder.forField(attributeClasses_IDField)
                .asRequired()
                .bind(cltvInflow -> String.valueOf(cltvInflow.getAttributeClassesId()),
                        (cltvInflow, value) -> cltvInflow.setAttributeClassesId(Long.parseLong(value)));
        binder.forField(connect_TypeField).asRequired().bind(CLTVInflow::getConnectType,
                CLTVInflow::setConnectType);
        binder.forField(cltvCategoryNameField).bind(CLTVInflow::getCltvCategoryName,
                CLTVInflow::setCltvCategoryName);
        binder.forField(controllingBrandingField).bind(CLTVInflow::getControllingBranding,
                CLTVInflow::setControllingBranding);
        binder.forField(cltvChargeNameField).bind(CLTVInflow::getCltvChargeName,
                CLTVInflow::setCltvChargeName);

        return new BinderCrudEditor<>(binder, editForm);
    }


    private void configureMissingGrid() {
        logView.logMessage(Constants.INFO, "Starting configureMissingGrid() for configure missing grid");
        List<CLTVInflow> allCLTVInflowData = projectConnectionService.getAllCLTVInflow(tableName, dbUrl, dbUser, dbPassword);

     //   String missing = "missing";
    /*    listOfControllingBrandingDetailed = allCLTVInflowData.stream()
                .map(CLTVInflow::getControllingBrandingDetailed)
                .filter(value -> value != null && !value.isEmpty() && !isMissing(value))
                .distinct()
                .collect(Collectors.toList());
*/
        listOfControllingBranding = allCLTVInflowData.stream()
                .map(CLTVInflow::getControllingBranding)
                .filter(value -> value != null && !value.isEmpty() && !isMissing(value))
                .distinct()
                .collect(Collectors.toList());

        listOfCLTVCategoryName = allCLTVInflowData.stream()
                .map(CLTVInflow::getCltvCategoryName)
                .filter(value ->  value != null && !value.isEmpty() && !isMissing(value))
                .distinct()
                .collect(Collectors.toList());

        listOfCLTVChargeName = allCLTVInflowData.stream()
                .map(CLTVInflow::getCltvChargeName)
                .filter(value ->  value != null && !value.isEmpty() && !isMissing(value))
                .distinct()
                .collect(Collectors.toList());

        missingGrid.addClassNames("Missing CLTV-Inflow-grid");
        missingGrid.setSizeFull();
        missingGrid.setHeightFull();

        missingGrid.setColumns("contractFeatureId", "contractFeatureName","cfTypeName", "contractFeatureSubCategoryName", "cfTypeClassName", "attributeClassesId", "attributeClassesName","cfDurationInMonth","connectType");


        Grid.Column contractFeatureIdColumn = missingGrid.getColumnByKey("contractFeatureId").setHeader("CF_ID").setFlexGrow(0).setSortable(true).setResizable(true);
        Grid.Column contractFeatureNameColumn = missingGrid.getColumnByKey("contractFeatureName").setHeader("CF_Name").setFlexGrow(0).setSortable(true).setResizable(true);
        Grid.Column attributeClassesIdColumn = missingGrid.getColumnByKey("attributeClassesId").setHeader("AttributeClasses_ID").setFlexGrow(0).setSortable(true).setResizable(true);
        Grid.Column cfTypeClassNameColumn = missingGrid.getColumnByKey("cfTypeClassName").setHeader("CF_TYPE_CLASS_NAME").setFlexGrow(0).setSortable(true).setResizable(true);
        Grid.Column attributeClassesNameColumn = missingGrid.getColumnByKey("attributeClassesName").setHeader("AttributeClasses_NAME").setFlexGrow(0).setSortable(true).setResizable(true);
        Grid.Column contractFeatureSubCategoryNameColumn = missingGrid.getColumnByKey("contractFeatureSubCategoryName").setHeader("CF_SubCategory_Name").setFlexGrow(0).setSortable(true).setResizable(true);
        Grid.Column cfTypeNameColumn = missingGrid.getColumnByKey("cfTypeName").setHeader("CF_TYPE_NAME").setFlexGrow(0).setSortable(true).setResizable(true);
        Grid.Column cfDurationInMonthColumn = missingGrid.getColumnByKey("cfDurationInMonth").setHeader("CF_Duration_in_Month").setFlexGrow(0).setSortable(true).setResizable(true);
        Grid.Column connectTypeColumn = missingGrid.getColumnByKey("connectType").setHeader("Connect_Type").setFlexGrow(0).setSortable(true).setResizable(true);

        // missingGrid.getColumnByKey("cltvCategoryName").setHeader("CLTV_Category_Name").setFlexGrow(0).setResizable(true);
        missingGrid.addComponentColumn(cltvInflow -> {
            if (isMissing(cltvInflow.getCltvCategoryName())) {
                ComboBox<String> comboBoxCategory = new ComboBox<>();
                comboBoxCategory.setPlaceholder("select or enter value...");
                if (listOfCLTVCategoryName != null && !listOfCLTVCategoryName.isEmpty()) {
                    comboBoxCategory.setItems(listOfCLTVCategoryName);
                }
                comboBoxCategory.setAllowCustomValue(true);
                comboBoxCategory.addCustomValueSetListener(e -> {
                    String customValue = e.getDetail();
                    listOfCLTVCategoryName.add(customValue);
                    comboBoxCategory.setItems(listOfCLTVCategoryName);
                    comboBoxCategory.setValue(customValue);
                    cltvInflow.setCltvCategoryName(customValue);
                });
                comboBoxCategory.addValueChangeListener(event -> {
                    String selectedValue = event.getValue();
                    cltvInflow.setCltvCategoryName(selectedValue);
                });

                return comboBoxCategory;
            } else {
                return new Text(getValidValue(cltvInflow.getCltvCategoryName()));
            }
        }).setHeader("CLTV_Category_Name").setFlexGrow(0).setSortable(true).setWidth("300px").setResizable(true);

        // missingGrid.getColumnByKey("controllingBrandingDetailed").setHeader("Controlling_Branding_Detailed").setFlexGrow(0).setResizable(true);
 /*       missingGrid.addComponentColumn(cltvInflow -> {
            if (isMissing(cltvInflow.getControllingBrandingDetailed())) {
                ComboBox<String> comboBoxBrandingDetailed = new ComboBox<>();
                comboBoxBrandingDetailed.setPlaceholder("select or enter value...");
                if (listOfControllingBrandingDetailed != null && !listOfControllingBrandingDetailed.isEmpty()) {
                    comboBoxBrandingDetailed.setItems(listOfControllingBrandingDetailed);
                }
                comboBoxBrandingDetailed.setAllowCustomValue(true);
                comboBoxBrandingDetailed.addCustomValueSetListener(e -> {
                    String customValue = e.getDetail();
                    listOfControllingBrandingDetailed.add(customValue);
                    comboBoxBrandingDetailed.setItems(listOfControllingBrandingDetailed);
                    comboBoxBrandingDetailed.setValue(customValue);
                    cltvInflow.setControllingBrandingDetailed(customValue);
                    saveModifiedCLTVInflow(cltvInflow, customValue);
                });
                comboBoxBrandingDetailed.addValueChangeListener(event -> {
                    String selectedValue = event.getValue();
                    cltvInflow.setControllingBrandingDetailed(selectedValue);
                    saveModifiedCLTVInflow(cltvInflow, selectedValue);
                });

                return comboBoxBrandingDetailed;
            } else {
                return new Text(getValidValue(cltvInflow.getControllingBrandingDetailed()));
            }
        }).setHeader("Controlling_Branding_Detailed").setFlexGrow(0).setWidth("300px").setResizable(true);*/

        // missingGrid.getColumnByKey("controllingBranding").setHeader("Controlling_Branding").setFlexGrow(0).setResizable(true);
        missingGrid.addComponentColumn(cltvInflow -> {
            if (isMissing(cltvInflow.getControllingBranding())) {
                ComboBox<String> comboBoxBranding = new ComboBox<>();
                comboBoxBranding.setPlaceholder("select or enter value...");
                if (listOfControllingBranding != null && !listOfControllingBranding.isEmpty()) {
                    comboBoxBranding.setItems(listOfControllingBranding);
                }
                comboBoxBranding.setAllowCustomValue(true);
                comboBoxBranding.addCustomValueSetListener(e -> {
                    String customValue = e.getDetail();
                    listOfControllingBranding.add(customValue);
                    comboBoxBranding.setItems(listOfControllingBranding);
                    comboBoxBranding.setValue(customValue);
                    cltvInflow.setControllingBranding(customValue);
                });
                comboBoxBranding.addValueChangeListener(event -> {
                    String selectedValue = event.getValue();
                    cltvInflow.setControllingBranding(selectedValue);
                });
                return comboBoxBranding;
            } else {
                return new Text(getValidValue(cltvInflow.getControllingBranding()));
            }
        }).setHeader("Controlling_Branding").setFlexGrow(0).setSortable(true).setWidth("300px").setResizable(true);

        //Grid.Column userColumn = missingGrid.getColumnByKey("user").setHeader("User").setFlexGrow(0).setResizable(true);
        // missingGrid.getColumnByKey("cltvChargeName").setHeader("CLTV_Charge_Name").setFlexGrow(0).setResizable(true);
        missingGrid.addComponentColumn(cltvInflow -> {
            if (isMissing(cltvInflow.getCltvChargeName())) {
                ComboBox<String> comboBoxChargeName = new ComboBox<>();
                comboBoxChargeName.setPlaceholder("select or enter value...");
                if (listOfCLTVChargeName != null && !listOfCLTVChargeName.isEmpty()) {
                    comboBoxChargeName.setItems(listOfCLTVChargeName);
                }
                comboBoxChargeName.setAllowCustomValue(true);
                comboBoxChargeName.addCustomValueSetListener(e -> {
                    String customValue = e.getDetail();
                    listOfCLTVChargeName.add(customValue);
                    comboBoxChargeName.setItems(listOfCLTVChargeName);
                    comboBoxChargeName.setValue(customValue);
                    cltvInflow.setCltvChargeName(customValue);
                });
                comboBoxChargeName.addValueChangeListener(event -> {
                    String selectedValue = event.getValue();
                    cltvInflow.setCltvChargeName(selectedValue);
                });

                return comboBoxChargeName;
            } else {
                return new Text(getValidValue(cltvInflow.getCltvChargeName()));
            }
        }).setHeader("CLTV_Charge_Name").setFlexGrow(0).setSortable(true).setWidth("300px").setResizable(true);

        // Add a column with a save button
        missingGrid.addComponentColumn(cltvInflow -> {
            Button saveButton = new Button("Save", click -> {
                String resultString = projectConnectionService.updateCLTVInflow(cltvInflow, tableName, dbUrl, dbUser, dbPassword);
                if (resultString.equals(Constants.OK)) {
                    logView.logMessage(Constants.INFO, "saveButton.addClickListener for update modified CLTVInflow data");
                    Notification.show("Uploaded successfully", 2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    modifiedCLTVInflow.clear();
                    updateGrid();
                    updateMissingGrid();
                } else {
                    logView.logMessage(Constants.ERROR, "Error while updating modified CLTVInflow data");
                    Notification.show("Error during upload: " + resultString, 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            return saveButton;
        }).setHeader("").setFlexGrow(0).setWidth("100px").setResizable(true);

        missingGrid.getColumns().forEach(col -> col.setAutoWidth(true));
        missingGrid.setSelectionMode(Grid.SelectionMode.NONE);
        missingGrid.setThemeName("dense");
    //    missingGrid.setEditOnClick(true);

        // default hide
       // contractFeatureNameColumn.setVisible(false);
        contractFeatureSubCategoryNameColumn.setVisible(false);
        cfTypeClassNameColumn.setVisible(false);

        contractFeatureIdColumn.setVisible(false);
        attributeClassesIdColumn.setVisible(false);
        cfTypeClassNameColumn.setVisible(false);
     //   attributeClassesNameColumn.setVisible(false);
        cfDurationInMonthColumn.setVisible(false);
        contractFeatureSubCategoryNameColumn.setVisible(false);
//        contractFeatureNameColumn.setVisible(false);
        cfTypeNameColumn.setVisible(false);
        //connectTypeColumn.setVisible(false);
       // userColumn.setVisible(false);

        missingShowHidebtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        ColumnToggleContextMenu columnToggleContextMenu = new ColumnToggleContextMenu(missingShowHidebtn);
        columnToggleContextMenu.addColumnToggleItem("CF_ID", contractFeatureIdColumn);
        columnToggleContextMenu.addColumnToggleItem("AttributeClasses_ID", attributeClassesIdColumn);
        columnToggleContextMenu.addColumnToggleItem("AttributeClasses_Name", attributeClassesNameColumn);
        columnToggleContextMenu.addColumnToggleItem("CF_SubCategory_Name", contractFeatureSubCategoryNameColumn);
        columnToggleContextMenu.addColumnToggleItem("CF_Name", contractFeatureNameColumn);
        columnToggleContextMenu.addColumnToggleItem("CF_Type_Name", cfTypeNameColumn);
        columnToggleContextMenu.addColumnToggleItem("Connect_Type", connectTypeColumn);
        columnToggleContextMenu.addColumnToggleItem("CF_Type_Class_Name", cfTypeClassNameColumn);
        columnToggleContextMenu.addColumnToggleItem("CF_Duration_in_Month", cfDurationInMonthColumn);
     //   columnToggleContextMenu.addColumnToggleItem("User", userColumn);
        logView.logMessage(Constants.INFO, "Ending configureMissingGrid() for configure missing grid");
    }

    private void configureCASAGrid() {
        logView.logMessage(Constants.INFO, "Starting configureCASAGrid() for configure CASA grid");

        System.out.println("ConfigureCASA-Grid:");

      //  List<CasaTerm> allCasaData = projectConnectionService.getAllCASATerms(casaQuery, casaDbUrl, casaDbUser, casaDbPassword);
        List<CasaTerm> allCasaData = new ArrayList<>();

        /*
        if(allCLTVInflowData!=null) {

            for (CasaTerm employee : allCasaData) {
                for (CLTVInflow secondEmployee : allCLTVInflowData) {
                    if (employee.getContractFeatureId() == secondEmployee.getContractFeatureId() && employee.getAttributeClassesId() == secondEmployee.getAttributeClassesId() && employee.getConnectType() == secondEmployee.getConnectType()   ) {
                        System.out.println("Bereits vorhanden: CF-ID: " + employee.getContractFeatureId() + ", AttributeClasses-ID: " + employee.getAttributeClassesId()  + ", Connect-Type: " + employee.getConnectType()  );
                        break;
                    }
                }
            }
        }
        else {
            System.out.println("allCLTVInflowData is null!!");
        }

         */



        GenericDataProvider dataProvider = new GenericDataProvider(allCasaData, "ContractFeature_id");
        casaGrid.setDataProvider(dataProvider);

        /*List<String> listOfCLTVCategoryName = allCasaData.stream()
                .map(CasaTerm::getCltvCategoryName)
                .filter(value ->  value != null && !value.isEmpty() && !isMissing(value))
                .distinct()
                .collect(Collectors.toList());
         */


        casaGrid.addClassNames("casa-grid");
        casaGrid.setSizeFull();
        casaGrid.setHeightFull();
        casaGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        casaGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        casaGrid.setThemeName("dense");


        //casaGrid.setColumns("contractFeatureId", "attributeClassesId", "cfTypeClassName", "attributeClassesName", "contractFeatureSubCategoryName","contractFeatureName","cfTypeName","cfDurationInMonth","connectType", "user");
        casaGrid.setColumns("contractFeatureId", "attributeClassesId", "attributeClassesName", "connectType", "cfTypeClassName", "termName");



        Grid.Column contractFeatureIdColumn = casaGrid.getColumnByKey("contractFeatureId").setHeader("ContractFeature_id").setFlexGrow(0).setSortable(true).setResizable(true);
        Grid.Column attributeClassesIdColumn = casaGrid.getColumnByKey("attributeClassesId").setHeader("AttributeClasses_ID").setFlexGrow(0).setSortable(true).setResizable(true);
        casaGrid.getColumnByKey("attributeClassesName").setHeader("AttributeClasses_NAME").setFlexGrow(0).setSortable(true).setResizable(true);
        casaGrid.getColumnByKey("connectType").setHeader("Connect_Type").setFlexGrow(0).setSortable(true).setResizable(true);
        casaGrid.getColumnByKey("cfTypeClassName").setHeader("CF_TYPE_CLASS_NAME").setFlexGrow(0).setSortable(true).setResizable(true);
        casaGrid.getColumnByKey("termName").setHeader("Termname").setFlexGrow(0).setSortable(true).setResizable(true);


        casaGrid.addComponentColumn(CasaTerm -> {

                ComboBox<String> comboBoxCategory = new ComboBox<>();
                comboBoxCategory.setPlaceholder("select CLTV_Category");
                if (listOfCLTVCategoryName != null && !listOfCLTVCategoryName.isEmpty()) {
                    comboBoxCategory.setItems(listOfCLTVCategoryName);
                }
                comboBoxCategory.setAllowCustomValue(true);
                comboBoxCategory.addCustomValueSetListener(e -> {
                    String customValue = e.getDetail();
                    listOfCLTVCategoryName.add(customValue);
                    comboBoxCategory.setItems(listOfCLTVCategoryName);
                    comboBoxCategory.setValue(customValue);
                    CasaTerm.setCltvCategoryName(customValue);
                    saveModifiedCasa(CasaTerm, customValue);
                });
                comboBoxCategory.addValueChangeListener(event -> {
                    String selectedValue = event.getValue();
                    CasaTerm.setCltvCategoryName(selectedValue);
                    saveModifiedCasa(CasaTerm, selectedValue);
                });

                return comboBoxCategory;

        }).setHeader("CLTV_Category_Name").setFlexGrow(0).setSortable(true).setWidth("450px").setResizable(true);

        casaGrid.addComponentColumn(CasaTerm -> {

            ComboBox<String> comboBoxControllingBranding = new ComboBox<>();
            comboBoxControllingBranding.setPlaceholder("select Branding");
            if (listOfControllingBranding != null && !listOfControllingBranding.isEmpty()) {
                comboBoxControllingBranding.setItems(listOfControllingBranding);
            }
            comboBoxControllingBranding.setAllowCustomValue(true);
            comboBoxControllingBranding.addCustomValueSetListener(e -> {
                String customValue = e.getDetail();
                listOfControllingBranding.add(customValue);
                comboBoxControllingBranding.setItems(listOfControllingBranding);
                comboBoxControllingBranding.setValue(customValue);
                CasaTerm.setCltvCategoryName(customValue);
                saveModifiedCasa(CasaTerm, customValue);
            });
            comboBoxControllingBranding.addValueChangeListener(event -> {
                String selectedValue = event.getValue();
                CasaTerm.setControllingBranding(selectedValue);
                saveModifiedCasa(CasaTerm, selectedValue);
            });

            return comboBoxControllingBranding;

        }).setHeader("ControllingBranding").setFlexGrow(0).setSortable(true).setWidth("500px").setResizable(true);


        casaGrid.addComponentColumn(CasaTerm -> {

            ComboBox<String> comboBoxCLTVChargeName = new ComboBox<>();
            comboBoxCLTVChargeName.setPlaceholder("select Charge Name");
            if (listOfCLTVChargeName != null && !listOfCLTVChargeName.isEmpty()) {
                comboBoxCLTVChargeName.setItems(listOfCLTVChargeName);
            }
            comboBoxCLTVChargeName.setAllowCustomValue(true);
            comboBoxCLTVChargeName.addCustomValueSetListener(e -> {
                String customValue = e.getDetail();
                listOfCLTVChargeName.add(customValue);
                comboBoxCLTVChargeName.setItems(listOfCLTVChargeName);
                comboBoxCLTVChargeName.setValue(customValue);
                CasaTerm.setCltvChargeName(customValue);
                saveModifiedCasa(CasaTerm, customValue);
            });
            comboBoxCLTVChargeName.addValueChangeListener(event -> {
                String selectedValue = event.getValue();
                CasaTerm.setCltvChargeName(selectedValue);
                saveModifiedCasa(CasaTerm, selectedValue);
            });

            return comboBoxCLTVChargeName;

        }).setHeader("CLTV_Charge_Name").setFlexGrow(0).setSortable(true).setWidth("500px").setResizable(true);


        casaGrid.addColumn(
                new ComponentRenderer<>(Button::new, (button, casaTerm) -> {
                    button.addThemeVariants(ButtonVariant.LUMO_ICON,
                            ButtonVariant.LUMO_SUCCESS,
                            ButtonVariant.LUMO_TERTIARY);
                    button.addClickListener(e -> {
                        System.out.println("Save: " + casaTerm.getContractFeatureId().toString() + " with Category: " + casaTerm.getCltvCategoryName() + "Branding: " + casaTerm.getControllingBranding() + " ChargeName: " + casaTerm.getCltvChargeName());
                        String ret = projectConnectionService.saveCASAToTargetTable(casaTerm, tableName, dbUrl, dbUser, dbPassword);

                        if (ret == Constants.OK) {
                            Notification.show("Upload Successfully", 6000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        } else {
                            Notification.show(ret, 6000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                        }

                        updateGrid(); //First update Grid for check existing entries.
                        updateCasaGrid();


                    });
                    button.setIcon(new Icon(VaadinIcon.CLOUD_DOWNLOAD_O));

                })).setHeader("upload");


        casaGrid.getColumns().forEach(col -> col.setAutoWidth(true));
        casaGrid.setSelectionMode(Grid.SelectionMode.SINGLE);

        contractFeatureIdColumn.setVisible(false);
        attributeClassesIdColumn.setVisible(false);

        casaShowHidebtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        ColumnToggleContextMenu columnToggleContextMenu = new ColumnToggleContextMenu(casaShowHidebtn);
        columnToggleContextMenu.addColumnToggleItem("CF_ID", contractFeatureIdColumn);
        columnToggleContextMenu.addColumnToggleItem("AttributeClasses_ID", attributeClassesIdColumn);




/*
        // missingGrid.getColumnByKey("controllingBrandingDetailed").setHeader("Controlling_Branding_Detailed").setFlexGrow(0).setResizable(true);
        missingGrid.addComponentColumn(cltvInflow -> {
            if (isMissing(cltvInflow.getControllingBrandingDetailed())) {
                ComboBox<String> comboBoxBrandingDetailed = new ComboBox<>();
                comboBoxBrandingDetailed.setPlaceholder("select or enter value...");
                if (listOfControllingBrandingDetailed != null && !listOfControllingBrandingDetailed.isEmpty()) {
                    comboBoxBrandingDetailed.setItems(listOfControllingBrandingDetailed);
                }
                comboBoxBrandingDetailed.setAllowCustomValue(true);
                comboBoxBrandingDetailed.addCustomValueSetListener(e -> {
                    String customValue = e.getDetail();
                    listOfControllingBrandingDetailed.add(customValue);
                    comboBoxBrandingDetailed.setItems(listOfControllingBrandingDetailed);
                    comboBoxBrandingDetailed.setValue(customValue);
                    cltvInflow.setControllingBrandingDetailed(customValue);
                    saveModifiedCLTVInflow(cltvInflow, customValue);
                });
                comboBoxBrandingDetailed.addValueChangeListener(event -> {
                    String selectedValue = event.getValue();
                    cltvInflow.setControllingBrandingDetailed(selectedValue);
                    saveModifiedCLTVInflow(cltvInflow, selectedValue);
                });

                return comboBoxBrandingDetailed;
            } else {
                return new Text(getValidValue(cltvInflow.getControllingBrandingDetailed()));
            }
        }).setHeader("Controlling_Branding_Detailed").setFlexGrow(0).setWidth("300px").setResizable(true);

        // missingGrid.getColumnByKey("controllingBranding").setHeader("Controlling_Branding").setFlexGrow(0).setResizable(true);
        missingGrid.addComponentColumn(cltvInflow -> {
            if (isMissing(cltvInflow.getControllingBranding())) {
                ComboBox<String> comboBoxBranding = new ComboBox<>();
                comboBoxBranding.setPlaceholder("select or enter value...");
                if (listOfControllingBranding != null && !listOfControllingBranding.isEmpty()) {
                    comboBoxBranding.setItems(listOfControllingBranding);
                }
                comboBoxBranding.setAllowCustomValue(true);
                comboBoxBranding.addCustomValueSetListener(e -> {
                    String customValue = e.getDetail();
                    listOfControllingBranding.add(customValue);
                    comboBoxBranding.setItems(listOfControllingBranding);
                    comboBoxBranding.setValue(customValue);
                    cltvInflow.setControllingBranding(customValue);
                    saveModifiedCLTVInflow(cltvInflow, customValue);
                });
                comboBoxBranding.addValueChangeListener(event -> {
                    String selectedValue = event.getValue();
                    cltvInflow.setControllingBranding(selectedValue);
                    saveModifiedCLTVInflow(cltvInflow, selectedValue);
                });
                return comboBoxBranding;
            } else {
                return new Text(getValidValue(cltvInflow.getControllingBranding()));
            }
        }).setHeader("Controlling_Branding").setFlexGrow(0).setWidth("300px").setResizable(true);

        Grid.Column userColumn = missingGrid.getColumnByKey("user").setHeader("User").setFlexGrow(0).setResizable(true);
        // missingGrid.getColumnByKey("cltvChargeName").setHeader("CLTV_Charge_Name").setFlexGrow(0).setResizable(true);
        missingGrid.addComponentColumn(cltvInflow -> {
            if (isMissing(cltvInflow.getCltvChargeName())) {
                ComboBox<String> comboBoxChargeName = new ComboBox<>();
                comboBoxChargeName.setPlaceholder("select or enter value...");
                if (listOfCLTVChargeName != null && !listOfCLTVChargeName.isEmpty()) {
                    comboBoxChargeName.setItems(listOfCLTVChargeName);
                }
                comboBoxChargeName.setAllowCustomValue(true);
                comboBoxChargeName.addCustomValueSetListener(e -> {
                    String customValue = e.getDetail();
                    listOfCLTVChargeName.add(customValue);
                    comboBoxChargeName.setItems(listOfCLTVChargeName);
                    comboBoxChargeName.setValue(customValue);
                    cltvInflow.setCltvChargeName(customValue);
                    saveModifiedCLTVInflow(cltvInflow, customValue);
                });
                comboBoxChargeName.addValueChangeListener(event -> {
                    String selectedValue = event.getValue();
                    cltvInflow.setCltvChargeName(selectedValue);
                    saveModifiedCLTVInflow(cltvInflow, selectedValue);
                });

                return comboBoxChargeName;
            } else {
                return new Text(getValidValue(cltvInflow.getCltvChargeName()));
            }
        }).setHeader("CLTV_Charge_Name").setFlexGrow(0).setWidth("300px").setResizable(true);

        missingGrid.getColumns().forEach(col -> col.setAutoWidth(true));
        missingGrid.setSelectionMode(Grid.SelectionMode.NONE);
        //    missingGrid.setEditOnClick(true);

        // default hide
        contractFeatureIdColumn.setVisible(false);
        attributeClassesIdColumn.setVisible(false);

        missingShowHidebtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        ColumnToggleContextMenu columnToggleContextMenu = new ColumnToggleContextMenu(missingShowHidebtn);
        columnToggleContextMenu.addColumnToggleItem("ContractFeature_id", contractFeatureIdColumn);
        columnToggleContextMenu.addColumnToggleItem("AttributeClasses_ID", attributeClassesIdColumn);
        columnToggleContextMenu.addColumnToggleItem("CF_Duration_in_Month", cfDurationInMonthColumn);
        columnToggleContextMenu.addColumnToggleItem("User", userColumn);





 */


        logView.logMessage(Constants.INFO, "Ending configureCASAGrid() for configure CASA grid");
    }


    private void saveModifiedCasa(CasaTerm cltvInflow, String selectedValue) {
        logView.logMessage(Constants.INFO, "Starting saveModifiedCasa() for save modified case");
        if(!isMissing(selectedValue)) {
            if(modifiedCasa.contains(cltvInflow)){
                modifiedCasa.remove(cltvInflow);
                modifiedCasa.add(cltvInflow);
            } else {
                modifiedCasa.add(cltvInflow);
            }
           // saveButton.setEnabled(true);
        } else {
            Notification.show("Please do not enter Missing value", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        logView.logMessage(Constants.INFO, "Ending saveModifiedCasa() for save modified case");
    }

    private void setUpInflowExportButton() {
        logView.logMessage(Constants.INFO, "Starting setUpExportButton() prepare excel file for export");

        inflowExportButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        inflowExportButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        inflowExportButton.addClickListener(clickEvent -> {
            System.out.println("export..click.....");
            Notification.show("Exportiere Inflow Daten ");
            try {
                List<CLTVInflow> listOfCLTVInflow = getInflowDataProviderAllItems();
                generateInflowExcelFile(listOfCLTVInflow);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        logView.logMessage(Constants.INFO, "Ending setUpExportButton() prepare excel file for export");
    }

    private void setUpCASAExportButton() {
        logView.logMessage(Constants.INFO, "Starting setUpExportButton() prepare excel file for export");

        casaExportButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        casaExportButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        casaExportButton.addClickListener(clickEvent -> {
            System.out.println("export..click.....");
            Notification.show("Exportiere CASA Daten ");
            try {
                List<CasaTerm> listOfCASAEntries = getCASADataProviderAllItems();
                generateExcelFile(listOfCASAEntries);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        logView.logMessage(Constants.INFO, "Ending setUpExportButton() prepare excel file for export");
    }
    private List<CLTVInflow> getInflowDataProviderAllItems() {
        logView.logMessage(Constants.INFO,"Starting getDataProviderAllItems for grid dataprovider list");
        DataProvider<CLTVInflow, Void> existDataProvider = (DataProvider<CLTVInflow, Void>) grid.getDataProvider();
        List<CLTVInflow> listOfCLTVInflow = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        logView.logMessage(Constants.INFO,"Ending getDataProviderAllItems for grid dataprovider list");
        return listOfCLTVInflow;
    }

    private List<CasaTerm> getCASADataProviderAllItems() {
        logView.logMessage(Constants.INFO,"Starting getDataProviderAllItems for grid dataprovider list");
        DataProvider<CasaTerm, Void> existDataProvider = (DataProvider<CasaTerm, Void>) casaGrid.getDataProvider();
        List<CasaTerm> listOfCASAEntries = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        logView.logMessage(Constants.INFO,"Ending getDataProviderAllItems for grid dataprovider list");
        return listOfCASAEntries;
    }

    public void generateInflowExcelFile(List<CLTVInflow> cltvInflowData) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("CLTV Inflow Data");

        // Create a header row
        Row headerRow = sheet.createRow(0);
        String[] columns = {"ContractFeature_id", "AttributeClasses_ID", "CF_TYPE_CLASS_NAME", "AttributeClasses_NAME",
                "ContractFeatureSubCategory_Name", "ContractFeature_Name", "CF_TYPE_NAME", "CF_Duration_in_Month",
                "Connect_Type", "CLTV_Category_Name", "Controlling_Branding", "CLTV_Charge_Name"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
        }

        // Create rows for each CLTVInflow object
        int rowNum = 1;
        for (CLTVInflow inflow : cltvInflowData) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(inflow.getContractFeatureId());
            row.createCell(1).setCellValue(inflow.getAttributeClassesId());
            row.createCell(2).setCellValue(inflow.getCfTypeClassName());
            row.createCell(3).setCellValue(inflow.getAttributeClassesName());
            row.createCell(4).setCellValue(inflow.getContractFeatureSubCategoryName());
            row.createCell(5).setCellValue(inflow.getContractFeatureName());
            row.createCell(6).setCellValue(inflow.getCfTypeName());
            row.createCell(7).setCellValue(inflow.getCfDurationInMonth());
            row.createCell(8).setCellValue(inflow.getConnectType());
            row.createCell(9).setCellValue(inflow.getCltvCategoryName());
            row.createCell(10).setCellValue(inflow.getControllingBranding());
            row.createCell(11).setCellValue(inflow.getCltvChargeName());
            //  row.createCell(12).setCellValue(inflow.getUser());
            //  row.createCell(13).setCellValue(inflow.getId());
        }

        // Resize all columns to fit the content size
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }



        String fileName = "Inflow_Mapping.xlsx";
        StreamResource streamResource = new StreamResource(fileName, () -> {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);
                byte[] dataBytes = outputStream.toByteArray();
                return new ByteArrayInputStream(dataBytes);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });

        Anchor inflowanchor = new Anchor(streamResource, "");
        inflowanchor.getElement().setAttribute("download", true);
        inflowanchor.getElement().getStyle().set("display", "none");
        add(inflowanchor);
        UI.getCurrent().getPage().executeJs("arguments[0].click()", inflowanchor);
    }

    public void generateExcelFile(List<CasaTerm> casaData) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("CASA Term");

        // Create a header row
        Row headerRow = sheet.createRow(0);
        String[] columns = {"ContractFeature_id", "AttributeClasses_ID", "AttributeClasses_NAME", "Connect_Type", "CF_TYPE_CLASS_NAME", "Term_Name",
                 "CLTV_Category_Name", "Controlling_Branding", "CLTV_Charge_Name"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
        }

        // Create rows for each CLTVInflow object
        int rowNum = 1;
        for (CasaTerm casa : casaData) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(casa.getContractFeatureId());
            row.createCell(1).setCellValue(casa.getAttributeClassesId());
            row.createCell(2).setCellValue(casa.getAttributeClassesName());
            row.createCell(3).setCellValue(casa.getConnectType());
            row.createCell(4).setCellValue(casa.getCfTypeClassName());
            row.createCell(5).setCellValue(casa.getTermName());
            row.createCell(6).setCellValue(casa.getCltvCategoryName());
            row.createCell(7).setCellValue(casa.getControllingBranding());
            row.createCell(8).setCellValue(casa.getCltvChargeName());
          //  row.createCell(12).setCellValue(inflow.getUser());
          //  row.createCell(13).setCellValue(inflow.getId());
        }

        // Resize all columns to fit the content size
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }



        String fileName = "CASA.xlsx";
        StreamResource streamResource = new StreamResource(fileName, () -> {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);
                byte[] dataBytes = outputStream.toByteArray();
                return new ByteArrayInputStream(dataBytes);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });

        Anchor anchor = new Anchor(streamResource, "");
        anchor.getElement().setAttribute("download", true);
        anchor.getElement().getStyle().set("display", "none");
        add(anchor);
        UI.getCurrent().getPage().executeJs("arguments[0].click()", anchor);
    }

    private boolean isMissing(String value) {
        // return value == null || value.trim().isEmpty() || "missing".equals(value);
        return missing_keyword.equals(value);
    }

    private String getValidValue (String value) {
        if(value == null || value.isEmpty()) {
            value = "";
        }
        return value;
    }

    private void saveModifiedCLTVInflow(CLTVInflow cltvInflow, String selectedValue) {
        logView.logMessage(Constants.INFO, "Starting saveModifiedCLTVInflow() for save modified CLTVInflow");
        if(!isMissing(selectedValue)) {
            if(modifiedCLTVInflow.contains(cltvInflow)){
                modifiedCLTVInflow.remove(cltvInflow);
                modifiedCLTVInflow.add(cltvInflow);
            } else {
                modifiedCLTVInflow.add(cltvInflow);
            }
        } else {
            Notification.show("Please do not enter Missing value", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        logView.logMessage(Constants.INFO, "Ending saveModifiedCLTVInflow() for save modified CLTVInflow");
    }

    private Component getUploadCASAMapping() {
        logView.logMessage(Constants.INFO, "Sarting getUpladTab() for set upload data");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();

        setupUploader();

        casaUploadButton.addClickListener(e -> {
            String resultString = projectConnectionService.updateListOfCASATerm(listOfUploadCASA, tableName, dbUrl, dbUser, dbPassword);
            if (resultString.equals(Constants.OK)) {
                logView.logMessage(Constants.INFO, "casaUploadButton.addClickListener for update modified CLTVInflow data");
                Notification.show(listOfUploadCASA.size() + "Uploaded successfully", 2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                logView.logMessage(Constants.ERROR, "Error while updating modified CLTVInflow data");
                Notification.show("Error during upload: " + resultString, 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        HorizontalLayout hl = new HorizontalLayout(singleFileUpload, casaUploadButton);
        content.add(hl);
        content.add(getUploadCasaTermGrid());
        return content;
    }

    private Component getUploadCasaTermGrid() {
        logView.logMessage(Constants.INFO, "Starting getCasaTermGrid() for CasaTerm Grid");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        uploadCASACrud = new Crud<>(CasaTerm.class, createUploadCasaTermEditor());
        uploadCASACrud.setToolbarVisible(false);
        uploadCASACrud.setHeightFull();
        uploadCASACrud.setThemeName("dense");
        setupUploadCasaTermGrid();
        uploadCASACrud.addThemeVariants(CrudVariant.NO_BORDER);
        content.add(uploadCASACrud);
        logView.logMessage(Constants.INFO, "Ending getCasaTermGrid() for CasaTerm Grid");
        return content;
    }

    private CrudEditor<CasaTerm> createUploadCasaTermEditor() {
        logView.logMessage(Constants.INFO, "createCasaTermEditor() for create Editor");
        FormLayout editForm = new FormLayout();
        Binder<CasaTerm> binder = new Binder<>(CasaTerm.class);

        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupUploadCasaTermGrid() {
        logView.logMessage(Constants.INFO, "Starting setupCasaTermGrid() for CasaTerm Grid");

        String CONTRACT_FEATURE_ID = "contractFeatureId";
        String ATTRIBUTE_CLASSES_ID = "attributeClassesId";
        String ATTRIBUTE_CLASSES_NAME = "attributeClassesName";
        String CONNECT_TYPE = "connectType";
        String CF_TYPE_CLASS_NAME = "cfTypeClassName";
        String TERM_NAME = "termName";
        String CLTV_CATEGORY_NAME = "cltvCategoryName";
        String CONTROLLING_BRANDING = "controllingBranding";
        String CLTV_CHARGE_NAME = "cltvChargeName";
        String EDIT_COLUMN = "vaadin-crud-edit-column";

        uploadCASAGrid = uploadCASACrud.getGrid();

        uploadCASAGrid.getColumnByKey(CONTRACT_FEATURE_ID).setHeader("Contract Feature ID").setWidth("120px").setFlexGrow(0).setResizable(true);
        uploadCASAGrid.getColumnByKey(ATTRIBUTE_CLASSES_ID).setHeader("Attribute Classes ID").setWidth("80px").setFlexGrow(0).setResizable(true);
        uploadCASAGrid.getColumnByKey(ATTRIBUTE_CLASSES_NAME).setHeader("Attribute Classes Name").setWidth("120px").setFlexGrow(0).setResizable(true);
        uploadCASAGrid.getColumnByKey(CONNECT_TYPE).setHeader("Connect Type").setWidth("80px").setFlexGrow(0).setResizable(true);
        uploadCASAGrid.getColumnByKey(CF_TYPE_CLASS_NAME).setHeader("CF Type Class Name").setWidth("120px").setFlexGrow(0).setResizable(true);
        uploadCASAGrid.getColumnByKey(TERM_NAME).setHeader("Term Name").setWidth("80px").setFlexGrow(0).setResizable(true);
        uploadCASAGrid.getColumnByKey(CLTV_CATEGORY_NAME).setHeader("CLTV Category Name").setWidth("120px").setFlexGrow(0).setResizable(true);
        uploadCASAGrid.getColumnByKey(CONTROLLING_BRANDING).setHeader("Controlling Branding").setWidth("120px").setFlexGrow(0).setResizable(true);
        uploadCASAGrid.getColumnByKey(CLTV_CHARGE_NAME).setHeader("CLTV Charge Name").setWidth("120px").setFlexGrow(0).setResizable(true);

        // Remove edit column if exists
        if (uploadCASAGrid.getColumnByKey(EDIT_COLUMN) != null) {
            uploadCASAGrid.removeColumn(uploadCASAGrid.getColumnByKey(EDIT_COLUMN));
        }

        // Automatically resize columns to fit content
        uploadCASAGrid.getColumns().forEach(col -> col.setAutoWidth(true));

        // Reorder the columns
        uploadCASAGrid.setColumnOrder(
                uploadCASAGrid.getColumnByKey(CONTRACT_FEATURE_ID),
                uploadCASAGrid.getColumnByKey(ATTRIBUTE_CLASSES_ID),
                uploadCASAGrid.getColumnByKey(ATTRIBUTE_CLASSES_NAME),
                uploadCASAGrid.getColumnByKey(CONNECT_TYPE),
                uploadCASAGrid.getColumnByKey(CF_TYPE_CLASS_NAME),
                uploadCASAGrid.getColumnByKey(TERM_NAME),
                uploadCASAGrid.getColumnByKey(CLTV_CATEGORY_NAME),
                uploadCASAGrid.getColumnByKey(CONTROLLING_BRANDING),
                uploadCASAGrid.getColumnByKey(CLTV_CHARGE_NAME)
        );

        // Set theme variants
        uploadCASAGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        uploadCASAGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        uploadCASAGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        uploadCASAGrid.setThemeName("dense");

        logView.logMessage(Constants.INFO, "Ending setupCasaTermGrid() for CasaTerm Grid");
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

            GenericDataProvider uploadCASADataprovider = new GenericDataProvider(listOfUploadCASA);
            uploadCASAGrid.setDataProvider(uploadCASADataprovider);
            singleFileUpload.clearFileList();
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

            String sheetName =   "CASA Term"; //"Exported Data";

            XSSFSheet sheet = my_xls_workbook.getSheet(sheetName);
            listOfUploadCASA = parseSheet(sheet, CasaTerm.class);

            logView.logMessage(Constants.INFO, "Ending parseExcelFile() for parse uploaded file");
        } catch (Exception e) {
            logView.logMessage(Constants.ERROR, "Error while parse uploaded file");
            e.printStackTrace();
        }
    }

    public <T> List<T>  parseSheet(XSSFSheet sheet, Class<T> targetType) {
        logView.logMessage(Constants.INFO, "### Start parseSheet() ###");
        List<T> resultList = new ArrayList<>();
        try {
            // List<T> resultList = new ArrayList<>();
            Iterator<Row> rowIterator = sheet.iterator();

            int rowNumber=0;
            Integer Error_count=0;
            //System.out.println("Sheet: " + sheet.getSheetName() + " has " + sheet.getPhysicalNumberOfRows() + " rows ") ;
            logView.logMessage(Constants.INFO, "Sheet: " + sheet.getSheetName() + " has " + sheet.getPhysicalNumberOfRows() + " rows ");

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
                    // for (int index = 0; index <= 9; index++) {
                    Cell cell = null;
//                    if (index != 0) {
//                        cell = row.getCell(index - 1);
//                    } else {
                    cell = row.getCell(index);
//                    }
                    if (cell != null && !cell.toString().isEmpty()) {
                        Field field = fields[index];
                        field.setAccessible(true);
//                        if (index == 0) {
//                            field.set(entity, rowNumber);
//                        } else {
                        if (field.getType() == int.class || field.getType() == Integer.class) {
                            field.set(entity, (int) cell.getNumericCellValue());
                        } else if (field.getType() == long.class || field.getType() == Long.class) {
                            field.set(entity, (long) cell.getNumericCellValue());
                        } else if (field.getType() == double.class || field.getType() == Double.class) {
                            field.set(entity, (double) cell.getNumericCellValue());

                        } else if (field.getType() == java.sql.Date.class) {
                            if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                                java.util.Date dateValue = cell.getDateCellValue();
                                field.set(entity, new java.sql.Date(dateValue.getTime()));
                            }
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

                                try {
                                    field.set(entity, cell.getStringCellValue());
                                }
                                catch (Exception e) {
                                    System.out.println("Field getType:" + field.getType());
                                    System.out.println(e.getMessage());
                                    int spalte = cell.getColumnIndex()+1;
                                    System.out.println("Zelle " + rowNumber + " Spalte: " + spalte + " Wert " +  cell.toString() + " konnte nicht automatisch als String verarbeitet werden, wird versucht manuell als String zu konvertieren...");
                                    Double value = cell.getNumericCellValue();
                                    field.set(entity, String.valueOf(value));

                                }

                            }
                        }
                        //    }
                    }
                }
                resultList.add(entity);
            }
            logView.logMessage(Constants.INFO, "Ending parseSheet() for parse sheet of file");
            return resultList;
        } catch (Exception e) {
            logView.logMessage(Constants.ERROR, "Error while parse sheet of file");
            e.printStackTrace();
            Notification.show(sheet.getSheetName() +" sheet having a parsing problem", 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return resultList;
        }

    }

    private static class ColumnToggleContextMenu extends ContextMenu {
        public ColumnToggleContextMenu(Component target) {
            super(target);
            setOpenOnClick(true);
        }

        void addColumnToggleItem(String label, Grid.Column<CLTVInflow> column) {
            MenuItem menuItem = this.addItem(label, e -> {
                column.setVisible(e.getSource().isChecked());
            });
            menuItem.setCheckable(true);
            menuItem.setChecked(column.isVisible());
           // menuItem.setKeepOpen(true);
        }
    }


    private Span createBadge(int value) {
        Span badge = new Span(String.valueOf(value));
        badge.getElement().getThemeList().add("badge small contrast");
        badge.getStyle().set("margin-inline-start", "var(--lumo-space-xs)");
        return badge;
    }

    public List<String> extractUniqueCltvCategoryNames(List<CLTVInflow> inflowData) {
        return inflowData.stream()
                .map(CLTVInflow::getCltvCategoryName)
                .filter(value -> value != null && !value.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> extractUniqueControllingBranding(List<CLTVInflow> inflowData) {
        return inflowData.stream()
                .map(CLTVInflow::getControllingBranding)
                .filter(value -> value != null && !value.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> extractUniqueCltvChargeNames(List<CLTVInflow> inflowData) {
        return inflowData.stream()
                .map(CLTVInflow::getCltvChargeName)
                .filter(value -> value != null && !value.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private void enableCustomValueSupport(ComboBox<String> comboBox, List<String> items) {
        comboBox.setAllowCustomValue(true);
        comboBox.addCustomValueSetListener(event -> {
            String customValue = event.getDetail();
            items.add(customValue);
            comboBox.setItems(items);
            comboBox.setValue(customValue);
        });
    }
}
