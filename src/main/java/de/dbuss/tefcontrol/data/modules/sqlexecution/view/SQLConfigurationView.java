package de.dbuss.tefcontrol.data.modules.sqlexecution.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.modules.sqlexecution.entity.Configuration;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.stream.Collectors;


@PageTitle("SQL Connection Configuration")
@Route(value = "config/:project_Id", layout= MainLayout.class)
@RolesAllowed("ADMIN")
public class SQLConfigurationView extends VerticalLayout {

    private final ProjectConnectionService projectConnectionService;
    private final ProjectParameterService projectParameterService;

    ConfigForm cf;
    Configuration config;

    Grid<Configuration> grid = new Grid<>(Configuration.class);
    TextField filterText = new TextField();
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String configTable;

    public SQLConfigurationView(ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService) {
        this.projectConnectionService = projectConnectionService;
        this.projectParameterService = projectParameterService;

        addClassName("configuration-view");
        setSizeFull();

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.SQL_EXECUTION.equals(projectParameter.getNamespace()))
                .collect(Collectors.toList());

        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : filteredProjectParameters) {
            if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                dbServer = projectParameter.getValue();
            } else if (Constants.DB_NAME.equals(projectParameter.getName())) {
                dbName = projectParameter.getValue();
            } else if (Constants.DB_USER.equals(projectParameter.getName())) {
                dbUser = projectParameter.getValue();
            } else if (Constants.DB_PASSWORD.equals(projectParameter.getName())) {
                dbPassword = projectParameter.getValue();
            } else if (Constants.TABLE.equals(projectParameter.getName())) {
                configTable = projectParameter.getValue();
            }
        }

        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";

        configureGrid();
        configureForm();

        add(getToolbar(), getContent());

        updateList();
        closeEditor();
        //    cf = new ConfigForm();
        //    cf.setWidth("25em");
        //    add(new H1("Konfiguration"));
        //    add (cf);
        //    updateView();

        // closeEditor();

        //     config=new Configuration("User","Password","URL");
        //     cf.setConfiguration(config);

    }

    private Component getContent() {
        HorizontalLayout content = new HorizontalLayout(grid, cf);
        content.setFlexGrow(2, grid);


        content.setFlexGrow(1, cf);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private void configureGrid() {
        grid.addClassNames("configuration-grid");
        grid.setSizeFull();
        grid.setColumns("name", "userName","db_Url");

        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.asSingleSelect().addValueChangeListener(e->editConfig(e.getValue()));
    }

    private void updateList() {

        grid.setItems(projectConnectionService.getSqlConnectionConfiguration(dbUrl, dbUser, dbPassword, configTable));
    }

    private void configureForm() {
        cf = new ConfigForm();
        cf.setWidth("25em");

        cf.addListener(ConfigForm.SaveEvent.class, this::saveConfig);
        //   cf.addListener(ConfigForm.DeleteEvent.class, this::deleteContact);
        cf.addListener(ConfigForm.CloseEvent.class, e-> closeEditor());

    }
    private void saveConfig(ConfigForm.SaveEvent event) {
        projectConnectionService.saveSqlConnectionConfiguration(dbUrl, dbUser, dbPassword, configTable, event.getConfiguration());
        updateList();
        closeEditor();
    }
    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addContactButton = new Button("Neu");
        addContactButton.addClickListener((e->addContact()));
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addContactButton);


        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addContact() {
        grid.asSingleSelect().clear();
        editConfig(new Configuration());
    }
    private void closeEditor() {
        cf.setConfiguration(null);
        cf.setVisible(false);
        removeClassName("editing");
    }

    private void updateView() {

    }
    private void editConfig(Configuration conf) {
        if(conf == null){
            closeEditor();
        } else {
            cf.setConfiguration(conf);
            cf.setVisible(true);
            addClassName("editing");
        }
    }
}
