package de.dbuss.tefcontrol.views.about;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.service.ProjectsService;
import de.dbuss.tefcontrol.views.MainLayout;
import org.springframework.beans.factory.annotation.Value;

import org.vaadin.tatu.Tree;

import java.util.Optional;

@PageTitle("About")
@Route(value = "about", layout = MainLayout.class)
@AnonymousAllowed
public class AboutView extends VerticalLayout {

    private final ProjectsService projectsService;

    @Value("${myuser.name}")
    private String Username;

    private TreeGrid<Projects> grid;

    public AboutView(ProjectsService projectsService) {

        super();
        this.projectsService = projectsService;

       // this.setSizeFull();
     //   createBasicTreeGridUsage();
         createTree();

       /* H2 header = new H2("Hallo " + Username);
        header.addClassNames(Margin.Top.XLARGE, Margin.Bottom.MEDIUM);
        add(header);
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        getStyle().set("text-align", "center");*/
    }

    private void createTree() {

        Long id = 1L;
        Optional<Projects> projects = projectsService.findById(id);

    //    editor.setValue(projects.get().getRichText());
        Tree<Projects> tree = new Tree<>(Projects::getName);
        tree.setItems(projectsService.getRootProjects(),projectsService::getChildProjects);

    //    tree.setItemIconProvider(item -> getIcon(item));
    //    tree.setItemIconSrcProvider(item -> getImageIconSrc(item));
        tree.setItemTooltipProvider(Projects::getDescription);

        tree.addExpandListener(event ->
                        System.out.println(String.format("Expanded %s item(s)", event.getItems().size()))
                        );
        tree.addCollapseListener(event ->
                        System.out.println(String.format("Collapsed %s item(s)", event.getItems().size()))
                        );

        tree.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null)
                System.out.println(event.getValue().getName() + " selected");



        });
        tree.setAllRowsVisible(true);
        //tree.setSizeFull();
        tree.setWidth("350px");
        add(tree);
    }


    private void createBasicTreeGridUsage() {

        grid = new TreeGrid<>();

        grid.setItems(projectsService.getRootProjects(), projectsService::getChildProjects);
        grid.addHierarchyColumn(Projects::getName).setHeader("Projects Name");
      //  grid.addColumn(Projects::getManager).setHeader("Manager");

        grid
                .asSingleSelect()
                .addValueChangeListener(
                        event -> {
                            refreshChildItems(event.getOldValue());
                            refreshChildItems(event.getValue());
                        }
                );

        grid.setClassNameGenerator(
                projects -> {
                    if (
                            grid.asSingleSelect().getValue() != null &&
                                    grid.asSingleSelect().getValue().equals(projects.getParent_id())
                    ) {
                        return "parent-selected";
                    } else return null;
                }
        );
       // grid.setAllRowsVisible(true);
        grid.setWidth("400px");
        grid.setHeight("500px");
        add(grid);
    }

    private void refreshChildItems(Projects projects) {
        if (projects != null) {
            grid.getTreeData().getChildren(projects).forEach(child -> {
                grid.getDataProvider().refreshItem(child, false);
            });
        }
    }


}
