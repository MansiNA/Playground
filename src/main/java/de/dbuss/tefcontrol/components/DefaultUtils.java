package de.dbuss.tefcontrol.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.server.StreamResource;
import com.wontlost.ckeditor.Config;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;
import de.dbuss.tefcontrol.data.Role;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.ProjectAttachments;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.entity.User;
import de.dbuss.tefcontrol.data.service.*;
import de.dbuss.tefcontrol.views.MainLayout;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DefaultUtils extends VerticalLayout {

    private final ProjectAttachmentsService projectAttachmentsService;
    private final ProjectsService projectsService;
    private VaadinCKEditor editor;
    private Grid<ProjectAttachmentsDTO> attachmentGrid;
    private Optional<Projects> projects;
    private int projectId;
    private Anchor downloadLink;
    private UI ui;

    public DefaultUtils(ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService) {
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;
    }

    public Component getProjectDescription() {
        Button saveBtn = new Button("save");
        Button editBtn = new Button("edit");
        saveBtn.setVisible(false);
        editBtn.setVisible(true);

        editBtn.setVisible(MainLayout.isAdmin);

        VerticalLayout content = new VerticalLayout();

        Config config = new Config();
        config.setBalloonToolBar(com.wontlost.ckeditor.Constants.Toolbar.values());
        config.setImage(new String[][]{},
                "", new String[]{"full", "alignLeft", "alignCenter", "alignRight"},
                new String[]{"imageTextAlternative", "|",
                        "imageStyle:alignLeft",
                        "imageStyle:full",
                        "imageStyle:alignCenter",
                        "imageStyle:alignRight"}, new String[]{});

        editor = new VaadinCKEditorBuilder().with(builder -> {

            builder.editorType = com.wontlost.ckeditor.Constants.EditorType.CLASSIC;
            builder.width = "95%";
            builder.readOnly = true;
            builder.hideToolbar=true;
            builder.config = config;
        }).createVaadinCKEditor();

        editor.setReadOnly(true);

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

        content.add(editor,editBtn,saveBtn);

        if(projects != null && projects.isPresent()) {
            editor.setValue(projects.map(Projects::getDescription).orElse(""));
        }
        return content;

    }

    public VerticalLayout getProjectAttachements() {

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
            ProjectAttachmentsDTO selectedAttachment = event.getItem();
            ProjectAttachments projectAttachments = projectAttachmentsService.findById(event.getItem().getId());
            if (selectedAttachment != null) {
                byte[] fileContent = projectAttachments.getFilecontent();
                String fileName = projectAttachments.getFilename();
                System.out.println(fileName+"------------------------------#####################");
                // Create a StreamResource with a unique key to avoid caching issues
                StreamResource streamResource = new StreamResource(fileName, () -> new ByteArrayInputStream(fileContent));

                // Create an Anchor component for downloading the file
                downloadLink = new Anchor(streamResource, fileName);
                downloadLink.getElement().setAttribute("download", true);
                downloadLink.getElement().getStyle().set("display", "none");
                // Add the download link to the UI
               // add(downloadLink);
              //  ui.add(downloadLink);
                UI.getCurrent().add(downloadLink);

                // Trigger the download link
                UI.getCurrent().getPage().executeJs("arguments[0].click()", downloadLink);
                // Trigger the download link
              //  ui.getPage().executeJs("arguments[0].click()", downloadLink);
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
            Optional<ProjectAttachmentsDTO> selectedAttachmentOptional = attachmentGrid.getSelectionModel().getFirstSelectedItem();
            if (selectedAttachmentOptional.isPresent()) {
                ProjectAttachmentsDTO selectedAttachment = selectedAttachmentOptional.get();
                System.out.println(selectedAttachment.getId());
                projectAttachmentsService.delete(selectedAttachment.getId());
                // Refresh the 'projects' entity to reflect the changes
                projects = projectsService.findById(projects.isPresent() ? projects.get().getId() : 1);
                if (projects.isPresent()) {
                    List<ProjectAttachmentsDTO> listOfAttachments = projectsService.getProjectAttachmentsWithoutFileContent(projects.get());
                    attachmentGrid.setItems(listOfAttachments);
                }

            }
            confirmationDialog.close(); // Close the confirmation dialog
        });

        cancelButton.addClickListener(cancelEvent -> {
            confirmationDialog.close(); // Close the confirmation dialog
        });

        GridContextMenu<ProjectAttachmentsDTO> contextMenu = attachmentGrid.addContextMenu();
        GridMenuItem<ProjectAttachmentsDTO> removeItem = contextMenu.addItem("Remove", event -> {
            confirmationDialog.open();
        });
        confirmationDialog.add(new Div(confirmButton, cancelButton));

        // Create a CRUD editor for editing the file data
        Crud<ProjectAttachmentsDTO> crud = new Crud<>(ProjectAttachmentsDTO.class, createEditor());

        // Add an "Edit" menu item
        GridMenuItem<ProjectAttachmentsDTO> editItem = contextMenu.addItem("Edit", event -> {
            Optional<ProjectAttachmentsDTO> selectedAttachmentOptional = event.getItem();
            if (selectedAttachmentOptional.isPresent()) {
                ProjectAttachmentsDTO selectedAttachment = selectedAttachmentOptional.get();
                crud.edit(selectedAttachment, Crud.EditMode.EXISTING_ITEM);

                crud.getDeleteButton().getElement().getStyle().set("display", "none");
                crud.setToolbarVisible(false);
                crud.getGrid().getElement().getStyle().set("display", "none");
                crud.getNewButton().getElement().getStyle().set("display", "none");

                UI.getCurrent().add(crud);
            }
        });

        crud.addSaveListener(event -> {
            ProjectAttachmentsDTO editedAttachment = event.getItem();
            System.out.println(editedAttachment.getId()+"....."+editedAttachment.getFilename()+"..."+editedAttachment.getProjectId()+"..."+editedAttachment.getDescription()+"..."+editedAttachment.getFilesizeKb()+"..."+editedAttachment.getFileContent());
            projectAttachmentsService.updateGridValues(editedAttachment);
            attachmentGrid.getDataProvider().refreshItem(editedAttachment);
        });

        fileUpload.addSucceededListener(event -> {
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
        if(projects != null && projects.isPresent()) {
            attachmentGrid.setItems(projectsService.getProjectAttachmentsWithoutFileContent(projects.get()));
        }
        attachmentsTabContent.add(attachmentGrid,fileUpload);
        content.add(attachmentsTabContent);

        return content;
    }

    private CrudEditor<ProjectAttachmentsDTO> createEditor() {
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
        return new BinderCrudEditor<>(editBinder, editFormLayout);
    }

    public void setAttachmentGridItems(List<ProjectAttachmentsDTO> projectAttachmentsDTOS) {
        attachmentGrid.setItems(projectAttachmentsDTOS);
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
        projects = projectsService.findById(projectId);
    }

    public void setDescription() {
        editor.setValue(projects.get().getDescription());
    }
}
