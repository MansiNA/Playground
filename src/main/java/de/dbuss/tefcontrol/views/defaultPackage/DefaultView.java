package de.dbuss.tefcontrol.views.defaultPackage;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
import de.dbuss.tefcontrol.data.entity.AgentJobs;
import de.dbuss.tefcontrol.data.entity.CLTV_HW_Measures;
import de.dbuss.tefcontrol.data.entity.ProjectAttachments;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.service.AgentJobsService;
import de.dbuss.tefcontrol.data.service.MSMService;
import de.dbuss.tefcontrol.data.service.ProjectAttachmentsService;
import de.dbuss.tefcontrol.data.service.ProjectsService;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


//@PageTitle("Default Mapping")
@Slf4j
@Route(value = "Default-Mapping/:project_Id", layout = MainLayout.class)
@RolesAllowed("USER")
public class DefaultView extends VerticalLayout  implements BeforeEnterObserver  {

    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private final AgentJobsService agentJobsService;
    private final MSMService msmService;
    private VaadinCKEditor editor;
    private Optional<Projects> projects;
    private Grid<ProjectAttachments> attachmentGrid;
    private List<ProjectAttachments> listOfProjectAttachments;
    private Grid<AgentJobs> gridAgentJobs;
    private Label lastRefreshLabel;
    private Label countdownLabel;
    private ScheduledExecutorService executor;
    private UI ui ;
    public DefaultView(ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService, AgentJobsService agentJobsService, MSMService msmService) {
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;
        this.agentJobsService = agentJobsService;
        this.msmService = msmService;

        countdownLabel = new Label();
        lastRefreshLabel=new Label();
        countdownLabel.setVisible(false);

        ui= UI.getCurrent();

        VerticalLayout vl = new VerticalLayout();
        Article text = new Article();
        text.add("Hier möglichst noch den jeweils ausgewählten Projekt-Namen mit Pfad ausgeben...(breadcrump like)");
        vl.add(text,getTabsheet());

        vl.setHeightFull();
        vl.setSizeFull();

        add(vl);
    }
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        String projectId = parameters.get("project_Id").orElse(null);

        if (projectId != null) {
            projects = projectsService.findById(Long.parseLong(projectId));
        }

        projects.ifPresent(value -> listOfProjectAttachments = projectsService.getProjectAttachments(value));

