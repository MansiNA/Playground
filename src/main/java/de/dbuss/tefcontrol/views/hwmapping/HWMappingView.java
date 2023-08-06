package de.dbuss.tefcontrol.views.hwmapping;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.gridpro.GridPro;
import com.vaadin.flow.component.gridpro.GridProVariant;
import com.vaadin.flow.component.gridpro.ItemUpdater;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.dbuss.tefcontrol.data.entity.CLTV_HW_Measures;
import de.dbuss.tefcontrol.data.service.CLTV_HW_MeasureService;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.function.Consumer;

@PageTitle("HW Mapping")
@Route(value = "HW-Mapping", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class HWMappingView extends VerticalLayout {


    private final CLTV_HW_MeasureService cltvHwMeasureService;
    GridPro<CLTV_HW_Measures> grid = new GridPro<>();
    TextField filterInt = new TextField();
    public HWMappingView(CLTV_HW_MeasureService cltvHwMeasureService) {

        this.cltvHwMeasureService = cltvHwMeasureService;

        addClassName("CLTVHW-list-view");
        setSizeFull();
        configureGrid();
        add(
                getToolbar(),
                getContent()
        );
//        updateList();

    }

    private void configureGrid() {
        grid.addClassNames("MSM-grid");
        grid.setSizeFull();

        //grid.setColumns("monat_ID", "device", "measure_Name","channel");

        Grid.Column<CLTV_HW_Measures> MonatColumn = grid.addColumn(CLTV_HW_Measures::getMonat_ID);
        Grid.Column<CLTV_HW_Measures> DeviceColumn = grid.addColumn(CLTV_HW_Measures::getDevice);
        Grid.Column<CLTV_HW_Measures> MeasureColumn = grid.addColumn(CLTV_HW_Measures::getMeasure_Name);
        Grid.Column<CLTV_HW_Measures> ChannelColumn = grid.addColumn(CLTV_HW_Measures::getChannel);


        Grid.Column<CLTV_HW_Measures> ValueColumn =  grid.addEditColumn(CLTV_HW_Measures::getValue)
                .text(new ItemUpdater<CLTV_HW_Measures, String>(){
                    @Override
                    public void accept(CLTV_HW_Measures currow, String s) {
                        //Update der Zeile
                        currow.setValue(s);
                        cltvHwMeasureService.update(currow,s);
                        //System.out.println("Update auf" + s);
                        updateList();
                    }});
        //.setHeader("Value");

        grid.addThemeVariants(GridProVariant.LUMO_HIGHLIGHT_EDITABLE_CELLS);

        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        List<CLTV_HW_Measures> people = cltvHwMeasureService.findAllProducts("");
        GridListDataView<CLTV_HW_Measures> dataView = grid.setItems(people);
        PersonFilter personFilter = new PersonFilter(dataView);

        grid.getHeaderRows().clear();
        HeaderRow headerRow = grid.appendHeaderRow();

        headerRow.getCell(MonatColumn).setComponent(createFilterHeader("Monat", personFilter::setMonat));
        headerRow.getCell(DeviceColumn).setComponent(createFilterHeader("Device", personFilter::setDevice));
        headerRow.getCell(MeasureColumn).setComponent(createFilterHeader("Measure", personFilter::setMeasure));
        headerRow.getCell(ChannelColumn).setComponent(createFilterHeader("Channel", personFilter::setChannel));
        headerRow.getCell(ValueColumn).setComponent(createFilterHeader("Value", personFilter::setValue));


//        grid.asSingleSelect().addValueChangeListener(event ->
//                editProduct(event.getValue()));

    }

    private static Component createFilterHeader(String labelText,
                                                Consumer<String> filterChangeConsumer) {
        Label label = new Label(labelText);
        label.getStyle().set("padding-top", "var(--lumo-space-m)")
                .set("font-size", "var(--lumo-font-size-xs)");
        TextField textField = new TextField();
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.setClearButtonVisible(true);
        textField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        textField.setWidthFull();
        textField.getStyle().set("max-width", "100%");
        textField.addValueChangeListener(
                e -> filterChangeConsumer.accept(e.getValue()));
        VerticalLayout layout = new VerticalLayout(label, textField);
        layout.getThemeList().clear();
        layout.getThemeList().add("spacing-xs");

        return layout;
    }
    private HorizontalLayout getToolbar() {
//        filterText.setPlaceholder("Filter by Device/Measure/Channel");
//        filterText.setClearButtonVisible(true);
//        filterText.setValueChangeMode(ValueChangeMode.LAZY);
//        filterText.addValueChangeListener(e -> updateList());

  /*      filterInt.setPlaceholder("Filter by Monat");
        filterInt.setClearButtonVisible(true);
        filterInt.setValueChangeMode(ValueChangeMode.LAZY);
        filterInt.addValueChangeListener(e -> updateMonat());*/

//        comboBox.setAutoOpen(true);
//        comboBox.setItems(cltvHwMeasureService.getMonate());
//        comboBox.setLabel("");
//        //comboBox.setHelperText("Helper text");
//        comboBox.setPlaceholder("Filter by Monat");
//        comboBox.setTooltipText("Filter auf vorhandenen Monat");
//        comboBox.addValueChangeListener(e -> updateMonat());
//     //   comboBox.setClearButtonVisible(true);
//        comboBox.setPrefixComponent(VaadinIcon.SEARCH.create());

        Button addProductButton = new Button("Add Monat");
        //     addProductButton.addClickListener(click -> addProduct());


        Button startJobButton = new Button("Start");
        //   startJobButton.addClickListener(click -> startJob());

        var toolbar = new HorizontalLayout(addProductButton, startJobButton);
        toolbar.addClassName("toolbar");

        return toolbar;
    }
    private Component getContent() {
        //  HorizontalLayout content = new HorizontalLayout(grid, form);
        HorizontalLayout content = new HorizontalLayout(grid);
        content.setFlexGrow(2,grid);
        //    content.setFlexGrow(1,form);
        content.addClassName("content");
        content.setSizeFull();

        return content;

    }

    private static class PersonFilter {
        private final GridListDataView<CLTV_HW_Measures> dataView;

        private String monat_ID;
        private String device;
        private String measure_Name;

        private String channel;

        private String value;

        public PersonFilter(GridListDataView<CLTV_HW_Measures> dataView) {
            this.dataView = dataView;
            this.dataView.addFilter(this::test);
        }

        public void setMonat(String fullName) {
            this.monat_ID = fullName;
            this.dataView.refreshAll();
        }

        public void setDevice(String email) {
            this.device = email;
            this.dataView.refreshAll();
        }

        public void setMeasure(String profession) {
            this.measure_Name = profession;
            this.dataView.refreshAll();
        }

        public void setChannel(String profession) {
            this.channel = profession;
            this.dataView.refreshAll();
        }
        public void setValue(String profession) {
            this.value = profession;
            this.dataView.refreshAll();
        }



        public boolean test(CLTV_HW_Measures person) {
            boolean matchesFullName = matches(person.getMonat_ID().toString(), monat_ID);
            boolean matchesDevice = matches(person.getDevice(), device);
            boolean matchesMeasure = matches(person.getMeasure_Name(), measure_Name);
            boolean matchesChannel = matches(person.getChannel(), channel);
            boolean matchesValue = matches(person.getValue(), value);

            return matchesFullName && matchesDevice && matchesMeasure && matchesChannel && matchesValue ;
        }

        private boolean matches(String value, String searchTerm) {
            return searchTerm == null || searchTerm.isEmpty()
                    || value.toLowerCase().contains(searchTerm.toLowerCase());
        }
    }

    private void updateList() {

        //   System.out.println("Suche nach String: " + filterText.getValue());
        //grid.setItems(cltvHwMeasureService.findAllProducts(filterText.getValue()));
    }
}
