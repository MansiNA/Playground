package de.dbuss.tefcontrol.views;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.wontlost.ckeditor.Config;
import com.wontlost.ckeditor.Constants;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;
import de.dbuss.tefcontrol.data.Role;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.entity.User;
import de.dbuss.tefcontrol.data.service.ProjectsService;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import jakarta.annotation.security.RolesAllowed;

import java.util.Optional;
import java.util.Set;

@PageTitle("Welcome")
@Route(value = "Welcome", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
 @AnonymousAllowed
public class WelcomeView extends VerticalLayout {
    private VaadinCKEditor editor;
    private AuthenticatedUser authenticatedUser;
    private ProjectsService projectsService;
    private boolean isAdmin;

    public WelcomeView(AuthenticatedUser authenticatedUser, ProjectsService projectsService) {
        this.authenticatedUser = authenticatedUser;
        this.projectsService = projectsService;

        add(new H2("Hallo und willkommen"));

        String yourContent ="Auf dieser Seite werden Tools und Hilfsmittel bereitgestellt, um Administrative-Tätigkeiten zu vereinfachen.<br />" +
                "Ebenso werden Funktionen bereitgestellt, Informationen direkt aus DB-Tabellen zu entnehmen.<br />" +
                "Ideen, Anregungen oder Verbesserungsvorschläge sind herzlich willkommen!&#128512;<br /><br />" +
                "Bitte mit Telefonica AD-User einloggen und die gewünschte Funktionalität auswählen.<br /><br />" +
                "Viele Grüße<br /><b>Euer Consys-Team</b>" ;

        Html html = new Html("<text>" + yourContent + "</text>");
        add(html);

        // Add RouterLink to ConfigurationView
        RouterLink configLink = new RouterLink("Go to Configuration", ConfigurationGridView.class);
        add(configLink);

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            System.out.println("User: " + user.getName());
            Set<Role> roles = user.getRoles();
            isAdmin = roles.stream()
                    .anyMatch(role -> role == Role.ADMIN);
            add( getProjectsDescription());
        }


    }

    private VerticalLayout getProjectsDescription() {

        Projects projects = projectsService.findByName("WelcomeView");
        Button saveBtn = new Button("save");
        Button editBtn = new Button("edit");
        saveBtn.setVisible(false);
        editBtn.setVisible(false);
        if (isAdmin) {
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

            projects.setDescription(editor.getValue());
            projectsService.update(projects);

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

        editor.setValue(projects != null ? projects.getDescription() : "");
        return content;

    }

}
