package de.dbuss.tefcontrol.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.entity.User;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.view.B2POutlookFINView;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.view.B2POutlookSUBView;
import de.dbuss.tefcontrol.data.modules.cltv_Inflow.view.CLTVInflowView;
import de.dbuss.tefcontrol.data.modules.cobi_administration.view.CobiAdminView;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.view.GenericCommentsView;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.view.PBICentralComments;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.view.PBIFlashFinancials;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.view.PBITechComments;
import de.dbuss.tefcontrol.data.modules.techkpi.view.Tech_KPIView;
import de.dbuss.tefcontrol.data.service.ProjectsService;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.about.AboutView;
import de.dbuss.tefcontrol.views.defaultPackage.DefaultView;
import de.dbuss.tefcontrol.views.hwmapping.HWMappingView;
import de.dbuss.tefcontrol.views.knowledgeBase.KnowledgeBaseView;
import de.dbuss.tefcontrol.data.modules.pfgproductmapping.view.PFGProductMappingView;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.dbuss.tefcontrol.views.login.LoginView;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.lineawesome.LineAwesomeIcon;
import org.vaadin.tatu.Tree;


/**
 * The main view is a top-level placeholder for other views.
 */
@Slf4j
public class MainLayout extends AppLayout {

    private H3 viewTitle;
    Projects selectedProject=new Projects();
    private AuthenticatedUser authenticatedUser;
    private AccessAnnotationChecker accessChecker;
    private ProjectsService projectsService;
    private LogService logService;

    // Map to associate URLs with view classes
    private Map<String, Class<? extends Component>> urlToViewMap = new HashMap<>();

    Image image = new Image("images/telefonica.svg", "Telefonica Image");

    private static final Logger logInfo = LoggerFactory.getLogger(MainLayout.class);
    public static String userName;

    public MainLayout(AuthenticatedUser authenticatedUser, AccessAnnotationChecker accessChecker, ProjectsService projectsService, LogService logService) {
        this.authenticatedUser = authenticatedUser;
        this.accessChecker = accessChecker;
        this.projectsService = projectsService;
        this.logService = logService;

        logInfo.info("start logs using file...###################");
        logService.addLogMessage(LogService.INFO, "Starting application in MainLayout");
      //  logService.addLogMessage(LogService.ERROR, ".....Starting application in MainLayout");
      //  logService.addLogMessage(LogService.WARN, "Starting application in MainLayout......");


        // Add mappings for URLs and view classes
        urlToViewMap.put(Constants.PFG_PRODUCT_MAPPING, PFGProductMappingView.class);
        urlToViewMap.put(Constants.HWMAPPING, HWMappingView.class);
       // urlToViewMap.put("kb", KnowledgeBaseView.class);
        urlToViewMap.put(Constants.Default_Mapping, DefaultView.class );
        urlToViewMap.put(Constants.PBI_CENTRAL_COMMENTS, PBICentralComments.class );
        urlToViewMap.put(Constants.TECH_KPI, Tech_KPIView.class );
        urlToViewMap.put(Constants.CLTV_INFLOW, CLTVInflowView.class );
        urlToViewMap.put(Constants.B2P_OUTLOOK_FIN, B2POutlookFINView.class);
        urlToViewMap.put(Constants.B2P_OUTLOOK_SUB, B2POutlookSUBView.class);
        urlToViewMap.put(Constants.PBI_TECH_COMMENTS, PBITechComments.class);
        urlToViewMap.put(Constants.PBI_FLASH_FINANCIALS, PBIFlashFinancials.class);
        urlToViewMap.put(Constants.GENERIC_COMMENTS, GenericCommentsView.class);
        urlToViewMap.put(Constants.COBI_ADMINISTRATION, CobiAdminView.class);


        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
     //   createHeader();

        logService.addLogMessage(LogService.INFO, "Ending application in MainLayout");
    }

