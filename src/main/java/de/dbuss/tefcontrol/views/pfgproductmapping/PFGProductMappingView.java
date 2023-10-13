package de.dbuss.tefcontrol.views.pfgproductmapping;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.wontlost.ckeditor.VaadinCKEditor;
import de.dbuss.tefcontrol.data.entity.ProductHierarchie;
import de.dbuss.tefcontrol.data.entity.ProjectConnection;
import de.dbuss.tefcontrol.data.service.ProductHierarchieService;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@PageTitle("PFG Product-Mapping")
@Route(value = "PFG-Mapping", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@RolesAllowed("USER")
public class PFGProductMappingView extends VerticalLayout {

    private final ProductHierarchieService service;

    private final ProjectConnectionService projectConnectionService;

    Grid<ProductHierarchie> grid = new Grid<>(ProductHierarchie.class);

    TextField filterText = new TextField();
    TabSheet tabSheet = new TabSheet();

  //  Button saveBtn = new Button("save");
 //   Button editBtn = new Button("edit");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    Div textArea = new Div();
    VerticalLayout messageLayout = new VerticalLayout();
    VaadinCKEditor editor;
    PFGProductForm form;
    Checkbox autorefresh = new Checkbox();
    private Label lastRefreshLabel;
    private Label countdownLabel;
    private ScheduledExecutorService executor;
    private UI ui ;
    Instant startTime;
    private String productsDb;
    private String selectedDbName;
    public PFGProductMappingView(@Value("${pfg_mapping_products}") String productsDb , ProductHierarchieService service, ProjectConnectionService projectConnectionService) {
        this.service = service;
        this.projectConnectionService = projectConnectionService;
        this.productsDb = productsDb;

        ui= UI.getCurrent();

        addClassName("list-view");
        setSizeFull();
        configureGrid();
        // configureAttachmentsGrid();
        configureForm();
        configureLoggingArea();

       // saveBtn.setVisible(false);
      //  editBtn.setVisible(true);

        HorizontalLayout hl = new HorizontalLayout();
        //   hl.add(getTabsheet(),saveBtn,editBtn);
        hl.add(getPFGMapping());

        hl.setHeightFull();
        hl.setSizeFull();

        ComboBox<String> databaseConnectionCB = new ComboBox<>();
        databaseConnectionCB.setAllowCustomValue(true);

        List<ProjectConnection> listOfProjectConnections = projectConnectionService.findAll();
        List<String> connectionNames = listOfProjectConnections.stream()
                .flatMap(connection -> {
                    String category = connection.getCategory();
                    if (category == null) {
                        return Stream.of(connection.getName());
                    }
                    return Stream.empty();
                })
                .collect(Collectors.toList());
        databaseConnectionCB.setItems(connectionNames);
        databaseConnectionCB.setValue(connectionNames.get(0));
        selectedDbName = connectionNames.get(0);



        add(databaseConnectionCB, hl);

      //  getCLTVALLProduct();

        updateList();
        closeEditor();

    }


    private void configureLoggingArea() {

        messageLayout = new VerticalLayout();
        messageLayout.setWidthFull();
        messageLayout.getStyle().set("background-color", "black");
        messageLayout.getStyle().set("color", "white");
        messageLayout.getStyle().set("position", "fixed");
        messageLayout.getStyle().set("bottom", "0");

        // Create and add messages

        messageLayout.add(textArea);

        // Add the layout to the main view
        add(messageLayout);

    }



    private void updateList() {

        grid.setItems(service.findAllProducts(filterText.getValue()));
    }

    private Component getContent() {

        HorizontalLayout content = new HorizontalLayout(grid, form);

        //  HorizontalLayout content = new HorizontalLayout(grid, form);
        //   HorizontalLayout content = new HorizontalLayout(tabSheet);
        //HorizontalLayout content = new HorizontalLayout(grid);
        //  content.setFlexGrow(2,grid);
        //  content.setFlexGrow(1,form);
        //  content.addClassName("content");
        //  content.setSizeFull();

        return content;

    }

    private Component getPFGMapping() {

        VerticalLayout vl = new VerticalLayout();

        HorizontalLayout content = new HorizontalLayout(grid, form);
        content.setFlexGrow(2,grid);
        content.setFlexGrow(1,form);
        content.addClassName("content");
        content.setSizeFull();
        content.setHeightFull();

        vl.add(getToolbar(),content);

        vl.setSizeFull();
        vl.setHeightFull();

        return vl;

    }

    private void configureForm() {

        form = new PFGProductForm(projectConnectionService.getCltvAllProducts(productsDb));
        form.setWidth("25em");
        //form.addSaveListener(this::saveProduct);

        form.addListener(PFGProductForm.SaveEvent.class,this::saveProduct);
        form.addListener(PFGProductForm.DeleteEvent.class, this::deleteProduct);
        form.addListener(PFGProductForm.CloseEvent.class, e -> closeEditor());


        //form.addDeleteListener(this::deleteProduct);
        //form.addCloseListener(e -> closeEditor());


    }

    private void saveProduct(PFGProductForm.SaveEvent event) {
        service.saveProduct(event.getProduct());
        updateList();
        closeEditor();
    }

    private void deleteProduct(PFGProductForm.DeleteEvent event) {
        service.deleteProduct(event.getProduct());
        updateList();
        closeEditor();
    }

    private void configureGrid() {
        grid.addClassNames("PFG-grid");
        grid.setSizeFull();
        grid.setHeightFull();
        grid.setColumns("pfg_Type", "node", "product_name");

        grid.getColumns().forEach(col -> col.setAutoWidth(true));

//        grid.asSingleSelect().addValueChangeListener(event ->
//                editProduct(event.getValue()));

        grid.addItemDoubleClickListener(event ->
                editProduct(event.getItem()));

    }

    private void editProduct(ProductHierarchie product) {
        if (product == null) {
            closeEditor();
        } else {
            form.setProduct(product);
            form.setVisible(true);
            addClassName("editing");
        }
    }
    private void closeEditor() {
        form.setProduct(null);
        form.setVisible(false);
        removeClassName("editing");
    }
    private void addProduct() {
        grid.asSingleSelect().clear();
        editProduct(new ProductHierarchie());
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by node/product...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());

        Button addProductButton = new Button("Add Mapping");
        addProductButton.addClickListener(click -> addProduct());


//        Button startJobButton = new Button("Start");
//        startJobButton.addClickListener(click -> startJob());

        //      var toolbar = new HorizontalLayout(filterText, addProductButton, startJobButton);
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addProductButton);
        toolbar.addClassName("toolbar");

        return toolbar;
    }


}
