package de.dbuss.tefcontrol.views;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.RolesAllowed;

@PageTitle("Welcome")
@Route(value = "Welcome", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@RolesAllowed({"MAPPING", "ADMIN", "USER"})
// @AnonymousAllowed
public class WelcomeView extends VerticalLayout {

    public WelcomeView() {
        add(new H2("Hallo und willkommen"));

        String yourContent ="Auf dieser Seite werden Tools und Hilfsmittel bereitgestellt, um Administrative-Tätigkeiten zu vereinfachen.<br />" +
                "Ebenso werden Funktionen bereitgestellt, Informationen direkt aus DB-Tabellen zu entnehmen.<br />" +
                "Ideen, Anregungen oder Verbesserungsvorschläge sind herzlich willkommen!&#128512;<br /><br />" +
                "Bitte im linken Auswahlmenü die gewünschte Funktionalität auswählen.<br /><br />" +
                "Viele Grüße<br /><b>Euer Consys-Team</b>" ;

        Html html = new Html("<text>" + yourContent + "</text>");
        Button button  = new Button("Qs");
        add(html,button);

        button.addClickListener(clickEvent -> {
            getUI().ifPresent(ui -> ui.navigate(QsView.class));
        });
    }
}