    private void createHeader() {
        H1 logo = new H1("eKP Web-Admin");
        logo.addClassNames("text-l","m-m");

      /*  String principal = "Michael@dbuss.de";
        String credentials ="gfdgfd";
        Authentication user= new UsernamePasswordAuthenticationToken(principal, credentials);
        SecurityContextHolder.getContext().setAuthentication(user);*/



        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();


        Image image = new Image("images/telefonica.svg", "Telefonica Image");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        System.out.println("angemeldeter User: " + auth.getName());


        HorizontalLayout header= new HorizontalLayout(new DrawerToggle(),logo);


        header.add(image);


        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");
        addToNavbar(header);
    }


    private void addHeaderContent() {
        log.info("Starting addHeaderContent() in mainlayout");

        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H3();
        viewTitle.setText("");
        //viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        Span version= new Span("V1.02");

        image.setHeight("60px");
        image.setWidth("150px");

        HorizontalLayout header= new HorizontalLayout(viewTitle,image, version);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(viewTitle);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");


        addToNavbar(true, toggle, header);
        log.info("Ending addHeaderContent() in mainlayout");


    }

    private void addDrawerContent() {
        log.info("Starting addDrawerContent() in mainlayout");
        System.out.println("Starting addDrawerContent() in mainlayout");
        RouterLink link = new RouterLink("login", LoginView.class);
        H2 appName = new H2("PIT");
        appName.addClassNames("text-l","m-m");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        Header header = new Header(appName);

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {

            Scroller scroller = new Scroller(createTree());
            addToDrawer(header, scroller,createFooter());

        } else
        {
            addToDrawer(new VerticalLayout(link));
        }


        //Scroller scroller = new Scroller(createTree());
        //scroller.addClassNames("AboutView");


        log.info("Ending addDrawerContent() in mainlayout");



    }

    private Tree createTree() {
        log.info("Starting createTree() in mainlayout");

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            userName = user.getUsername();
            projectsService.setUser(user);
        }

        Tree<Projects> tree = new Tree<>(Projects::getName);
        tree.setItems(projectsService.getRootProjects(),projectsService::getChildProjects);

        log.info("setItems createTree() in getRootProjects");
        //    tree.setItemIconProvider(item -> getIcon(item));
        //    tree.setItemIconSrcProvider(item -> getImageIconSrc(item));

      //  tree.setItemTooltipProvider(Projects::getDescription);

        log.info("setItemTooltipProvider createTree() in getDescription");
        tree.addExpandListener(event ->
                System.out.println(String.format("Expanded %s item(s)", event.getItems().size()))
        );
        tree.addCollapseListener(event ->
                System.out.println(String.format("Collapsed %s item(s)", event.getItems().size()))
        );
            tree.asSingleSelect().addValueChangeListener(event -> {
            log.info("Executing tree.asSingleSelect().addValueChangeListener in mainlayout");

            selectedProject = event.getValue();
            if (selectedProject != null) {

                viewTitle.setText(getProjectPath(selectedProject));

                String pageUrl = selectedProject.getPage_URL();
                Class<? extends Component> viewClass = urlToViewMap.get(pageUrl);
                Class<? extends Component> defaultViewClass = DefaultView.class;
                if (viewClass != null && accessChecker.hasAccess(viewClass)) {
                    UI.getCurrent().navigate(viewClass, new RouteParameters("project_Id",selectedProject.getId().toString()));
                } else if (accessChecker.hasAccess(defaultViewClass)) {
                    UI.getCurrent().navigate(defaultViewClass, new RouteParameters("project_Id",selectedProject.getId().toString()));
                } else {
                    Notification.show("Access denied for both views.", 3000, Notification.Position.MIDDLE);
                }
            }
        });
        tree.setAllRowsVisible(true);
    //    tree.setWidth("150px");


        //tree.addClassNames("text-l","m-m");
        tree.addClassNames(LumoUtility.FontSize.XXSMALL, LumoUtility.Margin.NONE);


