package de.dbuss.tefcontrol.data.modules.cltv_Inflow.view;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.gridpro.GridPro;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.modules.cltv_Inflow.entity.CLTVInflow;
import de.dbuss.tefcontrol.data.modules.cltv_Inflow.entity.CasaTerm;
import de.dbuss.tefcontrol.data.service.BackendService;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;


import java.util.*;
import java.util.stream.Collectors;

@PageTitle("CLTV Inflow-Mapping")
@Route(value = "CLTV_Inflow/:project_Id", layout = MainLayout.class)
@RolesAllowed({"MAPPING", "ADMIN"})
public class CLTVInflowView extends VerticalLayout implements BeforeEnterObserver {
    private final ProjectConnectionService projectConnectionService;
    private Crud<CLTVInflow> crud;
    private Grid<CLTVInflow> grid;

    List<CLTVInflow> allCLTVInflowData;

    private Grid<CLTVInflow> missingGrid = new Grid(CLTVInflow.class);
    private Grid<CasaTerm> casaGrid = new Grid(CasaTerm.class);

    //private GridPro<CLTVInflow> missingGrid = new GridPro<>(CLTVInflow.class);
    Button saveButton = new Button(Constants.SAVE);
    private List<CLTVInflow> modifiedCLTVInflow = new ArrayList<>();
    private List<CasaTerm> modifiedCasa = new ArrayList<>();
    private String tableName;
    private String casaTableName;
    private String casaDbUrl;

    private String casaDbUser;

    private String casaDbPassword;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    String missing_keyword= "nicht definiert";
    private String dbUrlOracle;
    private String dbUserOracle;
    private String dbPasswordOracle;

    private Button missingShowHidebtn = new Button("Show/Hide Columns");
    private Button casaShowHidebtn = new Button("Show/Hide Columns");
    private Button allEntriesShowHidebtn = new Button("Show/Hide Columns");
    private int projectId;
    List<String> listOfCLTVCategoryName;
    //List<String> listOfControllingBrandingDetailed;
    List<String> listOfControllingBranding;
    List<String> listOfCLTVChargeName;

