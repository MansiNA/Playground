package de.dbuss.tefcontrol.data.modules.saleschannelmapping.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.dbuss.tefcontrol.components.DefaultUtils;
import de.dbuss.tefcontrol.components.LogView;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.modules.saleschannelmapping.entity.CLTVSalesChannel;
import de.dbuss.tefcontrol.data.service.ProjectAttachmentsService;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.data.service.ProjectsService;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(value = "Sales_Channel_Mapping/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN"})
public class SalesChannelMapping extends VerticalLayout implements BeforeEnterObserver {

    private final ProjectConnectionService projectConnectionService;
    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private Crud<CLTVSalesChannel> crud;
    private Grid<CLTVSalesChannel> salesMappingGrid = new Grid<>(CLTVSalesChannel.class, false);
    private String targetTable;
    private String agentName;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private int projectId;
    private Optional<Projects> projects;
    private DefaultUtils defaultUtils;
    private List<ProjectAttachmentsDTO> listOfProjectAttachments;

    private Boolean isVisible = false;
    Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);
    private LogView logView;
    private Boolean isLogsVisible = false;

    public SalesChannelMapping(ProjectParameterService projectParameterService, ProjectConnectionService projectConnectionService, ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService) {

        this.projectConnectionService = projectConnectionService;
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;
        logView = new LogView();
        logView.logMessage(Constants.INFO, "Starting SalesChannelMapping");

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.SALES_CHANNEL_MAPPING.equals(projectParameter.getNamespace()))
                .collect(Collectors.toList());

        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : filteredProjectParameters) {
            //  if(projectParameter.getNamespace().equals(Constants.B2P_OUTLOOK_FIN)) {
            if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                dbServer = projectParameter.getValue();
            } else if (Constants.DB_NAME.equals(projectParameter.getName())) {
                dbName = projectParameter.getValue();
            } else if (Constants.DB_USER.equals(projectParameter.getName())) {
                dbUser = projectParameter.getValue();
            } else if (Constants.DB_PASSWORD.equals(projectParameter.getName())) {
                dbPassword = projectParameter.getValue();
            } else if (Constants.DB_JOBS.equals(projectParameter.getName())){
                agentName = projectParameter.getValue();
            }  else if (Constants.TABLE.equals(projectParameter.getName())) {
                targetTable = projectParameter.getValue();
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

/*        if(MainLayout.isAdmin) {
            UI.getCurrent().addShortcutListener(
                    () -> {
                        start_thread();
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

        }*/

        if (MainLayout.isAdmin) {
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

        logView.logMessage(Constants.INFO, "Ending SalesChannelMapping");
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

        tabSheet.add("Mapping", getMappingTab());
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
    private Component getMappingTab() {
        logView.logMessage(Constants.INFO, "Sarting getUpladTab() for set upload data");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();

        content.add(getSalesChannelGrid());

        logView.logMessage(Constants.INFO, "Ending getUpladTab() for set upload data");
        return content;
    }

    private Component getSalesChannelGrid() {
        logView.logMessage(Constants.INFO, "Starting getMGSRGrid() for get MGSRGrid");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        crud = new Crud<>(CLTVSalesChannel.class, createSalesChannelEditor());
        crud.setToolbarVisible(true);
        crud.setHeightFull();
        crud.setSizeFull();
        setupSalesChannelGrid();
        updateGrid();
        salesMappingGrid.addItemDoubleClickListener(event -> {
            CLTVSalesChannel  selectedEntity = event.getItem();
            crud.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            crud.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        crud.addSaveListener(event -> {
            logView.logMessage(Constants.INFO, "executing crud.addSaveListener for save editedAttachment in Attachment grid");
            CLTVSalesChannel  cltvSalesChannel = event.getItem();
            String resultString = projectConnectionService.saveOrUpdateCLTVSalesChannel(cltvSalesChannel, targetTable, dbUrl, dbUser, dbPassword);
            if (resultString.equals(Constants.OK)) {
                logView.logMessage(Constants.INFO, "save cltvSalesChannel data");
                Notification.show(" Update successfully", 2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updateGrid();
            } else {
                logView.logMessage(Constants.ERROR, "Error while saving cltvSalesChannel data");
                Notification.show("Error during upload: " + resultString, 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        content.add(crud);
        logView.logMessage(Constants.INFO, "Ending getMGSRGrid() for get MGSRGrid");
        return content;
    }

    private void updateGrid() {
        List<CLTVSalesChannel> listOfSalesChannel = projectConnectionService.getCLTVSalesChannels(targetTable, dbUrl, dbUser, dbPassword);
        if (listOfSalesChannel != null) {
            GenericDataProvider dataProvider = new GenericDataProvider(listOfSalesChannel, "lfdNr");
            salesMappingGrid.setDataProvider(dataProvider);
        }
    }

    private CrudEditor<CLTVSalesChannel> createSalesChannelEditor() {
        logView.logMessage(Constants.INFO, "createSalesChannelEditor() for create Editor");
        FormLayout editForm = new FormLayout();
        Binder<CLTVSalesChannel> binder = new Binder<>(CLTVSalesChannel.class);

        // Create fields
        IntegerField lfdNrField = new IntegerField("LfdNr");
        lfdNrField.setReadOnly(true);
        TextField salesChannelsCoOneBkIdField = new TextField("Sales Channels CoOne BK ID");
        TextField salesChannelsCoOneIdField = new TextField("Sales Channels CoOne ID");
        TextField salesChannelsNameField = new TextField("Sales Channels Name");
        TextField salesChannelCltvField = new TextField("Sales Channel CLTV");
        TextField userField = new TextField("User");
        DatePicker gueltigVonField = new DatePicker("Gueltig Von");
        DatePicker gueltigBisField = new DatePicker("Gueltig Bis");

        // Bind fields to Binder
        binder.forField(lfdNrField)
                .bind(CLTVSalesChannel::getLfdNr, CLTVSalesChannel::setLfdNr);

        binder.forField(salesChannelsCoOneBkIdField)
                .bind(CLTVSalesChannel::getSalesChannelsCoOneBkId, CLTVSalesChannel::setSalesChannelsCoOneBkId);

        binder.forField(salesChannelsCoOneIdField)
                .bind(CLTVSalesChannel::getSalesChannelsCoOneId, CLTVSalesChannel::setSalesChannelsCoOneId);

        binder.forField(salesChannelsNameField)
                .bind(CLTVSalesChannel::getSalesChannelsName, CLTVSalesChannel::setSalesChannelsName);

        binder.forField(salesChannelCltvField)
                .bind(CLTVSalesChannel::getSalesChannelCltv, CLTVSalesChannel::setSalesChannelCltv);

        binder.forField(userField)
                .bind(CLTVSalesChannel::getUser, CLTVSalesChannel::setUser);

        binder.forField(gueltigVonField)
                .bind(CLTVSalesChannel::getGueltigVon, CLTVSalesChannel::setGueltigVon);

        binder.forField(gueltigBisField)
                .bind(CLTVSalesChannel::getGueltigBis, CLTVSalesChannel::setGueltigBis);

        // Add fields to FormLayout
        editForm.add(lfdNrField, salesChannelsCoOneBkIdField, salesChannelsCoOneIdField, salesChannelsNameField,
                salesChannelCltvField,  gueltigVonField, gueltigBisField);

        // Return BinderCrudEditor with the Binder and FormLayout
        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupSalesChannelGrid() {
        logView.logMessage(Constants.INFO, "Starting setupSalesChannelGrid() for setup SalesChannelGrid");

        String LFDNR = "lfdNr";
        String SALES_CHANNELS_CO_ONE_BK_ID = "salesChannelsCoOneBkId";
        String SALES_CHANNELS_CO_ONE_ID = "salesChannelsCoOneId";
        String SALES_CHANNELS_NAME = "salesChannelsName";
        String SALES_CHANNEL_CLTV = "salesChannelCltv";
        String USER = "user";
        String GUELTIG_VON = "gueltigVon";
        String GUELTIG_BIS = "gueltigBis";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        salesMappingGrid = crud.getGrid();

        salesMappingGrid.getColumnByKey(LFDNR).setHeader("LfdNr").setWidth("80px").setFlexGrow(0).setResizable(true);
        salesMappingGrid.getColumnByKey(SALES_CHANNELS_CO_ONE_BK_ID).setHeader("Sales Channels CoOne BK ID").setWidth("200px").setFlexGrow(0).setResizable(true);
        salesMappingGrid.getColumnByKey(SALES_CHANNELS_CO_ONE_ID).setHeader("Sales Channels CoOne ID").setWidth("200px").setFlexGrow(0).setResizable(true);
        salesMappingGrid.getColumnByKey(SALES_CHANNELS_NAME).setHeader("Sales Channels Name").setWidth("200px").setFlexGrow(0).setResizable(true);
        salesMappingGrid.getColumnByKey(SALES_CHANNEL_CLTV).setHeader("Sales Channel CLTV").setWidth("200px").setFlexGrow(0).setResizable(true);
        salesMappingGrid.getColumnByKey(USER).setHeader("User").setWidth("120px").setFlexGrow(0).setResizable(true);
        salesMappingGrid.getColumnByKey(GUELTIG_VON).setHeader("Gueltig Von").setWidth("200px").setFlexGrow(0).setResizable(true);
        salesMappingGrid.getColumnByKey(GUELTIG_BIS).setHeader("Gueltig Bis").setWidth("200px").setFlexGrow(0).setResizable(true);

        salesMappingGrid.getColumns().forEach(col -> col.setAutoWidth(true));

        salesMappingGrid.removeColumnByKey(EDIT_COLUMN);
        salesMappingGrid.removeColumnByKey(USER);

        // Reorder the columns
        salesMappingGrid.setColumnOrder(
                salesMappingGrid.getColumnByKey(LFDNR),
                salesMappingGrid.getColumnByKey(SALES_CHANNELS_CO_ONE_BK_ID),
                salesMappingGrid.getColumnByKey(SALES_CHANNELS_CO_ONE_ID),
                salesMappingGrid.getColumnByKey(SALES_CHANNELS_NAME),
                salesMappingGrid.getColumnByKey(SALES_CHANNEL_CLTV),
                salesMappingGrid.getColumnByKey(GUELTIG_VON),
                salesMappingGrid.getColumnByKey(GUELTIG_BIS)
        );

        salesMappingGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        logView.logMessage(Constants.INFO, "Ending setupSalesChannelGrid() for setup SalesChannelGrid");
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
