package de.dbuss.tefcontrol.views.defaultPackage;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.model.Label;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.wontlost.ckeditor.Config;
import com.wontlost.ckeditor.Constants;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;
import de.dbuss.tefcontrol.data.entity.CLTV_HW_Measures;
import de.dbuss.tefcontrol.data.entity.ProjectAttachments;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.service.ProjectAttachmentsService;
import de.dbuss.tefcontrol.data.service.ProjectsService;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

//@PageTitle("Default Mapping")
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
    private VerticalLayout vl;
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
            tabSheet.add("DB-Jobs", new Div(new Text("This is the Job-Info/Execution tab")));
            tabSheet.add("QS", getProjectSQL());

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();

        return tabSheet;
    }

    private Component getProjectSQL() {

        VerticalLayout content = new VerticalLayout();
        Div sqlTabContent = new Div();
        sqlTabContent.setSizeFull();

        Select<String> select = new Select<>();
        select.setLabel("Choose Query SQL");
        select.setItems("Abfrage in der Quelle", "Abfrage im Ziel, mit vielen Informationen in dieser Beschreibung" );
        select.setPlaceholder("name from table project_sqls for selected project_id");
        select.setWidthFull();



        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Description",getSqlDescription());
        tabSheet.add("Connection",getsqllConnection());
        tabSheet.add("Query",getSqlQuery());

        sqlTabContent.add(select,tabSheet);
        sqlTabContent.setHeightFull();
        content.setHeightFull();
        content.add(sqlTabContent);
        return content;

    }

    private Component getsqllConnection() {
        VerticalLayout content = new VerticalLayout();
        Select<String> select = new Select<>();

        select.setItems("demuc5ak26", "dewsttwak11", "Test" );
        select.setPlaceholder("name from project_connections");
        select.setWidth("650 px");

        TextArea textArea = new TextArea();
        textArea.setWidthFull();
        textArea.setHeight("400 px");
        textArea.setValue("Description for selected connection (from Table [project_connections] for selected id");
        textArea.setReadOnly(true);

        content.add(select, textArea);
        content.setWidth("650 px");
        return content;
    }

    private Component getSqlQuery() {
        VerticalLayout content = new VerticalLayout();

        Div sqlTabContent = new Div();
        sqlTabContent.setSizeFull();

        VerticalLayout vl = new VerticalLayout();

        HorizontalLayout hl = new HorizontalLayout();

        Button executeButton = new Button("Execute");
        executeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button exportButton = new Button("Export");
        exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        hl.add(executeButton, exportButton);

        TextArea textArea = new TextArea();
        textArea.setWidthFull();
        textArea.setHeight("300 px");
        textArea.setValue("SQL from Table [project_sqls] Column sql for selected id");

        vl.add(textArea,hl);

        TextArea gridArea = new TextArea();
        gridArea.setWidthFull();
        gridArea.setHeight("600 px");
        gridArea.setValue("Show Result of Query in Dynamic-Grid");



        SplitLayout splitLayout = new SplitLayout(vl, gridArea);
        splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);


        splitLayout.setHeightFull();
        splitLayout.setWidthFull();


     //   content.setHeightFull();

        sqlTabContent.add(splitLayout);

        content.add(sqlTabContent);
        return content;

    }

    private VerticalLayout getSqlDescription() {

        VerticalLayout content = new VerticalLayout();
        TextArea textArea = new TextArea();
        textArea.setWidthFull();
        textArea.setHeight("250px");
        textArea.setValue("Description from Table [project_sqls] Column description for selected id");

        content.add(textArea);
        return content;
    }


    private VerticalLayout getProjectAttachements() {

        VerticalLayout content = new VerticalLayout();
        Div attachmentsTabContent = new Div();
        attachmentsTabContent.setSizeFull();

        // Create a file upload component
        buffer = new MultiFileMemoryBuffer();
        fileUpload = new Upload(buffer);
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
            String description = "no description yet";

            ProjectAttachments projectAttachments = new ProjectAttachments();
            projectAttachments.setFilename(fileName);
            projectAttachments.setDescription(description);
            projectAttachments.setFilecontent(fileContent);
            projectAttachments.setUpload_date(new Date());
            projectAttachments.setProject(projects.get());

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
