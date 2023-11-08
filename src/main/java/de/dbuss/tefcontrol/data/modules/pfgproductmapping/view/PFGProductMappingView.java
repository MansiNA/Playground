package de.dbuss.tefcontrol.data.modules.pfgproductmapping.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.gridpro.GridPro;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.modules.pfgproductmapping.entity.ProductHierarchie;
import de.dbuss.tefcontrol.data.entity.ProjectConnection;
import de.dbuss.tefcontrol.data.service.ProductHierarchieService;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@PageTitle("PFG Product-Mapping")
@Route(value = "PFG-Mapping", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@RolesAllowed({"USER", "ADMIN"})
public class PFGProductMappingView extends VerticalLayout {
    @Autowired
    private JdbcTemplate template;
    private final ProductHierarchieService service;
    private final ProjectConnectionService projectConnectionService;
    Grid<ProductHierarchie> grid = new Grid<>(ProductHierarchie.class);
    GridPro<ProductHierarchie> missingGrid = new GridPro<>(ProductHierarchie.class);
    Button startAgentBtn = new Button("Execute Job");
    Button saveButton = new Button("Save");
    TextField filterText = new TextField();
    Div textArea = new Div();
    VerticalLayout messageLayout = new VerticalLayout();
    PFGProductForm form;
    private UI ui ;
    private String productsDb;
    private String dBAgentName;
    private String selectedDbName;
    private String targetTable;
    private String targetView;
    private List<ProductHierarchie> modifiedProducts = new ArrayList<>();

    public PFGProductMappingView( ProductHierarchieService service, ProjectParameterService projectParameterService, ProjectConnectionService projectConnectionService) {
        this.service = service;
        this.projectConnectionService = projectConnectionService;
        ui= UI.getCurrent();

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        String missingQuery = null;
        String pfg_mapping_target = null;

        for (ProjectParameter projectParameter : listOfProjectParameters) {
            if(projectParameter.getNamespace().equals(Constants.PFGPRODUCTMAPPING)) {
                if (Constants.MAPPINGALLPRODUCTS.equals(projectParameter.getName())) {
                    productsDb = projectParameter.getValue();
                } else if (Constants.AGENT_NAME.equals(projectParameter.getName())) {
                    dBAgentName = projectParameter.getValue();
                } else if (Constants.MAPPINGMISSINGPRODUCTS.equals(projectParameter.getName())) {
                    missingQuery = projectParameter.getValue();
                } else if (Constants.PFG_TABLE.equals(projectParameter.getName())) {
                    pfg_mapping_target = projectParameter.getValue();
                }
            }
        }


        String [] targets = pfg_mapping_target.split(":");
        selectedDbName = targets[0];
        targetTable = targets[1];

        String [] targetViews = missingQuery.split(":");
        targetView = targetViews[1];

        addClassName("list-view");
        setSizeFull();
        configureGrid();
        configureMissingGrid();
        configureForm();
        configureLoggingArea();
        configureExecuteBtn();
        configureSaveBtn();

        HorizontalLayout hl = new HorizontalLayout();
        ComboBox<String> databaseConnectionCB = new ComboBox<>();
        databaseConnectionCB.setAllowCustomValue(true);

        List<ProjectConnection> listOfProjectConnections = projectConnectionService.findAll();
        List<String> connectionNames = listOfProjectConnections.stream()
                .flatMap(connection -> {
                    String category = connection.getCategory();
                    //if (category == null) {
                    if("PFG-Mapping".equals(category)){
                        return Stream.of(connection.getName());
                    }
                    return Stream.empty();
                })
                .collect(Collectors.toList());
        databaseConnectionCB.setItems(connectionNames);
       // databaseConnectionCB.setValue(connectionNames.get(0));
       // selectedDbName = connectionNames.get(0);
        databaseConnectionCB.setValue(selectedDbName);

        databaseConnectionCB.addValueChangeListener(event -> {
            selectedDbName = event.getValue();
            updateList();
            updateMissingGrid();
        });

        hl.add(databaseConnectionCB, startAgentBtn, saveButton);

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Missing Entries", getMissingMapping());
        tabSheet.add("All Entries",getPFGMapping());

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();
        tabSheet.addThemeVariants(TabSheetVariant.MATERIAL_BORDERED);

        add(tabSheet);
        add(hl,tabSheet );

        updateList();
        updateMissingGrid();
        closeEditor();
    }

    private void configureSaveBtn() {

        saveButton.addClickListener(e -> {
            if (modifiedProducts != null && !modifiedProducts.isEmpty()) {
                    String result = projectConnectionService.saveListOfProductHierarchie(modifiedProducts, selectedDbName, targetTable);
                    if (result.contains("ok")){
                        Notification.show(modifiedProducts.size()+" Uploaded successfully",2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        modifiedProducts.clear();
                    } else {
                        Notification.show( "Error during upload: "+ result,3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                    updateMissingGrid();
                    updateList();
            } else {
                Notification.show( "Not any changed in products",3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
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

    private void configureExecuteBtn() {

        String dbConnection="";
        String agentJobName="";

        try {
            //String[] dbName = productDb.split("\\.");
            String[] parts = dBAgentName.split(":");

            if (parts.length == 2) {
                dbConnection = parts[0];
                agentJobName = parts[1];

            } else {
                System.out.println("ERROR: No Connection/AgentJob for start Agent-Job!");
            }

          } catch (Exception e) {
               e.printStackTrace();
                 return ;
           }


        String finalAgentJobName = agentJobName;
        String finalDbConnection = dbConnection;
      //  selectedDbName = dbConnection;
        startAgentBtn.addClickListener(e->{

           String erg= startJob(selectedDbName,finalAgentJobName);

           if(!erg.contains("OK"))
           {
               Notification notification = Notification.show("ERROR: " + erg,10000, Notification.Position.MIDDLE);
               notification.addThemeVariants(NotificationVariant.LUMO_ERROR);

           }
           else {
               startAgentBtn.setEnabled(false);
           }
        });

        startAgentBtn.setText("Execute Job " + agentJobName);

        startAgentBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_SUCCESS);
        startAgentBtn.setTooltipText("Start of SQLServer Job: " + agentJobName );

    }

    private String startJob(String finalDbConnection, String finalAgentJobName) {

        String erg="";
        Notification notification = Notification.show("Starting Job " + finalAgentJobName + "...",5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        System.out.println("Start SQLServer-Job: " + finalAgentJobName + " on Connection: " + finalDbConnection);

        DataSource dataSource = projectConnectionService.getDataSource(finalDbConnection);
        template = new JdbcTemplate(dataSource);

        try {
            String sql = "msdb.dbo.sp_start_job @job_name='" + finalAgentJobName + "'";
            template.execute(sql);


        }
        catch (CannotGetJdbcConnectionException connectionException) {
            return connectionException.getMessage();
        } catch (Exception e) {
            // Handle other exceptions
            System.out.println("Exception: " + e.getMessage());
            return e.getMessage();
        }

        return "OK";


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

        //grid.setItems(service.findAllProducts(filterText.getValue()));
        List<ProductHierarchie> listOfProductHierarchie = projectConnectionService.fetchProductHierarchie(selectedDbName, targetTable, filterText.getValue());
        grid.setItems(listOfProductHierarchie);
        if (listOfProductHierarchie.isEmpty() && !projectConnectionService.getErrorMessage().isEmpty()){
            Notification.show(projectConnectionService.getErrorMessage(),4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
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

        var xx = projectConnectionService.getCltvAllProducts(productsDb);

        if (xx.isEmpty() && !projectConnectionService.getErrorMessage().isEmpty()){
            Notification.show(projectConnectionService.getErrorMessage(),4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }

        form = new PFGProductForm(xx);
        form.setWidth("25em");
        //form.addSaveListener(this::saveProduct);

        form.addListener(PFGProductForm.SaveEvent.class,this::saveProduct);
        form.addListener(PFGProductForm.DeleteEvent.class, this::deleteProduct);
        form.addListener(PFGProductForm.CloseEvent.class, e -> closeEditor());

        //form.addDeleteListener(this::deleteProduct);
        //form.addCloseListener(e -> closeEditor());
    }

    private void saveProduct(PFGProductForm.SaveEvent event) {
        String node = event.getProduct().getNode();
        String product = event.getProduct().getProduct_name();

        if (!node.startsWith("PFG_") || node.length()<9)
        {
            Notification notification = Notification.show("Node: Number of characters must be more then 9 and start with PFG_ ",5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if ( product==null ||product.isEmpty())
        {
            Notification notification = Notification.show("Please specify Product!",5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        String result = projectConnectionService.saveProductHierarchie(event.getProduct(), selectedDbName, targetTable);
        if (result.contains("ok")){
            Notification.show(" Uploaded successfully",2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            Notification.show( "Error during upload: "+ result,3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
      //  service.saveProduct(event.getProduct());
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
        grid.setColumns("product_name","pfg_Type", "node");

        grid.getColumnByKey("product_name").setHeader("Product").setWidth("400px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("pfg_Type").setHeader("PFG-Type").setWidth("120px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("node").setHeader("Node").setWidth("400px").setFlexGrow(0).setResizable(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);

//        grid.addItemDoubleClickListener(event ->
//                editProduct(event.getItem()));

    }

    private void configureMissingGrid() {
        List<ProductHierarchie> listOfProductHierarchie = projectConnectionService.fetchProductHierarchie(selectedDbName, targetTable, filterText.getValue());
        List<String> listOfNodes = listOfProductHierarchie.stream()
                .map(ProductHierarchie::getNode)
                .distinct()
                .collect(Collectors.toList());

        missingGrid.addClassNames("Missing PFG-grid");
        missingGrid.setSizeFull();
        missingGrid.setHeightFull();
        missingGrid.setColumns("product_name");

        missingGrid.getColumnByKey("product_name").setHeader("Product").setWidth("400px").setFlexGrow(0).setResizable(true);
        missingGrid.addComponentColumn(productHierarchie -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setItems("PFG (PO)", "PFG (PP)");
            comboBox.setValue("PFG (PO)");
            comboBox.setWidth("120px");
         //   comboBox.setHeight("40px");
            productHierarchie.setPfg_Type("PFG (PO)");
            comboBox.addValueChangeListener(event -> {
                productHierarchie.setPfg_Type(event.getValue());
                if(isValidNode(productHierarchie.getNode())) {
                    if(modifiedProducts.contains(productHierarchie)){
                        modifiedProducts.remove(productHierarchie);
                        modifiedProducts.add(productHierarchie);
                    } else {
                        modifiedProducts.add(productHierarchie);
                    }
                }
            });
            return comboBox;
        }).setHeader("PFG-Type").setFlexGrow(0).setWidth("200px").setResizable(true);

        missingGrid.addComponentColumn(productHierarchie -> {
            ComboBox<String> nodeComboBox = new ComboBox<>();
            nodeComboBox.setPlaceholder("select or enter value...");
            if(listOfNodes != null && !listOfNodes.isEmpty()) {
                nodeComboBox.setItems(listOfNodes);
             //   nodeComboBox.setValue(listOfNodes.get(0));
            }
            nodeComboBox.setAllowCustomValue(true);
            nodeComboBox.addCustomValueSetListener(e -> {
                String customValue = e.getDetail();
                if(isValidNode(customValue)) {
                    productHierarchie.setNode(customValue);
                    listOfNodes.add(customValue);
                    nodeComboBox.setItems(listOfNodes);
                    nodeComboBox.setValue(customValue);
                    if(modifiedProducts.contains(productHierarchie)){
                        modifiedProducts.remove(productHierarchie);
                        modifiedProducts.add(productHierarchie);
                    } else {
                        modifiedProducts.add(productHierarchie);
                    }
                } else {
                    Notification.show("Invalid Node: The Node must have more than 7 characters and start with 'PFG_'.", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            nodeComboBox.addValueChangeListener(event -> {
                String selectedValue = event.getValue();
                if(isValidNode(selectedValue)) {
                    productHierarchie.setNode(selectedValue);
                    if(modifiedProducts.contains(productHierarchie)){
                        modifiedProducts.remove(productHierarchie);
                        modifiedProducts.add(productHierarchie);
                    } else {
                        modifiedProducts.add(productHierarchie);
                    }
                } else {
                    Notification.show("Invalid Node: The Node must have more than 7 characters and start with 'PFG_'.", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });

            return nodeComboBox;
        }).setHeader("Node").setFlexGrow(0).setWidth("300px").setResizable(true);

      //  missingGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        missingGrid.setSelectionMode(Grid.SelectionMode.NONE);
        missingGrid.setEditOnClick(true);
        //missingGrid.addThemeVariants(GridProVariant.LUMO_HIGHLIGHT_EDITABLE_CELLS);

    }

    private static void showErrorNotification(String msg) {
        Notification notification = new Notification(msg, 5000,
                Notification.Position.BOTTOM_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.open();
    }

    private void updateMissingGrid(){
        try {
            List<String> missingProducts = projectConnectionService.getAllMissingProducts(selectedDbName, targetView);
            List<ProductHierarchie> missingData = new ArrayList<>();
            for (String product : missingProducts) {
                ProductHierarchie productHierarchie = new ProductHierarchie();
                productHierarchie.setProduct_name(product);
                missingData.add(productHierarchie);
            }
            missingGrid.setItems(missingData);
            if(missingProducts.isEmpty() && !projectConnectionService.getErrorMessage().isEmpty()) {
                Notification.show(projectConnectionService.getErrorMessage(),4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception ex) {
            ex.printStackTrace();

            Notification.show(projectConnectionService.getErrorMessage(),4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private boolean isValidNode(String node) {
        return node != null && node.length() > 7 && node.startsWith("PFG_");
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
    //    HorizontalLayout toolbar = new HorizontalLayout(filterText, addProductButton);
        HorizontalLayout toolbar = new HorizontalLayout(filterText);
        toolbar.setWidth("800px");
        toolbar.addClassName("toolbar");

        return toolbar;
    }
}
