package de.dbuss.tefcontrol.data.modules.cltv_Inflow.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.gridpro.GridPro;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.dbuss.tefcontrol.data.modules.cltv_Inflow.entity.CLTVInflow;
import de.dbuss.tefcontrol.data.entity.ProjectConnection;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@PageTitle("CLTV Product-Mapping")
@Route(value = "CLTV-Inflow", layout = MainLayout.class)
@RolesAllowed({"MAPPING", "ADMIN"})
public class CLTVInflowView extends VerticalLayout {
    private final ProjectConnectionService projectConnectionService;
    private Grid<CLTVInflow> grid = new Grid<>(CLTVInflow.class);
    private GridPro<CLTVInflow> missingGrid = new GridPro<>(CLTVInflow.class);
    private String selectedDbName;

    public CLTVInflowView(ProjectConnectionService projectConnectionService) {
        this.projectConnectionService = projectConnectionService;

        addClassName("list-view");
        setSizeFull();
        configureGrid();
        configureMissingGrid();

        HorizontalLayout hl = new HorizontalLayout();
        ComboBox<String> databaseConnectionCB = new ComboBox<>();
        databaseConnectionCB.setAllowCustomValue(true);

        List<ProjectConnection> listOfProjectConnections = projectConnectionService.findAll();
        List<String> connectionNames = listOfProjectConnections.stream()
                .flatMap(connection -> {
                    String category = connection.getCategory();
                    //if (category == null) {
                    if("CLTV-Inflow".equals(category)){
                        return Stream.of(connection.getName());
                    }
                    return Stream.empty();
                })
                .collect(Collectors.toList());
        databaseConnectionCB.setItems(connectionNames);
        //databaseConnectionCB.setValue(connectionNames.get(0));
         selectedDbName = connectionNames.get(0);
        databaseConnectionCB.setValue(selectedDbName);

        databaseConnectionCB.addValueChangeListener(event -> {
            selectedDbName = event.getValue();
          //  updateList();
          //  updateMissingGrid();
        });

        hl.add(databaseConnectionCB);

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Missing Entries", getMissingMapping());
        tabSheet.add("All Entries",getAllCLTV_Inflow());

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();
        tabSheet.addThemeVariants(TabSheetVariant.MATERIAL_BORDERED);

        add(tabSheet);
        add(hl,tabSheet );

        updateGrid();
      //  updateMissingGrid();
      //  closeEditor();
    }

    private void updateGrid() {

        List<CLTVInflow> allCLTVInflowData = projectConnectionService.getAllCLTVInflow(selectedDbName);
        grid.setItems(allCLTVInflowData);

    }

    private Component getAllCLTV_Inflow() {

        VerticalLayout vl = new VerticalLayout();

        HorizontalLayout content = new HorizontalLayout(grid);
        content.setFlexGrow(2,grid);
        content.addClassName("content");
        content.setSizeFull();
        content.setHeightFull();

        vl.add(content);

        vl.setSizeFull();
        vl.setHeightFull();

        return vl;

    }

    private Component getMissingMapping() {
        VerticalLayout vl = new VerticalLayout();

        //  HorizontalLayout content = new HorizontalLayout(missingGrid);
        HorizontalLayout content = new HorizontalLayout(missingGrid);

        content.addClassName("missingContent");
        content.setSizeFull();
        content.setHeightFull();

        vl.add(content);

        vl.setSizeFull();
        vl.setHeightFull();

        return vl;
    }

