package de.dbuss.tefcontrol.views.defaultPackage;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.wontlost.ckeditor.Config;
import com.wontlost.ckeditor.Constants;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;
import de.dbuss.tefcontrol.data.entity.ProjectAttachments;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.service.ProjectAttachmentsService;
import de.dbuss.tefcontrol.data.service.ProjectsService;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@PageTitle("Default Mapping")
@Route(value = "Default-Mapping/:project_Id", layout = MainLayout.class)
@RolesAllowed("USER")
public class DefaultView extends VerticalLayout  implements BeforeEnterObserver  {

    private ProjectsService projectsService;
    private ProjectAttachmentsService projectAttachmentsService;
    private TabSheet tabSheet;
    private Button saveBtn;
    private Button editBtn;
    private VaadinCKEditor editor;
    private Optional<Projects> projects;
    private HorizontalLayout hl;

    private Upload fileUpload;
    private Grid<ProjectAttachments> attachmentGrid;
    private MultiFileMemoryBuffer buffer;
    private List<ProjectAttachments> listOfProjectAttachments;


    public DefaultView(ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService) {
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;


        tabSheet = new TabSheet();
        saveBtn = new Button("save");
        editBtn = new Button("edit");
        saveBtn.setVisible(false);
        editBtn.setVisible(true);

        hl = new HorizontalLayout();
        hl.add(getTabsheet());

        hl.setHeightFull();
        hl.setSizeFull();

        add(hl);
    }
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        String projectId = parameters.get("project_Id").orElse(null);
        if (projectId != null) {
            projects = projectsService.findById(Long.parseLong(projectId));
        }

        if (projects.isPresent()) {
            listOfProjectAttachments = projectsService.getProjectAttachments(projects.get());
        }

        updateComponents();
        updateAttachmentGrid(listOfProjectAttachments);
    }

    private void updateComponents() {
        editor.setValue(projects.map(Projects::getDescription).orElse(""));
    }
    private void updateAttachmentGrid(List<ProjectAttachments> projectAttachments) {
        attachmentGrid.setItems(projectAttachments);
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
            editor.setReadOnly(true);

        }));

        editBtn.addClickListener(e->{
            editBtn.setVisible(false);
            saveBtn.setVisible(true);
            editor.setReadOnly(false);
        });

            tabSheet.add("Description", getProjectsDescription());
            tabSheet.add("Attachments", getProjectAttachements());
            tabSheet.add("DB-Jobs", new Div(new Text("This is the Job-Info/Execution tab")));
            tabSheet.add("QS", new Div(new Text("This is the QS tab content")));

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();

        return tabSheet;
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

        GridContextMenu<ProjectAttachments> contextMenu = attachmentGrid.addContextMenu();

        // Create a menu item for removing items
        GridMenuItem<ProjectAttachments> removeItem = contextMenu.addItem("Remove", event -> {
            Optional<ProjectAttachments> selectedAttachmentOptional = event.getItem();
            if (selectedAttachmentOptional.isPresent()) {
                ProjectAttachments selectedAttachment = selectedAttachmentOptional.get();
                projectAttachmentsService.delete(selectedAttachment.getId());
                // Refresh the 'projects' entity to reflect the changes
                projects = projectsService.findById(projects.get().getId());
                attachmentGrid.setItems(projectsService.getProjectAttachments(projects.get()));
            }
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
            String description = "this file..."; // Get description from user input

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

        Button changeReadonlyMode = new Button("change readonly mode");

        changeReadonlyMode.addClickListener((event -> {
            editor.setReadOnlyWithToolbarAction(!editor.isReadOnly());
        }));

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

        content.add(changeReadonlyMode,editor,editBtn,saveBtn);

        if(projects != null) {
            editor.setValue(projects.map(Projects::getDescription).orElse(""));
        }

        //content.add(saveBtn);

        //content.setSizeFull();
        //content.setHeightFull();

        return content;

    }

}