        log.info("Ending createTree() in mainlayout");
        return tree;
    }

    private String getProjectPath(Projects selectedProject) {

        if(selectedProject.getParent_id() != null )
        {
            String parent = projectsService.findById(selectedProject.getParent_id()).get().getName();
            return "Project: " + parent + " => " +  selectedProject.getName();
        }
        else {

            return "Project: " + selectedProject.getName();
        }


    }

    private void navigateToView(String url) {
        log.info("Starting navigateToView() in mainlayout");
        if (url != null) {
            getUI().ifPresent(ui -> {
                String route = "/" + url; // Assuming your route names match the URLs
                ui.navigate(route);
            });
        }
        log.info("Ending navigateToView() in mainlayout");
    }

    private SideNav createNavigation(String url) {
        log.info("Starting createNavigation() in mainlayout");
        SideNav nav = new SideNav();
        if (url.equals("PFG-Mapping") && accessChecker.hasAccess(PFGProductMappingView.class)) {
            nav.addItem(new SideNavItem("PFG Product-Mapping", PFGProductMappingView.class,
                    LineAwesomeIcon.ARROWS_ALT_H_SOLID.create()));

        }
        if (url.equals("HW-Mapping") &&  accessChecker.hasAccess(HWMappingView.class)) {
            nav.addItem(new SideNavItem("HW Mapping", HWMappingView.class, LineAwesomeIcon.TH_SOLID.create()));

        }

        if (url.equals("kb") && accessChecker.hasAccess(KnowledgeBaseView.class)) {
            nav.addItem(new SideNavItem("KB", KnowledgeBaseView.class, LineAwesomeIcon.INFO_CIRCLE_SOLID.create()));

        }


        if (accessChecker.hasAccess(AboutView.class)) {
            nav.addItem(new SideNavItem("About", AboutView.class, LineAwesomeIcon.INFO_CIRCLE_SOLID.create()));

        }
        if (accessChecker.hasAccess(WelcomeView.class)) {
            UI.getCurrent().navigate(WelcomeView.class);
        }

        log.info("Ending createNavigation() in mainlayout");
        return nav;
    }

    private Footer createFooter() {
        log.info("Starting createFooter() in mainlayout");
        Footer layout = new Footer();

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();

            Avatar avatar = new Avatar(user.getName());

            byte[] profilePictureData = user.getProfilePicture();
            if (profilePictureData != null && profilePictureData.length > 0) {
                StreamResource resource = new StreamResource("profile-pic",
                        () -> new ByteArrayInputStream(profilePictureData));
                avatar.setImageResource(resource);
            }
            avatar.setThemeName("xsmall");
            avatar.getElement().setAttribute("tabindex", "-1");

            MenuBar userMenu = new MenuBar();
            userMenu.setThemeName("tertiary-inline contrast");

            MenuItem userName = userMenu.addItem("");
            Div div = new Div();
            div.add(avatar);
            div.add(user.getName());
            div.add(new Icon("lumo", "dropdown"));
            div.getElement().getStyle().set("display", "flex");
            div.getElement().getStyle().set("align-items", "center");
            div.getElement().getStyle().set("gap", "var(--lumo-space-s)");
            userName.add(div);
            userName.getSubMenu().addItem("Sign out", e -> {
                authenticatedUser.logout();
            });

            layout.add(userMenu);
        } else {
            Anchor loginLink = new Anchor("login", "Sign in");
            layout.add(loginLink);
        }
        log.info("Ending createFooter() in mainlayout");
        return layout;
    }

    @Override
    protected void afterNavigation() {
        log.info("Staring afterNavigation() in mainlayout");
      //  viewTitle.setText(getCurrentPageTitle());
        super.afterNavigation();

      //  viewTitle.setText(selectedProject.getName());


        log.info("Ending afterNavigation() in mainlayout");
    }

    private String getCurrentPageTitle() {
        log.info("Staring getCurrentPageTitle() in mainlayout");
     //   PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        String title ="choose Project";
        title = selectedProject.getName();

        log.info("Ending getCurrentPageTitle() in mainlayout");
        //return title == null ? "" : title.value();
        return title == null ? "" : title;
    }
}
