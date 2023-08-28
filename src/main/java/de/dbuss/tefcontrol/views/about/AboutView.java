package de.dbuss.tefcontrol.views.about;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.dbuss.tefcontrol.data.entity.Department;
import de.dbuss.tefcontrol.data.service.DepartmentService;
import de.dbuss.tefcontrol.views.MainLayout;
import org.springframework.beans.factory.annotation.Value;

import org.vaadin.tatu.Tree;

import java.util.Optional;

@PageTitle("About")
@Route(value = "about", layout = MainLayout.class)
@AnonymousAllowed
public class AboutView extends VerticalLayout {

    private final DepartmentService departmentService;

    @Value("${myuser.name}")
    private String Username;

    private TreeGrid<Department> grid;

    public AboutView(DepartmentService departmentService) {

        super();
        this.departmentService = departmentService;

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
        Optional<Department> department = departmentService.findById(id);

    //    editor.setValue(department.get().getRichText());
    //    DepartmentData departmentData = new DepartmentData();
        Tree<Department> tree = new Tree<>(Department::getName);
        tree.setItems(departmentService.getRootDepartments(),departmentService::getChildDepartments);

    //    tree.setItemIconProvider(item -> getIcon(item));
    //    tree.setItemIconSrcProvider(item -> getImageIconSrc(item));
        tree.setItemTooltipProvider(Department::getDescription);

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
      //  DepartmentData departmentServicedepartmentData = new DepartmentData();

        grid = new TreeGrid<>();

        grid.setItems(departmentService.getRootDepartments(), departmentService::getChildDepartments);
        grid.addHierarchyColumn(Department::getName).setHeader("Department Name");
      //  grid.addColumn(Department::getManager).setHeader("Manager");

        grid
                .asSingleSelect()
                .addValueChangeListener(
                        event -> {
                            refreshChildItems(event.getOldValue());
                            refreshChildItems(event.getValue());
                        }
                );

        grid.setClassNameGenerator(
                department -> {
                    if (
                            grid.asSingleSelect().getValue() != null &&
                                    grid.asSingleSelect().getValue().equals(department.getParent_id())
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

    private void refreshChildItems(Department department) {
        if (department != null) {
            grid.getTreeData().getChildren(department).forEach(child -> {
                grid.getDataProvider().refreshItem(child, false);
            });
        }
    }


}
