package de.dbuss.tefcontrol.data.modules.cobi_administration.view;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.dbuss.tefcontrol.data.modules.cobi_administration.entity.CurrentPeriods;
import de.dbuss.tefcontrol.data.modules.cobi_administration.entity.CurrentScenarios;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.List;

@PageTitle("Administration")
@Route(value = "CobiAdministration/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "MAPPING", "FLIP"})
public class CobiAdminView extends VerticalLayout {
    private Button startBtn;

    public CobiAdminView() {

        startBtn = new Button("Start");

        H1 h1 = new H1("Cobi Administration");
        Article p1 = new Article();
        p1.setText("Auf diese Seite lässt verschiedene Einstellungen zur COBI-Beladung vornehmen.");

        H3 p2 = new H3();
        p2.setText("Dim Period:");

        Grid<CurrentPeriods> grid_period = new Grid<>(CurrentPeriods.class, false);
        grid_period.addColumn(CurrentPeriods::getCurrent_month).setHeader("Current-Month");
        grid_period.addColumn(CurrentPeriods::getPreliminary_month).setHeader("Preliminary-Month");
        grid_period.setWidth("250px");
        grid_period.setHeight("55px");
        grid_period.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        grid_period.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid_period.getColumns().forEach(e -> e.setResizable(Boolean.TRUE));

        List<CurrentPeriods> periods = new ArrayList<>();
        CurrentPeriods cp = new CurrentPeriods();
        cp.setCurrent_month("202312");
        cp.setPreliminary_month("202311");
        periods.add(cp);
        grid_period.setItems(periods);

        add(h1,p1, p2, grid_period);

        H3 p3 = new H3();
        p3.setText("Dim Scenario:");

        Grid<CurrentScenarios> grid_scenario = new Grid<>(CurrentScenarios.class, false);
        grid_scenario.addColumn(CurrentScenarios::getCurrent_QFC).setHeader("Current QFC");
        grid_scenario.addColumn(CurrentScenarios::getCurrent_Plan).setHeader("Current Plan");
        grid_scenario.addColumn(CurrentScenarios::getCurrent_Outlook).setHeader("Current Outlook");
        grid_scenario.setWidth("500px");
        grid_scenario.setHeight("55px");
        grid_scenario.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        grid_scenario.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid_scenario.getColumns().forEach(e -> e.setResizable(Boolean.TRUE));

        List<CurrentScenarios> scenarios = new ArrayList<>();
        CurrentScenarios cs = new CurrentScenarios();
        cs.setCurrent_Outlook("OUTLOOK XY");
        cs.setCurrent_QFC("QFCII_04_08");
        cs.setCurrent_Plan("0815_Plan1");
        scenarios.add(cs);
        grid_scenario.setItems(scenarios);


        add(p3,grid_scenario);

        Button okBtn = new Button("OK");

        add(okBtn);




    }
}
