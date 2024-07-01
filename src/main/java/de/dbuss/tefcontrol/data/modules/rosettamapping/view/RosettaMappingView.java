package de.dbuss.tefcontrol.data.modules.rosettamapping.view;

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
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
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
import de.dbuss.tefcontrol.data.modules.rosettamapping.entity.*;
import de.dbuss.tefcontrol.data.service.*;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

@Route(value = "Rosetta_Mapping/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN"})
public class RosettaMappingView extends VerticalLayout implements BeforeEnterObserver {

    private final ProjectConnectionService projectConnectionService;
    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private Crud<RosettaBrand> brandCrud;
    private Grid<RosettaBrand> brandGrid = new Grid<>(RosettaBrand.class, false);
    private Crud<RosettaKPI> kpiCrud;
    private Grid<RosettaKPI> kpiGrid = new Grid<>(RosettaKPI.class, false);
    private Crud<RosettaPartner> partnerCrud;
    private Grid<RosettaPartner> partnerGrid = new Grid<>(RosettaPartner.class, false);
    private Crud<RosettaChannel> channelCrud;
    private Grid<RosettaChannel> channelGrid = new Grid<>(RosettaChannel.class, false);
    private Crud<RosettaPaymentType> paymentTypeCrud;
    private Grid<RosettaPaymentType> paymentTypeGrid = new Grid<>(RosettaPaymentType.class, false);
    private Crud<RosettaProductTLN> productTLNCrud;
    private Grid<RosettaProductTLN> productTLNGrid = new Grid<>(RosettaProductTLN.class, false);
    private Crud<RosettaProductUSG> productUSGCrud;
    private Grid<RosettaProductUSG> productUSGGrid = new Grid<>(RosettaProductUSG.class, false);
    private Crud<RosettaUsageDirection> usageDirectionCrud;
    private Grid<RosettaUsageDirection> usageDirectionGrid = new Grid<>(RosettaUsageDirection.class, false);
    private String brandTable;
    private String kPITable;
    private String channelTable;
    private String partnerTable;
    private String paymentTypeTable;
    private String productTLNTable;
    private String productUSGTable;
    private String productUSGDirectionTable;
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