    private void configureGrid() {
        grid.addClassNames("CLTV-Inflow-grid");
        grid.setSizeFull();
        grid.setHeightFull();
        grid.setColumns("contractFeatureId", "attributeClassesId", "cfTypeClassName", "attributeClassesName", "contractFeatureSubCategoryName","contractFeatureName","cfTypeName","cfDurationInMonth","connectType", "cltvCategoryName","controllingBrandingDetailed", "controllingBranding", "user", "cltvChargeName");

        grid.getColumnByKey("contractFeatureId").setHeader("ContractFeature_id").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("attributeClassesId").setHeader("AttributeClasses_ID").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("cfTypeClassName").setHeader("CF_TYPE_CLASS_NAME").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("attributeClassesName").setHeader("AttributeClasses_NAME").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("contractFeatureSubCategoryName").setHeader("ContractFeatureSubCategory_Name").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("contractFeatureName").setHeader("ContractFeature_Name").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("cfTypeName").setHeader("CF_TYPE_NAME").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("cfDurationInMonth").setHeader("CF_Duration_in_Month").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("connectType").setHeader("Connect_Type").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("cltvCategoryName").setHeader("CLTV_Category_Name").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("controllingBrandingDetailed").setHeader("Controlling_Branding_Detailed").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("controllingBranding").setHeader("Controlling_Branding").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("user").setHeader("User").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("cltvChargeName").setHeader("CLTV_Charge_Name").setFlexGrow(0).setResizable(true);


        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);

//        grid.addItemDoubleClickListener(event ->
//                editProduct(event.getItem()));
    }

    private void configureMissingGrid() {

        missingGrid.addClassNames("Missing CLTV-Inflow-grid");
        missingGrid.setSizeFull();
        missingGrid.setHeightFull();

        missingGrid.setColumns("contractFeatureId", "attributeClassesId", "cfTypeClassName", "attributeClassesName", "contractFeatureSubCategoryName","contractFeatureName","cfTypeName","cfDurationInMonth","connectType", "cltvCategoryName","controllingBrandingDetailed", "controllingBranding", "user", "cltvChargeName");

        missingGrid.getColumnByKey("contractFeatureId").setHeader("ContractFeature_id").setFlexGrow(0).setResizable(true);
        missingGrid.getColumnByKey("attributeClassesId").setHeader("AttributeClasses_ID").setFlexGrow(0).setResizable(true);
        missingGrid.getColumnByKey("cfTypeClassName").setHeader("CF_TYPE_CLASS_NAME").setFlexGrow(0).setResizable(true);
        missingGrid.getColumnByKey("attributeClassesName").setHeader("AttributeClasses_NAME").setFlexGrow(0).setResizable(true);
        missingGrid.getColumnByKey("contractFeatureSubCategoryName").setHeader("ContractFeatureSubCategory_Name").setFlexGrow(0).setResizable(true);
        missingGrid.getColumnByKey("contractFeatureName").setHeader("ContractFeature_Name").setFlexGrow(0).setResizable(true);
        missingGrid.getColumnByKey("cfTypeName").setHeader("CF_TYPE_NAME").setFlexGrow(0).setResizable(true);
        missingGrid.getColumnByKey("cfDurationInMonth").setHeader("CF_Duration_in_Month").setFlexGrow(0).setResizable(true);
        missingGrid.getColumnByKey("connectType").setHeader("Connect_Type").setFlexGrow(0).setResizable(true);
        // missingGrid.getColumnByKey("cltvCategoryName").setHeader("CLTV_Category_Name").setFlexGrow(0).setResizable(true);
        missingGrid.addComponentColumn(cltvInflow -> {
            ComboBox<String> comboBoxCategory = new ComboBox<>();
            comboBoxCategory.setPlaceholder("select or enter value...");
            comboBoxCategory.setItems("hello", "hi");
            comboBoxCategory.setAllowCustomValue(true);
            comboBoxCategory.addCustomValueSetListener(e -> {
                String customValue = e.getDetail();

            });
            comboBoxCategory.addValueChangeListener(event -> {
                String selectedValue = event.getValue();

            });

            return comboBoxCategory;
        }).setHeader("CLTV_Category_Name").setFlexGrow(0).setWidth("300px").setResizable(true);

        // missingGrid.getColumnByKey("controllingBrandingDetailed").setHeader("Controlling_Branding_Detailed").setFlexGrow(0).setResizable(true);
        missingGrid.addComponentColumn(cltvInflow -> {
            ComboBox<String> comboBoxBrandingDetailed = new ComboBox<>();
            comboBoxBrandingDetailed.setPlaceholder("select or enter value...");
            comboBoxBrandingDetailed.setItems("hello", "hi");
            comboBoxBrandingDetailed.setAllowCustomValue(true);
            comboBoxBrandingDetailed.addCustomValueSetListener(e -> {
                String customValue = e.getDetail();

            });
            comboBoxBrandingDetailed.addValueChangeListener(event -> {
                String selectedValue = event.getValue();

            });

            return comboBoxBrandingDetailed;
        }).setHeader("Controlling_Branding_Detailed").setFlexGrow(0).setWidth("300px").setResizable(true);

        // missingGrid.getColumnByKey("controllingBranding").setHeader("Controlling_Branding").setFlexGrow(0).setResizable(true);
        missingGrid.addComponentColumn(cltvInflow -> {
            ComboBox<String> comboBoxBranding = new ComboBox<>();
            comboBoxBranding.setPlaceholder("select or enter value...");
            comboBoxBranding.setItems("hello", "hi");
            comboBoxBranding.setAllowCustomValue(true);
            comboBoxBranding.addCustomValueSetListener(e -> {
                String customValue = e.getDetail();

            });
            comboBoxBranding.addValueChangeListener(event -> {
                String selectedValue = event.getValue();

            });

            return comboBoxBranding;
        }).setHeader("Controlling_Branding").setFlexGrow(0).setWidth("300px").setResizable(true);

        missingGrid.getColumnByKey("user").setHeader("User").setFlexGrow(0).setResizable(true);
        // missingGrid.getColumnByKey("cltvChargeName").setHeader("CLTV_Charge_Name").setFlexGrow(0).setResizable(true);
        missingGrid.addComponentColumn(cltvInflow -> {
            ComboBox<String> comboBoxChargeName = new ComboBox<>();
            comboBoxChargeName.setPlaceholder("select or enter value...");
            comboBoxChargeName.setAllowCustomValue(true);
            comboBoxChargeName.addCustomValueSetListener(e -> {
                String customValue = e.getDetail();

            });
            comboBoxChargeName.addValueChangeListener(event -> {
                String selectedValue = event.getValue();

            });

            return comboBoxChargeName;
        }).setHeader("CLTV_Charge_Name").setFlexGrow(0).setWidth("300px").setResizable(true);

        missingGrid.getColumns().forEach(col -> col.setAutoWidth(true));

        missingGrid.setSelectionMode(Grid.SelectionMode.NONE);
        missingGrid.setEditOnClick(true);
    }

}
