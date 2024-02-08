package de.dbuss.tefcontrol.data.modules.pfgproductmapping.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
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
import com.vaadin.flow.router.*;
import de.dbuss.tefcontrol.components.DefaultUtils;
import de.dbuss.tefcontrol.components.LogView;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.view.B2POutlookFINView;
import de.dbuss.tefcontrol.data.modules.pfgproductmapping.entity.ProductHierarchie;
import de.dbuss.tefcontrol.data.service.*;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
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
@Route(value = "PFG_Product_Mapping/:project_Id", layout = MainLayout.class)
// @RouteAlias(value = "", layout = MainLayout.class)
@RolesAllowed({"CLTV", "ADMIN"})
public class PFGProductMappingView extends VerticalLayout implements BeforeEnterObserver {
    @Autowired
    private JdbcTemplate template;
    private final ProductHierarchieService service;
    private final ProjectConnectionService projectConnectionService;
    private final ProjectsService projectsService;
    private final BackendService backendService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private AuthenticatedUser authenticatedUser;
    Grid<ProductHierarchie> grid = new Grid<>(ProductHierarchie.class);
    GridPro<ProductHierarchie> missingGrid = new GridPro<>(ProductHierarchie.class);
    Button startAgentBtn = new Button("Execute Job");
   // Button saveButton = new Button("Save");
    TextField filterText = new TextField();
    Div textArea = new Div();
    VerticalLayout messageLayout = new VerticalLayout();
    PFGProductForm form;
    private UI ui ;
    private String productsDb;
    private String agentName;
    private String selectedDbName;
    private String targetTable;
    private String targetView;
    private List<ProductHierarchie> modifiedProducts = new ArrayList<>();
    private Boolean isVisible = false;
    private Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private int projectId;
    private int upload_id;
    private Optional<Projects> projects;
    private DefaultUtils defaultUtils;
    private List<ProjectAttachmentsDTO> listOfProjectAttachments;
    private QS_Grid qsGrid;
    private Button qsBtn;
    private Button uploadBtn;
    private LogView logView;
    private Boolean isLogsVisible = false;

    public PFGProductMappingView( ProductHierarchieService service, ProjectParameterService projectParameterService, ProjectConnectionService projectConnectionService, AuthenticatedUser authenticatedUser, BackendService backendService, ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService) {
        this.service = service;
        this.projectConnectionService = projectConnectionService;
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;
        this.backendService = backendService;
        this.authenticatedUser = authenticatedUser;

        ui= UI.getCurrent();
        logView = new LogView();
        logView.logMessage(Constants.INFO, "Starting PFGProductMappingView");

        uploadBtn = new Button("Upload");

        qsBtn = new Button("QS and Start Job");
        qsBtn.setEnabled(false);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.PFG_PRODUCT_MAPPING.equals(projectParameter.getNamespace()))
                .collect(Collectors.toList());