    public RosettaMappingView(ProjectParameterService projectParameterService, ProjectConnectionService projectConnectionService, ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService) {

        this.projectConnectionService = projectConnectionService;
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;
        logView = new LogView();
        logView.logMessage(Constants.INFO, "Starting RosettaMappingView");

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.ROSETTA_MAPPING.equals(projectParameter.getNamespace()))
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
            }  else if (Constants.BRAND_TABLE.equals(projectParameter.getName())) {
                brandTable = projectParameter.getValue();
            } else if (Constants.KPI_TABLE.equals(projectParameter.getName())) {
                kPITable = projectParameter.getValue();
            } else if (Constants.CHANNEL_TABLE.equals(projectParameter.getName())) {
                channelTable = projectParameter.getValue();
            } else if (Constants.PARTNER_TABLE.equals(projectParameter.getName())) {
                partnerTable = projectParameter.getValue();
            } else if (Constants.PAYMENTTYPE_TABLE.equals(projectParameter.getName())){
                paymentTypeTable = projectParameter.getValue();
            } else if (Constants.PRODUCTTLN_TABLE.equals(projectParameter.getName())) {
                productTLNTable = projectParameter.getValue();
            } else if (Constants.PRODUCTUSG_TABLE.equals(projectParameter.getName())){
                productUSGTable = projectParameter.getValue();
            }  else if (Constants.PRODUCTUSGDIRECTION_TABLE.equals(projectParameter.getName())){
                productUSGDirectionTable = projectParameter.getValue();
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

        logView.logMessage(Constants.INFO, "Ending RosettaMappingViewF");
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

        tabSheet.add("Mapping", getMappingAllTabs());
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
    private Component getMappingAllTabs() {
        logView.logMessage(Constants.INFO, "Sarting getUpladTab() for set upload data");
        VerticalLayout content = new VerticalLayout();

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Brand",getBrandGrid());
        tabSheet.add("Channel", getChannelGrid());
        tabSheet.add("KPI", getKPIGrid());
        tabSheet.add("Partner", getPartnerGrid());
        tabSheet.add("PaymentType", getPaymentTypeGrid());
        tabSheet.add("Product_TLN", getProductTLNGrid());
        tabSheet.add("Product_USG", getProductUSGGrid());
        tabSheet.add("UsageDirection", getUsageDirectionGrid());

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();
        tabSheet.addThemeVariants(TabSheetVariant.MATERIAL_BORDERED);

        content.setSizeFull();
        content.setHeightFull();

        content.add(tabSheet);

        logView.logMessage(Constants.INFO, "Ending getUpladTab() for set upload data");
        return content;
    }

    private Component getBrandGrid() {
        logView.logMessage(Constants.INFO, "Starting getMGSRGrid() for get MGSRGrid");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        brandCrud = new Crud<>(RosettaBrand.class, createBrandEditor());
        brandCrud.setToolbarVisible(true);
        brandCrud.setHeightFull();
        brandCrud.setSizeFull();
        setupBrandGrid();
        updateBrandGrid();
        brandGrid.addItemDoubleClickListener(event -> {
            RosettaBrand  selectedEntity = event.getItem();
            brandCrud.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            brandCrud.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        brandCrud.addSaveListener(event -> {
            logView.logMessage(Constants.INFO, "executing crud.addSaveListener for save editedAttachment in Attachment grid");
            RosettaBrand rosettaBrand = event.getItem();
            String resultString = projectConnectionService.saveOrUpdateRosettaBrand(rosettaBrand, brandTable, dbUrl, dbUser, dbPassword);
            if (resultString.equals(Constants.OK)) {
                logView.logMessage(Constants.INFO, "update modified rosettaBrand data");
                Notification.show(" Update successfully", 2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updateBrandGrid();
            } else {
                logView.logMessage(Constants.ERROR, "Error while updating modified rosettaBrand data");
                Notification.show("Error during upload: " + resultString, 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        content.add(brandCrud);
        logView.logMessage(Constants.INFO, "Ending getMGSRGrid() for get MGSRGrid");
        return content;
    }

    private void updateBrandGrid() {
        List<RosettaBrand> listOfBrands = projectConnectionService.getRosettaBrands(brandTable, dbUrl, dbUser, dbPassword);
        if (listOfBrands != null) {
            GenericDataProvider dataProvider = new GenericDataProvider(listOfBrands, "lfdNr");
            brandGrid.setDataProvider(dataProvider);
        }
    }

    private CrudEditor<RosettaBrand> createBrandEditor() {
        logView.logMessage(Constants.INFO, "createMGSREditor() for create Editor");
        FormLayout editForm = new FormLayout();
        Binder<RosettaBrand> binder = new Binder<>(RosettaBrand.class);

        // Create fields
        IntegerField lfdNrField = new IntegerField("LfdNr");
        lfdNrField.setReadOnly(true);
        TextField rosettaBrandField = new TextField("Rosetta Brand");
        TextField coOneSPSField = new TextField("CoOne SPS");
        TextField userField = new TextField("User");
      //  userField.setReadOnly(true);
        // Bind fields to Binder
        binder.forField(lfdNrField).asRequired()
                .bind(RosettaBrand::getLfdNr, RosettaBrand::setLfdNr);

        binder.forField(rosettaBrandField)
                .bind(RosettaBrand::getRosettaBrand, RosettaBrand::setRosettaBrand);

        binder.forField(coOneSPSField)
                .bind(RosettaBrand::getCoOneSPS, RosettaBrand::setCoOneSPS);

//        binder.forField(userField)
//                .bind(RosettaBrand::getUser, RosettaBrand::setUser);

        // Add fields to FormLayout
        editForm.add(lfdNrField, rosettaBrandField, coOneSPSField );

        // Return BinderCrudEditor with the Binder and FormLayout
        return new BinderCrudEditor<>(binder, editForm);
    }


    private void setupBrandGrid() {
        logView.logMessage(Constants.INFO, "Starting setupBrandGrid() for setup BrandGrid");

        String LFDNR = "lfdNr";
        String ROSETTA_BRAND = "rosettaBrand";
        String CO_ONE_SPS = "coOneSPS";
        String USER = "user";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        brandGrid = brandCrud.getGrid();

        brandGrid.getColumnByKey(LFDNR).setHeader("LfdNr").setWidth("80px").setFlexGrow(0).setResizable(true);
        brandGrid.getColumnByKey(ROSETTA_BRAND).setHeader("Rosetta Brand").setWidth("200px").setFlexGrow(0).setResizable(true);
        brandGrid.getColumnByKey(CO_ONE_SPS).setHeader("CoOne SPS").setWidth("200px").setFlexGrow(0).setResizable(true);
        brandGrid.getColumnByKey(USER).setHeader("User").setWidth("120px").setFlexGrow(0).setResizable(true);

        brandGrid.getColumns().forEach(col -> col.setAutoWidth(true));

        brandGrid.removeColumn(brandGrid.getColumnByKey(EDIT_COLUMN));
        brandGrid.removeColumn(brandGrid.getColumnByKey(USER));

        // Reorder the columns
        brandGrid.setColumnOrder(brandGrid.getColumnByKey(LFDNR),
                brandGrid.getColumnByKey(ROSETTA_BRAND),
                brandGrid.getColumnByKey(CO_ONE_SPS));
            //    brandGrid.getColumnByKey(USER));

        brandGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        logView.logMessage(Constants.INFO, "Ending setupBrandGrid() for setup BrandGrid");
    }


    private Component getChannelGrid() {
        logView.logMessage(Constants.INFO, "Starting getChannelGrid() for get ChannelGrid");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        channelCrud = new Crud<>(RosettaChannel.class, createChannelEditor());
        channelCrud.setToolbarVisible(true);
        channelCrud.setHeightFull();
        channelCrud.setSizeFull();
        setupChannelGrid();
        updateChannelGrid();
        channelGrid.addItemDoubleClickListener(event -> {
            RosettaChannel  selectedEntity = event.getItem();
            channelCrud.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            channelCrud.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        channelCrud.addSaveListener(event -> {
            logView.logMessage(Constants.INFO, "executing crud.addSaveListener for save editedAttachment in Attachment grid");
            RosettaChannel rosettaChannel = event.getItem();
            String resultString = projectConnectionService.saveOrUpdateRosettaChannel(rosettaChannel, channelTable, dbUrl, dbUser, dbPassword);
            if (resultString.equals(Constants.OK)) {
                logView.logMessage(Constants.INFO, "save rosettaChannel data");
                Notification.show(" Update successfully", 2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updateChannelGrid();
            } else {
                logView.logMessage(Constants.ERROR, "Error while saving rosettaChannel data");
                Notification.show("Error during upload: " + resultString, 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        content.add(channelCrud);
        logView.logMessage(Constants.INFO, "Ending getChannelGrid() for get ChannelGrid");
        return content;
    }

    private void updateChannelGrid() {
        List<RosettaChannel> listOfdata = projectConnectionService.getRosettaChannels(channelTable, dbUrl, dbUser, dbPassword);
        if (listOfdata != null) {

            GenericDataProvider dataProvider = new GenericDataProvider(listOfdata, "lfdNr");
            channelGrid.setDataProvider(dataProvider);
        }
    }

    private CrudEditor<RosettaChannel> createChannelEditor() {
        logView.logMessage(Constants.INFO, "createChannelEditor() for create Editor");
        FormLayout editForm = new FormLayout();
        Binder<RosettaChannel> binder = new Binder<>(RosettaChannel.class);

        // Create fields
        IntegerField lfdNrField = new IntegerField("LfdNr");
        lfdNrField.setReadOnly(true);
        TextField rosettaChannelField = new TextField("Rosetta Channel");
        TextField coOneChannelField = new TextField("CoOne Channel");
        TextField userField = new TextField("User");

        // Bind fields to Binder
        binder.forField(lfdNrField) .asRequired()
                .bind(RosettaChannel::getLfdNr, RosettaChannel::setLfdNr);

        binder.forField(rosettaChannelField)
                .bind(RosettaChannel::getRosettaChannel, RosettaChannel::setRosettaChannel);

        binder.forField(coOneChannelField)
                .bind(RosettaChannel::getCoOneChannel, RosettaChannel::setCoOneChannel);

        binder.forField(userField)
                .bind(RosettaChannel::getUser, RosettaChannel::setUser);

        // Add fields to FormLayout
        editForm.add(lfdNrField, rosettaChannelField, coOneChannelField);

        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupChannelGrid() {
        logView.logMessage(Constants.INFO, "Starting setupChannelGrid() for setup ChannelGrid");

        String LFDNR = "lfdNr";
        String ROSETTA_CHANNEL = "rosettaChannel";
        String CO_ONE_CHANNEL = "coOneChannel";
        String USER = "user";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        channelGrid = channelCrud.getGrid();

        channelGrid.getColumnByKey(LFDNR).setHeader("LfdNr").setWidth("80px").setFlexGrow(0).setResizable(true);
        channelGrid.getColumnByKey(ROSETTA_CHANNEL).setHeader("Rosetta Channel").setWidth("200px").setFlexGrow(0).setResizable(true);
        channelGrid.getColumnByKey(CO_ONE_CHANNEL).setHeader("CoOne Channel").setWidth("200px").setFlexGrow(0).setResizable(true);
        channelGrid.getColumnByKey(USER).setHeader("User").setWidth("120px").setFlexGrow(0).setResizable(true);

        channelGrid.getColumns().forEach(col -> col.setAutoWidth(true));

        channelGrid.removeColumnByKey(EDIT_COLUMN);
        channelGrid.removeColumn(channelGrid.getColumnByKey(USER));

        // Reorder the columns
        channelGrid.setColumnOrder(channelGrid.getColumnByKey(LFDNR),
                channelGrid.getColumnByKey(ROSETTA_CHANNEL),
                channelGrid.getColumnByKey(CO_ONE_CHANNEL));
              //  channelGrid.getColumnByKey(USER));

        channelGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        logView.logMessage(Constants.INFO, "Ending setupChannelGrid() for setup ChannelGrid");
    }


    private Component getKPIGrid() {
        logView.logMessage(Constants.INFO, "Starting getKPIGrid() for get KPIGrid");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        kpiCrud = new Crud<>(RosettaKPI.class, createKPIEditor());
        kpiCrud.setToolbarVisible(true);
        kpiCrud.setHeightFull();
        kpiCrud.setSizeFull();
        setupKPIGrid();
        updateKPIGrid();
        kpiGrid.addItemDoubleClickListener(event -> {
            RosettaKPI  selectedEntity = event.getItem();
            kpiCrud.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            kpiCrud.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        kpiCrud.addSaveListener(event -> {
            logView.logMessage(Constants.INFO, "executing crud.addSaveListener for save row in grid");
            RosettaKPI rosettaKPI = event.getItem();
            String resultString = projectConnectionService.saveOrUpdateRosettaKPI(rosettaKPI, kPITable, dbUrl, dbUser, dbPassword);
            if (resultString.equals(Constants.OK)) {
                logView.logMessage(Constants.INFO, "save rosettaKPI data");
                Notification.show(" Update successfully", 2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updateKPIGrid();
            } else {
                logView.logMessage(Constants.ERROR, "Error while saving rosettaKPI data");
                Notification.show("Error during upload: " + resultString, 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        content.add(kpiCrud);
        logView.logMessage(Constants.INFO, "Ending getKPIGrid() for get KPIGrid");
        return content;
    }
    private void updateKPIGrid() {
        List<RosettaKPI> listOfdata = projectConnectionService.getRosettaKPIs(kPITable, dbUrl, dbUser, dbPassword);
        if (listOfdata != null) {
            GenericDataProvider dataProvider = new GenericDataProvider(listOfdata, "lfdNr");
            kpiGrid.setDataProvider(dataProvider);
        }

    }
    private CrudEditor<RosettaKPI> createKPIEditor() {
        logView.logMessage(Constants.INFO, "createKPIEditor() for create Editor");
        FormLayout editForm = new FormLayout();
        Binder<RosettaKPI> binder = new Binder<>(RosettaKPI.class);

        // Create fields
        IntegerField lfdNrField = new IntegerField("LfdNr");
        lfdNrField.setReadOnly(true);
        TextField rosettaKPIField = new TextField("Rosetta KPI");
        TextField coOneMeasureField = new TextField("CoOne Measure");
        TextField userField = new TextField("User");

        // Bind fields to Binder
        binder.forField(lfdNrField) .asRequired()
                .bind(RosettaKPI::getLfdNr, RosettaKPI::setLfdNr);

        binder.forField(rosettaKPIField)
                .bind(RosettaKPI::getRosettaKPI, RosettaKPI::setRosettaKPI);

        binder.forField(coOneMeasureField)
                .bind(RosettaKPI::getCoOneMeasure, RosettaKPI::setCoOneMeasure);

        binder.forField(userField)
                .bind(RosettaKPI::getUser, RosettaKPI::setUser);

        // Add fields to FormLayout
        editForm.add(lfdNrField, rosettaKPIField, coOneMeasureField);

        // Return BinderCrudEditor with the Binder and FormLayout
        return new BinderCrudEditor<>(binder, editForm);
    }


    private void setupKPIGrid() {
        logView.logMessage(Constants.INFO, "Starting setupKPIGrid() for setup KPIGrid");

        String LFDNR = "lfdNr";
        String ROSETTA_KPI = "rosettaKPI";
        String CO_ONE_MEASURE = "coOneMeasure";
        String USER = "user";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        kpiGrid = kpiCrud.getGrid();

        kpiGrid.getColumnByKey(LFDNR).setHeader("LfdNr").setWidth("80px").setFlexGrow(0).setResizable(true);
        kpiGrid.getColumnByKey(ROSETTA_KPI).setHeader("Rosetta KPI").setWidth("200px").setFlexGrow(0).setResizable(true);
        kpiGrid.getColumnByKey(CO_ONE_MEASURE).setHeader("CoOne Measure").setWidth("200px").setFlexGrow(0).setResizable(true);
        kpiGrid.getColumnByKey(USER).setHeader("User").setWidth("120px").setFlexGrow(0).setResizable(true);

        kpiGrid.getColumns().forEach(col -> col.setAutoWidth(true));

        kpiGrid.removeColumnByKey(EDIT_COLUMN);
        kpiGrid.removeColumn(kpiGrid.getColumnByKey(USER));

        // Reorder the columns
        kpiGrid.setColumnOrder(kpiGrid.getColumnByKey(LFDNR),
                kpiGrid.getColumnByKey(ROSETTA_KPI),
                kpiGrid.getColumnByKey(CO_ONE_MEASURE));
           //     kpiGrid.getColumnByKey(USER));

        kpiGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        logView.logMessage(Constants.INFO, "Ending setupKPIGrid() for setup KPIGrid");
    }

    private Component getPartnerGrid() {
        logView.logMessage(Constants.INFO, "Starting getPartnerGrid() for get PartnerGrid");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        partnerCrud = new Crud<>(RosettaPartner.class, createPartnerEditor());
        partnerCrud.setToolbarVisible(true);
        partnerCrud.setHeightFull();
        partnerCrud.setSizeFull();
        setupPartnerGrid();
        updatePartnerGrid();

        partnerGrid.addItemDoubleClickListener(event -> {
            RosettaPartner  selectedEntity = event.getItem();
            partnerCrud.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            partnerCrud.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        partnerCrud.addSaveListener(event -> {
            logView.logMessage(Constants.INFO, "executing crud.addSaveListener for save row in grid");
            RosettaPartner rosettaPartner = event.getItem();
            String resultString = projectConnectionService.saveOrUpdateRosettaPartner(rosettaPartner, partnerTable, dbUrl, dbUser, dbPassword);
            if (resultString.equals(Constants.OK)) {
                logView.logMessage(Constants.INFO, "save rosettaPartner data");
                Notification.show(" Update successfully", 2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updatePartnerGrid();
            } else {
                logView.logMessage(Constants.ERROR, "Error while saving rosettaPartner data");
                Notification.show("Error during upload: " + resultString, 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        content.add(partnerCrud);
        logView.logMessage(Constants.INFO, "Ending getPartnerGrid() for get PartnerGrid");
        return content;
    }

    private void updatePartnerGrid() {
        List<RosettaPartner> listOfdata = projectConnectionService.getRosettaPartners(partnerTable, dbUrl, dbUser, dbPassword);
        if (listOfdata != null) {
            GenericDataProvider dataProvider = new GenericDataProvider(listOfdata, "lfdNr");
            partnerCrud.setDataProvider(dataProvider);
        }
    }
    private CrudEditor<RosettaPartner> createPartnerEditor() {
        logView.logMessage(Constants.INFO, "createPartnerEditor() for create Editor");
        FormLayout editForm = new FormLayout();
        Binder<RosettaPartner> binder = new Binder<>(RosettaPartner.class);

        // Create fields
        IntegerField lfdNrField = new IntegerField("LfdNr");
        lfdNrField.setReadOnly(true);
        TextField rosettaPartnerField = new TextField("Rosetta Partner");
        TextField coOneSPSField = new TextField("CoOne SPS");
        TextField coOnePaymentTypeField = new TextField("CoOne Payment Type");
        TextField userField = new TextField("User");

        // Bind fields to Binder
        binder.forField(lfdNrField) .asRequired()
                .bind(RosettaPartner::getLfdNr, RosettaPartner::setLfdNr);

        binder.forField(rosettaPartnerField)
                .bind(RosettaPartner::getRosettaPartner, RosettaPartner::setRosettaPartner);

        binder.forField(coOneSPSField)
                .bind(RosettaPartner::getCoOneSPS, RosettaPartner::setCoOneSPS);

        binder.forField(coOnePaymentTypeField)
                .bind(RosettaPartner::getCoOnePaymentType, RosettaPartner::setCoOnePaymentType);

        binder.forField(userField)
                .bind(RosettaPartner::getUser, RosettaPartner::setUser);

        // Add fields to FormLayout
        editForm.add(lfdNrField, rosettaPartnerField, coOneSPSField, coOnePaymentTypeField);

        // Return BinderCrudEditor with the Binder and FormLayout
        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupPartnerGrid() {
        logView.logMessage(Constants.INFO, "Starting setupPartnerGrid() for setup PartnerGrid");

        String LFDNR = "lfdNr";
        String ROSETTA_PARTNER = "rosettaPartner";
        String CO_ONE_SPS = "coOneSPS";
        String CO_ONE_PAYMENT_TYPE = "coOnePaymentType";
        String USER = "user";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        partnerGrid = partnerCrud.getGrid();

        partnerGrid.getColumnByKey(LFDNR).setHeader("LfdNr").setWidth("80px").setFlexGrow(0).setResizable(true);
        partnerGrid.getColumnByKey(ROSETTA_PARTNER).setHeader("Rosetta Partner").setWidth("200px").setFlexGrow(0).setResizable(true);
        partnerGrid.getColumnByKey(CO_ONE_SPS).setHeader("CoOne SPS").setWidth("200px").setFlexGrow(0).setResizable(true);
        partnerGrid.getColumnByKey(CO_ONE_PAYMENT_TYPE).setHeader("CoOne Payment Type").setWidth("200px").setFlexGrow(0).setResizable(true);
        partnerGrid.getColumnByKey(USER).setHeader("User").setWidth("120px").setFlexGrow(0).setResizable(true);

        partnerGrid.getColumns().forEach(col -> col.setAutoWidth(true));

        partnerGrid.removeColumnByKey(EDIT_COLUMN);
        partnerGrid.removeColumn(partnerGrid.getColumnByKey(USER));

        // Reorder the columns
        partnerGrid.setColumnOrder(partnerGrid.getColumnByKey(LFDNR),
                partnerGrid.getColumnByKey(ROSETTA_PARTNER),
                partnerGrid.getColumnByKey(CO_ONE_SPS),
                partnerGrid.getColumnByKey(CO_ONE_PAYMENT_TYPE));
            //    partnerGrid.getColumnByKey(USER));

        partnerGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        logView.logMessage(Constants.INFO, "Ending setupPartnerGrid() for setup PartnerGrid");
    }

    private Component getPaymentTypeGrid() {
        logView.logMessage(Constants.INFO, "Starting getPaymentTypeGrid() for get PaymentTypeGrid");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        paymentTypeCrud = new Crud<>(RosettaPaymentType.class, createPaymentTypeEditor());
        paymentTypeCrud.setToolbarVisible(true);
        paymentTypeCrud.setHeightFull();
        paymentTypeCrud.setSizeFull();
        setupPaymentTypeGrid();
        updatePaymentTypeGrid();
        paymentTypeGrid.addItemDoubleClickListener(event -> {
            RosettaPaymentType  selectedEntity = event.getItem();
            paymentTypeCrud.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            paymentTypeCrud.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        paymentTypeCrud.addSaveListener(event -> {
            logView.logMessage(Constants.INFO, "executing crud.addSaveListener for save row in grid");
            RosettaPaymentType rosettaPaymentType = event.getItem();
            String resultString = projectConnectionService.saveOrUpdateRosettaPaymentType(rosettaPaymentType, paymentTypeTable, dbUrl, dbUser, dbPassword);
            if (resultString.equals(Constants.OK)) {
                logView.logMessage(Constants.INFO, "save rosettaPaymentType data");
                Notification.show(" Update successfully", 2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updatePaymentTypeGrid();
            } else {
                logView.logMessage(Constants.ERROR, "Error while saving rosettaPaymentType data");
                Notification.show("Error during upload: " + resultString, 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        content.add(paymentTypeCrud);
        logView.logMessage(Constants.INFO, "Ending getPaymentTypeGrid() for get PaymentTypeGrid");
        return content;
    }

    private void updatePaymentTypeGrid() {
        List<RosettaPaymentType> listOfdata = projectConnectionService.getRosettaPaymentTypes(paymentTypeTable, dbUrl, dbUser, dbPassword);
        if (listOfdata != null) {
            GenericDataProvider dataProvider = new GenericDataProvider(listOfdata, "lfdNr");
            paymentTypeGrid.setDataProvider(dataProvider);
        }
    }

    private CrudEditor<RosettaPaymentType> createPaymentTypeEditor() {
        logView.logMessage(Constants.INFO, "createPaymentTypeEditor() for create Editor");
        FormLayout editForm = new FormLayout();
        Binder<RosettaPaymentType> binder = new Binder<>(RosettaPaymentType.class);

        // Create fields
        IntegerField lfdNrField = new IntegerField("LfdNr");
        lfdNrField.setReadOnly(true);
        TextField rosettaPaymentTypeField = new TextField("Rosetta Payment Type");
        TextField coOnePaymentTypeField = new TextField("CoOne Payment Type");
        TextField userField = new TextField("User");

        // Bind fields to Binder
        binder.forField(lfdNrField) .asRequired()
                .bind(RosettaPaymentType::getLfdNr, RosettaPaymentType::setLfdNr  );

        binder.forField(rosettaPaymentTypeField)
                .bind(RosettaPaymentType::getRosettaPaymentType, RosettaPaymentType::setRosettaPaymentType);

        binder.forField(coOnePaymentTypeField)
                .bind(RosettaPaymentType::getCoOnePaymentType, RosettaPaymentType::setCoOnePaymentType);

        binder.forField(userField)
                .bind(RosettaPaymentType::getUser, RosettaPaymentType::setUser);

        // Add fields to FormLayout
        editForm.add(lfdNrField, rosettaPaymentTypeField, coOnePaymentTypeField);

        // Return BinderCrudEditor with the Binder and FormLayout
        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupPaymentTypeGrid() {
        logView.logMessage(Constants.INFO, "Starting setupPaymentTypeGrid() for setup PaymentTypeGrid");

        String LFDNR = "lfdNr";
        String ROSETTA_PAYMENT_TYPE = "rosettaPaymentType";
        String CO_ONE_PAYMENT_TYPE = "coOnePaymentType";
        String USER = "user";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        paymentTypeGrid = paymentTypeCrud.getGrid();

        paymentTypeGrid.getColumnByKey(LFDNR).setHeader("LfdNr").setWidth("80px").setFlexGrow(0).setResizable(true);
        paymentTypeGrid.getColumnByKey(ROSETTA_PAYMENT_TYPE).setHeader("Rosetta Payment Type").setWidth("200px").setFlexGrow(0).setResizable(true);
        paymentTypeGrid.getColumnByKey(CO_ONE_PAYMENT_TYPE).setHeader("CoOne Payment Type").setWidth("200px").setFlexGrow(0).setResizable(true);
        paymentTypeGrid.getColumnByKey(USER).setHeader("User").setWidth("120px").setFlexGrow(0).setResizable(true);

        paymentTypeGrid.getColumns().forEach(col -> col.setAutoWidth(true));

        paymentTypeGrid.removeColumnByKey(EDIT_COLUMN);
        paymentTypeGrid.removeColumn(paymentTypeGrid.getColumnByKey(USER));

        // Reorder the columns
        paymentTypeGrid.setColumnOrder(paymentTypeGrid.getColumnByKey(LFDNR),
                paymentTypeGrid.getColumnByKey(ROSETTA_PAYMENT_TYPE),
                paymentTypeGrid.getColumnByKey(CO_ONE_PAYMENT_TYPE));
             //   paymentTypeGrid.getColumnByKey(USER));

        paymentTypeGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        logView.logMessage(Constants.INFO, "Ending setupPaymentTypeGrid() for setup PaymentTypeGrid");
    }

    private Component getProductTLNGrid() {
        logView.logMessage(Constants.INFO, "Starting getProductTLNGrid() for get ProductTLNGrid");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        productTLNCrud = new Crud<>(RosettaProductTLN.class, createProductTLNEditor());
        productTLNCrud.setToolbarVisible(true);
        productTLNCrud.setHeightFull();
        productTLNCrud.setSizeFull();
        setupProductTLNGrid();
        updateProductTLNGrid();
        productTLNGrid.addItemDoubleClickListener(event -> {
            RosettaProductTLN  selectedEntity = event.getItem();
            productTLNCrud.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            productTLNCrud.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        productTLNCrud.addSaveListener(event -> {
            logView.logMessage(Constants.INFO, "executing crud.addSaveListener for save row in grid");
            RosettaProductTLN rosettaProductTLN = event.getItem();
            String resultString = projectConnectionService.saveOrUpdateRosettaProductTLN(rosettaProductTLN, productTLNTable, dbUrl, dbUser, dbPassword);
            if (resultString.equals(Constants.OK)) {
                logView.logMessage(Constants.INFO, "save rosettaProductTLN data");
                Notification.show(" Update successfully", 2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updateProductTLNGrid();
            } else {
                logView.logMessage(Constants.ERROR, "Error while saving rosettaProductTLN data");
                Notification.show("Error during upload: " + resultString, 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });


        content.add(productTLNCrud);
        logView.logMessage(Constants.INFO, "Ending getProductTLNGrid() for get ProductTLNGrid");
        return content;
    }

    private void updateProductTLNGrid() {
        List<RosettaProductTLN> listOfdata = projectConnectionService.getRosettaProductTLNs(productTLNTable, dbUrl, dbUser, dbPassword);
        if (listOfdata != null) {
            GenericDataProvider dataProvider = new GenericDataProvider(listOfdata, "lfdNr");
            productTLNGrid.setDataProvider(dataProvider);
        }
    }
    private CrudEditor<RosettaProductTLN> createProductTLNEditor() {
        logView.logMessage(Constants.INFO, "createProductTLNEditor() for create Editor");
        FormLayout editForm = new FormLayout();
        Binder<RosettaProductTLN> binder = new Binder<>(RosettaProductTLN.class);

        // Create fields
        IntegerField lfdNrField = new IntegerField("LfdNr");
        lfdNrField.setReadOnly(true);
        TextField rosettaProductField = new TextField("Rosetta Product");
        TextField coOneContractTypeField = new TextField("CoOne Contract Type");
        TextField userField = new TextField("User");

        // Bind fields to Binder
        binder.forField(lfdNrField) .asRequired()
                .bind(RosettaProductTLN::getLfdNr, RosettaProductTLN::setLfdNr);

        binder.forField(rosettaProductField)
                .bind(RosettaProductTLN::getRosettaProduct, RosettaProductTLN::setRosettaProduct);

        binder.forField(coOneContractTypeField)
                .bind(RosettaProductTLN::getCoOneContractType, RosettaProductTLN::setCoOneContractType);

        binder.forField(userField)
                .bind(RosettaProductTLN::getUser, RosettaProductTLN::setUser);

        // Add fields to FormLayout
        editForm.add(lfdNrField, rosettaProductField, coOneContractTypeField);

        // Return BinderCrudEditor with the Binder and FormLayout
        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupProductTLNGrid() {
        logView.logMessage(Constants.INFO, "Starting setupProductTLNGrid() for setup ProductTLNGrid");

        String LFDNR = "lfdNr";
        String ROSETTA_PRODUCT = "rosettaProduct";
        String CO_ONE_CONTRACT_TYPE = "coOneContractType";
        String USER = "user";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        productTLNGrid = productTLNCrud.getGrid();

        productTLNGrid.getColumnByKey(LFDNR).setHeader("LfdNr").setWidth("80px").setFlexGrow(0).setResizable(true);
        productTLNGrid.getColumnByKey(ROSETTA_PRODUCT).setHeader("Rosetta Product").setWidth("200px").setFlexGrow(0).setResizable(true);
        productTLNGrid.getColumnByKey(CO_ONE_CONTRACT_TYPE).setHeader("CoOne Contract Type").setWidth("200px").setFlexGrow(0).setResizable(true);
        productTLNGrid.getColumnByKey(USER).setHeader("User").setWidth("120px").setFlexGrow(0).setResizable(true);

        productTLNGrid.getColumns().forEach(col -> col.setAutoWidth(true));

        productTLNGrid.removeColumnByKey(EDIT_COLUMN);
        productTLNGrid.removeColumn(productTLNGrid.getColumnByKey(USER));

        // Reorder the columns
        productTLNGrid.setColumnOrder(productTLNGrid.getColumnByKey(LFDNR),
                productTLNGrid.getColumnByKey(ROSETTA_PRODUCT),
                productTLNGrid.getColumnByKey(CO_ONE_CONTRACT_TYPE));
            //    productTLNGrid.getColumnByKey(USER));

        productTLNGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        logView.logMessage(Constants.INFO, "Ending setupProductTLNGrid() for setup ProductTLNGrid");
    }

    private Component getProductUSGGrid() {
        logView.logMessage(Constants.INFO, "Starting getProductUSGGrid() for get ProductUSGGrid");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        productUSGCrud = new Crud<>(RosettaProductUSG.class, createProductUSGEditor());
        productUSGCrud.setToolbarVisible(true);
        productUSGCrud.setHeightFull();
        productUSGCrud.setSizeFull();
        setupProductUSGGrid();
        updateProductUSGGrid();
        productUSGGrid.addItemDoubleClickListener(event -> {
            RosettaProductUSG  selectedEntity = event.getItem();
            productUSGCrud.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            productUSGCrud.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        productUSGCrud.addSaveListener(event -> {
            logView.logMessage(Constants.INFO, "executing crud.addSaveListener for save row in grid");
            RosettaProductUSG rosettaProductUSG = event.getItem();
            String resultString = projectConnectionService.saveOrUpdateRosettaProductUSG(rosettaProductUSG, productUSGTable, dbUrl, dbUser, dbPassword);
            if (resultString.equals(Constants.OK)) {
                logView.logMessage(Constants.INFO, "save rosettaProductUSG data");
                Notification.show(" Update successfully", 2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updateProductUSGGrid();
            } else {
                logView.logMessage(Constants.ERROR, "Error while saving rosettaProductUSG data");
                Notification.show("Error during upload: " + resultString, 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        content.add(productUSGCrud);
        logView.logMessage(Constants.INFO, "Ending getProductUSGGrid() for get ProductUSGGrid");
        return content;
    }

    private void updateProductUSGGrid() {
        List<RosettaProductUSG> listOfdata = projectConnectionService.getRosettaProductUSGs(productUSGTable, dbUrl, dbUser, dbPassword);
        if (listOfdata != null) {
            GenericDataProvider dataProvider = new GenericDataProvider(listOfdata, "lfdNr");
            productUSGGrid.setDataProvider(dataProvider);
        }
    }
    public CrudEditor<RosettaProductUSG> createProductUSGEditor() {
        logView.logMessage(Constants.INFO, "createProductUSGEditor() for create Editor");
        FormLayout editForm = new FormLayout();
        Binder<RosettaProductUSG> binder = new Binder<>(RosettaProductUSG.class);

        // Create fields
        IntegerField lfdNrField = new IntegerField("LfdNr");
        lfdNrField.setReadOnly(true);
        TextField rosettaProductField = new TextField("Rosetta Product");
        TextField coOneMeasureField = new TextField("CoOne Measure");
        TextField userField = new TextField("User");

        // Bind fields to Binder
        binder.forField(lfdNrField) .asRequired()
                .bind(RosettaProductUSG::getLfdNr, RosettaProductUSG::setLfdNr);

        binder.forField(rosettaProductField)
                .bind(RosettaProductUSG::getRosettaProduct, RosettaProductUSG::setRosettaProduct);

        binder.forField(coOneMeasureField)
                .bind(RosettaProductUSG::getCoOneMeasure, RosettaProductUSG::setCoOneMeasure);

        binder.forField(userField)
                .bind(RosettaProductUSG::getUser, RosettaProductUSG::setUser);

        // Add fields to FormLayout
        editForm.add(lfdNrField, rosettaProductField, coOneMeasureField);

        // Return BinderCrudEditor with the Binder and FormLayout
        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupProductUSGGrid() {
        logView.logMessage(Constants.INFO, "Starting setupProductUSGGrid() for setup ProductUSGGrid");

        String LFDNR = "lfdNr";
        String ROSETTA_PRODUCT = "rosettaProduct";
        String CO_ONE_MEASURE = "coOneMeasure";
        String USER = "user";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        productUSGGrid = productUSGCrud.getGrid();

        productUSGGrid.getColumnByKey(LFDNR).setHeader("LfdNr").setWidth("80px").setFlexGrow(0).setResizable(true);
        productUSGGrid.getColumnByKey(ROSETTA_PRODUCT).setHeader("Rosetta Product").setWidth("200px").setFlexGrow(0).setResizable(true);
        productUSGGrid.getColumnByKey(CO_ONE_MEASURE).setHeader("CoOne Measure").setWidth("200px").setFlexGrow(0).setResizable(true);
        productUSGGrid.getColumnByKey(USER).setHeader("User").setWidth("120px").setFlexGrow(0).setResizable(true);

        productUSGGrid.getColumns().forEach(col -> col.setAutoWidth(true));

        productUSGGrid.removeColumnByKey(EDIT_COLUMN);
        productUSGGrid.removeColumn(productUSGGrid.getColumnByKey(USER));

        // Reorder the columns
        productUSGGrid.setColumnOrder(productUSGGrid.getColumnByKey(LFDNR),
                productUSGGrid.getColumnByKey(ROSETTA_PRODUCT),
                productUSGGrid.getColumnByKey(CO_ONE_MEASURE));
            //    productUSGGrid.getColumnByKey(USER));

        productUSGGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        logView.logMessage(Constants.INFO, "Ending setupProductUSGGrid() for setup ProductUSGGrid");
    }

    private Component getUsageDirectionGrid() {
        logView.logMessage(Constants.INFO, "Starting getUsageDirectionGrid() for get UsageDirectionGrid");
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        usageDirectionCrud = new Crud<>(RosettaUsageDirection.class, createUsageDirectionEditor());
        usageDirectionCrud.setToolbarVisible(true);
        usageDirectionCrud.setHeightFull();
        usageDirectionCrud.setSizeFull();
        setupUsageDirectionGrid();
        updateUsageDirectionGrid();
        usageDirectionGrid.addItemDoubleClickListener(event -> {
            RosettaUsageDirection  selectedEntity = event.getItem();
            usageDirectionCrud.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            usageDirectionCrud.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        usageDirectionCrud.addSaveListener(event -> {
            logView.logMessage(Constants.INFO, "executing crud.addSaveListener for save row in grid");
            RosettaUsageDirection usageDirection = event.getItem();
            String resultString = projectConnectionService.saveOrUpdateRosettaUsageDirection(usageDirection, productUSGDirectionTable, dbUrl, dbUser, dbPassword);
            if (resultString.equals(Constants.OK)) {
                logView.logMessage(Constants.INFO, "save usageDirection data");
                Notification.show(" Update successfully", 2000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updateUsageDirectionGrid();
            } else {
                logView.logMessage(Constants.ERROR, "Error while saving usageDirection data");
                Notification.show("Error during upload: " + resultString, 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        content.add(usageDirectionCrud);
        logView.logMessage(Constants.INFO, "Ending getUsageDirectionGrid() for get UsageDirectionGrid");
        return content;
    }

    private void updateUsageDirectionGrid() {
        List<RosettaUsageDirection> listOfdata = projectConnectionService.getRosettaUsageDirections(productUSGDirectionTable, dbUrl, dbUser, dbPassword);
        if (listOfdata != null) {
            GenericDataProvider dataProvider = new GenericDataProvider(listOfdata, "lfdNr");
            usageDirectionGrid.setDataProvider(dataProvider);
        }
    }
    public CrudEditor<RosettaUsageDirection> createUsageDirectionEditor() {
        logView.logMessage(Constants.INFO, "createUsageDirectionEditor() for create Editor");
        FormLayout editForm = new FormLayout();
        Binder<RosettaUsageDirection> binder = new Binder<>(RosettaUsageDirection.class);

        // Create fields
        IntegerField lfdNrField = new IntegerField("LfdNr");
        lfdNrField.setReadOnly(true);
        TextField rosettaUsageDirectionField = new TextField("Rosetta Usage Direction");
        TextField coOneUsageDirectionField = new TextField("CoOne Usage Direction");
        TextField userField = new TextField("User");

        // Bind fields to Binder
        binder.forField(lfdNrField) .asRequired()
                .bind(RosettaUsageDirection::getLfdNr, RosettaUsageDirection::setLfdNr);

        binder.forField(rosettaUsageDirectionField)
                .bind(RosettaUsageDirection::getRosettaUsageDirection, RosettaUsageDirection::setRosettaUsageDirection);

        binder.forField(coOneUsageDirectionField)
                .bind(RosettaUsageDirection::getCoOneUsageDirection, RosettaUsageDirection::setCoOneUsageDirection);

        binder.forField(userField)
                .bind(RosettaUsageDirection::getUser, RosettaUsageDirection::setUser);

        // Add fields to FormLayout
        editForm.add(lfdNrField, rosettaUsageDirectionField, coOneUsageDirectionField);

        // Return BinderCrudEditor with the Binder and FormLayout
        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupUsageDirectionGrid() {
        logView.logMessage(Constants.INFO, "Starting setupUsageDirectionGrid() for setup UsageDirectionGrid");

        String LFDNR = "lfdNr";
        String ROSETTA_USAGE_DIRECTION = "rosettaUsageDirection";
        String CO_ONE_USAGE_DIRECTION = "coOneUsageDirection";
        String USER = "user";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        usageDirectionGrid = usageDirectionCrud.getGrid();

        usageDirectionGrid.getColumnByKey(LFDNR).setHeader("LfdNr").setWidth("80px").setFlexGrow(0).setResizable(true);
        usageDirectionGrid.getColumnByKey(ROSETTA_USAGE_DIRECTION).setHeader("Rosetta Usage Direction").setWidth("200px").setFlexGrow(0).setResizable(true);
        usageDirectionGrid.getColumnByKey(CO_ONE_USAGE_DIRECTION).setHeader("CoOne Usage Direction").setWidth("200px").setFlexGrow(0).setResizable(true);
        usageDirectionGrid.getColumnByKey(USER).setHeader("User").setWidth("120px").setFlexGrow(0).setResizable(true);

        usageDirectionGrid.getColumns().forEach(col -> col.setAutoWidth(true));

        usageDirectionGrid.removeColumn(usageDirectionGrid.getColumnByKey(EDIT_COLUMN));
        usageDirectionGrid.removeColumn(usageDirectionGrid.getColumnByKey(USER));

        // Reorder the columns
        usageDirectionGrid.setColumnOrder(usageDirectionGrid.getColumnByKey(LFDNR),
                usageDirectionGrid.getColumnByKey(ROSETTA_USAGE_DIRECTION),
                usageDirectionGrid.getColumnByKey(CO_ONE_USAGE_DIRECTION));
            //    usageDirectionGrid.getColumnByKey(USER));

        usageDirectionGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        logView.logMessage(Constants.INFO, "Ending setupUsageDirectionGrid() for setup UsageDirectionGrid");
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
