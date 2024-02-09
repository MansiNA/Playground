package de.dbuss.tefcontrol.data.modules.hwmapping.view;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import de.dbuss.tefcontrol.components.DefaultUtils;
import de.dbuss.tefcontrol.components.LogView;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.CLTV_HW_Measures;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.view.B2POutlookFINView;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.Subscriber;
import de.dbuss.tefcontrol.data.service.ProjectAttachmentsService;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.data.service.ProjectsService;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.vaadin.addons.componentfactory.PivotTable;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@PageTitle("HWMapping")
@Route(value = "HW_Mapping/:project_Id", layout = MainLayout.class)
@RolesAllowed({"CLTV", "ADMIN"})
public class HWMapping extends VerticalLayout implements BeforeEnterObserver {

    private final ProjectParameterService projectParameterService;
    private final ProjectConnectionService projectConnectionService;
    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private List<CLTV_HW_Measures> listOfCLTVMeasures;
    private Crud<CLTV_HW_Measures> crud;
    private Grid<CLTV_HW_Measures> grid;
    private Checkbox checkbox;
    private HorizontalLayout hl;
    private Button downloadButton;
    private String exportPath;
    private List<CLTV_HW_Measures> fetchListOfCLTVMeasures;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    String dbServer = null;
    String dbName = null;

