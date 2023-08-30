package de.dbuss.tefcontrol.views.defaultPackage;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.service.CLTV_HW_MeasureService;
import de.dbuss.tefcontrol.data.service.ProjectsService;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.Optional;

@PageTitle("Default Mapping")
@Route(value = "Default-Mapping/:project_Id", layout = MainLayout.class)
@RolesAllowed("USER")
public class DefaultView extends VerticalLayout implements BeforeEnterObserver {

    private ProjectsService projectsService;
    private Tab descriptionTab = new Tab("Description");
    private Tab attachmentsTab = new Tab("Attachments");
    private Tab dbJobsTab = new Tab("DB-Jobs");

    private Tab qsTab = new Tab("QS");

    private Optional<Projects> projects;

    private VerticalLayout tabContentLayout = new VerticalLayout();

    public DefaultView(ProjectsService projectsService) {
        this.projectsService = projectsService;

        tabContentLayout.setSizeFull();
        add(createTabSheet(), tabContentLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        String projectId = parameters.get("project_Id").orElse(null);
        if (projectId != null) {
             projects = projectsService.findById(Long.parseLong(projectId));
        }
        setSelectedTabContent(createDescriptionTabContent());
    }
    private Tabs createTabSheet() {
        Tabs tabs = new Tabs(descriptionTab, attachmentsTab, dbJobsTab, qsTab);
        tabs.setOrientation(Tabs.Orientation.HORIZONTAL);
        tabs.addSelectedChangeListener(event -> setSelectedTabContent(getSelectedTabContent(event.getSelectedTab())));
        return tabs;
    }

    private void setSelectedTabContent(Component content) {
        tabContentLayout.removeAll();
        tabContentLayout.add(content);
    }

    private Component getSelectedTabContent(Tab selectedTab) {
        if (selectedTab.equals(descriptionTab)) {
            return createDescriptionTabContent();
        } else if (selectedTab.equals(attachmentsTab)) {
            return createAttachmentsTabContent();
        } else if (selectedTab.equals(dbJobsTab)) {
            return createDbJobsTabContent();
        } else if (selectedTab.equals(qsTab)) {
            return createQsTabContent();
        }
        return new Div(); // Default content
    }

    private VerticalLayout createDescriptionTabContent() {
        VerticalLayout descriptionContentLayout = new VerticalLayout();
        TextArea descriptionTextArea = new TextArea();
        descriptionTextArea.setWidthFull();
        if (projects != null) {
            descriptionTextArea.setValue(projects.map(Projects::getDescription).orElse(""));
        } else {
            descriptionTextArea.setValue(""); // Set a default value if projects is not present
        }
        descriptionContentLayout.add(descriptionTextArea);
        descriptionContentLayout.add(new Button("edit"));
        return descriptionContentLayout;
    }

    private VerticalLayout createAttachmentsTabContent() {
        VerticalLayout attachmentsContentLayout = new VerticalLayout();
        attachmentsContentLayout.add(new Text("This is the attachments tab content."));
        attachmentsContentLayout.add(new Button("Click Me in Attachments Tab"));
        return attachmentsContentLayout;
    }

    private VerticalLayout createDbJobsTabContent() {
        VerticalLayout dbJobsContentLayout = new VerticalLayout();
        dbJobsContentLayout.add(new Text("This is the DB-Jobs tab content."));
        dbJobsContentLayout.add(new Button("Click Me in DB-Jobs Tab"));
        return dbJobsContentLayout;
    }

    private VerticalLayout createQsTabContent() {
        VerticalLayout dbJobsContentLayout = new VerticalLayout();
        dbJobsContentLayout.add(new Text("This is the QS tab content."));
        dbJobsContentLayout.add(new Button("Click Me in QS Tab"));
        return dbJobsContentLayout;
    }

}
