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
import de.dbuss.tefcontrol.data.modules.cltv_Inflow.entity.CLTVInflowEntity;
import de.dbuss.tefcontrol.data.modules.pfgproductmapping.entity.ProductHierarchie;
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
public class CLTVInflow extends VerticalLayout {
    private final ProjectConnectionService projectConnectionService;
    private Grid<CLTVInflowEntity> grid = new Grid<>(CLTVInflowEntity.class);
    private GridPro<CLTVInflowEntity> missingGrid = new GridPro<>(CLTVInflowEntity.class);
    private String selectedDbName;

    public CLTVInflow( ProjectConnectionService projectConnectionService) {
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

      //  updateList();
      //  updateMissingGrid();
      //  closeEditor();
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
        grid.setColumns("cltvCategoryName","controllingBrandingDetailed", "controllingBranding", "cltvChargeName");

        grid.getColumnByKey("cltvCategoryName").setHeader("CLTV_Category_Name").setWidth("200px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("controllingBrandingDetailed").setHeader("Controlling_Branding_Detailed").setWidth("200px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("controllingBranding").setHeader("Controlling_Branding").setWidth("200px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("cltvChargeName").setHeader("Controlling_Branding").setWidth("200px").setFlexGrow(0).setResizable(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);

//        grid.addItemDoubleClickListener(event ->
//                editProduct(event.getItem()));
    }

    private void configureMissingGrid() {

        missingGrid.addClassNames("Missing CLTV-Inflow-grid");
        missingGrid.setSizeFull();
        missingGrid.setHeightFull();
        missingGrid.setColumns("contractFeatureId");

        missingGrid.getColumnByKey("contractFeatureId").setHeader("ContractFeatureId").setWidth("400px").setFlexGrow(0).setResizable(true);
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

        missingGrid.setSelectionMode(Grid.SelectionMode.NONE);
        missingGrid.setEditOnClick(true);
    }

}
