package de.dbuss.tefcontrol.views.defaultPackage;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;

import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.data.binder.Binder;

import com.vaadin.flow.data.renderer.NativeButtonRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.wontlost.ckeditor.Config;
import com.wontlost.ckeditor.Constants;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;
import de.dbuss.tefcontrol.GlobalProperties;
import de.dbuss.tefcontrol.data.Role;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.service.*;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;


//@PageTitle("Default Mapping")
@Slf4j
@Route(value = "Default-Mapping/:project_Id", layout = MainLayout.class)
@RolesAllowed("USER")
public class DefaultView extends VerticalLayout  implements BeforeEnterObserver  {

    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private final AgentJobsService agentJobsService;
    private final MSMService msmService;
    private final ProjectSqlService projectSqlService;
    private final ProjectConnectionService projectConnectionService;
    private AuthenticatedUser authenticatedUser;
    private VaadinCKEditor editor;
    private Optional<Projects> projects;
    Optional<ProjectSql> selectedProjectSql;
    private Grid<ProjectAttachmentsDTO> attachmentGrid;
    private List<ProjectAttachmentsDTO> listOfProjectAttachments;
    private List<LinkedHashMap<String, Object>> rows;
    private Grid<AgentJobs> gridAgentJobs;
    private Label lastRefreshLabel;
    private Label countdownLabel;
    private ScheduledExecutorService executor;
    private Select<String> select;
    private Select<String> selectConnection;
    private TextArea sqlDescriptionTextArea;
    private UI ui ;
    private String selectedDbName;
    Button executeButton;
    Button exportButton;

    public DefaultView(ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService, AgentJobsService agentJobsService, MSMService msmService, ProjectSqlService projectSqlService, ProjectConnectionService projectConnectionService, AuthenticatedUser authenticatedUser) {
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;
        this.agentJobsService = agentJobsService;
        this.msmService = msmService;
        this.projectSqlService = projectSqlService;
        this.projectConnectionService = projectConnectionService;
        this.authenticatedUser = authenticatedUser;

        countdownLabel = new Label();
        lastRefreshLabel=new Label();
        countdownLabel.setVisible(false);

        executeButton = new Button("Execute");
        exportButton = new Button("Export");
        //executeButton.setEnabled(false);

        ui= UI.getCurrent();

        HorizontalLayout vl = new HorizontalLayout();
        vl.add(getTabsheet());

        vl.setHeightFull();
        vl.setSizeFull();



      //  Span info= new Span();
      //  var xx = GlobalProperties.getCache().get("ProjektName");
      //  info.setText(xx);
      //  info.setTitle("Pfad");

       // add(info, vl);
        add(vl);
    }
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        String projectId = parameters.get("project_Id").orElse(null);

        if (projectId != null) {
            projects = projectsService.findById(Long.parseLong(projectId));
            executeButton.setEnabled(false);
            exportButton.setEnabled(false);
        }

        //projects.ifPresent(value -> listOfProjectAttachments = projectsService.getProjectAttachments(value));
        projects.ifPresent(value -> listOfProjectAttachments = projectsService.getProjectAttachmentsWithoutFileContent(value));

