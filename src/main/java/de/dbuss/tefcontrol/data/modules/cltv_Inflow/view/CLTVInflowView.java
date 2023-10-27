package de.dbuss.tefcontrol.data.modules.cltv_Inflow.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.modules.cltv_Inflow.entity.CLTVInflow;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;


import java.util.*;
import java.util.stream.Collectors;

@PageTitle("CLTV Product-Mapping")
@Route(value = "CLTV-Inflow", layout = MainLayout.class)
@RolesAllowed({"MAPPING", "ADMIN"})
public class CLTVInflowView extends VerticalLayout {
    private final ProjectConnectionService projectConnectionService;
    private Crud<CLTVInflow> crud;
    private Grid<CLTVInflow> grid;
    private GridPro<CLTVInflow> missingGrid = new GridPro<>(CLTVInflow.class);
    private Button saveButton = new Button("Save");
    private List<CLTVInflow> modifiedCLTVInflow = new ArrayList<>();
    private String selectedDbName;
    private String tableName;
    private Button missingShowHidebtn = new Button("Show/Hide Columns");
    private Button allEntriesShowHidebtn = new Button("Show/Hide Columns");

    public CLTVInflowView(ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService) {
        this.projectConnectionService = projectConnectionService;

        addClassName("list-view");
        setSizeFull();
        configureSaveBtn();

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        String dbServer = null;

        for (ProjectParameter projectParameter : listOfProjectParameters) {
            if(projectParameter.getNamespace().equals("CLTV_Inflow")) {
                if ("DB_Server".equals(projectParameter.getName())) {
                    dbServer = projectParameter.getValue();
                } else if ("DB_Name".equals(projectParameter.getName())) {
                    selectedDbName = projectParameter.getValue();
                } else if ("Table".equals(projectParameter.getName())) {
                    tableName = projectParameter.getValue();
                }
            }
        }

        Text databaseDetail = new Text("Connected to: "+ dbServer+ " Database: " + selectedDbName+ " Table: " + tableName);

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Missing Entries", getMissingCLTV_InflowGrid());
        tabSheet.add("All Entries",getCLTV_InflowGrid());

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();
        tabSheet.addThemeVariants(TabSheetVariant.MATERIAL_BORDERED);

        HorizontalLayout hl = new HorizontalLayout();
        hl.add(saveButton,databaseDetail);
        hl.setAlignItems(Alignment.BASELINE);
   //     add(saveButton);
     //   add(databaseDetail);
        add(hl, tabSheet );

        updateGrid();
        updateMissingGrid();
      //  closeEditor();
    }

    private void updateGrid() {

        List<CLTVInflow> allCLTVInflowData = projectConnectionService.getAllCLTVInflow(selectedDbName, tableName);
        GenericDataProvider dataProvider = new GenericDataProvider(allCLTVInflowData, "ContractFeature_id");
      //  grid.setItems(allCLTVInflowData);
        grid.setDataProvider(dataProvider);
    }

    private void updateMissingGrid() {

        List<CLTVInflow> allCLTVInflowData = projectConnectionService.getAllCLTVInflow(selectedDbName, tableName);
        List<CLTVInflow> missingList = allCLTVInflowData.stream()
                .filter(item -> "missing".equals(item.getCltvCategoryName()) ||
                        "missing".equals(item.getControllingBrandingDetailed()) ||
                        "missing".equals(item.getControllingBranding()) ||
                        "missing".equals(item.getCltvChargeName()))
                .collect(Collectors.toList());
        missingGrid.setItems(missingList);
    }