    private Boolean isVisible = false;
    Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);

    public CLTVInflowView(ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService, BackendService backendService) {
        this.projectConnectionService = projectConnectionService;

        saveButton.setEnabled(false);
        missingShowHidebtn.setVisible(false);
        allEntriesShowHidebtn.setVisible(false);

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
                }else if (Constants.TABLE.equals(projectParameter.getName())) {
                    tableName = projectParameter.getValue();
                }else if (Constants.CASA_TABLE.equals(projectParameter.getName())) {
                    casaTableName = projectParameter.getValue();
                }else if (Constants.CASA_DB_URL.equals(projectParameter.getName())) {
                    casaDbUrl = projectParameter.getValue();
                }else if (Constants.CASA_DB_USER.equals(projectParameter.getName())) {
                    casaDbUser = projectParameter.getValue();
                }else if (Constants.CASA_DB_PASSWORD.equals(projectParameter.getName())) {
                    casaDbPassword = projectParameter.getValue();
        }
          //  }
        }

        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";


     //   Text databaseDetail = new Text("Connected to: "+ dbServer+ ", Database: " + dbName+ ", Table: " + tableName);
        setProjectParameterGrid(filteredProjectParameters);
        //Componente QS-Grid:
        //qsGrid = new QS_Grid(projectConnectionService, backendService);

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("CASA Entries", getCASA_Grid());
        tabSheet.add("Missing Entries", getMissingCLTV_InflowGrid());
        tabSheet.add("All Entries",getCLTV_InflowGrid());

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();
        tabSheet.addThemeVariants(TabSheetVariant.MATERIAL_BORDERED);

        tabSheet.addSelectedChangeListener(event -> {
         //   System.out.println("Tab: " + event.getSelectedTab().toString());
            switch(event.getSelectedTab().toString()){
                case "Tab{Missing Entries}":
                    missingShowHidebtn.setVisible(true);
                    casaShowHidebtn.setVisible(false);
                    allEntriesShowHidebtn.setVisible(false);
                    break;

                case "Tab{All Entries}":
                    missingShowHidebtn.setVisible(false);
                    casaShowHidebtn.setVisible(false);
                    allEntriesShowHidebtn.setVisible(true);
                    break;

                case "Tab{CASA Entries}":
                    missingShowHidebtn.setVisible(false);
                    casaShowHidebtn.setVisible(true);
                    allEntriesShowHidebtn.setVisible(false);
                    break;

            }

        });



        HorizontalLayout hl = new HorizontalLayout();
        // hl.add(saveButton,databaseDetail);
        hl.add(saveButton,missingShowHidebtn,casaShowHidebtn,allEntriesShowHidebtn);

        hl.setAlignItems(Alignment.BASELINE);
        add(hl, parameterGrid, tabSheet );

        saveButton.addClickListener(e ->{

            if (modifiedCLTVInflow != null && !modifiedCLTVInflow.isEmpty()) {
                String resultString = projectConnectionService.updateListOfCLTVInflow(modifiedCLTVInflow, tableName, dbUrl, dbUser, dbPassword);
                if (resultString.equals(Constants.OK)) {
                    Notification.show(modifiedCLTVInflow.size() + " Uploaded successfully", 2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    modifiedCLTVInflow.clear();
                    updateGrid();
                    updateMissingGrid();
                } else {
                    Notification.show("Error during upload: " + resultString, 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
            saveButton.setEnabled(false);
        });

        updateGrid();
        updateMissingGrid();

        allCLTVInflowData = projectConnectionService.getAllCLTVInflow(tableName, dbUrl, dbUser, dbPassword);

        updateCasaGrid();

        parameterGrid.setVisible(false);

        if(MainLayout.isAdmin) {
            UI.getCurrent().addShortcutListener(
                    () -> {
                        isVisible = !isVisible;
                        parameterGrid.setVisible(isVisible);
                    },
                    Key.KEY_I, KeyModifier.ALT);
        }
    }

    private Span createBadge(int value) {
        Span badge = new Span(String.valueOf(value));
        badge.getElement().getThemeList().add("badge small contrast");
        badge.getStyle().set("margin-inline-start", "var(--lumo-space-xs)");
        return badge;
    }

    private Component getCASA_Grid() {

        VerticalLayout vl = new VerticalLayout();
        configureCASAGrid();
        vl.setAlignItems(Alignment.END);
        vl.add(casaGrid);
        vl.setSizeFull();
        vl.setHeightFull();
        return vl;
    }



    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        projectId = Integer.parseInt(parameters.get("project_Id").orElse(null));
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

        }
    }
    private void updateGrid() {

        allCLTVInflowData = projectConnectionService.getAllCLTVInflow(tableName, dbUrl, dbUser, dbPassword);
        GenericDataProvider dataProvider = new GenericDataProvider(allCLTVInflowData, "ContractFeature_id");
      //  grid.setItems(allCLTVInflowData);
        grid.setDataProvider(dataProvider);
    }

    private void updateCasaGrid() {

        List<CasaTerm> allCasaData = projectConnectionService.getAllCASATerms(casaTableName, casaDbUrl, casaDbUser, casaDbPassword);

        List<CasaTerm> existingEntries=new ArrayList<>();

        if(allCLTVInflowData!=null) {

            for (CasaTerm employee : allCasaData) {
                for (CLTVInflow secondEmployee : allCLTVInflowData) {
                    if (employee.getContractFeatureId().equals(secondEmployee.getContractFeatureId())) {
                        System.out.println("Bereits vorhanden: " + employee.getContractFeatureId() + ", CF-ID: " + employee.getContractFeatureId());
                        existingEntries.add(employee);
                        break;
                    }
                }
            }
            if (allCasaData.size() == 0) {
                Notification.show( projectConnectionService.getErrorMessage(),3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
        else {
            System.out.println("allCLTVInflowData is null!!");
        }

        allCasaData.removeAll(existingEntries);






        GenericDataProvider dataProvider = new GenericDataProvider(allCasaData, "ContractFeature_id");
        casaGrid.setDataProvider(dataProvider);
    }

    private void updateMissingGrid() {



        List<CLTVInflow> allCLTVInflowData = projectConnectionService.getAllCLTVInflow(tableName, dbUrl, dbUser, dbPassword);
        List<CLTVInflow> missingList = allCLTVInflowData.stream()
                .filter(item -> missing_keyword.equals(item.getCltvCategoryName()) ||
             //           missing_keyword.equals(item.getControllingBrandingDetailed()) ||
                        missing_keyword.equals(item.getControllingBranding()) ||
                        missing_keyword.equals(item.getCltvChargeName()))
                .collect(Collectors.toList());
        missingGrid.setItems(missingList);
    }

    private Component getCLTV_InflowGrid() {

        VerticalLayout content = new VerticalLayout();
        crud = new Crud<>(CLTVInflow.class, createEditor());
        configureGrid();
        //content.setFlexGrow(2,crud);
        crud.setToolbarVisible(false);
        crud.setSizeFull();
        content.setAlignItems(Alignment.END);
        content.add(crud);
        content.setHeightFull();
        return content;
    }

    private Component getMissingCLTV_InflowGrid() {
        VerticalLayout vl = new VerticalLayout();
        configureMissingGrid();
        vl.setAlignItems(Alignment.END);
        vl.add(missingGrid);
        vl.setSizeFull();
        vl.setHeightFull();
        return vl;
    }

    private void configureGrid() {

        String EDIT_COLUMN = "vaadin-crud-edit-column";
        grid = crud.getGrid();
        grid.setSizeFull();
        grid.setHeightFull();
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
        grid.getColumnByKey("cltvCategoryName").setHeader("CLTV_Category_Name").setFlexGrow(0).setResizable(true);
      //  grid.getColumnByKey("controllingBrandingDetailed").setHeader("Controlling_Branding_Detailed").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("controllingBranding").setHeader("Controlling_Branding").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("cltvChargeName").setHeader("CLTV_Charge_Name").setFlexGrow(0).setResizable(true);
        Grid.Column userColumn = grid.getColumnByKey("user").setHeader("User").setFlexGrow(0).setResizable(true);

        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        grid.removeColumn(grid.getColumnByKey(EDIT_COLUMN));
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);


        // set column order
        List<Grid.Column<CLTVInflow>> columnOrder = Arrays.asList(
                contractFeatureIdColumn,contractFeatureNameColumn,cfTypeNameColumn,contractFeatureSubCategoryNameColumn,cfTypeClassNameColumn,attributeClassesIdColumn,attributeClassesNameColumn,cfDurationInMonthColumn,connectTypeColumn,
                grid.getColumnByKey("cltvCategoryName"),
        //        grid.getColumnByKey("controllingBrandingDetailed"),
                grid.getColumnByKey("controllingBranding"),
                grid.getColumnByKey("cltvChargeName"),
                userColumn
        );

        grid.setColumnOrder(columnOrder);

        // default hide
        contractFeatureIdColumn.setVisible(false);
        attributeClassesIdColumn.setVisible(false);
        attributeClassesNameColumn.setVisible(false);
        cfDurationInMonthColumn.setVisible(false);
        userColumn.setVisible(false);
        connectTypeColumn.setVisible(false);

        allEntriesShowHidebtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        ColumnToggleContextMenu columnToggleContextMenu = new ColumnToggleContextMenu(allEntriesShowHidebtn);
        columnToggleContextMenu.addColumnToggleItem("ContractFeature_id", contractFeatureIdColumn);
        columnToggleContextMenu.addColumnToggleItem("AttributeClasses_ID", attributeClassesIdColumn);
        columnToggleContextMenu.addColumnToggleItem("AttributeClasses_Name", attributeClassesNameColumn);


        columnToggleContextMenu.addColumnToggleItem("CF_Duration_in_Month", cfDurationInMonthColumn);
        columnToggleContextMenu.addColumnToggleItem("Connect Type", connectTypeColumn);
        columnToggleContextMenu.addColumnToggleItem("User", userColumn);

    }

    private CrudEditor<CLTVInflow> createEditor() {

    //    TextField cF_TYPE_CLASS_NAME = new TextField("CF_TYPE_CLASS_NAME");
        FormLayout editForm = new FormLayout();
        Binder<CLTVInflow> binder = new Binder<>(CLTVInflow.class);
     //   binder.forField(cF_TYPE_CLASS_NAME).asRequired().bind(CLTVInflow::getCfTypeClassName, CLTVInflow::setCfTypeClassName);
        return new BinderCrudEditor<>(binder, editForm);
    }

    private void configureMissingGrid() {
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

        missingGrid.setColumns("contractFeatureId", "contractFeatureName","cfTypeName", "contractFeatureSubCategoryName", "cfTypeClassName", "attributeClassesId", "attributeClassesName","cfDurationInMonth","connectType", "user");


        Grid.Column contractFeatureIdColumn = missingGrid.getColumnByKey("contractFeatureId").setHeader("CF_ID").setFlexGrow(0).setResizable(true);
        Grid.Column contractFeatureNameColumn = missingGrid.getColumnByKey("contractFeatureName").setHeader("CF_Name").setFlexGrow(0).setResizable(true);
        Grid.Column attributeClassesIdColumn = missingGrid.getColumnByKey("attributeClassesId").setHeader("AttributeClasses_ID").setFlexGrow(0).setResizable(true);
        Grid.Column cfTypeClassNameColumn = missingGrid.getColumnByKey("cfTypeClassName").setHeader("CF_TYPE_CLASS_NAME").setFlexGrow(0).setResizable(true);
        Grid.Column attributeClassesNameColumn = missingGrid.getColumnByKey("attributeClassesName").setHeader("AttributeClasses_NAME").setFlexGrow(0).setResizable(true);
        Grid.Column contractFeatureSubCategoryNameColumn = missingGrid.getColumnByKey("contractFeatureSubCategoryName").setHeader("CF_SubCategory_Name").setFlexGrow(0).setResizable(true);
        Grid.Column cfTypeNameColumn = missingGrid.getColumnByKey("cfTypeName").setHeader("CF_TYPE_NAME").setFlexGrow(0).setResizable(true);
        Grid.Column cfDurationInMonthColumn = missingGrid.getColumnByKey("cfDurationInMonth").setHeader("CF_Duration_in_Month").setFlexGrow(0).setResizable(true);
        Grid.Column connectTypeColumn = missingGrid.getColumnByKey("connectType").setHeader("Connect_Type").setFlexGrow(0).setResizable(true);

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
                    saveModifiedCLTVInflow(cltvInflow, customValue);
                });
                comboBoxCategory.addValueChangeListener(event -> {
                    String selectedValue = event.getValue();
                    cltvInflow.setCltvCategoryName(selectedValue);
                    saveModifiedCLTVInflow(cltvInflow, selectedValue);
                });

                return comboBoxCategory;
            } else {
                return new Text(getValidValue(cltvInflow.getCltvCategoryName()));
            }
        }).setHeader("CLTV_Category_Name").setFlexGrow(0).setWidth("300px").setResizable(true);

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
      //  contractFeatureIdColumn.setVisible(false);
        attributeClassesIdColumn.setVisible(false);
      //  cfTypeClassNameColumn.setVisible(false);
      //  attributeClassesNameColumn.setVisible(false);
        cfDurationInMonthColumn.setVisible(false);
