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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

//@PageTitle("Default Mapping")
@Route(value = "Default-Mapping/:project_Id", layout = MainLayout.class)
@RolesAllowed("USER")
public class DefaultView extends VerticalLayout  implements BeforeEnterObserver  {

    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private final AgentJobsService agentJobsService;
    private final MSMService msmService;
    private TabSheet tabSheet;
    private Button saveBtn;
    private Button editBtn;
    private VaadinCKEditor editor;
    private Optional<Projects> projects;
    private VerticalLayout vl;
    private Upload fileUpload;
    private Grid<ProjectAttachments> attachmentGrid;
    private MultiFileMemoryBuffer buffer;
    private List<ProjectAttachments> listOfProjectAttachments;
    private Grid<AgentJobs> gridAgentJobs;
    Checkbox autorefresh = new Checkbox();
    Div textArea = new Div();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private Label lastRefreshLabel;
    private Label countdownLabel;
    private ScheduledExecutorService executor;
    private UI ui ;
    Instant startTime;
    public DefaultView(ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService, AgentJobsService agentJobsService, MSMService msmService) {
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;
        this.agentJobsService = agentJobsService;
        this.msmService = msmService;


        tabSheet = new TabSheet();
        saveBtn = new Button("save");
        editBtn = new Button("edit");
        saveBtn.setVisible(false);
        editBtn.setVisible(true);

        countdownLabel = new Label();
        lastRefreshLabel=new Label();
        countdownLabel.setVisible(false);

        ui= UI.getCurrent();

        vl = new VerticalLayout();
        Article text = new Article();
        //text.add(projects.get().getName());
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

        updateComponents();
        updateAttachmentGrid(listOfProjectAttachments);
        updateAgentJobGrid();
    }

    private void updateComponents() {
        editor.setValue(projects.map(Projects::getDescription).orElse(""));
    }
    private void updateAttachmentGrid(List<ProjectAttachments> projectAttachments) {
        attachmentGrid.setItems(projectAttachments);
    }
    private void updateAgentJobGrid() {
        gridAgentJobs.setItems(agentJobsService.findbyJobName(projects.get().getAgentJobs()));
    }
    private TabSheet getTabsheet() {

        //Edit-Button nur im Tab Description anzeigen:

        tabSheet.addSelectedChangeListener(e->{

            if (e.getSelectedTab().getLabel().contains("Description"))
            {
                editBtn.setVisible(true);
            }
            else {
                editBtn.setVisible(false);
            }

        });
        saveBtn.addClickListener((event -> {

            projects.get().setDescription(editor.getValue());
            projectsService.update(projects.get());

            editBtn.setVisible(true);
            saveBtn.setVisible(false);
            //editor.setReadOnly(true);
            editor.setReadOnlyWithToolbarAction(!editor.isReadOnly());

        }));

        editBtn.addClickListener(e->{
            editor.setReadOnlyWithToolbarAction(!editor.isReadOnly());
            editBtn.setVisible(false);
            saveBtn.setVisible(true);
            //editor.setReadOnly(false);
        });

            tabSheet.add("Description", getProjectsDescription());
            tabSheet.add("Attachments", getProjectAttachements());
            tabSheet.add("DB-Jobs", getAgentJobTab());
            tabSheet.add("QS", new Div(new Text("This is the QS tab content")));

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();

        return tabSheet;
    }

    private Component getAgentJobTab() {
        System.out.println("getAgentJobTab.....start");
        configureAgentJobGrid();

        VerticalLayout content = new VerticalLayout(gridAgentJobs);

        content.setSizeFull();
        content.setHeight("250px");

        content.add(getAgentJobToolbar());

        System.out.println("getAgentJobTab.....end");
        return content;

    }

