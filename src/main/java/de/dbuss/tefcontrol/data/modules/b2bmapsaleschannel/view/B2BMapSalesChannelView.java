package de.dbuss.tefcontrol.data.modules.b2bmapsaleschannel.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
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
import de.dbuss.tefcontrol.data.modules.b2bmapsaleschannel.entity.MapSalesChannel;
import de.dbuss.tefcontrol.data.service.*;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(value = "B2B_MapSalesChannel/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN"})
public class B2BMapSalesChannelView extends VerticalLayout implements BeforeEnterObserver {

    private final ProjectConnectionService projectConnectionService;
    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private Crud<MapSalesChannel> crud;
    private Grid<MapSalesChannel> mapSalesChannelGrid;
    private String tableName;
    private String sourceTable;
    private String agentName;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private int projectId;
    private Optional<Projects> projects;
    private DefaultUtils defaultUtils;
    private List<ProjectAttachmentsDTO> listOfProjectAttachments;
    private ListenableFuture<String> future;
    private BackendService backendService;
    private AuthenticatedUser authenticatedUser;
    private Boolean isVisible = false;
    private Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);
    private LogView logView;
    private Boolean isLogsVisible = false;
    private TextField nodeField;
    private TextField childField;
    private List<MapSalesChannel> mapSalesChannelList;
    //private Button showAllBtn;
    private QS_Grid qsGrid;
    private Button qsBtn;
    private int upload_id;

    public B2BMapSalesChannelView(ProjectParameterService projectParameterService, ProjectConnectionService projectConnectionService, BackendService backendService, AuthenticatedUser authenticatedUser, ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService) {

        this.backendService = backendService;
        this.projectConnectionService = projectConnectionService;
        this.authenticatedUser=authenticatedUser;
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;

        logView = new LogView();
        logView.logMessage(Constants.INFO, "Starting B2BMapSalesChannelView");

        qsBtn = new Button("QS and Start Job");
      //  qsBtn.setEnabled(false);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.B2B_MAPSALESCHANNEL.equals(projectParameter.getNamespace()))
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
            } else if (Constants.SOURCETABLE.equals(projectParameter.getName())){
                sourceTable = projectParameter.getValue();
            } else if (Constants.DB_JOBS.equals(projectParameter.getName())){
                agentName = projectParameter.getValue();
            }
            // }
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
                        isLogsVisible = !isLogsVisible;
                        logView.setVisible(isLogsVisible);
                    },
                    Key.KEY_V, KeyModifier.ALT);

            UI.getCurrent().addShortcutListener(
                    () -> {
                        isVisible = !isVisible;
                        parameterGrid.setVisible(isVisible);
                    },
                    Key.KEY_I, KeyModifier.ALT);

        }

        logView.logMessage(Constants.INFO, "Ending B2BMapSalesChannelView");
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

        mapSalesChannelList = projectConnectionService.getMapSalesChannelList(dbUrl, dbUser, dbPassword, sourceTable);

      //  showAllBtn = new Button("show all");
        // saveBtn = new Button("save");

        VerticalLayout content = new VerticalLayout();
        content.setHeightFull();

        HorizontalLayout hl = new HorizontalLayout();
        hl.setAlignItems(Alignment.BASELINE);
        hl.add( qsBtn, qsGrid);

        content.add(hl);
        content.add(getMapSalesChannelGrid());
        GenericDataProvider genericDataProvider = new GenericDataProvider(mapSalesChannelList);
        mapSalesChannelGrid.setDataProvider(genericDataProvider);
        setupDataProviderEvent();

        qsBtn.addClickListener(e ->{
            logView.logMessage(Constants.INFO, "Starting qsBtn.addClickListener for save and QsGrid ");
            ProjectUpload projectUpload = new ProjectUpload();
            projectUpload.setFileName("");
            //  projectUpload.setUserName(MainLayout.userName);
            Optional<User> maybeUser = authenticatedUser.get();
            if (maybeUser.isPresent()) {
                User user = maybeUser.get();
                projectUpload.setUserName(user.getUsername());
            }
            projectUpload.setModulName("B2BMapSalesChannel");

            projectConnectionService.getJdbcConnection(dbUrl, dbUser, dbPassword);

            upload_id = projectConnectionService.saveUploadedGenericFileData(projectUpload);

            if (upload_id==-1)
            {
                Notification.show("Error in B2BMapSalesChannel saveUploadedGenericFileData! ", 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            mapSalesChannelList = getMapSalesChannelDataProviderAllItems();
            System.out.println("Upload_ID: " + upload_id);
            logView.logMessage(Constants.INFO, "Saving MapSalesChannel in saveMapSalesChannel() ");
            String result = projectConnectionService.saveMapSalesChannel(dbUrl, dbUser, dbPassword, tableName, upload_id, mapSalesChannelList );
            Notification notification;
            if (result.equals(Constants.OK)) {
                notification = Notification.show("Rows Uploaded successfully", 6000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                notification = Notification.show("Error during upload: " + result, 15000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }

            hl.remove(qsGrid);
            qsGrid = new QS_Grid(projectConnectionService, backendService);
            hl.add(qsGrid);

            CallbackHandler callbackHandler = new CallbackHandler();
            qsGrid.createDialog(callbackHandler, projectId, upload_id);
            qsGrid.showDialog(true);
            logView.logMessage(Constants.INFO, "Ending qsBtn.addClickListener for save and QsGrid ");

        });

        logView.logMessage(Constants.INFO, "Ending getUpladTab() for set upload data");
        return content;
    }
    public class CallbackHandler implements QS_Callback {
        // Die Methode, die aufgerufen wird, wenn die externe Methode abgeschlossen ist
        @Override
        public void onComplete(String result) {
            logView.logMessage(Constants.INFO, "Starting CallbackHandler onComplete for execute Start Job");
            if(!result.equals("Cancel")) {

                logView.logMessage(Constants.INFO,"executeStartJobSteps: upload_id->" + upload_id + " AgentName->" + agentName );

                qsGrid.executeStartJobSteps(upload_id, agentName);
            }
            logView.logMessage(Constants.INFO, "Ending CallbackHandler onComplete for execute Start Job");
        }
    }

    private Component getMapSalesChannelGrid() {
        logView.logMessage(Constants.INFO, "Starting getMapSalesChannelGrid() for get MapSalsChannel crud Grid");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        crud = new Crud<>(MapSalesChannel.class, createEditor());

        crud.setToolbarVisible(false);
        crud.getDeleteButton().setVisible(false);
        crud.setHeightFull();
        crud.setSizeFull();
        setupMapSalsChannelGrid();
        content.add(crud);
        logView.logMessage(Constants.INFO, "Ending getMapSalesChannelGrid() for get MapSalsChannel crud Grid");
        return content;
    }

    private CrudEditor<MapSalesChannel> createEditor() {
        logView.logMessage(Constants.INFO, "createEditor() for create Editor");

        TextField salesChannelField = new TextField("SalesChannel");
        salesChannelField.setEnabled(false);
        TextArea channelField = new TextArea("Channel");
        FormLayout editFormLayout = new FormLayout(salesChannelField, channelField);
        Binder<MapSalesChannel> editBinder = new Binder<>(MapSalesChannel.class);
        //editBinder.bindInstanceFields(editFormLayout);
        editBinder.forField(salesChannelField).asRequired().bind(MapSalesChannel:: getSalesChannel,
                MapSalesChannel::setSalesChannel);
        editBinder.forField(channelField).asRequired().bind(MapSalesChannel::getChannel,
                MapSalesChannel::setChannel);

        return new BinderCrudEditor<>(editBinder, editFormLayout);
    }

    private void setupMapSalsChannelGrid() {
        logView.logMessage(Constants.INFO, "Starting setupMapSalsChannelGrid() for setup MapSalsChannelGrid");

        String ID = "id";
        String SALESCHANNEL = "salesChannel";
        String CHANNEL = "channel";
        String LOADDATE = "loadDate";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        mapSalesChannelGrid = crud.getGrid();

     //   mapSalesChannelGrid.setColumns(SALESCHANNEL, CHANNEL);

        mapSalesChannelGrid.getColumnByKey(ID).setHeader("Id").setWidth("120px").setFlexGrow(0).setResizable(true);
        mapSalesChannelGrid.getColumnByKey(SALESCHANNEL).setHeader("SalesChannel").setWidth("120px").setFlexGrow(0).setResizable(true);
        mapSalesChannelGrid.getColumnByKey(CHANNEL).setHeader("Channel").setWidth("60px").setFlexGrow(0).setResizable(true);
        mapSalesChannelGrid.getColumnByKey(LOADDATE).setHeader("loaddate").setWidth("60px").setFlexGrow(0).setResizable(true);
        mapSalesChannelGrid.getColumns().forEach(col -> col.setAutoWidth(true));

        //  cltvProductGrid.removeColumn(cltvProductGrid.getColumnByKey(EDIT_COLUMN));
        mapSalesChannelGrid.removeColumn(mapSalesChannelGrid.getColumnByKey(ID));
         mapSalesChannelGrid.removeColumn(mapSalesChannelGrid.getColumnByKey(LOADDATE));

        // Reorder the columns (alphabetical by default)
        mapSalesChannelGrid.setColumnOrder(mapSalesChannelGrid.getColumnByKey(SALESCHANNEL)
                , mapSalesChannelGrid.getColumnByKey(CHANNEL)
                , mapSalesChannelGrid.getColumnByKey(EDIT_COLUMN)
        );
        //    , gridFinancials.getColumnByKey(EDIT_COLUMN));

        mapSalesChannelGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        logView.logMessage(Constants.INFO, "Ending setupMapSalsChannelGrid() for setup MapSalsChannelGrid");

    }
    private void setupDataProviderEvent() {
        logView.logMessage(Constants.INFO, "Starting setupDataProviderEvent() for setup dataProvider");
        GenericDataProvider dataProvider = new GenericDataProvider(getMapSalesChannelDataProviderAllItems());

        crud.addDeleteListener(

            deleteEvent -> {
                Notification.show("Delete not allowed! ", 4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);

                //dataProvider.delete(deleteEvent.getItem());
                //mapSalesChannelGrid.setDataProvider(dataProvider);
                }


                );
        crud.addSaveListener(
                saveEvent -> {
                    dataProvider.persist(saveEvent.getItem());
                    mapSalesChannelGrid.setDataProvider(dataProvider);
                });
        logView.logMessage(Constants.INFO, "Ending setupDataProviderEvent() for setup dataProvider");
    }

    private List<MapSalesChannel> getMapSalesChannelDataProviderAllItems() {
        logView.logMessage(Constants.INFO, "getMapSalesChannelDataProviderAllItems() for get dataProvider all item");
        DataProvider<MapSalesChannel, Void> existDataProvider = (DataProvider<MapSalesChannel, Void>) mapSalesChannelGrid.getDataProvider();
        List<MapSalesChannel> listOfGenericComments = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        return listOfGenericComments;
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