        String missingQuery = null;
        String pfg_mapping_target = null;
        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : filteredProjectParameters) {
            //  if(projectParameter.getNamespace().equals(Constants.PFG_PRODUCT_MAPPING)) {
            if (Constants.MAPPINGALLPRODUCTS.equals(projectParameter.getName())) {
                productsDb = projectParameter.getValue();
            } else if (Constants.AGENT_NAME.equals(projectParameter.getName())) {
                agentName = projectParameter.getValue();
            } else if (Constants.MAPPINGMISSINGPRODUCTS.equals(projectParameter.getName())) {
                targetView = projectParameter.getValue();
            } else if (Constants.PFG_TABLE.equals(projectParameter.getName())) {
                targetTable = projectParameter.getValue();
            } else if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                dbServer = projectParameter.getValue();
            } else if (Constants.DB_NAME.equals(projectParameter.getName())) {
                dbName = projectParameter.getValue();
            } else if (Constants.DB_USER.equals(projectParameter.getName())) {
                dbUser = projectParameter.getValue();
            } else if (Constants.DB_PASSWORD.equals(projectParameter.getName())) {
                dbPassword = projectParameter.getValue();
            }
            //  }
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
        logView.logMessage(Constants.INFO, "Ending PFGProductMappingView");
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
        logView.logMessage(Constants.INFO, "Sarting getUpladTab() for set upload data");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        addClassName("list-view");
        setSizeFull();
        configureGrid();
        configureForm();
        configureLoggingArea();
        configureMissingGrid();
       // configureExecuteBtn();

        HorizontalLayout hl = new HorizontalLayout();

        hl.add(uploadBtn, qsBtn, qsGrid);

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Missing Entries", getMissingMapping());
        tabSheet.add("All Entries",getPFGMapping());

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();
        tabSheet.addThemeVariants(TabSheetVariant.MATERIAL_BORDERED);

        content.add(tabSheet);
        content.add(hl, tabSheet );

        updateList();
        updateMissingGrid();
        closeEditor();

        uploadBtn.addClickListener(e ->{
            logView.logMessage(Constants.INFO, "Uploading in uploadBtn.addClickListener");
            save2db();
            qsBtn.setEnabled(true);
        });

        qsBtn.addClickListener(e ->{
            logView.logMessage(Constants.INFO, "executing sqls in qsBtn.addClickListener");
            //   if (qsGrid.projectId != projectId) {
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

    private void save2db() {
        logView.logMessage(Constants.INFO, "Starting save2db() for save data");
        ProjectUpload projectUpload = new ProjectUpload();
        projectUpload.setFileName("");
        //projectUpload.setUserName(MainLayout.userName);

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            projectUpload.setUserName(user.getUsername());
        }

        projectUpload.setModulName("B2POutlookFIN");

        logView.logMessage(Constants.INFO, "Get file upload id from database");
        projectConnectionService.getJdbcConnection(dbUrl, dbUser, dbPassword); // Set Connection to target DB
        upload_id = projectConnectionService.saveUploadedGenericFileData(projectUpload);

        projectUpload.setUploadId(upload_id);

        System.out.println("Upload_ID: " + upload_id);

        if (modifiedProducts != null && !modifiedProducts.isEmpty()) {
            String result = projectConnectionService.saveListOfProductHierarchie(modifiedProducts, dbUrl, dbUser, dbPassword, targetTable);
            if (result.equals(Constants.OK)) {
                Notification.show(modifiedProducts.size() + " Uploaded successfully", 2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                modifiedProducts.clear();
            } else {
                Notification.show("Error during upload: " + result, 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            updateMissingGrid();
            updateList();
        } else {
            Notification.show("Not any changed in products", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        logView.logMessage(Constants.INFO, "Ending save2db() for save data");
    }

    private Component getMissingMapping() {
        logView.logMessage(Constants.INFO, "Staring getMissingMapping() for save data");
        VerticalLayout vl = new VerticalLayout();

      //  HorizontalLayout content = new HorizontalLayout(missingGrid);
        HorizontalLayout content = new HorizontalLayout(missingGrid);

        content.addClassName("missingContent");
        content.setSizeFull();
        content.setHeightFull();

        vl.add(content);
        vl.setSizeFull();
        vl.setHeightFull();

        logView.logMessage(Constants.INFO, "Ending getMissingMapping() for save data");
        return vl;
    }

    private void configureExecuteBtn() {
        logView.logMessage(Constants.INFO, "Staring configureExecuteBtn() for save data");
        String dbConnection="";
        String agentJobName="";

        try {
            //String[] dbName = productDb.split("\\.");
            String[] parts = agentName.split(":");

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

        startAgentBtn.addClickListener(e->{

           String erg= startJob(finalAgentJobName);

           if(!erg.equals(Constants.OK))
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
        logView.logMessage(Constants.INFO, "Ending configureExecuteBtn() for save data");
    }

    private String startJob( String finalAgentJobName) {

        String erg="";
        Notification notification = Notification.show("Starting Job " + finalAgentJobName + "...",5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        DataSource dataSource = projectConnectionService.getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
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

        return Constants.OK;


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
        logView.logMessage(Constants.INFO, "Staring updateList() for update data");
        //grid.setItems(service.findAllProducts(filterText.getValue()));
        List<ProductHierarchie> listOfProductHierarchie = projectConnectionService.fetchProductHierarchie(dbUrl, dbUser, dbPassword, targetTable, filterText.getValue());
        grid.setItems(listOfProductHierarchie);
        if (listOfProductHierarchie.isEmpty() && !projectConnectionService.getErrorMessage().isEmpty()){
            Notification.show(projectConnectionService.getErrorMessage(),4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        logView.logMessage(Constants.INFO, "Ending updateList() for update data");
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
        logView.logMessage(Constants.INFO, "Staring getPFGMapping() for update data");
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
        logView.logMessage(Constants.INFO, "Ending getPFGMapping() for update data");
        return vl;

    }

    private void configureForm() {
        logView.logMessage(Constants.INFO, "Staring configureForm() for update data");
        var xx = projectConnectionService.getCltvAllProducts(dbUrl, dbUser, dbPassword, productsDb);

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
        logView.logMessage(Constants.INFO, "Ending configureForm() for update data");
    }

    private void saveProduct(PFGProductForm.SaveEvent event) {
        logView.logMessage(Constants.INFO, "Staring saveProduct() for save product");
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

        String result = projectConnectionService.saveProductHierarchie(event.getProduct(), dbUrl, dbUser, dbPassword, targetTable);
        if (result.equals(Constants.OK)){
            Notification.show(" Uploaded successfully",2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            Notification.show( "Error during upload: "+ result,3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
      //  service.saveProduct(event.getProduct());
        updateList();
        closeEditor();
        logView.logMessage(Constants.INFO, "Ending saveProduct() for save product");
    }

    private void deleteProduct(PFGProductForm.DeleteEvent event) {
        logView.logMessage(Constants.INFO, "deleteProduct() for save product");
        service.deleteProduct(event.getProduct());
        updateList();
        closeEditor();
    }

    private void configureGrid() {
        logView.logMessage(Constants.INFO, "Starting configureGrid() for all product grid");
        grid.addClassNames("PFG-grid");
        grid.setSizeFull();
        grid.setHeightFull();
        grid.setColumns("product_name","pfg_Type", "node");

        grid.getColumnByKey("product_name").setHeader("Product").setWidth("350px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("pfg_Type").setHeader("PFG-Type").setWidth("150px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey("node").setHeader("Node").setWidth("650px").setFlexGrow(0).setResizable(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid.setWidthFull();

//        grid.addItemDoubleClickListener(event ->
//                editProduct(event.getItem()));
        logView.logMessage(Constants.INFO, "Ending configureGrid() for all product grid");
    }

    private void configureMissingGrid() {
        logView.logMessage(Constants.INFO, "Starting configureMissingGrid() for missing grid");
        List<ProductHierarchie> listOfProductHierarchie = projectConnectionService.fetchProductHierarchie(dbUrl, dbUser, dbPassword, targetTable, filterText.getValue());
        List<String> listOfNodes = listOfProductHierarchie.stream()
                .map(ProductHierarchie::getNode)
                .distinct()
                .collect(Collectors.toList());

        missingGrid.addClassNames("Missing PFG-grid");
        missingGrid.setSizeFull();
        missingGrid.setHeightFull();
        missingGrid.setColumns("product_name");

        missingGrid.getColumnByKey("product_name").setHeader("Product").setWidth("350px").setFlexGrow(0).setResizable(true);
        missingGrid.addComponentColumn(productHierarchie -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setItems("PFG (PO)", "PFG (PP)");
            comboBox.setValue("PFG (PO)");
            comboBox.setWidth("130px");
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
        }).setHeader("PFG-Type").setFlexGrow(0).setWidth("140px");

        missingGrid.addComponentColumn(productHierarchie -> {
            ComboBox<String> nodeComboBox = new ComboBox<>();
            nodeComboBox.setPlaceholder("select or enter value...");
            if(listOfNodes != null && !listOfNodes.isEmpty()) {
                nodeComboBox.setItems(listOfNodes);
             //   nodeComboBox.setValue(listOfNodes.get(0));
            }
            nodeComboBox.setAllowCustomValue(true);
            nodeComboBox.setWidth("500px");
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
        }).setHeader("Node").setFlexGrow(0).setWidth("510px");

      //  missingGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        missingGrid.setSelectionMode(Grid.SelectionMode.NONE);
        missingGrid.setEditOnClick(true);
        //missingGrid.addThemeVariants(GridProVariant.LUMO_HIGHLIGHT_EDITABLE_CELLS);
        logView.logMessage(Constants.INFO, "Ending configureMissingGrid() for missing grid");
    }

    private static void showErrorNotification(String msg) {
        Notification notification = new Notification(msg, 5000,
                Notification.Position.BOTTOM_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.open();
    }

    private void updateMissingGrid(){
        logView.logMessage(Constants.INFO, "Starting updateMissingGrid() for update missing grid");
        try {
            List<String> missingProducts = projectConnectionService.getAllMissingProducts(dbUrl, dbUser, dbPassword, targetView);
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
        logView.logMessage(Constants.INFO, "Ending updateMissingGrid() for update missing grid");
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