        updateDescription();
        updateAttachmentGrid(listOfProjectAttachments);
        updateAgentJobGrid();
    }

    private void updateDescription() {
        log.info("executing updateDescription() for project description");
        editor.setValue(projects.map(Projects::getDescription).orElse(""));
    }
    private void updateAttachmentGrid(List<ProjectAttachments> projectAttachments) {
        log.info("executing updateAttachmentGrid() for project attachments");
        attachmentGrid.setItems(projectAttachments);
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
        tabSheet.add("QS", new Div(new Text("This is the QS tab content")));

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
        fileUpload.setAcceptedFileTypes(".doc", ".xlsx",".xls" , ".ppt", ".msg", ".pdf");
        fileUpload.setUploadButton(new Button("Search file"));


        Div dropLabel = new Div();
        dropLabel.setText("or drop new File(s) here");

        fileUpload.setDropLabel(dropLabel);

        fileUpload.setDropLabelIcon(new Div());

        // Create a grid to display attachments
        attachmentGrid = new Grid<>();
        attachmentGrid.addColumn(ProjectAttachments::getFilename).setResizable(true).setSortable(true).setHeader("File Name");
        /*attachmentGrid.addColumn(attachment -> {
            String fileName = attachment.getFilename();
            String[] parts = fileName.split("\\.");
            if (parts.length > 0) {
                return parts[parts.length - 1];
            } else {
                return "";
            }
        }).setHeader("File Type");*/
        attachmentGrid.addColumn(ProjectAttachments::getDescription).setResizable(true).setHeader("Description");
        attachmentGrid.addColumn(ProjectAttachments::getUpload_date).setResizable(true).setSortable(true).setHeader("Date");
        attachmentGrid.addColumn(ProjectAttachments::getFilesizekb).setResizable(true).setHeader("FileSize");

        attachmentGrid.addItemDoubleClickListener(event -> {
            log.info("executing attachmentGrid.addItemDoubleClickListener for file download in Attachment tab");
            ProjectAttachments selectedAttachment = event.getItem();
            if (selectedAttachment != null) {
                byte[] fileContent = selectedAttachment.getFilecontent();
                String fileName = selectedAttachment.getFilename();

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
            Optional<ProjectAttachments> selectedAttachmentOptional = attachmentGrid.getSelectionModel().getFirstSelectedItem();
            if (selectedAttachmentOptional.isPresent()) {
                ProjectAttachments selectedAttachment = selectedAttachmentOptional.get();
                projectAttachmentsService.delete(selectedAttachment.getId());
                // Refresh the 'projects' entity to reflect the changes
                projects = projectsService.findById(projects.get().getId());
                attachmentGrid.setItems(projectsService.getProjectAttachments(projects.get()));
            }
            confirmationDialog.close(); // Close the confirmation dialog
        });

        cancelButton.addClickListener(cancelEvent -> {
            log.info("executing cancelButton.addClickListener for cancel selected file in Attachment tab");
            confirmationDialog.close(); // Close the confirmation dialog
        });

        GridContextMenu<ProjectAttachments> contextMenu = attachmentGrid.addContextMenu();
        GridMenuItem<ProjectAttachments> removeItem = contextMenu.addItem("Remove", event -> {
            log.info("executing removeItem contextMenu open when right click in Attachment grid");
            confirmationDialog.open();
        });
        confirmationDialog.add(new Div(confirmButton, cancelButton));

        // Create a CRUD editor for editing the file data
        Crud<ProjectAttachments> crud = new Crud<>(ProjectAttachments.class, createEditor());

        // Add an "Edit" menu item
        GridMenuItem<ProjectAttachments> editItem = contextMenu.addItem("Edit", event -> {
            log.info("executing editItem contextMenu open when right click in Attachment grid");
            Optional<ProjectAttachments> selectedAttachmentOptional = event.getItem();
            if (selectedAttachmentOptional.isPresent()) {
                ProjectAttachments selectedAttachment = selectedAttachmentOptional.get();
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
            ProjectAttachments editedAttachment = event.getItem();
            projectAttachmentsService.update(editedAttachment);
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

            ProjectAttachments projectAttachments = new ProjectAttachments();
            projectAttachments.setFilename(fileName);
            projectAttachments.setDescription(description);
            projectAttachments.setFilecontent(fileContent);
            projectAttachments.setUpload_date(new Date());
            projectAttachments.setProject(projects.get());
            projectAttachments.setFilesizekb(fileSizeKB);

            projectAttachmentsService.update(projectAttachments);

            // Refresh the 'projects' entity to reflect the changes
            projects = projectsService.findById(projects.get().getId());

            attachmentGrid.setItems(projectsService.getProjectAttachments(projects.get()));
            fileUpload.clearFileList();

        });
        if(projects != null) {
            attachmentGrid.setItems(projectsService.getProjectAttachments(projects.get()));
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
        editBtn.setVisible(true);
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
                            Notification notification = Notification.show("Job " + clickedItem.getName() + " wurde gestartet...",6000, Notification.Position.TOP_END);
                            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
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
        var erg= msmService.startJob(jobName);
        if (!erg.contains("OK"))
        {
            Notification.show(erg, 5000, Notification.Position.MIDDLE);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        Div textArea = new Div();
        Article article=new Article();
        article.setText(LocalDateTime.now().format(formatter) + ": Job " + jobName + " gestartet..." );
        textArea.add (article);

        Thread.sleep(2000);
        gridAgentJobs.setItems(getAgentJobs());
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
    private CrudEditor<ProjectAttachments> createEditor() {
        log.info("Starting createEditor() for ProjectAttachments Attachment tab");
        TextField filename = new TextField("Edit Filename");
        TextArea description = new TextArea("Edit Description");
        description.setSizeFull();
        FormLayout editFormLayout = new FormLayout(filename, description);
        Binder<ProjectAttachments> editBinder = new Binder<>(ProjectAttachments.class);
        //editBinder.bindInstanceFields(editFormLayout);
        editBinder.forField(filename).asRequired().bind(ProjectAttachments::getFilename,
                ProjectAttachments::setFilename);
        editBinder.forField(description).asRequired().bind(ProjectAttachments::getDescription,
                ProjectAttachments::setDescription);
        log.info("Ending createEditor() for ProjectAttachments Attachment tab");
        return new BinderCrudEditor<>(editBinder, editFormLayout);
    }

}