//        contractFeatureSubCategoryNameColumn.setVisible(false);
//        contractFeatureNameColumn.setVisible(false);
        cfTypeNameColumn.setVisible(false);
        connectTypeColumn.setVisible(false);
        userColumn.setVisible(false);

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
        columnToggleContextMenu.addColumnToggleItem("User", userColumn);

    }

    private void configureCASAGrid() {

        List<CasaTerm> allCasaData = projectConnectionService.getAllCASATerms(casaTableName, casaDbUrl, casaDbUser, casaDbPassword);

        if(allCLTVInflowData!=null) {

            for (CasaTerm employee : allCasaData) {
                for (CLTVInflow secondEmployee : allCLTVInflowData) {
                    if (employee.getContractFeatureId() == secondEmployee.getContractFeatureId()) {
                        System.out.println("Bereits vorhanden: " + employee.getContractFeatureId() + ", CF-ID: " + employee.getContractFeatureId());
                        break;
                    }
                }
            }
        }
        else {
            System.out.println("allCLTVInflowData is null!!");
        }



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

        //casaGrid.setColumns("contractFeatureId", "attributeClassesId", "cfTypeClassName", "attributeClassesName", "contractFeatureSubCategoryName","contractFeatureName","cfTypeName","cfDurationInMonth","connectType", "user");
        casaGrid.setColumns("contractFeatureId", "attributeClassesId", "attributeClassesName", "connectType", "cfTypeClassName", "termName");



        Grid.Column contractFeatureIdColumn = casaGrid.getColumnByKey("contractFeatureId").setHeader("ContractFeature_id").setFlexGrow(0).setResizable(true);
        Grid.Column attributeClassesIdColumn = casaGrid.getColumnByKey("attributeClassesId").setHeader("AttributeClasses_ID").setFlexGrow(0).setResizable(true);
        casaGrid.getColumnByKey("attributeClassesName").setHeader("AttributeClasses_NAME").setFlexGrow(0).setResizable(true);
        casaGrid.getColumnByKey("connectType").setHeader("Connect_Type").setFlexGrow(0).setResizable(true);
        casaGrid.getColumnByKey("cfTypeClassName").setHeader("CF_TYPE_CLASS_NAME").setFlexGrow(0).setResizable(true);
        casaGrid.getColumnByKey("termName").setHeader("Termname").setFlexGrow(0).setResizable(true);


        casaGrid.addComponentColumn(CasaTerm -> {

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
                    CasaTerm.setCltvCategoryName(customValue);
                    saveModifiedCasa(CasaTerm, customValue);
                });
                comboBoxCategory.addValueChangeListener(event -> {
                    String selectedValue = event.getValue();
                    CasaTerm.setCltvCategoryName(selectedValue);
                    saveModifiedCasa(CasaTerm, selectedValue);
                });

                return comboBoxCategory;

        }).setHeader("CLTV_Category_Name").setFlexGrow(0).setWidth("300px").setResizable(true);

        casaGrid.addComponentColumn(CasaTerm -> {

            ComboBox<String> comboBoxControllingBranding = new ComboBox<>();
            comboBoxControllingBranding.setPlaceholder("select or enter value...");
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

        }).setHeader("ControllingBranding").setFlexGrow(0).setWidth("300px").setResizable(true);


        casaGrid.addComponentColumn(CasaTerm -> {

            ComboBox<String> comboBoxCLTVChargeName = new ComboBox<>();
            comboBoxCLTVChargeName.setPlaceholder("select or enter value...");
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
                CasaTerm.setControllingBranding(selectedValue);
                saveModifiedCasa(CasaTerm, selectedValue);
            });

            return comboBoxCLTVChargeName;

        }).setHeader("CLTV_Charge_Name").setFlexGrow(0).setWidth("300px").setResizable(true);






        casaGrid.addColumn(
                new ComponentRenderer<>(Button::new, (button, casaTerm) -> {
                    button.addThemeVariants(ButtonVariant.LUMO_ICON,
                            ButtonVariant.LUMO_SUCCESS,
                            ButtonVariant.LUMO_TERTIARY);
                    button.addClickListener(e -> System.out.println("Save: " + casaTerm.getContractFeatureId().toString() + " with Category: " + casaTerm.getCltvCategoryName()));
                    button.setIcon(new Icon(VaadinIcon.CLOUD_DOWNLOAD_O));
                })).setHeader("get entry");


        casaGrid.getColumns().forEach(col -> col.setAutoWidth(true));
        casaGrid.setSelectionMode(Grid.SelectionMode.NONE);

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



    }


    private void saveModifiedCasa(CasaTerm cltvInflow, String selectedValue) {
        if(!isMissing(selectedValue)) {
            if(modifiedCasa.contains(cltvInflow)){
                modifiedCasa.remove(cltvInflow);
                modifiedCasa.add(cltvInflow);
            } else {
                modifiedCasa.add(cltvInflow);
            }
            saveButton.setEnabled(true);
        } else {
            Notification.show("Please do not enter Missing value", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
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
        if(!isMissing(selectedValue)) {
            if(modifiedCLTVInflow.contains(cltvInflow)){
                modifiedCLTVInflow.remove(cltvInflow);
                modifiedCLTVInflow.add(cltvInflow);
            } else {
                modifiedCLTVInflow.add(cltvInflow);
            }
            saveButton.setEnabled(true);
        } else {
            Notification.show("Please do not enter Missing value", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
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
}