    private void configureSaveBtn() {

        saveButton.addClickListener(e -> {
            if (modifiedCLTVInflow != null && !modifiedCLTVInflow.isEmpty()) {
                String result = projectConnectionService.updateListOfCLTVInflow(modifiedCLTVInflow, selectedDbName, tableName);
                if (result.contains("ok")){
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
        });
    }

    private Component getCLTV_InflowGrid() {

        VerticalLayout content = new VerticalLayout();
        crud = new Crud<>(CLTVInflow.class, createEditor());
        configureGrid();
        //content.setFlexGrow(2,crud);
        crud.setToolbarVisible(false);
        crud.setSizeFull();
        content.add(allEntriesShowHidebtn);
        content.setAlignItems(Alignment.END);
        content.add(crud);
        content.setHeightFull();
        return content;
    }

    private Component getMissingCLTV_InflowGrid() {
        VerticalLayout vl = new VerticalLayout();
        configureMissingGrid();
        vl.add(missingShowHidebtn);
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

        Grid.Column contractFeatureIdColumn = grid.getColumnByKey("contractFeatureId").setHeader("ContractFeature_id").setFlexGrow(0).setResizable(true);
        Grid.Column attributeClassesIdColumn = grid.getColumnByKey("attributeClassesId").setHeader("AttributeClasses_ID").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("cfTypeClassName").setHeader("CF_TYPE_CLASS_NAME").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("attributeClassesName").setHeader("AttributeClasses_NAME").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("contractFeatureSubCategoryName").setHeader("ContractFeatureSubCategory_Name").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("contractFeatureName").setHeader("ContractFeature_Name").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("cfTypeName").setHeader("CF_TYPE_NAME").setFlexGrow(0).setResizable(true);
        Grid.Column cfDurationInMonthColumn = grid.getColumnByKey("cfDurationInMonth").setHeader("CF_Duration_in_Month").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("connectType").setHeader("Connect_Type").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("cltvCategoryName").setHeader("CLTV_Category_Name").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("controllingBrandingDetailed").setHeader("Controlling_Branding_Detailed").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("controllingBranding").setHeader("Controlling_Branding").setFlexGrow(0).setResizable(true);
        Grid.Column userColumn = grid.getColumnByKey("user").setHeader("User").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("cltvChargeName").setHeader("CLTV_Charge_Name").setFlexGrow(0).setResizable(true);

        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        grid.removeColumn(grid.getColumnByKey(EDIT_COLUMN));
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);

        // set column order
        List<Grid.Column<CLTVInflow>> columnOrder = Arrays.asList(
                contractFeatureIdColumn,
                attributeClassesIdColumn,
                grid.getColumnByKey("cfTypeClassName"),
                grid.getColumnByKey("attributeClassesName"),
                grid.getColumnByKey("contractFeatureSubCategoryName"),
                grid.getColumnByKey("contractFeatureName"),
                grid.getColumnByKey("cfTypeName"),
                cfDurationInMonthColumn,
                grid.getColumnByKey("connectType"),
                grid.getColumnByKey("cltvCategoryName"),
                grid.getColumnByKey("controllingBrandingDetailed"),
                grid.getColumnByKey("controllingBranding"),
                userColumn,
                grid.getColumnByKey("cltvChargeName")
        );

        grid.setColumnOrder(columnOrder);

        // default hide
        contractFeatureIdColumn.setVisible(false);
        attributeClassesIdColumn.setVisible(false);

        allEntriesShowHidebtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        ColumnToggleContextMenu columnToggleContextMenu = new ColumnToggleContextMenu(allEntriesShowHidebtn);
        columnToggleContextMenu.addColumnToggleItem("ContractFeature_id", contractFeatureIdColumn);
        columnToggleContextMenu.addColumnToggleItem("AttributeClasses_ID", attributeClassesIdColumn);
        columnToggleContextMenu.addColumnToggleItem("CF_Duration_in_Month", cfDurationInMonthColumn);
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
        List<CLTVInflow> allCLTVInflowData = projectConnectionService.getAllCLTVInflow(selectedDbName, tableName);

     //   String missing = "missing";
        List<String> listOfControllingBrandingDetailed = allCLTVInflowData.stream()
                .map(CLTVInflow::getControllingBrandingDetailed)
                .filter(value -> value != null && !value.isEmpty() && !isMissing(value))
                .distinct()
                .collect(Collectors.toList());

        List<String> listOfControllingBranding = allCLTVInflowData.stream()
                .map(CLTVInflow::getControllingBranding)
                .filter(value -> value != null && !value.isEmpty() && !isMissing(value))
                .distinct()
                .collect(Collectors.toList());

        List<String> listOfCLTVCategoryName = allCLTVInflowData.stream()
                .map(CLTVInflow::getCltvCategoryName)
                .filter(value ->  value != null && !value.isEmpty() && !isMissing(value))
                .distinct()
                .collect(Collectors.toList());

        List<String> listOfCLTVChargeName = allCLTVInflowData.stream()
                .map(CLTVInflow::getCltvChargeName)
                .filter(value ->  value != null && !value.isEmpty() && !isMissing(value))
                .distinct()
                .collect(Collectors.toList());

        missingGrid.addClassNames("Missing CLTV-Inflow-grid");
        missingGrid.setSizeFull();
        missingGrid.setHeightFull();

        missingGrid.setColumns("contractFeatureId", "attributeClassesId", "cfTypeClassName", "attributeClassesName", "contractFeatureSubCategoryName","contractFeatureName","cfTypeName","cfDurationInMonth","connectType", "user");

        Grid.Column contractFeatureIdColumn = missingGrid.getColumnByKey("contractFeatureId").setHeader("ContractFeature_id").setFlexGrow(0).setResizable(true);
        Grid.Column attributeClassesIdColumn = missingGrid.getColumnByKey("attributeClassesId").setHeader("AttributeClasses_ID").setFlexGrow(0).setResizable(true);
        missingGrid.getColumnByKey("cfTypeClassName").setHeader("CF_TYPE_CLASS_NAME").setFlexGrow(0).setResizable(true);
        missingGrid.getColumnByKey("attributeClassesName").setHeader("AttributeClasses_NAME").setFlexGrow(0).setResizable(true);
        missingGrid.getColumnByKey("contractFeatureSubCategoryName").setHeader("ContractFeatureSubCategory_Name").setFlexGrow(0).setResizable(true);
        missingGrid.getColumnByKey("contractFeatureName").setHeader("ContractFeature_Name").setFlexGrow(0).setResizable(true);
        missingGrid.getColumnByKey("cfTypeName").setHeader("CF_TYPE_NAME").setFlexGrow(0).setResizable(true);
        Grid.Column cfDurationInMonthColumn = missingGrid.getColumnByKey("cfDurationInMonth").setHeader("CF_Duration_in_Month").setFlexGrow(0).setResizable(true);
        missingGrid.getColumnByKey("connectType").setHeader("Connect_Type").setFlexGrow(0).setResizable(true);
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
        missingGrid.setEditOnClick(true);

        // default hide
        contractFeatureIdColumn.setVisible(false);
        attributeClassesIdColumn.setVisible(false);

        missingShowHidebtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        ColumnToggleContextMenu columnToggleContextMenu = new ColumnToggleContextMenu(missingShowHidebtn);
        columnToggleContextMenu.addColumnToggleItem("ContractFeature_id", contractFeatureIdColumn);
        columnToggleContextMenu.addColumnToggleItem("AttributeClasses_ID", attributeClassesIdColumn);
        columnToggleContextMenu.addColumnToggleItem("CF_Duration_in_Month", cfDurationInMonthColumn);
        columnToggleContextMenu.addColumnToggleItem("User", userColumn);

    }
    private boolean isMissing(String value) {
        // return value == null || value.trim().isEmpty() || "missing".equals(value);
        return "missing".equals(value);
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
