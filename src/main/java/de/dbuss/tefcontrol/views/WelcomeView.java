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
import jakarta.annotation.security.RolesAllowed;

@PageTitle("Welcome")
@Route(value = "Welcome", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
 @AnonymousAllowed
public class WelcomeView extends VerticalLayout {

    public WelcomeView() {
        add(new H2("Hallo und willkommen"));

        String yourContent ="Auf dieser Seite werden Tools und Hilfsmittel bereitgestellt, um Administrative-Tätigkeiten zu vereinfachen.<br />" +
                "Ebenso werden Funktionen bereitgestellt, Informationen direkt aus DB-Tabellen zu entnehmen.<br />" +
                "Ideen, Anregungen oder Verbesserungsvorschläge sind herzlich willkommen!&#128512;<br /><br />" +
                "Bitte mit Telefonica AD-User einloggen und die gewünschte Funktionalität auswählen.<br /><br />" +
                "Viele Grüße<br /><b>Euer Consys-Team</b>" ;

        Html html = new Html("<text>" + yourContent + "</text>");
        add(html);

        // Add RouterLink to ConfigurationView
      //  RouterLink configLink = new RouterLink("Go to Configuration", ConfigurationGridView.class);
      //  add(configLink);
    }
}
