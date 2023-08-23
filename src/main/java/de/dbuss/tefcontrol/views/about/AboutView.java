package de.dbuss.tefcontrol.views.about;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import de.dbuss.tefcontrol.data.entity.Department;
import de.dbuss.tefcontrol.data.repository.DepartmentData;
import de.dbuss.tefcontrol.views.MainLayout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.vaadin.tatu.Tree;

@PageTitle("About")
@Route(value = "about", layout = MainLayout.class)
@AnonymousAllowed

public class AboutView extends VerticalLayout {

    @Value("${myuser.name}")
    private String Username;

    private TreeGrid<Department> grid;

    public AboutView() {


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
        DepartmentData departmentData = new DepartmentData();
        Tree<Department> tree = new Tree<>(Department::getName);
        tree.setItems(departmentData.getRootDepartments(),
                departmentData::getChildDepartments);

    //    tree.setItemIconProvider(item -> getIcon(item));
    //    tree.setItemIconSrcProvider(item -> getImageIconSrc(item));
        tree.setItemTooltipProvider(Department::getManager);

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
        DepartmentData departmentData = new DepartmentData();

        grid = new TreeGrid<>();

        grid.setItems(departmentData.getRootDepartments(), departmentData::getChildDepartments);
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
                                    grid.asSingleSelect().getValue().equals(department.getParent())
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
