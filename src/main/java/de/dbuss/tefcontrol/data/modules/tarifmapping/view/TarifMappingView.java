package de.dbuss.tefcontrol.data.modules.tarifmapping.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dnd.GridDragEndEvent;
import com.vaadin.flow.component.grid.dnd.GridDragStartEvent;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.dbuss.tefcontrol.components.DefaultUtils;
import de.dbuss.tefcontrol.components.LogView;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.entity.OutlookMGSR;
import de.dbuss.tefcontrol.data.modules.tarifmapping.entity.CLTVProduct;
import de.dbuss.tefcontrol.data.modules.tarifmapping.entity.MissingCLTVProduct;
import de.dbuss.tefcontrol.data.service.*;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.method.P;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(value = "Tarif_Mapping/:project_Id", layout = MainLayout.class)
@RolesAllowed({"FLIP", "ADMIN"})
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
    private String fileName;
    private int upload_id;
    ListenableFuture<String> future;
    BackendService backendService;
    private AuthenticatedUser authenticatedUser;
    private Boolean isVisible = false;
    Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);
    private LogView logView;
    private Boolean isLogsVisible = false;

    Dialog dialog;
    static TextField nodeField;
    static TextField childField;

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

    private static VerticalLayout createDialogLayout() {

        childField = new TextField("Child");
        childField.setEnabled(false);
        nodeField = new TextField("Node");
        nodeField.setEnabled(false);

        VerticalLayout dialogLayout = new VerticalLayout(childField, nodeField);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "18rem").set("max-width", "100%");

        return dialogLayout;
    }

    private static Button createChildButton(Dialog dialog) {
        Button addChildButton = new Button("Add Child", e -> dialog.close());
        addChildButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        return addChildButton;
    }

    private static Button createNodeButton(Dialog dialog) {
        Button addChildButton = new Button("Add Node", e -> dialog.close());
        addChildButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        return addChildButton;
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

        Button showAllBtn= new Button("show all");

        VerticalLayout content = new VerticalLayout();

        content.setWidth("600px");
        content.setHeight("500px");
        content.add(showAllBtn);

        setupCLTVProductGrid();
        setupMissingCLTVProductGrid();

        // Set data for grids
        List<CLTVProduct> cltvProducts = projectConnectionService.getCLTVProducts(dbUrl, dbUser, dbPassword, tableName);
        List<MissingCLTVProduct> missingCLTVProducts = projectConnectionService.getMissingCLTVProducts(dbUrl, dbUser, dbPassword, missingTableName);
        cltvProductGrid.setItems(cltvProducts);
        missingCLTVProductGrid.setItems(missingCLTVProducts);

        missingCLTVProductGrid.setDropMode(GridDropMode.ON_GRID);
        missingCLTVProductGrid.setRowsDraggable(true);
        missingCLTVProductGrid.addDragStartListener(this::handleDragStart);
        cltvProductGrid.addDropListener(e -> {
            System.out.println("Dropped" +e.getDropTargetItem());
            dialog.open();


        });


        // Enable drag-and-drop for grids
        enableDragAndDrop(cltvProductGrid);
        enableDragAndDrop(missingCLTVProductGrid);

        // Add grids to the layout
        content.add(cltvProductGrid, missingCLTVProductGrid);

        logView.logMessage(Constants.INFO, "Ending getUpladTab() for set upload data");
        return content;
    }

    private void handleDragStart(GridDragStartEvent<MissingCLTVProduct> e) {
        draggedItem = e.getDraggedItems().get(0);
        System.out.println("Dragged start for" + draggedItem.getTariffGroupId());

        childField.setValue(draggedItem.getTariffGroupId());
        nodeField.setValue(draggedItem.getTariffGroupL4Name());

    }

    private void handleDragEnd(GridDragEndEvent<MissingCLTVProduct> e) {

        System.out.println("Dragged end " );
    }
    private MissingCLTVProduct draggedItem;


    private void setupCLTVProductGrid() {
        cltvProductGrid = new Grid<>(CLTVProduct.class);
        String NODE = "node";
        String CHILD = "child";
        String CLTVTARIF = "cltvTarif";
        String PRODUCTTYPE = "productType";
        String USER = "user";
        String VERARDATUM = "verarbDatum";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        cltvProductGrid.setColumns(NODE, CHILD, CLTVTARIF, PRODUCTTYPE, USER, VERARDATUM);

        cltvProductGrid.getColumnByKey(NODE).setHeader("Node").setWidth("120px").setFlexGrow(0).setResizable(true);
        cltvProductGrid.getColumnByKey(CHILD).setHeader("Child").setWidth("60px").setFlexGrow(0).setResizable(true);
        cltvProductGrid.getColumnByKey(CLTVTARIF).setHeader("CltvTarif").setWidth("80px").setFlexGrow(0).setResizable(true);
        cltvProductGrid.getColumnByKey(PRODUCTTYPE).setHeader("ProductType").setWidth("80px").setFlexGrow(0).setResizable(true);
        cltvProductGrid.getColumnByKey(USER).setHeader("User").setWidth("100px").setFlexGrow(0).setResizable(true);
        cltvProductGrid.getColumnByKey(VERARDATUM).setHeader("VerarbDatum").setWidth("200px").setFlexGrow(0).setResizable(true);

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
                , cltvProductGrid.getColumnByKey(VERARDATUM)
        );
        //    , gridFinancials.getColumnByKey(EDIT_COLUMN));

        cltvProductGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        logView.logMessage(Constants.INFO, "Ending setupMGSRGrid() for setup MGSRGrid");

    }

    private void setupMissingCLTVProductGrid() {
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
        logView.logMessage(Constants.INFO, "Ending setupMGSRGrid() for setup MGSRGrid");
    }
    private void enableDragAndDrop(Grid<?> grid) {
        grid.setDropMode(GridDropMode.BETWEEN);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
      //  grid.addDragEndListener(this::handleDragEnd);
        // Add drop listener
        missingCLTVProductGrid.addDropListener(event -> {
            // Get the dragged item
            MissingCLTVProduct draggedItem = event.getDropTargetItem().get();

            // Remove the item from the source grid
            List<MissingCLTVProduct> missingCLTVProducts= getMissingDataProviderAllItems();
            missingCLTVProducts.remove(draggedItem);
            missingCLTVProductGrid.setItems(missingCLTVProducts);
            missingCLTVProductGrid.getDataProvider().refreshAll();

            // Add the item to the target grid
         //   cltvProductGrid.getDataProvider().getItems().add(new CLTVProduct(draggedItem));
            List<CLTVProduct> cltvProducts = getCLTVProductDataProviderAllItems();
            CLTVProduct cltvProduct = new CLTVProduct();
            cltvProduct.setChild(draggedItem.getTariffGroupName());
            cltvProduct.setNode(draggedItem.getTariffGroupId());
            cltvProducts.add(cltvProduct);
            cltvProductGrid.setItems(cltvProducts);
            cltvProductGrid.getDataProvider().refreshAll();
        });


    }
    private List<CLTVProduct> getCLTVProductDataProviderAllItems() {
        DataProvider<CLTVProduct, Void> existDataProvider = (DataProvider<CLTVProduct, Void>) cltvProductGrid.getDataProvider();
        List<CLTVProduct> listOfCLTVProducts = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        return listOfCLTVProducts;
    }

    private List<MissingCLTVProduct> getMissingDataProviderAllItems() {
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
        parameterGrid.setHeight("200px");
        parameterGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        logView.logMessage(Constants.INFO, "Ending setProjectParameterGrid() for set database detail in Grid");
    }
}
