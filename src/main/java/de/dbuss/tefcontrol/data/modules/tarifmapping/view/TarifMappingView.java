package de.dbuss.tefcontrol.data.modules.tarifmapping.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dnd.GridDragEndEvent;
import com.vaadin.flow.component.grid.dnd.GridDragStartEvent;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.dbuss.tefcontrol.components.DefaultUtils;
import de.dbuss.tefcontrol.components.LogView;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.entity.OutlookMGSR;
import de.dbuss.tefcontrol.data.modules.tarifmapping.entity.CLTVProduct;
import de.dbuss.tefcontrol.data.modules.tarifmapping.entity.MissingCLTVProduct;
import de.dbuss.tefcontrol.data.service.*;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(value = "Tarif_Mapping/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN"})
public class TarifMappingView extends VerticalLayout implements BeforeEnterObserver {

    private final ProjectConnectionService projectConnectionService;
    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private List<List<OutlookMGSR>> listOfAllSheets = new ArrayList<>();
    private Grid<CLTVProduct> cltvProductGrid;
    private Grid<MissingCLTVProduct> missingCLTVProductGrid;
    private String tableName;
    private String missingTableName;
    int sheetNr = 0;
    private String agentName;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private int projectId;
    private Optional<Projects> projects;
    private DefaultUtils defaultUtils;
    private List<ProjectAttachmentsDTO> listOfProjectAttachments;
    ListenableFuture<String> future;
    BackendService backendService;
    private AuthenticatedUser authenticatedUser;
    private Boolean isVisible = false;
    Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);
    private LogView logView;
    private Boolean isLogsVisible = false;
    private MissingCLTVProduct draggedItem;
    Dialog dialog;
    private TextField nodeField;
    private TextField childField;
    private List<CLTVProduct> cltvAllProducts;
    private List<CLTVProduct> listOfFilterCltvProducts;
    private List<CLTVProduct> updatedCltvProductsList;
    private List<MissingCLTVProduct> missingCLTVProducts;
    private Button showAllBtn;
   // private Button saveBtn;
    public TarifMappingView(ProjectParameterService projectParameterService, ProjectConnectionService projectConnectionService, BackendService backendService, AuthenticatedUser authenticatedUser, ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService) {

        this.backendService = backendService;
        this.projectConnectionService = projectConnectionService;
        this.authenticatedUser=authenticatedUser;
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;
        logView = new LogView();
        logView.logMessage(Constants.INFO, "Starting TarifMappingView");

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.TARIFMAPPING.equals(projectParameter.getNamespace()))
                .collect(Collectors.toList());

        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : filteredProjectParameters) {
            //  if(projectParameter.getNamespace().equals(Constants.TARIFMAPPING)) {
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
            } else if (Constants.MISSING_TABLE.equals(projectParameter.getName())){
                missingTableName = projectParameter.getValue();
            }
            // }
        }

        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";

        setProjectParameterGrid(filteredProjectParameters);
        defaultUtils = new DefaultUtils(projectsService, projectAttachmentsService);

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
                        isLogsVisible = !isLogsVisible;
                        logView.setVisible(isLogsVisible);
                    },
                    Key.KEY_V, KeyModifier.ALT);
            UI.getCurrent().addShortcutListener(
                    () -> future.cancel(true),
                    Key.KEY_S, KeyModifier.ALT);

            UI.getCurrent().addShortcutListener(
                    () -> {
                        isVisible = !isVisible;
                        parameterGrid.setVisible(isVisible);
                    },
                    Key.KEY_I, KeyModifier.ALT);

        }

        dialog = new Dialog();

        dialog.setHeaderTitle("Add Mapping");

        VerticalLayout dialogLayout = createDialogLayout();
        dialog.add(dialogLayout);

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(createChildButton(dialog));
        dialog.getFooter().add(createNodeButton(dialog));

        logView.logMessage(Constants.INFO, "Ending TarifMappingView");
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
        logView.logMessage(Constants.INFO, "Sarting getUpladTab() for set upload data");

        cltvAllProducts = projectConnectionService.getCLTVProducts(dbUrl, dbUser, dbPassword, tableName);
        missingCLTVProducts = projectConnectionService.getMissingCLTVProducts(dbUrl, dbUser, dbPassword, missingTableName);

        showAllBtn = new Button("show all");
        // saveBtn = new Button("save");

        VerticalLayout content = new VerticalLayout();

        //content.setWidth("600px");
        //content.setHeight("500px");
        HorizontalLayout hl = new HorizontalLayout(showAllBtn);
        hl.setAlignItems(Alignment.BASELINE);
        content.add(hl);

        setupCLTVProductGrid();
        setupMissingCLTVProductGrid();

        listOfFilterCltvProducts = cltvAllProducts.stream()
                .filter(product -> product.getCltvTarif() == null || product.getProductType() == null)
                .collect(Collectors.toList());

        cltvProductGrid.setItems(listOfFilterCltvProducts);
        missingCLTVProductGrid.setItems(missingCLTVProducts);

        missingCLTVProductGrid.setDropMode(GridDropMode.ON_GRID);
        missingCLTVProductGrid.setRowsDraggable(true);
        missingCLTVProductGrid.addDragStartListener(this::handleDragStart);
        cltvProductGrid.addDropListener(e -> {
            dialog.open();
        });

        showAllBtn.addClickListener(event -> toggleView());
        updatedCltvProductsList = new ArrayList<>();