    private String agentName;
    private String sql_addMonths;
    private String tableName;
    private int projectId;
    private Boolean isVisible = false;
    Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);
    private Optional<Projects> projects;
    private DefaultUtils defaultUtils;
    private List<ProjectAttachmentsDTO> listOfProjectAttachments;
    private LogView logView;
    private Boolean isLogsVisible = false;

    public HWMapping(@Value("${csv_exportPath}") String p_exportPath, ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService, ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService) {

        this.exportPath = p_exportPath;
        this.projectConnectionService = projectConnectionService;
        this.projectParameterService = projectParameterService;
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;

        logView = new LogView();
        logView.logMessage(Constants.INFO, "Starting HWMapping");

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.HW_MAPPING.equals(projectParameter.getNamespace()))
                .collect(Collectors.toList());

        for (ProjectParameter projectParameter : filteredProjectParameters) {
         //   if(projectParameter.getNamespace().equals(Constants.HW_MAPPING)) {
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
                } else if (Constants.DB_JOBS.equals(projectParameter.getName())) {
                    agentName = projectParameter.getValue();
                }  else if (Constants.SQL_ADDMONTHS.equals(projectParameter.getName())) {
                    sql_addMonths = projectParameter.getValue();
                }
          //  }
        }
        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";
        //Text databaseDetail = new Text("Connected to: "+ dbServer+ ", Database: " + dbName+ ", Table Financials: " + financialsTableName + ", Table Subscriber: " + subscriberTableName+ ", Table Unitdeepdive: "+ unitTableName);
     //   Text databaseDetail = new Text("Connected to: "+ dbServer+ ", Database: " + dbName + " AgentJob: " + agentName);

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
        logView.logMessage(Constants.INFO, "Ending HWMapping");
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
        logView.logMessage(Constants.INFO, "Update Description in updateDescription()");
        defaultUtils.setProjectId(projectId);
        defaultUtils.setDescription();
    }
    private void updateAttachmentGrid(List<ProjectAttachmentsDTO> projectAttachmentsDTOS) {
        logView.logMessage(Constants.INFO, "Update Attachment in updateAttachmentGrid()");
        defaultUtils.setProjectId(projectId);
        defaultUtils.setAttachmentGridItems(projectAttachmentsDTOS);
    }
    private Component getUpladTab() {
        logView.logMessage(Constants.INFO, "Sarting getUpladTab() for set upload data");
        VerticalLayout content = new VerticalLayout();

        content.setSizeFull();
        content.setHeightFull();
        crud = new Crud<>(CLTV_HW_Measures.class, createEditor());
        listOfCLTVMeasures = new ArrayList<CLTV_HW_Measures>();

        downloadButton = new Button("Download");
        Button saveButton = new Button("Save");
        Button addMonthButton = new Button("add Month");
        Button startJobBtn = new Button("Start Essbase");
        startJobBtn.setEnabled(true);

        setupGrid();
        setUpDownloadButton();

        crud.setHeight("600px");

        HorizontalLayout horl = new HorizontalLayout();
        horl.setWidthFull();
        horl.add( saveButton, addMonthButton, downloadButton, startJobBtn);
        horl.setAlignItems(Alignment.CENTER);

        updateCLTVHWMeasuredataGrid();

        saveButton.addClickListener(clickEvent -> {
            logView.logMessage(Constants.INFO,"executing saveButton.addClickListener for Save data in DB");
            List<CLTV_HW_Measures>allItems = getDataProviderAllItems();

            Notification notification = Notification.show(allItems.size() + " Rows Uploaded start",2000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            String result = projectConnectionService.write2DB(allItems, dbUrl, dbUser,dbPassword, tableName);
            if (result.equals(Constants.OK)){
                notification = Notification.show(allItems.size() + " Rows Uploaded successfully",3000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            else
            {
                notification = Notification.show("Error during upload!",4000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            startJobBtn.setEnabled(true);
        });

        // Create a confirmation dialog for removing files
        Dialog confirmationDialog = new Dialog();
        confirmationDialog.setCloseOnOutsideClick(false);
        confirmationDialog.setCloseOnEsc(false);
        confirmationDialog.add(new Text("neuen Monat hinzufügen?"));
        Button addButton = new Button("Ja");
        Button cancelButton = new Button("Nein");
        addButton.getStyle().set("margin-right", "10px");

        addMonthButton.addClickListener(e -> {
            //ToDO Abfrage, ob wirklich ein neuer Monat hinzugefügt werden soll...
            confirmationDialog.open();
        });

        addButton.addClickListener(confirmEvent -> {
            log.info("executing confirmButton.addClickListener for delete selected file in Attachment tab");
            logView.logMessage(Constants.INFO,"executing addMonthButton.addClickListener for add months in DB");
            String message = projectConnectionService.addMonthsInCLTVHWMeasure(dbUrl, dbUser,dbPassword, sql_addMonths);
            if (message.equals(Constants.OK)) {
                Notification.show("added successfully", 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updateCLTVHWMeasuredataGrid();

            } else {
                Notification.show(message, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            confirmationDialog.close(); // Close the confirmation dialog
        });

        cancelButton.addClickListener(cancelEvent -> {
            log.info("executing cancelButton.addClickListener for cancel selected file in Attachment tab");
            confirmationDialog.close(); // Close the confirmation dialog
        });
        // Add buttons to the footer
        confirmationDialog.getFooter().add(cancelButton);
        confirmationDialog.getFooter().add(addButton);

        startJobBtn.addClickListener(e -> {
            logView.logMessage(Constants.INFO,"executing startJobBtn.addClickListener for start agent");
            String message = projectConnectionService.startAgent(projectId);
            if (!message.contains("Error")) {
                Notification.show(message, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show(message, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        checkbox = new Checkbox();
        checkbox.setLabel("Show Pivot");
        checkbox.addClickListener(e->{showPivot();});
        content.add(horl, crud, checkbox);

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

    private void updateCLTVHWMeasuredataGrid() {
        logView.logMessage(Constants.INFO, "updateCLTVHWMeasuredataGrid for fetch data from DB");
        try {
            // Perform fetch operations using the selected data source
            fetchListOfCLTVMeasures = projectConnectionService.getCLTVHWMeasuresData(tableName, dbUrl, dbUser, dbPassword);
            GenericDataProvider dataProvider = new GenericDataProvider(fetchListOfCLTVMeasures);

            crud.setDataProvider(dataProvider);
            setupDataProviderEvent();

         //   Notification notification = Notification.show(" Rows fetch successfully", 3000, Notification.Position.MIDDLE);
         //   notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (Exception e) {
            String errormessage = projectConnectionService.getErrorMessage();
            Notification notification = Notification.show("Error during fetch: " + errormessage, 4000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void setUpDownloadButton() {
        logView.logMessage(Constants.INFO, "Starting setUpDownloadButton() prepare excel file for export");
        String exportFileName = "HW_Mapping.xls";
        downloadButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        downloadButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        downloadButton.addClickListener(clickEvent -> {
            Notification.show("Exportiere Daten ");
            try {
                generateAndExportExcel(exportPath + exportFileName);

                File file = new File(exportPath + exportFileName);
                StreamResource streamResource = new StreamResource(file.getName(), () -> getStream(file));

                Anchor downloadLink = new Anchor(streamResource, file.getName());
                downloadLink.getElement().setAttribute("download", true);
                downloadLink.getElement().getStyle().set("display", "none");
                // Add the download link to the UI
                add(downloadLink);
                // Trigger the download link
                UI.getCurrent().getPage().executeJs("arguments[0].click()", downloadLink);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        logView.logMessage(Constants.INFO, "Ending setUpDownloadButton() prepare excel file for export");
    }
    public void generateAndExportExcel(String file) {
        logView.logMessage(Constants.INFO, "Starting generateAndExportExcel() generate excel file");
        try {
            // List<CLTV_HW_Measures> dataToExport = projectConnectionService.fetchDataFromDatabase(selectedDbName);
            List<CLTV_HW_Measures> dataToExport = getDataProviderAllItems();

            // Create a new Excel workbook and sheet using Apache POI
            Workbook workBook = new HSSFWorkbook();
            Sheet sheet1 = workBook.createSheet("Sheet1");

            // Create CellStyle for headers
            CellStyle cs = workBook.createCellStyle();
            Font f = workBook.createFont();
            f.setBold(true);
            f.setFontHeightInPoints((short) 12);
            cs.setFont(f);

            // Create headers in the first row
            Row headerRow = sheet1.createRow(0);
            String[] headers = {"id", "monat_id", "device", "measure_name", "channel", "value"};
            for (int i = 0; i < headers.length; i++) {
                Cell headerCell = headerRow.createCell(i);
                headerCell.setCellValue(headers[i]);
                headerCell.setCellStyle(cs);
            }

            // Fill data in subsequent rows
            int rowIndex = 1;
            for (CLTV_HW_Measures measure : dataToExport) {
                Row dataRow = sheet1.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(measure.getId());
                dataRow.createCell(1).setCellValue(measure.getMonat_ID());
                dataRow.createCell(2).setCellValue(measure.getDevice());
                dataRow.createCell(3).setCellValue(measure.getMeasure_Name());
                dataRow.createCell(4).setCellValue(measure.getChannel());
                dataRow.createCell(5).setCellValue(measure.getValue());
            }

            // Write the workbook to the specified file
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workBook.write(fileOut);
            }

            // Close the workbook
            workBook.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logView.logMessage(Constants.INFO, "Ending generateAndExportExcel() generate excel file");
    }

    private InputStream getStream(File file) {
        logView.logMessage(Constants.INFO, "Starting getStream() for excel file");
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        logView.logMessage(Constants.INFO, "Starting getStream() for excel file");
        return stream;
    }

    private CrudEditor<CLTV_HW_Measures> createEditor() {
        logView.logMessage(Constants.INFO, "Starting createEditor() for crude editor");
        TextField value = new TextField("Value");

        FormLayout editForm = new FormLayout(value);

        Binder<CLTV_HW_Measures> binder = new Binder<>(CLTV_HW_Measures.class);
        binder.forField(value).asRequired().bind(CLTV_HW_Measures::getValue,
                CLTV_HW_Measures::setValue);
        logView.logMessage(Constants.INFO, "Ending createEditor() for crude editor");
        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupGrid() {
        logView.logMessage(Constants.INFO, "Starting setupGrid() for crude editor");
        String ID = "id";
        String MONAT_ID = "monat_ID";
        String DEVICE = "device";
        String MEASURE_NAME = "measure_Name";
        String CHANNEL = "channel";
        String VALUE = "value";
        String EDIT_COLUMN = "vaadin-crud-edit-column";
        grid = crud.getGrid();

        grid.getColumnByKey(ID).setHeader("Id").setWidth("80px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(MONAT_ID).setHeader("Monat_ID").setWidth("200px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(DEVICE).setHeader("Device").setWidth("200px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(MEASURE_NAME).setHeader("Measure_Name").setWidth("80px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(CHANNEL).setHeader("Channel").setWidth("200px").setFlexGrow(0).setResizable(true);
        grid.getColumnByKey(VALUE).setHeader("Value").setWidth("150px").setFlexGrow(0).setResizable(true);

        grid.removeColumn(grid.getColumnByKey(ID));
        grid.removeColumn(grid.getColumnByKey(EDIT_COLUMN));
        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        // Reorder the columns (alphabetical by default)
        grid.setColumnOrder(grid.getColumnByKey(MONAT_ID), grid.getColumnByKey(DEVICE), grid.getColumnByKey(MEASURE_NAME), grid.getColumnByKey(CHANNEL)
                , grid.getColumnByKey(VALUE)
                );

        grid.addItemDoubleClickListener(event -> {
            CLTV_HW_Measures selectedEntity = event.getItem();
            crud.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            crud.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        crud.setToolbarVisible(false);
        logView.logMessage(Constants.INFO, "Ending setupGrid() for crude editor");
    }

    private void setupDataProviderEvent() {
        logView.logMessage(Constants.INFO, "Starting setupDataProviderEvent() for crude editor save, delete events");
        GenericDataProvider dataProvider = new GenericDataProvider(getDataProviderAllItems());

        crud.addDeleteListener(
                deleteEvent -> {dataProvider.delete(deleteEvent.getItem());
                    logView.logMessage(Constants.INFO,"Executing crud.addDeleteListener for crude editor delete events");
                    crud.setDataProvider(dataProvider);
                });
        crud.addSaveListener(
                saveEvent -> {
                    dataProvider.persist(saveEvent.getItem());
                    logView.logMessage(Constants.INFO,"Executing crud.addDeleteListener for crude editor save events");
                    crud.setDataProvider(dataProvider);
                });
        logView.logMessage(Constants.INFO, "Ending setupDataProviderEvent() for crude editor save, delete events");
    }

    private void showPivot() {
        logView.logMessage(Constants.INFO, "Staring showPivot() for pivot table");
        if(!checkbox.getValue())
        {
            remove(hl);
            return;
        }

        PivotTable.PivotData pivotData = new PivotTable.PivotData();
        pivotData.addColumn("Monat", String.class);
        pivotData.addColumn("Device", String.class);
        pivotData.addColumn("Measure", String.class);
        pivotData.addColumn("Channel", String.class);
        pivotData.addColumn("Wert", Double.class);
        // pivotData.addColumn("filled", Boolean.class);

        PivotTable.PivotOptions pivotOptions = new PivotTable.PivotOptions();
        pivotOptions.setCols("Monat");
        pivotOptions.setRows("Device","Measure","Channel");
        pivotOptions.setAggregator("Sum","Wert");

        fetchListOfCLTVMeasures.forEach(e->{
          //  System.out.println(e.getMonat_ID() + "|" + e.getDevice() + "|" + e.getMeasure_Name() + "|" +e.getChannel() + "|" + e.getValue());

            pivotData.addRow(e.getMonat_ID().toString(),e.getDevice(), e.getMeasure_Name(),e.getChannel(), Double.parseDouble(e.getValue()) );

        });

        //PivotTable table = new PivotTable(pivotData, pivotOptions, PivotTable.PivotMode.INTERACTIVE);
        PivotTable pivotTable = new PivotTable(pivotData, pivotOptions, PivotTable.PivotMode.NONINTERACTIVE);

        hl =new HorizontalLayout();
        hl.add(pivotTable);
        add(hl);

        // Damit Gesamtsummen (Totals) nicht angezeigt werden in styles.css einfügen:
        //  .pvtTotal, .pvtTotalLabel, .pvtGrandTotal {display: none}
        logView.logMessage(Constants.INFO, "Ending showPivot() for pivot table");
    }

    private List<CLTV_HW_Measures> getDataProviderAllItems() {
        logView.logMessage(Constants.INFO,"Starting getDataProviderAllItems for grid dataprovider list");
        DataProvider<CLTV_HW_Measures, Void> existDataProvider = (DataProvider<CLTV_HW_Measures, Void>) grid.getDataProvider();
        List<CLTV_HW_Measures> listOfCLTVMeasures = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        logView.logMessage(Constants.INFO,"Ending getDataProviderAllItems for grid dataprovider list");
        return listOfCLTVMeasures;
    }
}