    private Component getAgentJobToolbar() {
        System.out.println("getAgentJobToolbar.....start");
        Button refreshBtn = new Button("refresh");
        refreshBtn.addClickListener(e->{
            //System.out.println("Refresh Button gedrückt!");

            var agentJobs=getAgentJobs();

            //gridAgentJobs.setItems(getAgentJobs());

            gridAgentJobs.setItems(agentJobs);
            updateLastRefreshLabel();

        });

        countdownLabel.setText("initialisiert");

        autorefresh.setLabel("Auto-Refresh");

        autorefresh.addClickListener(e->{

            if (autorefresh.getValue()){
                System.out.println("Autorefresh wird eingeschaltet.");
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

        System.out.println("getAgentJobToolbar.....end");
        return layout;

    }

    private void startCountdown(Duration duration) {
        System.out.println("startCountdown.....start");
        executor = Executors.newSingleThreadScheduledExecutor();

        startTime = Instant.now();

        updateLastRefreshLabel();

        executor.scheduleAtFixedRate(() -> {

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
        System.out.println("startCountdown.....end");
    }

    private void stopCountdown() {
        System.out.println("stopCountdown.....start");
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        System.out.println("stopCountdown.....end");
    }

    private Duration calculateRemainingTime(Duration duration, Instant startTime) {
        System.out.println("calculateRemainingTime.....start");
        Instant now = Instant.now();
        Instant endTime = startTime.plus(duration);
        System.out.println("calculateRemainingTime.....end");
        return Duration.between(now, endTime);
    }

    private void updateLastRefreshLabel() {
        System.out.println("updateLastRefreshLabel.....start");
        LocalTime currentTime = LocalTime.now();
        String formattedTime = currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        lastRefreshLabel.setText("letzte Aktualisierung: " + formattedTime);
        System.out.println("updateLastRefreshLabel.....end");
    }
    private VerticalLayout getProjectAttachements() {

        VerticalLayout content = new VerticalLayout();
        Div attachmentsTabContent = new Div();
        attachmentsTabContent.setSizeFull();

        // Create a file upload component
        buffer = new MultiFileMemoryBuffer();
        fileUpload = new Upload(buffer);
        fileUpload.setAcceptedFileTypes(".doc", ".xlsx",".xls" , ".ppt", ".msg");

        // Create a grid to display attachments
        attachmentGrid = new Grid<>();
        attachmentGrid.addColumn(ProjectAttachments::getFilename).setHeader("File Name");
        attachmentGrid.addColumn(attachment -> {
            String fileName = attachment.getFilename();
            String[] parts = fileName.split("\\.");
            if (parts.length > 0) {
                return parts[parts.length - 1];
            } else {
                return "";
            }
        }).setHeader("File Type");
        attachmentGrid.addColumn(ProjectAttachments::getDescription).setHeader("Description");

        attachmentGrid.addItemDoubleClickListener(event -> {
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
            confirmationDialog.close(); // Close the confirmation dialog
        });

        GridContextMenu<ProjectAttachments> contextMenu = attachmentGrid.addContextMenu();
        GridMenuItem<ProjectAttachments> removeItem = contextMenu.addItem("Remove", event -> {
            confirmationDialog.open();
        });
        confirmationDialog.add(new Div(confirmButton, cancelButton));

        // Create a CRUD editor for editing the file data
        Crud<ProjectAttachments> crud = new Crud<>(ProjectAttachments.class, createEditor());

        // Add an "Edit" menu item
        GridMenuItem<ProjectAttachments> editItem = contextMenu.addItem("Edit", event -> {
            Optional<ProjectAttachments> selectedAttachmentOptional = event.getItem();
            if (selectedAttachmentOptional.isPresent()) {
                ProjectAttachments selectedAttachment = selectedAttachmentOptional.get();
                System.out.println("edit menu click..");
                crud.edit(selectedAttachment, Crud.EditMode.EXISTING_ITEM);

                crud.getDeleteButton().getElement().getStyle().set("display", "none");
                crud.setToolbarVisible(false);

                crud.getGrid().getElement().getStyle().set("display", "none");
                crud.getNewButton().getElement().getStyle().set("display", "none");

                add(crud);
            }
        });

        //editItem.add(crud);
        crud.addSaveListener(event -> {
            ProjectAttachments editedAttachment = event.getItem();
            projectAttachmentsService.update(editedAttachment);
            attachmentGrid.getDataProvider().refreshItem(editedAttachment);
        });

        fileUpload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            String fileType = event.getMIMEType();
            byte[] fileContent = new byte[0];
            try {
                fileContent = buffer.getInputStream(fileName).readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // Extract description from the user input (you might need a separate input field for this)
            String description = "this file...";

            ProjectAttachments projectAttachments = new ProjectAttachments();
            projectAttachments.setFilename(fileName);
            projectAttachments.setDescription(description);
            projectAttachments.setFilecontent(fileContent);
            projectAttachments.setProject(projects.get());

            projectAttachmentsService.update(projectAttachments);

            // Refresh the 'projects' entity to reflect the changes
            projects = projectsService.findById(projects.get().getId());

            attachmentGrid.setItems(projectsService.getProjectAttachments(projects.get()));

        });
        if(projects != null) {
            attachmentGrid.setItems(projectsService.getProjectAttachments(projects.get()));
        }
        attachmentsTabContent.add(fileUpload, attachmentGrid);
        content.add(attachmentsTabContent);
        return content;
    }

    private VerticalLayout getProjectsDescription() {

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

        content.add(editor,editBtn,saveBtn);

        if(projects != null) {
            editor.setValue(projects.map(Projects::getDescription).orElse(""));
        }

        return content;

    }

    private void configureAgentJobGrid() {
        System.out.println("configureAgentJobGrid.....start");
        gridAgentJobs = new Grid<>(AgentJobs.class);
        gridAgentJobs.addClassNames("PFG-AgentJobs");
        //gridAttachments.setSizeFull();
        gridAgentJobs.setColumns("name", "job_Activity", "duration_Min", "jobStartDate", "jobStopDate", "jobNextRunDate", "result" );

        //select JobName, JobEnabled,JobDescription, JobActivity, DurationMin, JobStartDate, JobLastExecutedStep, JobExecutedStepDate, JobStopDate, JobNextRunDate, Result from job_status

        gridAgentJobs.addColumn(
                new NativeButtonRenderer<>("Run",
                        clickedItem -> {
                            System.out.println("clicked:" + clickedItem.getName());
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
        System.out.println("configureAgentJobGrid.....end");
    }

    private void startJob(String jobName) throws InterruptedException {
        System.out.println("startJob.....start");
        var erg= msmService.startJob(jobName);
        if (!erg.contains("OK"))
        {
            Notification.show(erg, 5000, Notification.Position.MIDDLE);
        }

        Article article=new Article();
        article.setText(LocalDateTime.now().format(formatter) + ": Job " + jobName + " gestartet..." );
        textArea.add (article);

        Thread.sleep(2000);
        gridAgentJobs.setItems(getAgentJobs());
        System.out.println("startJob.....end");
    }

    private List<AgentJobs> getAgentJobs() {
        System.out.println("getAgentJobs.....start");
        if (projects != null) {
            String jobName = projects.get().getAgentJobs();
            System.out.println("@@@.............."+jobName);
            return agentJobsService.findbyJobName(jobName);
        }
        else {
            Projects projects = projectsService.search("PFG_Cube");
            System.out.println("getAgentJobs.....end");
            System.out.println("@@@ ....optional................"+projects.getAgentJobs());
            return agentJobsService.findbyJobName(projects.getAgentJobs());
        }
    }
    private CrudEditor<ProjectAttachments> createEditor() {
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
        return new BinderCrudEditor<>(editBinder, editFormLayout);
    }

}