//        saveBtn.addClickListener(event -> {
//            updateCLTVProduct();
//        });
        // Enable drag-and-drop for grids
        enableDragAndDrop(cltvProductGrid);
      //  enableDragAndDrop(missingCLTVProductGrid);

        // Add grids to the layout
        content.add(cltvProductGrid, missingCLTVProductGrid);

        logView.logMessage(Constants.INFO, "Ending getUpladTab() for set upload data");
        return content;
    }
    private void toggleView() {
        logView.logMessage(Constants.INFO, "Sarting toggleView() for show all data Or missing data");
        if (showAllBtn.getText().equals("show only missing")) {
            // Show all rows
          //  saveBtn.setVisible(true);
            cltvProductGrid.setItems(listOfFilterCltvProducts);
            showAllBtn.setText("show all");
        } else {
            // Show only missing rows
          //  saveBtn.setVisible(false);
            cltvAllProducts = projectConnectionService.getCLTVProducts(dbUrl, dbUser, dbPassword, tableName);
            cltvProductGrid.setItems(cltvAllProducts);
            showAllBtn.setText("show only missing");
        }
        logView.logMessage(Constants.INFO, "Ending toggleView() for show all data Or missing data");
    }
    private void handleDragStart(GridDragStartEvent<MissingCLTVProduct> e) {
        logView.logMessage(Constants.INFO, "Sarting handleDragStart() for drag & drop");
        draggedItem = e.getDraggedItems().get(0);
        System.out.println("Dragged start for" + draggedItem.getTariffGroupId());

        childField.setValue(draggedItem.getTariffGroupId());
        nodeField.setValue(draggedItem.getTariffGroupL4Name());
        logView.logMessage(Constants.INFO, "Ending handleDragStart() for drag & drop");
    }

    private VerticalLayout createDialogLayout() {
        logView.logMessage(Constants.INFO, "Sarting createDialogLayout() for child and node dialog");
        childField = new TextField("Child");
        childField.setEnabled(false);
        nodeField = new TextField("Node");
        nodeField.setEnabled(false);

        VerticalLayout dialogLayout = new VerticalLayout(childField, nodeField);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "18rem").set("max-width", "100%");
        logView.logMessage(Constants.INFO, "Ending createDialogLayout() for child and node dialog");
        return dialogLayout;
    }

    private Button createChildButton(Dialog dialog) {
        logView.logMessage(Constants.INFO, "Sarting createChildButton() for add child button");
        Button addChildButton = new Button("Add Child", e -> dialog.close());
        addChildButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        addChildButton.addClickListener(clickEvent -> {
            CLTVProduct cltvProduct = new CLTVProduct();
            cltvProduct.setChild(childField.getValue());
            cltvProduct.setUser(MainLayout.userName);
            listOfFilterCltvProducts.add(cltvProduct);
            cltvProductGrid.setItems(listOfFilterCltvProducts);
            missingCLTVProducts.remove(draggedItem);
            missingCLTVProductGrid.setItems(missingCLTVProducts);
          //  projectConnectionService.deleteMissingCLTVProduct(dbUrl, dbUser, dbPassword, missingTableName, draggedItem.getTariffGroupId());
            save2DB(cltvProduct);
        });
        logView.logMessage(Constants.INFO, "Ending createChildButton() for add child button");
        return addChildButton;
    }
    private Button createNodeButton(Dialog dialog) {
        logView.logMessage(Constants.INFO, "Starting createNodeButton() for add node button");
        Button addNodeButton = new Button("Add Node", e -> dialog.close());
        addNodeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addNodeButton.addClickListener(clickEvent -> {
            CLTVProduct cltvProduct = new CLTVProduct();
            cltvProduct.setNode(nodeField.getValue());
            cltvProduct.setUser(MainLayout.userName);
            listOfFilterCltvProducts.add(cltvProduct);
            cltvProductGrid.setItems(listOfFilterCltvProducts);
            missingCLTVProducts.remove(draggedItem);
            missingCLTVProductGrid.setItems(missingCLTVProducts);
         //   projectConnectionService.deleteMissingCLTVProduct(dbUrl, dbUser, dbPassword, missingTableName, draggedItem.getTariffGroupId());
            save2DB(cltvProduct);
        });
        logView.logMessage(Constants.INFO, "Ending createNodeButton() for add node button");
        return addNodeButton;
    }

    private void save2DB(CLTVProduct cltvProduct) {
        logView.logMessage(Constants.INFO, "Starting save2DB() for saving data");
        String result = projectConnectionService.saveCLTVProduct(cltvProduct, dbUrl, dbUser, dbPassword, tableName);
        Notification notification;
        if (result.equals(Constants.OK)){
            logView.logMessage(Constants.INFO, "Saved file data in database");
            notification = Notification.show("CLTVProduct Uploaded successfully",5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            logView.logMessage(Constants.ERROR, "Error while saving file data in database");
            notification = Notification.show("Error during CLTVProduct upload " + result ,5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        logView.logMessage(Constants.INFO, "Ending save2DB() for saving data");
    }

    private void handleDragEnd(GridDragEndEvent<MissingCLTVProduct> e) {

        System.out.println("Dragged end " );
    }

    private void setupCLTVProductGrid() {
        logView.logMessage(Constants.INFO, "Starting setupCLTVProductGrid() for setup CLTVProductGrid");
        cltvProductGrid = new Grid<>(CLTVProduct.class);

        List<String> cltvtarrifList = cltvAllProducts.stream()
                .map(CLTVProduct::getCltvTarif)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<String> productTypeList = cltvAllProducts.stream()
                .map(CLTVProduct::getProductType)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        for (CLTVProduct product : cltvAllProducts) {
            String line = String.format("Node: %s, Child: %s, CLTV Tariff: %s, Product Type: %s, User: %s, Verarb Datum: %s",
                    product.getNode(), product.getChild(), product.getCltvTarif(),
                    product.getProductType(), product.getUser(), product.getVerarbDatum());

            System.out.println(line);
        }

        String NODE = "node";
        String CHILD = "child";
        String CLTVTARIF = "cltvTarif";
        String PRODUCTTYPE = "productType";
        String USER = "user";
        String VERARDATUM = "verarbDatum";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        cltvProductGrid.setColumns(NODE, CHILD, USER);

        cltvProductGrid.getColumnByKey(NODE).setHeader("Node").setWidth("120px").setFlexGrow(0).setResizable(true);
        cltvProductGrid.getColumnByKey(CHILD).setHeader("Child").setWidth("60px").setFlexGrow(0).setResizable(true);
       // cltvProductGrid.getColumnByKey(CLTVTARIF).setHeader("CltvTarif").setWidth("80px").setFlexGrow(0).setResizable(true);
      //  cltvProductGrid.getColumnByKey(PRODUCTTYPE).setHeader("ProductType").setWidth("80px").setFlexGrow(0).setResizable(true);
        cltvProductGrid.getColumnByKey(USER).setHeader("User").setWidth("100px").setFlexGrow(0).setResizable(true);
      //  cltvProductGrid.getColumnByKey(VERARDATUM).setHeader("VerarbDatum").setWidth("200px").setFlexGrow(0).setResizable(true);

        cltvProductGrid.addComponentColumn(cltvProduct -> {
            ComboBox<String> cltvTarrifCombobox = new ComboBox<>();
            cltvTarrifCombobox.setPlaceholder("select or enter value...");
            if(cltvtarrifList != null && !cltvtarrifList.isEmpty()) {
                cltvTarrifCombobox.setItems(cltvtarrifList);
            }
            System.out.println(cltvProduct.getNode() + "...."+ cltvProduct.getChild() + cltvProduct.getCltvTarif());
            if(cltvProduct.getCltvTarif() != null) {
                cltvTarrifCombobox.setValue(cltvProduct.getCltvTarif());
            }
            cltvTarrifCombobox.setAllowCustomValue(true);
            cltvTarrifCombobox.setWidth("260px");
            cltvTarrifCombobox.addCustomValueSetListener(e -> {
                String customValue = e.getDetail();
                cltvtarrifList.add(customValue);
                cltvTarrifCombobox.setItems(cltvtarrifList);
                cltvTarrifCombobox.setValue(customValue);
                cltvProduct.setCltvTarif(customValue);
                updatedCltvProductsList.add(cltvProduct);
                updateCLTVProduct();
            });
            cltvTarrifCombobox.addValueChangeListener(event -> {
                cltvProduct.setCltvTarif(event.getValue());
                updatedCltvProductsList.add(cltvProduct);
                updateCLTVProduct();
            });
            return cltvTarrifCombobox;
        }).setKey(CLTVTARIF).setHeader("CltvTarif").setFlexGrow(0).setWidth("140px");

        cltvProductGrid.addComponentColumn(cltvProduct -> {
            ComboBox<String> productTypeCombobox = new ComboBox<>();
            productTypeCombobox.setPlaceholder("select or enter value...");
            if(productTypeList != null && !productTypeList.isEmpty()) {
                productTypeCombobox.setItems(productTypeList);
            }
            System.out.println(cltvProduct.getNode() + "...."+ cltvProduct.getChild() + cltvProduct.getProductType());
            if(cltvProduct.getProductType() != null) {
                productTypeCombobox.setValue(cltvProduct.getProductType());
            }

            productTypeCombobox.setAllowCustomValue(true);
            productTypeCombobox.setWidth("260px");
            productTypeCombobox.addCustomValueSetListener(e -> {
                String customValue = e.getDetail();
                productTypeList.add(customValue);
                productTypeCombobox.setItems(productTypeList);
                cltvProduct.setProductType(customValue);
                updatedCltvProductsList.add(cltvProduct);
                updateCLTVProduct();
            });
            productTypeCombobox.addValueChangeListener(event -> {
                cltvProduct.setProductType(event.getValue());
                updatedCltvProductsList.add(cltvProduct);
                updateCLTVProduct();
            });
            return productTypeCombobox;
        }).setKey(PRODUCTTYPE).setHeader("ProductType").setFlexGrow(0).setWidth("140px");

        cltvProductGrid.getColumns().forEach(col -> col.setAutoWidth(true));
        // gridMGSR.setHeightFull();
        // gridMGSR.setSizeFull();

       //  cltvProductGrid.removeColumn(cltvProductGrid.getColumnByKey(EDIT_COLUMN));
       // cltvProductGrid.removeColumn(cltvProductGrid.getColumnByKey(VERARDATUM));

        // Reorder the columns (alphabetical by default)
        cltvProductGrid.setColumnOrder(cltvProductGrid.getColumnByKey(NODE)
                , cltvProductGrid.getColumnByKey(CHILD)
                , cltvProductGrid.getColumnByKey(CLTVTARIF)
                , cltvProductGrid.getColumnByKey(PRODUCTTYPE)
                , cltvProductGrid.getColumnByKey(USER)
            //    , cltvProductGrid.getColumnByKey(VERARDATUM)
        );
        //    , gridFinancials.getColumnByKey(EDIT_COLUMN));

        cltvProductGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        logView.logMessage(Constants.INFO, "Ending setupCLTVProductGrid() for setup CLTVProductGrid");

    }

    private void updateCLTVProduct() {
        logView.logMessage(Constants.INFO, "Starting updateCLTVProduct() for update CLTVProduct");
        Notification notification;
        if(!updatedCltvProductsList.isEmpty()) {
            String result = projectConnectionService.updateCLTVProducts(dbUrl, dbUser, dbPassword, tableName, updatedCltvProductsList);
            if (result.equals(Constants.OK)) {
                logView.logMessage(Constants.INFO, "Updated data in database");
                updateGridList();
                notification = Notification.show("CLTVProduct updated successfully", 5000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                logView.logMessage(Constants.ERROR, "Error while updating data in database");
                notification = Notification.show("Error during CLTVProduct update " + result, 5000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } else {
            logView.logMessage(Constants.ERROR, "no any data for update");
            notification = Notification.show("No any changes data", 5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        logView.logMessage(Constants.INFO, "Ending updateCLTVProduct() for update CLTVProduct");
    }

    private void updateGridList() {
        logView.logMessage(Constants.INFO, "Starting updateGridList() for update cltvproduct Grid");
        for (CLTVProduct cltvProduct : updatedCltvProductsList) {
            if (cltvProduct.getCltvTarif() != null && cltvProduct.getProductType() != null) {
                listOfFilterCltvProducts.remove(cltvProduct);
             //   cltvProductGrid.setItems(listOfFilterCltvProducts);
            }
        }
        logView.logMessage(Constants.INFO, "Ending updateGridList() for update cltvproduct Grid");
    }
    private void setupMissingCLTVProductGrid() {
        logView.logMessage(Constants.INFO, "Starting setupMissingCLTVProductGrid() for setup missing cltvproduct Grid");
        missingCLTVProductGrid = new Grid<>(MissingCLTVProduct.class);
        String TARIFFGROUPID = "tariffGroupId";
        String TARIFFGROUPNAME = "tariffGroupName";
        String TARIFFGROUPL4NAME = "tariffGroupL4Name";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        missingCLTVProductGrid.setColumns(TARIFFGROUPID, TARIFFGROUPNAME, TARIFFGROUPL4NAME);
        missingCLTVProductGrid.getColumnByKey(TARIFFGROUPID).setHeader("TariffGroupId").setWidth("120px").setFlexGrow(0).setResizable(true);
        missingCLTVProductGrid.getColumnByKey(TARIFFGROUPNAME).setHeader("TariffGroupName").setWidth("60px").setFlexGrow(0).setResizable(true);
        missingCLTVProductGrid.getColumnByKey(TARIFFGROUPL4NAME).setHeader("TariffGroupL4Name").setWidth("80px").setFlexGrow(0).setResizable(true);

        missingCLTVProductGrid.getColumns().forEach(col -> col.setAutoWidth(true));

       // missingCLTVProductGrid.removeColumn(missingCLTVProductGrid.getColumnByKey(EDIT_COLUMN));

        // Reorder the columns (alphabetical by default)
        missingCLTVProductGrid.setColumnOrder(missingCLTVProductGrid.getColumnByKey(TARIFFGROUPID)
                , missingCLTVProductGrid.getColumnByKey(TARIFFGROUPNAME)
                , missingCLTVProductGrid.getColumnByKey(TARIFFGROUPL4NAME)
        );
        //    , gridFinancials.getColumnByKey(EDIT_COLUMN));

        missingCLTVProductGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        logView.logMessage(Constants.INFO, "Ending setupMissingCLTVProductGrid() for setup missing cltvproduct Grid");
    }
    private void enableDragAndDrop(Grid<?> grid) {
        grid.setDropMode(GridDropMode.BETWEEN);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
    }
    private List<CLTVProduct> getCLTVProductDataProviderAllItems() {
        logView.logMessage(Constants.INFO, "Starting getCLTVProductDataProviderAllItems() for get cltvproduct list from grid");
        DataProvider<CLTVProduct, Void> existDataProvider = (DataProvider<CLTVProduct, Void>) cltvProductGrid.getDataProvider();
        List<CLTVProduct> listOfCLTVProducts = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        return listOfCLTVProducts;
    }

    private List<MissingCLTVProduct> getMissingDataProviderAllItems() {
        logView.logMessage(Constants.INFO, "Starting getCLTVProductDataProviderAllItems() for get missingCltvproduct list from grid");
        DataProvider<MissingCLTVProduct, Void> existDataProvider = (DataProvider<MissingCLTVProduct, Void>) missingCLTVProductGrid.getDataProvider();
        List<MissingCLTVProduct> listOfMissingCLTVProduct = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        return listOfMissingCLTVProduct;
    }
    private void setProjectParameterGrid(List<ProjectParameter> listOfProjectParameters) {
        logView.logMessage(Constants.INFO, "Starting setProjectParameterGrid() for set database detail in Grid");
        parameterGrid = new Grid<>(ProjectParameter.class, false);
        parameterGrid.addColumn(ProjectParameter::getName).setHeader("Name").setAutoWidth(true).setResizable(true);
        parameterGrid.addColumn(ProjectParameter::getValue).setHeader("Value").setAutoWidth(true).setResizable(true);
        parameterGrid.addColumn(ProjectParameter::getDescription).setHeader("Description").setAutoWidth(true).setResizable(true);

        parameterGrid.setItems(listOfProjectParameters);
        parameterGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
      //  parameterGrid.setHeight("200px");
        parameterGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        logView.logMessage(Constants.INFO, "Ending setProjectParameterGrid() for set database detail in Grid");
    }
}