        updateDescription();
        updateAttachmentGrid(listOfProjectAttachments);
        updateAgentJobGrid();
        setSelectedSql();


    }

    private void updatesqlDescription(Optional<ProjectSql> selectedProjectSql) {
        log.info("executing updatesqlDescription() for project description....."+selectedProjectSql);
        sqlDescriptionTextArea.setValue(selectedProjectSql.isPresent() ? selectedProjectSql.get().getDescription() : "No description available");
    }

    private void updateDescription() {
        log.info("executing updateDescription() for project description");
      //  GlobalProperties.putCache("ProjektName",projects.get().getName());
        //GlobalProperties.cache

        editor.setValue(projects.map(Projects::getDescription).orElse(""));
    }
    private void updateAttachmentGrid(List<ProjectAttachmentsDTO> projectAttachmentsDTOS) {
        log.info("executing updateAttachmentGrid() for project attachments");
        attachmentGrid.setItems(projectAttachmentsDTOS);
    }
    private void updateAgentJobGrid() {
        log.info("executing updateAgentJobGrid() for Agent Job grid");
        gridAgentJobs.setItems(agentJobsService.findbyJobName(projects.get().getAgent_Jobs()));
    }
    private TabSheet getTabsheet() {

        log.info("Starting getTabsheet() for Tabsheet");
        TabSheet tabSheet = new TabSheet();

        tabSheet.add("Description", getProjectsDescription());
        tabSheet.add("Attachments", getProjectAttachements());
        tabSheet.add("DB-Jobs", getAgentJobTab());
        tabSheet.add("QS", getProjectSQL());

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();
        log.info("Ending getTabsheet() for Tabsheet");
        return tabSheet;
    }

    private Component getAgentJobTab() {
        log.info("Starting getAgentJobTab() for DB-jobs tab");
        configureAgentJobGrid();

        VerticalLayout content = new VerticalLayout(gridAgentJobs);

        content.setSizeFull();
        content.setHeight("250px");

        content.add(getAgentJobToolbar());
        log.info("Ending getAgentJobTab() for DB-jobs tab");
        return content;

    }

    private Component getAgentJobToolbar() {
        log.info("Starting getAgentJobToolbar() for DB-jobs tab");
        Button refreshBtn = new Button("refresh");
        Checkbox autorefresh = new Checkbox();

        refreshBtn.addClickListener(e->{
            log.info("executing refreshBtn.addClickListener for DB-jobs tab");
            var agentJobs=getAgentJobs();
            gridAgentJobs.setItems(agentJobs);
            updateLastRefreshLabel();
        });

        countdownLabel.setText("initialisiert");
        autorefresh.setLabel("Auto-Refresh");

        autorefresh.addClickListener(e->{
            if (autorefresh.getValue()){
                log.info("executing autorefresh.addClickListener for DB-jobs tab");
                startCountdown(Duration.ofSeconds(60));
                //  countdownLabel.setText("timer on");
                countdownLabel.setVisible(true);
             /*   Integer count = 0;
                while (count < 10) {
                    // Sleep to emulate background work
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    String message = "This is update " + count++;

                    ui.access(() -> add(new Span(message)));
                }*/
                // Inform that we are done
                ui.access(() -> {
                    add(new Span("Done updating"));
                });
            }
            else{
                System.out.println("Autorefresh wird ausgeschaltet.");
                stopCountdown();
                countdownLabel.setVisible(false);
            }
        });

        updateLastRefreshLabel();
        HorizontalLayout layout = new HorizontalLayout(refreshBtn,lastRefreshLabel, autorefresh, countdownLabel);
        layout.setPadding(false);
        layout.setAlignItems(FlexComponent.Alignment.BASELINE);

        log.info("Ending getAgentJobToolbar() for DB-jobs tab");
        return layout;

    }

    private void startCountdown(Duration duration) {
        log.info("Starting startCountdown() for DB-jobs tab");
        executor = Executors.newSingleThreadScheduledExecutor();

        Instant startTime = Instant.now();
        updateLastRefreshLabel();

        executor.scheduleAtFixedRate(() -> {
            log.info("executing executor.scheduleAtFixedRate for start count down in DB-jobs tab");
           /* Command com = new Command() {
                @Override
                public void execute() {
                    countdownLabel.setText("ongoing");
                }
            };
            ui.access(com);*/
            ui.access(() -> {
                Duration remainingTime = calculateRemainingTime(duration, startTime);
                //       updateCountdownLabel(remainingTime);
                countdownLabel.setText("ongoing");
                //System.out.println("UI-ID: " + ui.toString());
            });
            ui.notify();
        }, 0, 1, java.util.concurrent.TimeUnit.SECONDS);
        log.info("Ending startCountdown() for DB-jobs tab");
    }
  
    private Component getProjectSQL() {
        log.info("Starting getProjectSQL() for QS tab");
        VerticalLayout content = new VerticalLayout();
        Div sqlTabContent = new Div();
        sqlTabContent.setSizeFull();

        selectConnection = new Select<>();
        select = new Select<>();
        select.setLabel("Choose Query SQL");
        setSelectedSql();
        select.setPlaceholder("name from table project_sqls for selected project_id");
        select.setWidthFull();

        TextArea sqlQuerytextArea = new TextArea();

        Grid<LinkedHashMap<String, Object>> resultGrid = new Grid<>();
        resultGrid.removeAllColumns();

        select.addValueChangeListener(event -> {
            log.info("executing select.addValueChangeListener for database selection " + event.getValue());
            selectedProjectSql = projectSqlService.findByName(event.getValue());
            if(selectedProjectSql.isPresent()){
                selectedDbName = selectedProjectSql.get().getProjectConnection().getName();
                executeButton.setEnabled(true);
            }
            updatesqlDescription(selectedProjectSql);
            setValueForConnection(selectedProjectSql);
            sqlQuerytextArea.setValue(selectedProjectSql.isPresent()? selectedProjectSql.get().getSql():"");
            resultGrid.removeAllColumns();
        });

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Description",getSqlDescription());
        tabSheet.add("Connection",getsqllConnection());
        tabSheet.add("Query",getSqlQuery(sqlQuerytextArea, resultGrid));

        sqlTabContent.add(select,tabSheet);
        sqlTabContent.setHeightFull();
        content.setHeightFull();
        content.add(sqlTabContent);
        log.info("Ending getProjectSQL() for QS tab");
        return content;

    }

    private void setValueForConnection(Optional<ProjectSql> selectedProjectSql) {
        log.info("Executing setValueForConnection() for connection of sql"+ selectedProjectSql);
        if(selectedProjectSql.isPresent()){
            selectConnection.setValue(selectedProjectSql.get().getProjectConnection().getName());
        }

      //  selectConnection.setItems(selectedProjectSql.isPresent() ? selectedProjectSql.get().getProjectConnection().getName() : "No connection");
    }

    private void setSelectedSql() {
        if(projects != null) {
            List<ProjectSql> listOfSql = projectsService.getProjectSqls(projects.get());
            if(listOfSql.size() != 0) {
                select.setItems(listOfSql.stream()
                        .map(ProjectSql::getName)
                        .collect(Collectors.toList()));
            } else {
                select.setItems("No sql present");
            }
        }
    }

    private VerticalLayout getSqlDescription() {
        log.info("Starting getSqlDescription() for QS tab");
        VerticalLayout content = new VerticalLayout();
        sqlDescriptionTextArea = new TextArea();
        sqlDescriptionTextArea.setWidthFull();
        sqlDescriptionTextArea.setHeight("250px");
        sqlDescriptionTextArea.setValue("No description available");
        content.add(sqlDescriptionTextArea);
        log.info("Ending getSqlDescription() for QS tab");
        return content;
    }

    private Component getsqllConnection() {
        log.info("Starting getsqllConnection() for data get based on sql");
        VerticalLayout content = new VerticalLayout();

        selectConnection.setPlaceholder("name from project_connections");
        selectConnection.setWidth("650 px");
        List<ProjectConnection> projectConnections = projectConnectionService.findAll();
        selectConnection.setItems(
                projectConnections
                        .stream()
                        .map(ProjectConnection::getName)
                        .collect(Collectors.toList())
        );

        TextArea textArea = new TextArea();
        textArea.setWidthFull();
        textArea.setValue("Description for selected connection (from Table [project_connections] for selected id");
        textArea.setHeight("400 px");
        textArea.setReadOnly(true);

        selectConnection.addValueChangeListener(event -> {
            log.info("executing selectConnection.addValueChangeListener for database selection " + event.getValue());
            if(event.getValue() != null) {
                selectedDbName = event.getValue();
                ProjectConnection connection = projectConnectionService.findByName(selectedDbName).get();
                textArea.setValue(connection.getDescription());
            }
        });

        content.add(selectConnection, textArea);
        content.setWidth("650 px");
        log.info("Ending getsqllConnection() for data get based on sql");
        return content;
    }

    private Component getSqlQuery(TextArea sqlQuerytextArea, Grid<LinkedHashMap<String, Object>> resultGrid) {
        log.info("Starting getSqlQuery() for data get based on sql");
        VerticalLayout content = new VerticalLayout();

        Div sqlTabContent = new Div();
        sqlTabContent.setSizeFull();

        VerticalLayout vl = new VerticalLayout();
        HorizontalLayout hl = new HorizontalLayout();

        executeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        exportButton.setEnabled(false);

        hl.add(executeButton, exportButton);

        sqlQuerytextArea.setWidthFull();
        sqlQuerytextArea.setHeight("300 px");
        sqlQuerytextArea.setPlaceholder("SQL from Table [project_sqls] Column sql for selected id");
        //sqlQuerytextArea.setValue("SQL from Table [project_sqls] Column sql for selected id");

        vl.add(sqlQuerytextArea,hl);


        SplitLayout splitLayout = new SplitLayout(vl, resultGrid);
        splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);

        splitLayout.setHeightFull();
        splitLayout.setWidthFull();

        executeButton.addClickListener( event -> {
            resultGrid.removeAllColumns();

            resultGrid.setPageSize(50);
            resultGrid.setHeight("800px");

            resultGrid.getStyle().set("resize", "vertical");
            resultGrid.getStyle().set("overflow", "auto");
            selectedProjectSql.ifPresent(sql -> {
                log.info("executing executeButton.addClickListener for data get based on sql");
                //String query = sql.getSql();
                String query = sqlQuerytextArea.getValue();
                try {
                    //selectedDbName = sql.getProjectConnection().getName();
                    rows = projectConnectionService.getDataFromDatabase(selectedDbName, query);
                    if(!rows.isEmpty()){
                        resultGrid.setItems(rows); // rows is the result of retrieveRows
                        // Add the columns based on the first row
                        LinkedHashMap<String, Object> s = rows.get(0);
                        for (Map.Entry<String, Object> entry : s.entrySet()) {
                            resultGrid.addColumn(h -> truncateText((String) h.get(entry.getKey()))).setHeader(entry.getKey()).setAutoWidth(true).setResizable(true).setSortable(true);
                        }

                        resultGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
                        resultGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
                        resultGrid.addThemeVariants(GridVariant.LUMO_COMPACT);

                        exportButton.setEnabled(true);
                        this.setPadding(false);
                        this.setSpacing(false);
                        this.setBoxSizing(BoxSizing.CONTENT_BOX);
                    } else {
                        Notification.show("No rows in selected database").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    }
                } catch (Exception e) {
                    // Handle the exception here
                    log.error("Error while retrieving data from the database: " + e.getMessage(), e);
                    Notification.show("An error occurred while retrieving data from the database.").addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
        });

        exportButton.addClickListener(event -> {
            log.info("executing exportButton.addClickListener for data get based on sql");
            if (!rows.isEmpty()) {
                generateExcelFile(rows);
            } else {
                Notification.show("No data to export.");
            }
        });
     //   content.setHeightFull();
        sqlTabContent.add(splitLayout);

        content.add(sqlTabContent);
        log.info("Ending getSqlQuery() for data get based on sql");
        return content;

    }

    private void generateExcelFile(List<LinkedHashMap<String, Object>> rows) {
            // Create a new Excel workbook and sheet
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Exported Data");

            // Create a header row with column names
            Row headerRow = sheet.createRow(0);
            int cellIndex = 0;
            for (String columnName : rows.get(0).keySet()) {
                Cell cell = headerRow.createCell(cellIndex++);
                cell.setCellValue(columnName);
            }

            // Populate the data rows
            int rowIndex = 1;
            for (LinkedHashMap<String, Object> row : rows) {
                Row dataRow = sheet.createRow(rowIndex++);
                cellIndex = 0;
                for (Object value : row.values()) {
                    Cell cell = dataRow.createCell(cellIndex++);
                    if (value != null) {
                        if (value instanceof Number) {
                            cell.setCellValue(((Number) value).doubleValue());
                        } else if (value instanceof String) {
                            cell.setCellValue((String) value);
                        } else if (value instanceof Boolean) {
                            cell.setCellValue((Boolean) value);
                        } else {
                            // Handle other data types as needed
                            cell.setCellValue(value.toString());
                        }
                    } else {
                        cell.setCellValue(""); // Handle null values as empty strings
                    }
                }
            }

            // Set up the download
            String fileName = "exported_data.xlsx";
            StreamResource streamResource = new StreamResource(fileName, () -> {
                try {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    workbook.write(outputStream);
                    byte[] dataBytes = outputStream.toByteArray();
                    return new ByteArrayInputStream(dataBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            });

            Anchor anchor = new Anchor(streamResource, "");
            anchor.getElement().setAttribute("download", true);
            anchor.getElement().getStyle().set("display", "none");
            add(anchor);
            UI.getCurrent().getPage().executeJs("arguments[0].click()", anchor);
    }

    private void stopCountdown() {
        log.info("Starting stopCountdown() for DB-jobs tab");
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        log.info("Ending stopCountdown() for DB-jobs tab");
    }

    private Duration calculateRemainingTime(Duration duration, Instant startTime) {
        log.info("Starting calculateRemainingTime() for DB-jobs tab");
        Instant now = Instant.now();
        Instant endTime = startTime.plus(duration);
        log.info("Ending calculateRemainingTime() for DB-jobs tab");
        return Duration.between(now, endTime);
    }

    private void updateLastRefreshLabel() {
        log.info("Starting updateLastRefreshLabel() for DB-jobs tab");
        LocalTime currentTime = LocalTime.now();
        String formattedTime = currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        lastRefreshLabel.setText("letzte Aktualisierung: " + formattedTime);
        log.info("Ending updateLastRefreshLabel() for DB-jobs tab");
    }
    private VerticalLayout getProjectAttachements() {
        log.info("Starting getProjectAttachements() for Attachment tab");
        VerticalLayout content = new VerticalLayout();
        Div attachmentsTabContent = new Div();
        attachmentsTabContent.setSizeFull();

        // Create a file upload component
        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        Upload fileUpload = new Upload(buffer);
        fileUpload.setAcceptedFileTypes(".doc", ".xlsx",".xls" , ".ppt", ".msg", ".pdf", ".pptx", ".ppt");
        fileUpload.setUploadButton(new Button("Search file"));

        Div dropLabel = new Div();
        dropLabel.setText("or drop new File(s) here");

        fileUpload.setDropLabel(dropLabel);
        fileUpload.setDropLabelIcon(new Div());

        // Create a grid to display attachments
        attachmentGrid = new Grid<>();
        attachmentGrid.addColumn(ProjectAttachmentsDTO::getFilename).setResizable(true).setSortable(true).setHeader("File Name");
        /*attachmentGrid.addColumn(attachment -> {
            String fileName = attachment.getFilename();
            String[] parts = fileName.split("\\.");
            if (parts.length > 0) {
                return parts[parts.length - 1];
            } else {
                return "";
            }
        }).setHeader("File Type");*/
        attachmentGrid.addColumn(ProjectAttachmentsDTO::getDescription).setResizable(true).setHeader("Description");
        attachmentGrid.addColumn(ProjectAttachmentsDTO::getUploadDate).setResizable(true).setSortable(true).setHeader("Date");
        attachmentGrid.addColumn(ProjectAttachmentsDTO::getFilesizeKb).setResizable(true).setHeader("FileSize");

        attachmentGrid.addItemDoubleClickListener(event -> {
            log.info("executing attachmentGrid.addItemDoubleClickListener for file download in Attachment tab");
            ProjectAttachmentsDTO selectedAttachment = event.getItem();
            ProjectAttachments projectAttachments = projectAttachmentsService.findById(event.getItem().getId());
            if (selectedAttachment != null) {
                byte[] fileContent = projectAttachments.getFilecontent();
                String fileName = projectAttachments.getFilename();

                // Create a StreamResource with a unique key to avoid caching issues
                StreamResource streamResource = new StreamResource(fileName, () -> new ByteArrayInputStream(fileContent));

                // Create an Anchor component for downloading the file
                Anchor downloadLink = new Anchor(streamResource, fileName);
                downloadLink.getElement().setAttribute("download", true);
                downloadLink.getElement().getStyle().set("display", "none");
                // Add the download link to the UI
                add(downloadLink);
                // Trigger the download link
                UI.getCurrent().getPage().executeJs("arguments[0].click()", downloadLink);
            }
        });

        // Create a confirmation dialog for removing files
        Dialog confirmationDialog = new Dialog();
        confirmationDialog.setCloseOnOutsideClick(false);
        confirmationDialog.setCloseOnEsc(false);
        confirmationDialog.add(new Text("Are you sure you want to delete this file?"));
        Button confirmButton = new Button("Confirm");
        Button cancelButton = new Button("Cancel");
        confirmButton.getStyle().set("margin-right", "10px");

        confirmButton.addClickListener(confirmEvent -> {
            log.info("executing confirmButton.addClickListener for delete selected file in Attachment tab");
            Optional<ProjectAttachmentsDTO> selectedAttachmentOptional = attachmentGrid.getSelectionModel().getFirstSelectedItem();
            if (selectedAttachmentOptional.isPresent()) {
                ProjectAttachmentsDTO selectedAttachment = selectedAttachmentOptional.get();
                System.out.println(selectedAttachment.getId());
                projectAttachmentsService.delete(selectedAttachment.getId());
                // Refresh the 'projects' entity to reflect the changes
                projects = projectsService.findById(projects.get().getId());
                List<ProjectAttachmentsDTO> listOfAttachments = projectsService.getProjectAttachmentsWithoutFileContent(projects.get());
                attachmentGrid.setItems(listOfAttachments);
            }
            confirmationDialog.close(); // Close the confirmation dialog
        });

        cancelButton.addClickListener(cancelEvent -> {
            log.info("executing cancelButton.addClickListener for cancel selected file in Attachment tab");
            confirmationDialog.close(); // Close the confirmation dialog
        });

        GridContextMenu<ProjectAttachmentsDTO> contextMenu = attachmentGrid.addContextMenu();
        GridMenuItem<ProjectAttachmentsDTO> removeItem = contextMenu.addItem("Remove", event -> {
            log.info("executing removeItem contextMenu open when right click in Attachment grid");
            confirmationDialog.open();
        });
        confirmationDialog.add(new Div(confirmButton, cancelButton));

        // Create a CRUD editor for editing the file data
        Crud<ProjectAttachmentsDTO> crud = new Crud<>(ProjectAttachmentsDTO.class, createEditor());

        // Add an "Edit" menu item
        GridMenuItem<ProjectAttachmentsDTO> editItem = contextMenu.addItem("Edit", event -> {
            log.info("executing editItem contextMenu open when right click in Attachment grid");
            Optional<ProjectAttachmentsDTO> selectedAttachmentOptional = event.getItem();
            if (selectedAttachmentOptional.isPresent()) {
                ProjectAttachmentsDTO selectedAttachment = selectedAttachmentOptional.get();
                crud.edit(selectedAttachment, Crud.EditMode.EXISTING_ITEM);

                crud.getDeleteButton().getElement().getStyle().set("display", "none");
                crud.setToolbarVisible(false);
                crud.getGrid().getElement().getStyle().set("display", "none");
                crud.getNewButton().getElement().getStyle().set("display", "none");

                add(crud);
            }
        });

        crud.addSaveListener(event -> {
            log.info("executing crud.addSaveListener for save editedAttachment in Attachment grid");
            ProjectAttachmentsDTO editedAttachment = event.getItem();
            System.out.println(editedAttachment.getId()+"....."+editedAttachment.getFilename()+"..."+editedAttachment.getProjectId()+"..."+editedAttachment.getDescription()+"..."+editedAttachment.getFilesizeKb()+"..."+editedAttachment.getFileContent());
            projectAttachmentsService.updateGridValues(editedAttachment);
            attachmentGrid.getDataProvider().refreshItem(editedAttachment);
        });

        fileUpload.addSucceededListener(event -> {
            log.info("executing fileUpload.addSucceededListener for fileUpload in Attachment grid");
            String fileName = event.getFileName();
            String fileType = event.getMIMEType();
            int fileSizeKB = (int) (event.getContentLength() / 1024.0);  // Divide by 1024 to get KB;
            byte[] fileContent = new byte[0];
            try {
                fileContent = buffer.getInputStream(fileName).readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // Extract description from the user input (you might need a separate input field for this)
            String description = "no description yet";

            ProjectAttachmentsDTO projectAttachmentsDTO = new ProjectAttachmentsDTO();
            projectAttachmentsDTO.setFilename(fileName);
            projectAttachmentsDTO.setDescription(description);
            projectAttachmentsDTO.setFileContent(fileContent);
            projectAttachmentsDTO.setUploadDate(new Date());
            projectAttachmentsDTO.setProjectId(projects.get().getId());
            projectAttachmentsDTO.setFilesizeKb(fileSizeKB);

            projectAttachmentsService.update(projectAttachmentsDTO);

            // Refresh the 'projects' entity to reflect the changes
            projects = projectsService.findById(projects.get().getId());

            attachmentGrid.setItems(projectsService.getProjectAttachmentsWithoutFileContent(projects.get()));
            fileUpload.clearFileList();

        });
        if(projects != null) {
            attachmentGrid.setItems(projectsService.getProjectAttachmentsWithoutFileContent(projects.get()));
        }
        attachmentsTabContent.add(attachmentGrid,fileUpload);
        content.add(attachmentsTabContent);
        log.info("Ending getProjectAttachements() for Attachment tab");

        return content;
    }

    private VerticalLayout getProjectsDescription() {
        log.info("Starting getProjectsDescription() for Description tab");

        Button saveBtn = new Button("save");
        Button editBtn = new Button("edit");
        saveBtn.setVisible(false);
        editBtn.setVisible(true);
        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            System.out.println("User: " + user.getName());
            Set<Role> roles = user.getRoles();
            boolean isAdmin = roles.stream()
                    .anyMatch(role -> role == Role.ADMIN);
            editBtn.setVisible(isAdmin);
        }
        VerticalLayout content = new VerticalLayout();

        Config config = new Config();
        config.setBalloonToolBar(Constants.Toolbar.values());
        config.setImage(new String[][]{},
                "", new String[]{"full", "alignLeft", "alignCenter", "alignRight"},
                new String[]{"imageTextAlternative", "|",
                        "imageStyle:alignLeft",
                        "imageStyle:full",
                        "imageStyle:alignCenter",
                        "imageStyle:alignRight"}, new String[]{});

        editor = new VaadinCKEditorBuilder().with(builder -> {

            builder.editorType = Constants.EditorType.CLASSIC;
            builder.width = "95%";
            builder.readOnly = true;
            builder.hideToolbar=true;
            builder.config = config;
        }).createVaadinCKEditor();

        editor.setReadOnly(true);

        saveBtn.addClickListener((event -> {
            log.info("executing saveBtn.addClickListener for description save button click");
            projects.get().setDescription(editor.getValue());
            projectsService.update(projects.get());

            editBtn.setVisible(true);
            saveBtn.setVisible(false);
            //editor.setReadOnly(true);
            editor.setReadOnlyWithToolbarAction(!editor.isReadOnly());

        }));

        editBtn.addClickListener(e->{
            log.info("executing editBtn.addClickListener for description edit button click");
            editor.setReadOnlyWithToolbarAction(!editor.isReadOnly());
            editBtn.setVisible(false);
            saveBtn.setVisible(true);
            //editor.setReadOnly(false);
        });

        content.add(editor,editBtn,saveBtn);

        if(projects != null) {
            editor.setValue(projects.map(Projects::getDescription).orElse(""));
        }
        log.info("Ending getProjectsDescription() for Description tab");
        return content;

    }

    private void configureAgentJobGrid() {
        log.info("Starting configureAgentJobGrid() for DB-jobs tab");
        gridAgentJobs = new Grid<>(AgentJobs.class);
        gridAgentJobs.addClassNames("PFG-AgentJobs");
        //gridAttachments.setSizeFull();
        gridAgentJobs.setColumns("name", "job_activity", "duration_Min", "jobStartDate", "jobStopDate", "jobNextRunDate", "result" );

        //select JobName, JobEnabled,JobDescription, JobActivity, DurationMin, JobStartDate, JobLastExecutedStep, JobExecutedStepDate, JobStopDate, JobNextRunDate, Result from job_status

        gridAgentJobs.addColumn(
                new NativeButtonRenderer<>("Run",
                        clickedItem -> {
                            log.info("executing NativeButtonRenderer for Run and clickedItem in gridAgentJobs grid "+clickedItem.getName());

                            try {
                                startJob(clickedItem.getName());
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        })
        );
        gridAgentJobs.getColumns().forEach(col -> col.setAutoWidth(true));
        gridAgentJobs.setItems(getAgentJobs());
        log.info("Ending configureAgentJobGrid() for DB-jobs tab");
    }

    private void startJob(String jobName) throws InterruptedException {
        log.info("Starting startJob() for DB-jobs tab");
        String dbName= projects.isPresent()? projects.get().getAgent_db() : "";
        var erg= msmService.startJob(jobName, dbName);

        if (erg.contains("OK"))
        {
            Notification notification = Notification.show("Job " + jobName + " wurde gestartet...",6000, Notification.Position.TOP_END);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
            Div textArea = new Div();
            Article article=new Article();
            article.setText(LocalDateTime.now().format(formatter) + ": Job " + jobName + " gestartet..." );
            textArea.add (article);

            Thread.sleep(2000);
            gridAgentJobs.setItems(getAgentJobs());
        } else {
            Notification.show(erg, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        log.info("Ending startJob() for DB-jobs tab");
    }

    private List<AgentJobs> getAgentJobs() {
        log.info("Starting getAgentJobs() for DB-jobs tab");
        if (projects != null) {
            String jobName = projects.get().getAgent_Jobs();
            log.info("Ending getAgentJobs() for DB-jobs tab");
            return agentJobsService.findbyJobName(jobName);
        }
        else {
            Projects projects = projectsService.findByName("PFG_Cube");
            log.info("Ending getAgentJobs() for DB-jobs tab");
            return agentJobsService.findbyJobName(projects.getAgent_Jobs());
        }
    }
    private CrudEditor<ProjectAttachmentsDTO> createEditor() {
        log.info("Starting createEditor() for ProjectAttachments Attachment tab");
        TextField filename = new TextField("Edit Filename");
        TextArea description = new TextArea("Edit Description");
        description.setSizeFull();
        FormLayout editFormLayout = new FormLayout(filename, description);
        Binder<ProjectAttachmentsDTO> editBinder = new Binder<>(ProjectAttachmentsDTO.class);
        //editBinder.bindInstanceFields(editFormLayout);
        editBinder.forField(filename).asRequired().bind(ProjectAttachmentsDTO::getFilename,
                ProjectAttachmentsDTO::setFilename);
        editBinder.forField(description).asRequired().bind(ProjectAttachmentsDTO::getDescription,
                ProjectAttachmentsDTO::setDescription);
        log.info("Ending createEditor() for ProjectAttachments Attachment tab");
        return new BinderCrudEditor<>(editBinder, editFormLayout);
    }
    private String truncateText(String columnValue) {
        int maxDisplayLength = 50; // Adjust this as needed
        if (columnValue != null && columnValue.length() > maxDisplayLength) {
            // If the text is too long, truncate it and add "..."
            return columnValue.substring(0, maxDisplayLength) + "...";
        } else {
            // If the text is within the limit, display it as is
            return columnValue;
        }
    }

}
