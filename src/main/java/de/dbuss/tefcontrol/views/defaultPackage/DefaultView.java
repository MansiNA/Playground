package de.dbuss.tefcontrol.views.defaultPackage;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.*;
import com.wontlost.ckeditor.Config;
import com.wontlost.ckeditor.Constants;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.service.ProjectsService;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.Optional;

@PageTitle("Default Mapping")
@Route(value = "Default-Mapping/:project_Id", layout = MainLayout.class)
@RolesAllowed("USER")
public class DefaultView extends VerticalLayout  implements BeforeEnterObserver  {

    private ProjectsService projectsService;
    private TabSheet tabSheet;
    private Button saveBtn;
    private Button editBtn;
    private VaadinCKEditor editor;
    private Optional<Projects> projects;
    private HorizontalLayout hl;

    public DefaultView(ProjectsService projectsService) {
        this.projectsService = projectsService;

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
        System.out.println("constructor........");

    }
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        String projectId = parameters.get("project_Id").orElse(null);
        if (projectId != null) {
            projects = projectsService.findById(Long.parseLong(projectId));
        }

        updateComponents();
    }

    private void updateComponents() {
        editor.setValue(projects.map(Projects::getDescription).orElse(""));
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
            tabSheet.add("Attachments", new Div(new Text("This is the Attachments tab content")));
            tabSheet.add("DB-Jobs", new Div(new Text("This is the Job-Info/Execution tab")));
            tabSheet.add("QS", new Div(new Text("This is the QS tab content")));

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();

        return tabSheet;
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
