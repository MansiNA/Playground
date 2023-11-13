package de.dbuss.tefcontrol.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.model.Label;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.QS;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.view.TechCommentView;

import java.util.ArrayList;
import java.util.List;

public class QS_Grid extends Composite<Div> {

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

        Grid<QS> grid = new Grid<>(QS.class, false);
        grid.addColumn(QS::getName).setHeader("QS-Name");
        grid.addColumn(QS::getResult).setHeader("Result");

        //Demo Content
        //ToDO: Fill with QS-Result from projectId
        List<QS> results = new ArrayList<>();
        QS qs1 = new QS();
        QS qs2 = new QS();
        qs1.setName("Testfall 1");
        qs1.setResult("OK");

        qs2.setName("Testfall 2");
        qs2.setResult("failed");
        results.add(qs1);
        results.add(qs2);
        grid.setItems(results);

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
            qs_callback.onComplete("Canceld");
        });

        Button okButton = new Button("Start Job");

        //ToDO okButtonn should only displyd if all QS ok
        //okButton.setEnabled(false);
        okButton.addClickListener(e -> {
            qsDialog.close();
            qs_callback.onComplete("Start further prcessing...");
        });

        HorizontalLayout hl = new HorizontalLayout(closeButton,okButton);


        dialogLayout.add(paragraph,grid,hl);

        return dialogLayout;

    }

}
