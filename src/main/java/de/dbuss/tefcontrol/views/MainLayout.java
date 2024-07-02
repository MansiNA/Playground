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
import de.dbuss.tefcontrol.data.modules.HUBFlowMapping.view.HUBFlowMappingView;
import de.dbuss.tefcontrol.data.modules.adjustmentrefx.entity.AdjustmentsREFX;
import de.dbuss.tefcontrol.data.modules.adjustmentrefx.view.AdjustmentsREFXView;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.view.B2POutlookFINView;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.view.B2POutlookSUBView;
import de.dbuss.tefcontrol.data.modules.b2bmapsaleschannel.view.B2BMapSalesChannelView;
import de.dbuss.tefcontrol.data.modules.cltv_Inflow.view.CLTVInflowView;
import de.dbuss.tefcontrol.data.modules.administration.view.CobiAdminView;
import de.dbuss.tefcontrol.data.modules.hwmapping.view.HWMapping;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.view.GenericCommentsView;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.view.PBICentralComments;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.view.PBIFlashFinancials;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.view.PBITechComments;
import de.dbuss.tefcontrol.data.modules.administration.view.ReportAdminView;
import de.dbuss.tefcontrol.data.modules.kpi.Strategic_KPIView;
import de.dbuss.tefcontrol.data.modules.rosettamapping.view.RosettaMappingView;
import de.dbuss.tefcontrol.data.modules.saleschannelmapping.view.SalesChannelMapping;
import de.dbuss.tefcontrol.data.modules.sqlexecution.view.SQLConfigurationView;
import de.dbuss.tefcontrol.data.modules.sqlexecution.view.SQLExecutionView;
import de.dbuss.tefcontrol.data.modules.tarifmapping.view.TarifMappingView;
import de.dbuss.tefcontrol.data.modules.kpi.Tech_KPIView;
import de.dbuss.tefcontrol.data.modules.underlying_cobi;
import de.dbuss.tefcontrol.data.modules.userimport.ImportDimLineTapete;
import de.dbuss.tefcontrol.data.service.ProjectsService;
import de.dbuss.tefcontrol.data.service.UserService;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.about.AboutView;
import de.dbuss.tefcontrol.views.defaultPackage.DefaultView;
import de.dbuss.tefcontrol.views.hwmapping.HWMappingView;
import de.dbuss.tefcontrol.views.knowledgeBase.KnowledgeBaseView;
import de.dbuss.tefcontrol.data.modules.pfgproductmapping.view.PFGProductMappingView;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.dbuss.tefcontrol.views.login.LoginView;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    private UserService userService;

    // Map to associate URLs with view classes
    private Map<String, Class<? extends Component>> urlToViewMap = new HashMap<>();

    Image image = new Image("images/telefonica.svg", "Telefonica");

    private static final Logger logInfo = LoggerFactory.getLogger(MainLayout.class);
    public static String userName;
    public static boolean isAdmin;
    public static List<String> userRoles;
    private LoginView loginView;

    @Value("${pit.value}")
    private String headerName;

    public MainLayout(@Value("${pit.value}") String headerName, AuthenticatedUser authenticatedUser, AccessAnnotationChecker accessChecker, ProjectsService projectsService, LogService logService, UserService userService) {
        this.authenticatedUser = authenticatedUser;
        this.accessChecker = accessChecker;
        this.projectsService = projectsService;
        this.logService = logService;
        this.userService = userService;
        this.headerName = headerName;

        logInfo.info("start logs using file...###################");
        logService.addLogMessage(LogService.INFO, "Starting application in MainLayout");

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
        urlToViewMap.put(Constants.HW_MAPPING, HWMapping.class);
        urlToViewMap.put(Constants.REPORT_ADMINISTRATION, ReportAdminView.class);
        urlToViewMap.put(Constants.TARIFMAPPING, TarifMappingView.class);
        urlToViewMap.put(Constants.B2B_MAPSALESCHANNEL, B2BMapSalesChannelView.class);
        urlToViewMap.put(Constants.Strategic_KPI, Strategic_KPIView.class);
        urlToViewMap.put(Constants.DimLineTapete, ImportDimLineTapete.class);
        urlToViewMap.put(Constants.UnderlyingCobi, underlying_cobi.class);
        urlToViewMap.put(Constants.ADJUSTMENTREFX, AdjustmentsREFXView.class);
        urlToViewMap.put(Constants.HUB_FLOW_MAPPING, HUBFlowMappingView.class);
        urlToViewMap.put(Constants.SQL_EXECUTION, SQLExecutionView.class);
      //  urlToViewMap.put(Constants.CONFIG, SQLConfigurationView.class);
        urlToViewMap.put(Constants.ROSETTA_MAPPING, RosettaMappingView.class);
        urlToViewMap.put(Constants.SALES_CHANNEL_MAPPING, SalesChannelMapping.class);

        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
     //   createHeader();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        userRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        isAdmin = checkAdminRole();
        logService.addLogMessage(LogService.INFO, "Ending application in MainLayout");
    }

    private void addHeaderContent() {
        log.info("Starting addHeaderContent() in mainlayout");

        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H3();
        viewTitle.setText("");
        //viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        Span version= new Span("V1.5");

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
       // H2 appName = new H2("PIT");
        H2 appName = new H2(headerName);
        appName.addClassNames("text-l","m-m");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        Header header = new Header(appName);

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {

            Scroller scroller = new Scroller(createTree());
            addToDrawer(header, scroller,createFooter());

        } else
        {
            //loginView = new LoginView(authenticatedUser);
            loginView = new LoginView(authenticatedUser, logService, userService);
            addToDrawer(new VerticalLayout(link));
           // addToDrawer(new VerticalLayout(loginView));
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

        if (loginView != null) {
         //   loginView.openLoginOverlay();
        }
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

    public static boolean checkAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication !=  null  && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                return userDetails.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            }
        }
        return false;
    }

}
