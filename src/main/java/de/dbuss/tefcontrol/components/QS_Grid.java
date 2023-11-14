package de.dbuss.tefcontrol.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.dbuss.tefcontrol.data.entity.ProjectQSEntity;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.QS;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;

import java.util.ArrayList;
import java.util.List;

public class QS_Grid extends Composite<Div> {

    private List<ProjectQSEntity> listOfProjectQs;
    Dialog qsDialog = new Dialog();
    QS_Callback qs_callback;
    int projectId;

    public QS_Grid() {
    }

    public void createDialog(QS_Callback callback, int projectId) {
        this.qs_callback=callback;
        this.projectId=projectId;

        VerticalLayout dialogLayout = createDialogLayout();
        qsDialog.add(dialogLayout);
        qsDialog.setDraggable(true);
        qsDialog.setResizable(true);
        qsDialog.setVisible(false);
        qsDialog.setHeaderTitle("QS for Project-ID " + projectId );
        getContent().add(qsDialog);
    }

    public void showDialog(boolean show)
    {
        if (show){
            qsDialog.open();
            qsDialog.setVisible(true);

        }
        else
        {
            qsDialog.close();
            qsDialog.setVisible(false);
        }
    }

    private VerticalLayout createDialogLayout() {

        Grid<ProjectQSEntity> grid = new Grid<>(ProjectQSEntity.class, false);
        grid.addColumn(ProjectQSEntity::getName).setHeader("QS-Name");
       // grid.addColumn(ProjectQSEntity::getResult).setHeader("Result");

        grid.addComponentColumn(projectQs -> {

            HorizontalLayout layout = new HorizontalLayout();
            Icon icon = new Icon();
            String status = projectQs.getResult();

            if ("Failed".equals(status)) {
                icon = VaadinIcon.CLOSE_CIRCLE.create();
                icon.getElement().getThemeList().add("badge error");
                layout.add(icon);
            } else if ("Ok".equals(status)) {
                icon = VaadinIcon.CHECK.create();
                icon.getElement().getThemeList().add("badge success");
                layout.add(icon);
            } else {
                icon = VaadinIcon.SPINNER.create();
                icon.getElement().getThemeList().add("badge spinner");
                layout.add(status);
                System.out.println(status);
            }
            icon.getStyle().set("padding", "var(--lumo-space-xs");

            return layout;

        }).setHeader("Result").setFlexGrow(0).setWidth("300px").setResizable(true);

        grid.setItems(listOfProjectQs);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("min-width", "300px")
                .set("max-width", "100%").set("height", "100%");

        Paragraph paragraph = new Paragraph(
                "Please check failed Checks. Only when all tests are ok further processing can startet");


        Button closeButton = new Button("Close");
        closeButton.addClickListener(e -> {
            qsDialog.close();
            qs_callback.onComplete("Cancel");
        });

        Button okButton = new Button("Start Job");

        // if all result Ok then okbutton enable
        boolean allCorrect = listOfProjectQs.stream()
                .allMatch(projectQs -> "Ok".equals(projectQs.getResult()));

        okButton.setEnabled(allCorrect);

        okButton.addClickListener(e -> {
            qsDialog.close();
            qs_callback.onComplete("Start further prcessing...");
        });

        HorizontalLayout hl = new HorizontalLayout(closeButton,okButton);


        dialogLayout.add(paragraph,grid,hl);

        return dialogLayout;

    }

    public  void setListOfProjectQs(List<ProjectQSEntity> listOfProjectQs) {
        this.listOfProjectQs = listOfProjectQs;
    }

}
