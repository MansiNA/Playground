package de.dbuss.tefcontrol.data.modules.techkpi.view;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.*;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.InputStream;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


@PageTitle("Tech KPI | TEF-Control")
@Route(value = "Tech_KPI/:project_Id", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class Tech_KPIView extends VerticalLayout implements BeforeEnterObserver {

    private JdbcTemplate jdbcTemplate;
    private final ProjectConnectionService projectConnectionService;
    private Button uploadBtn;
    private String actualsTableName;
    private String factTableName;
    private String planTableName;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    Article article = new Article();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    Div textArea = new Div();
    // Div message = new Div();
    UI ui=UI.getCurrent();
    MemoryBuffer memoryBuffer = new MemoryBuffer();
    Upload singleFileUpload = new Upload(memoryBuffer);

    InputStream fileData_Fact;
    InputStream fileData_Actuals;
    InputStream fileData_Plan;
    String fileName = "";
    long contentLength = 0;
    String mimeType = "";
    Grid<KPI_Fact> gridFact;
    Grid<KPI_Actuals> gridActuals;
    Grid<KPI_Plan> gridPlan;

  //  Grid<QS_Status> gridQS;

    //H3 h3_Fact= new H3();
    //H3 h3_Actuals= new H3();
    //H3 h3_Plan= new H3();

 //   Article description = new Article();

    String factInfo = "KPI_Fact 0 rows";
    String actualsInfo = "KPI_Actuals 0 rows";
    String planInfo = "KPI Plan 0 rows";

    ProgressBar progressBarFact = new ProgressBar();
    ProgressBar progressBarPlan = new ProgressBar();
    ProgressBar progressBarActuals = new ProgressBar();

    private List<KPI_Fact> listOfKPI_Fact = new ArrayList<KPI_Fact>();
    private List<KPI_Actuals> listOfKPI_Actuals = new ArrayList<KPI_Actuals>();

    private List<KPI_Plan> listOfKPI_Plan = new ArrayList<KPI_Plan>();
    Accordion accordion;
    AccordionPanel factPanel;
    AccordionPanel planPanel;
    AccordionPanel actualsPanel;
    private int projectId;
    private QS_Grid qsGrid;
    private Button qsBtn;

    //Div htmlDivToDO;
    //CheckboxGroup<String> TodoList;

    public Tech_KPIView(JdbcTemplate jdbcTemplate, ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectConnectionService = projectConnectionService;

        uploadBtn = new Button("Upload");
        uploadBtn.setEnabled(false);

        qsBtn = new Button("QS and Start Job");
        qsBtn.setEnabled(false);

        progressBarPlan.setVisible(false);
        progressBarActuals.setVisible(false);
        progressBarFact.setVisible(false);

   //     description.add("Tool für upload der KPI-Excel Tabelle.\n Bitte als 1. Datei hochladen.");

        setupKPIActualsGrid();
        setupKPIFactGrid();
        setupKPIPlanGrid();
        //setupQSGrid();

        setupUploader();

        // message.setText("1. Datei hochladen.");


    //    TodoList = new CheckboxGroup<>();
    //    TodoList.setLabel("ToDo");
    //    TodoList.setItems("KPI_DB.xlsx hochladen");

     /*   TodoList.addValueChangeListener(event -> {
            String selectedItems = event.getValue().stream()
                    .collect(Collectors.joining(", "));
            //   System.out.println("Ausgewählte: " + selectedItems);

            if (selectedItems.contains("KPI_DB.xlsx hochladen"))
            {
                //    Notification notification = Notification.show("Bitte zuerst KPI_DB.xlsx hochladen!",5000, Notification.Position.MIDDLE);
                //    notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
                TodoList.deselect("KPI_DB.xlsx hochladen");
            }


            if (selectedItems.contains("QS bestätigen"))
            {
                saveButton.setEnabled(true);

            }

        });*/


   //     TodoList.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);



        Details details = new Details("Import Details", textArea);
        details.setOpened(false);


        HorizontalLayout hl = new HorizontalLayout();
        hl.setAlignItems(Alignment.CENTER);

        Div htmlDiv = new Div();
        htmlDiv.getElement().setProperty("innerHTML", "<h2>Import KPI Excel-File</h2><p>Mit dieser Seite lässt sich die KPI_DB.xlsx " +
                "Datei direkt in die Datenbank einlesen.</br>Die Daten der Blätter \"<b>KPI_Plan</b>\", \"<b>KPI_Actuals</b>\" und \"<b>KPI_Fact</b>\" werden automatisch in die Stage Tabellen <ul><li>Stage_Tech_KPI.KPI_Plan</li><li>Stage_Tech_KPI.KPI_Actuals</li><li>Stage_Tech_KPI.KPI_Fact</li></ul>geladen. " +
                "Dazu einfach die Datei auswählen oder per drag&drop hochladen. </br>Nach einer entsprechenden QS-Rückmeldung bzgl. Datenqualität, kann die weitere Verarbeitung per Button \"Freigabe\" erfolgen.");

        // Div zur Ansicht hinzufügen
        add(htmlDiv);

   //     htmlDivToDO = new Div();
        // htmlDivQS.getElement().setProperty("innerHTML", "<b style=\"color:blue;\">QS-Übersicht:</b>");
   //     htmlDivToDO.getElement().setProperty("innerHTML", "<h4><u>Aktuelles ToDo:</u> <b>Bitte die Datei KPI-DB.xlsx hochladen!</b></h4>");

   //     add(TodoList);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : listOfProjectParameters) {
            if(projectParameter.getNamespace().equals(Constants.TECH_KPI)) {
                if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                    dbServer = projectParameter.getValue();
                } else if (Constants.DB_NAME.equals(projectParameter.getName())) {
                    dbName = projectParameter.getValue();
                } else if (Constants.DB_USER.equals(projectParameter.getName())) {
                    dbUser = projectParameter.getValue();
                } else if (Constants.DB_PASSWORD.equals(projectParameter.getName())) {
                    dbPassword = projectParameter.getValue();
                }else if (Constants.TABLE_ACTUALS.equals(projectParameter.getName())) {
                    actualsTableName = projectParameter.getValue();
                } else if (Constants.TABLE_FACT.equals(projectParameter.getName())) {
                    factTableName = projectParameter.getValue();
                } else if (Constants.TABLE_PLAN.equals(projectParameter.getName())) {
                    planTableName = projectParameter.getValue();
                }
            }
        }
        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";
        //Text databaseDetail = new Text("Connected to: "+ dbServer+ ", Database: " + dbName+ ", Table Financials: " + financialsTableName + ", Table Subscriber: " + subscriberTableName+ ", Table Unitdeepdive: "+ unitTableName);
        Text databaseDetail = new Text("Connected to: "+ dbServer+ ", Database: " + dbName);

        //Componente QS-Grid:
        qsGrid = new QS_Grid(projectConnectionService);

        hl.setAlignItems(Alignment.BASELINE);
        hl.add(singleFileUpload, uploadBtn, qsBtn, databaseDetail, qsGrid);

        uploadBtn.addClickListener(e->{

            ui.setPollInterval(500);

            savePlanEntities();
            saveActualsEntities();
            saveFactEntities();

        });

        qsBtn.addClickListener(e ->{
            if (qsGrid.projectId != projectId) {
                CallbackHandler callbackHandler = new CallbackHandler();
                qsGrid.createDialog(callbackHandler, projectId);
            }
            qsGrid.showDialog(true);
        });

        //  h3_Fact.add("Fact 0 rows");
        //  h3_Actuals.add("Actuals 0 rows");
        //  h3_Plan.add("Plan 0 rows");

        //add(hl, progressBarFact, progressBarPlan,progressBarActuals, details, h3_Fact, gridFact, h3_Actuals, gridActuals, h3_Plan, gridPlan );
        add(hl, progressBarFact, progressBarPlan, progressBarActuals);

        add(details);

        accordion = new Accordion();

        factPanel = new AccordionPanel("KPI_Fact (not loaded)", gridFact);
        accordion.add(factPanel);
        planPanel = new AccordionPanel("KPI_Plan (not loaded)", gridPlan);
        accordion.add(planPanel);
        actualsPanel = new AccordionPanel("KPI_Actuals (not loaded)", gridActuals);
        accordion.add(actualsPanel);


        accordion.add(actualsPanel);
        accordion.add(factPanel);
        accordion.add(planPanel);
        accordion.setWidthFull();
        accordion.setHeightFull();
        accordion.close();

        Div htmlDivQS = new Div();
        // htmlDivQS.getElement().setProperty("innerHTML", "<b style=\"color:blue;\">QS-Übersicht:</b>");
        htmlDivQS.getElement().setProperty("innerHTML", "<b>QS-Übersicht:</b>");

        //  add(htmlDivQS,gridQS,accordion);
        add(htmlDivQS,accordion);

    }
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        projectId = Integer.parseInt(parameters.get("project_Id").orElse(null));
    }

    public class CallbackHandler implements QS_Callback {
        // Die Methode, die aufgerufen wird, wenn die externe Methode abgeschlossen ist
        @Override
        public void onComplete(String result) {
            if(!result.equals("Cancel")) {
                String message = projectConnectionService.startAgent(projectId);
                if (!message.contains("Error")) {
                    Notification.show(message, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show(message, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        }
    }
   /* private void setupQSGrid() {
        gridQS = new Grid<>(QS_Status.class, false);

        gridQS.setHeight("220px");
        gridQS.setWidth("500px");
        //  gridQS.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        //  gridFact.addColumn(KPI_Fact::getRow).setHeader("Zeile");
        gridQS.addColumn(QS_Status::getSheet).setResizable(true).setHeader("Sheet");
        gridQS.addColumn(QS_Status::getQSName).setResizable(true).setHeader("QS-Step");
        gridQS.addComponentColumn(item -> createStatusIcon(item.getStatus()))
                .setTooltipGenerator(item -> item.getStatus())
                .setHeader("QS-Status");
        gridQS.getElement().getStyle().set("border", "none");

        gridQS.setItems(new QS_Status("KPI_Plan", "Check Primary Key", "OK")
                , new QS_Status("KPI_Plan", "Check Empty Rows", "Error")
                , new QS_Status("KPI_Actuals", "Check Primary Key", "OK")
                , new QS_Status("KPI_Actuals", "Check Empty Rows", "OK")
                , new QS_Status("KPI_Fact", "Check Primary Key", "OK")
                , new QS_Status("KPI_Fact", "Check Scenario Column", "OK")
                , new QS_Status("KPI_Fact", "Check Empty Rows", "OK"));

        gridQS.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        gridQS.addThemeVariants(GridVariant.LUMO_COMPACT);
        gridQS.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);


        gridQS.addClassName("small-grid");
    }*/

    private Icon createStatusIcon(String status) {
        boolean isAvailable = Constants.OK.equals(status);
        Icon icon;
        if (isAvailable) {
            icon = VaadinIcon.CHECK.create();
            icon.getElement().getThemeList().add("badge success");
        } else {
            icon = VaadinIcon.CLOSE_SMALL.create();
            icon.getElement().getThemeList().add("badge error");
        }
        icon.getStyle().set("padding", "var(--lumo-space-xs");
        return icon;
    }

    private void savePlanEntities() {
        int totalRows = listOfKPI_Plan.size();
        AtomicReference<String> returnStatus= new AtomicReference<>("false");
        progressBarPlan.setVisible(true);
        progressBarPlan.setMin(0);
        progressBarPlan.setMax(totalRows);
        progressBarPlan.setValue(0);

        //  message.setText(LocalDateTime.now().format(formatter) + ": Info: saving KPI_Plan to database...");
        // truncateTable("[Stage_Tech_KPI].[KPI_Plan]");
        new Thread(() -> {

            try {

                int batchSize = 1000; // Die Anzahl der Zeilen, die auf einmal verarbeitet werden sollen

                projectConnectionService.deleteTableData(dbUrl, dbUser, dbPassword, planTableName);

                for (int i = 1; i < totalRows; i += batchSize) {

                    int endIndex = Math.min(i + batchSize, totalRows);

                    List<KPI_Plan> batchData = listOfKPI_Plan.subList(i, endIndex);

                    System.out.println("Verarbeitete Zeilen: " + endIndex + " von " + totalRows);

                    //savePlanBlock(batchData);
                    String resultKPIPlan = projectConnectionService.saveKPIPlan(batchData, planTableName);

                    returnStatus.set(resultKPIPlan);

                    int finalI = i;
                    ui.access(() -> {
                        progressBarPlan.setValue((double) finalI);
                        System.out.println("Fortschritt aktualisiert auf: " + finalI);
                        //message.setText(LocalDateTime.now().format(formatter) + ": Info: saving to database (" + endIndex + "/" + totalRows +")");
                    });

                }

            } catch (Exception e) {
                ui.access(() -> {
                    Notification.show("Error during KPI_Plan upload! ", 4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                });
                e.printStackTrace();
            }
            ui.access(() -> {
                progressBarPlan.setVisible(false);

                if (returnStatus.toString().equals(Constants.OK))
                {
                    Notification.show("KPI_Plan saved " + totalRows + " rows.",3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
                else
                {
                    Notification.show("Error during KPI_Plan upload! " + returnStatus.toString(), 4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
                //     message.setText(LocalDateTime.now().format(formatter) + ": Info: KPI_Plan saved " + totalRows + " rows");

            });

        }).start();

    }
    private void saveActualsEntities() {
        String sheet="KPI_Actuals";
        AtomicReference<String> returnStatus= new AtomicReference<>("false");
        int totalRows = listOfKPI_Actuals.size();
        progressBarActuals.setVisible(true);
        progressBarActuals.setMin(0);
        progressBarActuals.setMax(totalRows);
        progressBarActuals.setValue(0);

        //    message.setText(message.getText() + "\n" + LocalDateTime.now().format(formatter) + ": Info: saving " + sheet + " to database...");

        //  truncateTable("[Stage_Tech_KPI].[KPI_Actuals]");

        new Thread(() -> {

            // Do some long running task
            try {
                System.out.println("Upload " + sheet + "-Data to DB");

                int batchSize = 1000; // Die Anzahl der Zeilen, die auf einmal verarbeitet werden sollen

                projectConnectionService.deleteTableData(dbUrl, dbUser, dbPassword, actualsTableName);

                for (int i = 1; i < totalRows; i += batchSize) {

                    int endIndex = Math.min(i + batchSize, totalRows);

                    List<KPI_Actuals> batchData = listOfKPI_Actuals.subList(i, endIndex);

                    System.out.println("Verarbeitete Zeilen: " + endIndex + " von " + totalRows);

                    // saveActualsBlock(batchData);

                    String resultKPIActuals = projectConnectionService.saveKPIActuals(batchData, actualsTableName);

                    returnStatus.set(resultKPIActuals);

                    int finalI = i;
                    ui.access(() -> {
                        progressBarActuals.setValue((double) finalI);
                        System.out.println("Fortschritt aktualisiert auf: " + finalI);
                        //    message.setText(message.getText() + "\n" + LocalDateTime.now().format(formatter) + ": Info: " + sheet + " saving to database (" + endIndex + "/" + totalRows +")");
                    });

                }


            } catch (Exception e) {
                ui.access(() -> {
                    Notification.show("Error during KPI_Actuals upload! ", 4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                });
                e.printStackTrace();
            }
            ui.access(() -> {

                progressBarActuals.setVisible(false);

                if (returnStatus.toString().equals(Constants.OK))
                {
                    Notification.show("KPI_Actuals saved " + totalRows + " rows.",3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
                else
                {
                    Notification.show("Error during KPI_Actuals upload! " + returnStatus.toString(), 4000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }

            });

        }).start();

    }

    private void saveFactEntities() {
        AtomicReference<String> returnStatus= new AtomicReference<>("false");
        int totalRows = listOfKPI_Fact.size();
        progressBarFact.setVisible(true);
        progressBarFact.setMin(0);
        progressBarFact.setMax(totalRows);
        progressBarFact.setValue(0);


        //    message.setText(LocalDateTime.now().format(formatter) + ": Info: saving KPI_Fact to database...");
        //  truncateTable("[Stage_Tech_KPI].[KPI_Fact]");
        new Thread(() -> {

            // Do some long running task
            try {
                System.out.println("Upload Data to DB");

                int batchSize = 1000; // Die Anzahl der Zeilen, die auf einmal verarbeitet werden sollen

                projectConnectionService.deleteTableData(dbUrl, dbUser, dbPassword, factTableName);

                for (int i = 1; i < totalRows; i += batchSize) {

                    if (Thread.interrupted()) {
                        System.out.println("Thread hat interrupt bekommen");
                        // Hier könntest du aufräumen oder andere Aktionen ausführen, bevor der Thread beendet wird
                        return; // Verlässt den Thread
                    }
                    else {
                        System.out.println("Thread läuft noch...");
                    }

                    int endIndex = Math.min(i + batchSize, totalRows);

                    List<KPI_Fact> batchData = listOfKPI_Fact.subList(i, endIndex);

                    System.out.println("Verarbeitete Zeilen: " + endIndex + " von " + totalRows);

                    //saveFactBlock(batchData);

                    String resultKPIFact = projectConnectionService.saveKPIFact(batchData, factTableName);
                    returnStatus.set(resultKPIFact);

                    System.out.println("ResultKPIFact: " + returnStatus.toString());
                    //ToDO: Check why for-loop not exited in event of an error
                    if (returnStatus.toString().equals(Constants.OK)){
                        System.out.println("Alles in Butter...");
                    }
                    else{
                        System.out.println("Fehler aufgetreten...");
                        Thread.currentThread().interrupt(); // Interrupt-Signal setzen

                        ui.access(() -> {
                            progressBarFact.setVisible(false);
                            Notification.show("Error during KPI_Fact upload! " + returnStatus.toString(), 15000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                            ui.setPollInterval(-1);
                        });


                        return;

                    }


                    int finalI = i;
                    ui.access(() -> {
                        progressBarFact.setValue((double) finalI);
                        System.out.println("Fortschritt aktualisiert auf: " + finalI);
                        //         message.setText(LocalDateTime.now().format(formatter) + ": Info: saving to database (" + endIndex + "/" + totalRows +")");
                    });

                }
            } catch (Exception e) {
                ui.access(() -> {

                    Notification.show("Error during KPI_Fact upload! ", 15000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                  
                });
                e.printStackTrace();
            }
            ui.access(() -> {
                progressBarFact.setVisible(false);

                if (returnStatus.toString().equals(Constants.OK))
                {
                    Notification.show("KPI_Fact saved " + totalRows + " rows.",5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    qsBtn.setEnabled(true);
                }
                else
                {
                    Notification.show("Error during KPI_Fact upload! " + returnStatus.toString(), 15000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }

                ui.setPollInterval(-1);
            });

        }).start();

    }

    private void truncateTable(String tableName) {

        String sql="truncate table " + tableName;

        jdbcTemplate.execute(sql);

    }

    private void saveActualsBlock(List<KPI_Actuals> batchData) {

        String sql = "INSERT INTO [Stage_Tech_KPI].[KPI_Actuals] (Zeile, [NT_ID],[WTAC_ID],[sort],[M2_Area],[M1_Network],[M3_Service],[M4_Dimension],[M5_Tech],[M6_Detail],[KPI_long],[Runrate],[Unit],[Description],[SourceReport],[SourceInput],[SourceComment] ,[SourceContact] ,[SourceLink] ) VALUES (?, ?, ?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        jdbcTemplate.batchUpdate(sql, batchData, batchData.size(), (ps, entity) -> {

            ps.setInt(1, entity.row);
            ps.setString(2, entity.getNT_ID());
            ps.setString(3, entity.getWTAC_ID());
            ps.setInt(4, entity.getSort());
            ps.setString(5, entity.getM2_Area());
            ps.setString(6, entity.getM1_Network());
            ps.setString(7, entity.getM3_Service());
            ps.setString(8, entity.getM4_Dimension());
            ps.setString(9, entity.getM5_Tech());
            ps.setString(10, entity.getM6_Detail());
            ps.setString(11, entity.getKPI_long());
            ps.setString(12, entity.getRunrate());
            ps.setString(13, entity.getUnit());
            ps.setString(14, entity.getDescription());
            ps.setString(15, entity.getSourceReport());
            ps.setString(16, entity.getSourceInput());
            ps.setString(17, entity.getSourceComment());
            ps.setString(18, entity.getSourceContact());
            ps.setString(19, entity.getSourceLink());
        });

    }
    private void savePlanBlock(List<KPI_Plan> batchData) {

        String sql = "INSERT INTO [Stage_Tech_KPI].[KPI_Plan] (Zeile, NT_ID, Spalte1, Scenario, VersionDate, VersionComment, Runrate) VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, batchData, batchData.size(), (ps, entity) -> {

            java.sql.Date versionDate = null;
            Integer row=-1;
            if(entity.getVersionDate() != null)
            {
                versionDate = new java.sql.Date(entity.getVersionDate().getTime());
            }
            ps.setInt(1,entity.getRow());
            ps.setString(2, entity.getNT_ID());
            ps.setString(3, entity.getSpalte1());
            ps.setString(4, entity.getScenario());
            ps.setDate(5, versionDate);
            ps.setString(6, entity.getVersionComment());
            ps.setString(7, entity.getRunrate());
            //  ps.setDate(3, new java.sql.Date(2023,01,01));
            //ps.setDate(3, new java.sql.Date(entity.getDate().getTime() ));
            //ps.setDouble (4, entity.getWert());
        });

    }
    private void saveFactBlock(List<KPI_Fact> batchData) {

        String sql = "INSERT INTO [Stage_Tech_KPI].[KPI_Fact] (Zeile, NT_ID, Runrate, Scenario,[Date],Wert) VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, batchData, batchData.size(), (ps, entity) -> {

            ps.setInt(1, entity.getRow());
            ps.setString(2, entity.getNT_ID());
            ps.setString(3, entity.getRunrate());
            ps.setString(4, entity.getScenario());
            //  ps.setDate(3, new java.sql.Date(2023,01,01));
            ps.setDate(5, new java.sql.Date(entity.getDate().getTime() ));
            ps.setDouble (6, entity.getWert());
        });

    }

    private void setupKPIActualsGrid() {
        gridActuals = new Grid<>(KPI_Actuals.class, false);

        gridActuals.setHeight("300px");

        gridActuals.addColumn(KPI_Actuals::getRow).setHeader("Zeile");

        gridActuals.addColumn(KPI_Actuals::getNT_ID).setHeader("NT ID");
        gridActuals.addColumn(KPI_Actuals::getWTAC_ID).setHeader("WTAC ID");
        gridActuals.addColumn(KPI_Actuals::getSort).setHeader("Sort");
        gridActuals.addColumn(KPI_Actuals::getM2_Area).setHeader("M2_Area");
        gridActuals.addColumn(KPI_Actuals::getM1_Network).setHeader("M1_Network");
        gridActuals.addColumn(KPI_Actuals::getM3_Service).setHeader("M3_Service");
        gridActuals.addColumn(KPI_Actuals::getM4_Dimension).setHeader("M4_Dimension");
        gridActuals.addColumn(KPI_Actuals::getM5_Tech).setHeader("M5_Tech");
        gridActuals.addColumn(KPI_Actuals::getM6_Detail).setHeader("M6_Detail");
        gridActuals.addColumn(KPI_Actuals::getKPI_long).setHeader("KPI long");
        gridActuals.addColumn(KPI_Actuals::getRunrate).setHeader("Runrate");
        gridActuals.addColumn(KPI_Actuals::getUnit).setHeader("Unit");
        gridActuals.addColumn(KPI_Actuals::getDescription).setHeader("Description");
        gridActuals.addColumn(KPI_Actuals::getSourceReport).setHeader("SourceReport");
        gridActuals.addColumn(KPI_Actuals::getSourceInput).setHeader("SourceInput");
        gridActuals.addColumn(KPI_Actuals::getSourceComment).setHeader("SourceComment");
        gridActuals.addColumn(KPI_Actuals::getSourceContact).setHeader("SourceContact");
        gridActuals.addColumn(KPI_Actuals::getSourceLink).setHeader("SourceLink");

    }

    private void setupKPIFactGrid() {
        gridFact = new Grid<>(KPI_Fact.class, false);

        gridFact.setHeight("300px");

        gridFact.addColumn(KPI_Fact::getRow).setHeader("Zeile");

        gridFact.addColumn(KPI_Fact::getNT_ID).setHeader("NT ID");
        gridFact.addColumn(KPI_Fact::getScenario).setHeader("Scenario");
        gridFact.addColumn(KPI_Fact::getRunrate).setHeader("Runrate");
        gridFact.addColumn(KPI_Fact::getDate).setHeader("Date");
        gridFact.addColumn(KPI_Fact::getWert).setHeader("Wert");

    }

    private void setupKPIPlanGrid() {
        gridPlan = new Grid<>(KPI_Plan.class, false);

        gridPlan.setHeight("300px");

        gridPlan.addColumn(KPI_Plan::getRow).setHeader("Zeile");

        gridPlan.addColumn(KPI_Plan::getNT_ID).setHeader("NT ID");
        gridPlan.addColumn(KPI_Plan::getSpalte1).setHeader("Spalte1");
        gridPlan.addColumn(KPI_Plan::getScenario).setHeader("Scenario");
        gridPlan.addColumn(KPI_Plan::getVersionDate).setHeader("VersionDate");
        gridPlan.addColumn(KPI_Plan::getVersionComment).setHeader("VersionComment");
        gridPlan.addColumn(KPI_Plan::getRunrate).setHeader("Runrate");


    }
    private void setupUploader() {
        System.out.println("setup uploader................start");
        singleFileUpload.setWidth("450px");

        singleFileUpload.addSucceededListener(event -> {
            // Get information about the uploaded file
            fileData_Fact = memoryBuffer.getInputStream();
            fileData_Actuals = memoryBuffer.getInputStream();
            fileData_Plan = memoryBuffer.getInputStream();
            fileName = event.getFileName();
            contentLength = event.getContentLength();
            mimeType = event.getMIMEType();

            listOfKPI_Fact = parseExcelFile_Fact(fileData_Fact, fileName,"KPI_Fact");
            listOfKPI_Actuals = parseExcelFile_Actuals(fileData_Actuals, fileName,"KPI_Actuals");
            listOfKPI_Plan = parseExcelFile_Plan(fileData_Plan, fileName, "KPI_Plan");

            gridFact.setItems(listOfKPI_Fact);

            gridActuals.setItems(listOfKPI_Actuals);

            gridPlan.setItems(listOfKPI_Plan);

            singleFileUpload.clearFileList();

//            htmlDivToDO.getElement().setProperty("innerHTML", "<u>ToDo:</u> <b>Wenn keine QS Probleme aufgetreten sind, per Button \"Freigabe\" weitere Verarbeitung starten</b>");
            //    message.setText("2. Button >Import< for upload to Database");

//            TodoList.setItems("KPI_DB.xlsx hochgeladen", "QS bestätigen");
//            TodoList.select("KPI_DB.xlsx hochgeladen");
            //  TodoList.setEnabled(true);

            //h3_Fact.removeAll();
            //h3_Fact.add("Fact (" + listOfKPI_Fact.size() + " rows)");

            factInfo="Fact (" + listOfKPI_Fact.size() + " rows)";
            actualsInfo="Fact (" + listOfKPI_Actuals.size() + " rows)";
            planInfo="Fact (" + listOfKPI_Plan.size() + " rows)";

            //h3_Actuals.removeAll();
            //h3_Actuals.add("Actuals (" + listOfKPI_Actuals.size() + " rows)");

            //h3_Plan.removeAll();
            //h3_Plan.add("Plan (" + listOfKPI_Plan.size() + " rows)");

            uploadBtn.setEnabled(true);

        });
        System.out.println("setup uploader................over");
    }
    public List<KPI_Fact> parseExcelFile_Fact(InputStream fileData, String fileName, String sheetName) {


        List<KPI_Fact> listOfKPI_Fact = new ArrayList<>();
        try {
            if(fileName.isEmpty() || fileName.length()==0)
            {
                article=new Article();
                article.setText(LocalDateTime.now().format(formatter) + ": Error: Keine Datei angegeben!");
                textArea.add(article);
            }

            if(!mimeType.contains("openxmlformats-officedocument"))
            {
                article=new Article();
                article.setText(LocalDateTime.now().format(formatter) + ": Error: ungültiges Dateiformat!");
                textArea.add(article);
            }

            System.out.println("Excel import: "+  fileName + " => Mime-Type: " + mimeType  + " Größe " + contentLength + " Byte");
            textArea.setText(LocalDateTime.now().format(formatter) + ": Info: Verarbeite Datei: " + fileName + " (" + contentLength + " Byte)");
            //message.setText(LocalDateTime.now().format(formatter) + ": Info: reading file: " + fileName);

            //addRowsBT.setEnabled(false);
            //replaceRowsBT.setEnabled(false);
            //spinner.setVisible(true);

            //  HSSFWorkbook my_xls_workbook = new HSSFWorkbook(fileData);
            XSSFWorkbook my_xls_workbook = new XSSFWorkbook(fileData);
            //   HSSFSheet my_worksheet = my_xls_workbook.getSheetAt(0);
            XSSFSheet my_worksheet = my_xls_workbook.getSheet("KPI_Fact");
            Iterator<Row> rowIterator = my_worksheet.iterator();

            Integer RowNumber=0;
            Integer Error_count=0;

            while(rowIterator.hasNext() )
            {
                KPI_Fact kPI_Fact = new KPI_Fact();
                Row row = rowIterator.next();
                RowNumber++;

                //if (RowNumber>200){ break; }



                Iterator<Cell> cellIterator = row.cellIterator();
                while(cellIterator.hasNext()) {

                    if(RowNumber==1 ) //Überschrift nicht betrachten
                    {
                        break;
                    }


                    Cell cell = cellIterator.next();
                    kPI_Fact.setRow(RowNumber);

                    if(cell.getColumnIndex()==0)
                    {
                        String ColumnName="NT ID";
                        try {
                            kPI_Fact.setNT_ID(checkCellString(sheetName, cell, RowNumber,ColumnName));
                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;

                        }
                    }

                    if(cell.getColumnIndex()==1)
                    {
                        String ColumnName="Runrate";
                        try {
                            kPI_Fact.setRunrate(checkCellString(sheetName, cell, RowNumber,ColumnName));
                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;

                        }
                    }
                    if(cell.getColumnIndex()==2)
                    {
                        String ColumnName="Scenario";
                        try {
                            kPI_Fact.setScenario(checkCellString(sheetName, cell, RowNumber,ColumnName));
                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;

                        }
                    }

                    if(cell.getColumnIndex()==3)
                    {
                        String ColumnName="Date";
                        try {
                            kPI_Fact.setDate(checkCellDate(sheetName, cell, RowNumber,ColumnName));
                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;

                        }
                    }

                    if(cell.getColumnIndex()==4)
                    {
                        String ColumnName="Wert";
                        try {
                            kPI_Fact.setWert(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;

                        }
                    }

                }

                listOfKPI_Fact.add(kPI_Fact);

            }

            article=new Article();
            article.getStyle().set("white-space","pre-line");
            article.add("\n");
            article.add(LocalDateTime.now().format(formatter) + " " + sheetName + ": Count Rows: " + listOfKPI_Fact.size() + " Count Errrors: " + Error_count);
            article.add("\n");
            textArea.add(article);

            planInfo = "KPI Plan 0 rows";

            System.out.println("Anzahl Zeilen im Excel: " + listOfKPI_Fact.size());
            accordion.remove(factPanel);
            factPanel = new AccordionPanel( "KPI_Fact (" + listOfKPI_Fact.size()+ " rows)", gridFact);
            accordion.add(factPanel);

            return listOfKPI_Fact;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
    public List<KPI_Actuals> parseExcelFile_Actuals(InputStream fileData, String fileName, String sheetName) {


        List<KPI_Actuals> listOfKPI_Actuals = new ArrayList<>();
        try {
            if(fileName.isEmpty() || fileName.length()==0)
            {
                article=new Article();
                article.setText(LocalDateTime.now().format(formatter) + ": Error: Keine Datei angegeben!");
                textArea.add(article);
            }

            if(!mimeType.contains("openxmlformats-officedocument"))
            {
                article=new Article();
                article.setText(LocalDateTime.now().format(formatter) + ": Error: ungültiges Dateiformat!");
                textArea.add(article);
            }


            XSSFWorkbook my_xls_workbook = new XSSFWorkbook(fileData);
            XSSFSheet my_worksheet = my_xls_workbook.getSheet(sheetName);
            Iterator<Row> rowIterator = my_worksheet.iterator();

            Integer RowNumber=0;
            Integer Error_count=0;



            while(rowIterator.hasNext())
            {
                KPI_Actuals kPI_Actuals = new KPI_Actuals();
                Row row = rowIterator.next();
                RowNumber++;

                // if (RowNumber>20){ break; }



                Iterator<Cell> cellIterator = row.cellIterator();
                while(cellIterator.hasNext()) {

                    if(RowNumber==1 ) //Überschrift nicht betrachten
                    {
                        break;
                    }


                    Cell cell = cellIterator.next();
                    kPI_Actuals.setRow(RowNumber);

                    if(cell.getColumnIndex()==0)
                    {
                        String ColumnName="NT ID";
                        try {
                            kPI_Actuals.setNT_ID(checkCellString(sheetName, cell, RowNumber,ColumnName));
                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }
                    }


                    if(cell.getColumnIndex()==1) {
                        String ColumnName="WTAC ID";
                        try {

                            kPI_Actuals.setWTAC_ID(checkCellString(sheetName,cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }

                    }

                    if(cell.getColumnIndex()==2) {
                        String ColumnName="sort";
                        try {
                            kPI_Actuals.setSort(checkCellNumeric(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }

                    }

                    if(cell.getColumnIndex()==3) {
                        String ColumnName="M2_Area";
                        try {
                            kPI_Actuals.setM2_Area(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }

                    }

                    if(cell.getColumnIndex()==4) {
                        String ColumnName="M1_Network";
                        try {
                            kPI_Actuals.setM1_Network(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }

                    }

                    if(cell.getColumnIndex()==5) {
                        String ColumnName="M3_Service";
                        try {
                            kPI_Actuals.setM3_Service(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }
                    }

                    if(cell.getColumnIndex()==6) {
                        String ColumnName="M4_Dimension";
                        try {
                            kPI_Actuals.setM4_Dimension(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }
                    }


                    if(cell.getColumnIndex()==7) {
                        String ColumnName="M5_Tech";
                        try {
                            kPI_Actuals.setM5_Tech(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }
                    }

                    if(cell.getColumnIndex()==8) {
                        String ColumnName="M6_Detail";
                        try {
                            kPI_Actuals.setM6_Detail(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }
                    }

                    if(cell.getColumnIndex()==9) {
                        String ColumnName="KPI long";
                        try {
                            kPI_Actuals.setKPI_long(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }
                    }

                    if(cell.getColumnIndex()==10) {
                        String ColumnName="Runrate";
                        try {
                            kPI_Actuals.setRunrate(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }
                    }

                    if(cell.getColumnIndex()==11) {
                        String ColumnName="Unit";
                        try {
                            kPI_Actuals.setUnit(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }
                    }

                    if(cell.getColumnIndex()==12) {
                        String ColumnName="Description";
                        try {
                            kPI_Actuals.setDescription(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }
                    }

                    if(cell.getColumnIndex()==13) {
                        String ColumnName="SourceReport";
                        try {
                            kPI_Actuals.setSourceReport(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }
                    }

                    if(cell.getColumnIndex()==14) {
                        String ColumnName="SourceInput";
                        try {
                            kPI_Actuals.setSourceInput(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }
                    }

                    if(cell.getColumnIndex()==15) {
                        String ColumnName="SourceComment";
                        try {
                            kPI_Actuals.setSourceComment(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }
                    }

                    if(cell.getColumnIndex()==16) {
                        String ColumnName="SourceContact";
                        try {
                            kPI_Actuals.setSourceContact(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }
                    }

                    if(cell.getColumnIndex()==17) {
                        String ColumnName="SourceLink";
                        try {
                            kPI_Actuals.setSourceLink(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;
                        }
                    }



                }

                listOfKPI_Actuals.add(kPI_Actuals);
            }

            article=new Article();
            article.getStyle().set("white-space","pre-line");
            article.add("\n");
            article.add(LocalDateTime.now().format(formatter) + " " + sheetName + ": Count Rows: " + listOfKPI_Actuals.size() + " Count Errrors: " + Error_count);
            article.add("\n");
            textArea.add(article);


            System.out.println("Anzahl Zeilen im Excel: " + listOfKPI_Actuals.size());
            accordion.remove(actualsPanel);
            actualsPanel = new AccordionPanel( "KPI_Actuals (" + listOfKPI_Actuals.size()+ " rows)", gridActuals);
            accordion.add(actualsPanel);

            return listOfKPI_Actuals;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public List<KPI_Plan> parseExcelFile_Plan(InputStream fileData, String fileName, String sheetName) {


        List<KPI_Plan> listOfKPI_Plan = new ArrayList<>();
        try {
            if(fileName.isEmpty() || fileName.length()==0)
            {
                article=new Article();
                article.setText(LocalDateTime.now().format(formatter) + ": Error: Keine Datei angegeben!");
                textArea.add(article);
            }

            if(!mimeType.contains("openxmlformats-officedocument"))
            {
                article=new Article();
                article.setText(LocalDateTime.now().format(formatter) + ": Error: ungültiges Dateiformat!");
                textArea.add(article);
            }


            XSSFWorkbook my_xls_workbook = new XSSFWorkbook(fileData);
            XSSFSheet my_worksheet = my_xls_workbook.getSheet(sheetName);
            Iterator<Row> rowIterator = my_worksheet.iterator();

            Integer RowNumber=0;
            Integer Error_count=0;
            Boolean sheedEnd=false;



            while(rowIterator.hasNext() && !sheedEnd)
            {
                KPI_Plan kPI_Plan = new KPI_Plan();
                Row row = rowIterator.next();
                RowNumber++;

                // if (RowNumber>20){ break; }

                Iterator<Cell> cellIterator = row.cellIterator();
                while(cellIterator.hasNext()) {

                    if(RowNumber==1 ) //Überschrift nicht betrachten
                    {
                        break;
                    }


                    Cell cell = cellIterator.next();


                    if(cell.getColumnIndex()==0)
                    {
                        String ColumnName="NT ID";
                        try {

                            String ntID=checkCellString(sheetName, cell, RowNumber,ColumnName);

                            if(ntID.isEmpty()) //Leere Excel Zellen nicht betrachten
                            {
                                sheedEnd=true;
                                break;
                            }
                            kPI_Plan.setNT_ID(ntID);
                            kPI_Plan.setRow(RowNumber);


                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;

                        }
                    }



                    if(cell.getColumnIndex()==1) {
                        String ColumnName="Spalte1";
                        try {
                            kPI_Plan.setSpalte1(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;

                        }
                    }

                    if(cell.getColumnIndex()==2) {
                        String ColumnName="Scenario";
                        try {
                            kPI_Plan.setScenario(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;

                        }
                    }

                    if(cell.getColumnIndex()==3) {
                        String ColumnName="VersionDate";
                        try {
                            kPI_Plan.setVersionDate(checkCellDate(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;

                        }
                    }


                    if(cell.getColumnIndex()==4) {
                        String ColumnName="VersionComment";
                        try {
                            kPI_Plan.setVersionComment(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;

                        }
                    }


                    if(cell.getColumnIndex()==5) {
                        String ColumnName="Runrate";
                        try {
                            kPI_Plan.setRunrate(checkCellString(sheetName, cell, RowNumber, ColumnName));
                        } catch (Exception e) {
                            article = new Article();
                            article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            Error_count++;

                        }
                    }



                }

                listOfKPI_Plan.add(kPI_Plan);
            }



            article=new Article();
            article.getStyle().set("white-space","pre-line");
            article.add("\n");
            article.add(LocalDateTime.now().format(formatter) + " " + sheetName + ": Count Rows: " + listOfKPI_Plan.size() + " Count Errrors: " + Error_count);
            article.add("\n");
            textArea.add(article);

            System.out.println("Anzahl Zeilen im Excel: " + listOfKPI_Plan.size());

            accordion.remove(planPanel);
            planPanel = new AccordionPanel( "KPI_Plan (" + listOfKPI_Plan.size()+ " rows)", gridPlan);
            accordion.add(planPanel);

            return listOfKPI_Plan;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private Double checkCellDouble(String sheetName, Cell cell, Integer zeile, String spalte) {

        try {

            switch (cell.getCellType()){
                case Cell.CELL_TYPE_NUMERIC:
                    return  (double) cell.getNumericCellValue();
                case Cell.CELL_TYPE_STRING:
                    return null;
                case Cell.CELL_TYPE_FORMULA:
                    return null;
                case Cell.CELL_TYPE_BLANK:
                    return null;
                case Cell.CELL_TYPE_BOOLEAN:
                    return null;
                case Cell.CELL_TYPE_ERROR:
                    return null;

            }
            article.add("\n" + sheetName + " Zeile " + zeile.toString() + ", column >" + spalte + "< konnte in checkCellDouble nicht aufgelöst werden. Typ=" + cell.getCellType());
            textArea.add(article);

        }
        catch(Exception e){
            switch (e.getMessage()) {
                case "Cannot get a text value from a error formula cell":

                    article = new Article();
                    article.setText("\n" + sheetName + ": Info: row >" + zeile.toString() + "<, column " + spalte + ": formula cell error => replaced to 0");
                    textArea.add(article);

                    return  null;

            }
            System.out.println("Zeile " + zeile.toString() + ", Spalte " + spalte + " konnte in checkCellDouble nicht aufgelöst werden. Typ=" + cell.getCellType() + e.getMessage());
        }


        return  null;



        /*


        if (cell.getCellType()!=Cell.CELL_TYPE_NUMERIC)
        {
            System.out.println("Zeile " + zeile.toString() + ", Spalte " + spalte + " konnte nicht gelesen werden, da ExcelTyp nicht numerisch!");
            //     textArea.setValue(textArea.getValue() + "\n" + LocalDateTime.now().format(formatter) + ": Error: Zeile " + zeile.toString() + ", Spalte " + spalte + " konnte nicht gelesen werden, da ExcelTyp nicht Numeric!");
            article.add("\nZeile " + zeile.toString() + ", Spalte " + spalte + "  konnte nicht gelesen werden, da ExcelTyp nicht Numeric!");
            textArea.add(article);
            return 0.0;
        }
        else
        {
            //System.out.println("Spalte: " + spalte + " Zeile: " + zeile.toString() + " Wert: " + cell.getNumericCellValue());
            return  (double) cell.getNumericCellValue();
        }

         */

    }

    private String checkCellString(String sheetName, Cell cell, Integer zeile, String spalte) {

        try {

            switch (cell.getCellType()){
                case Cell.CELL_TYPE_NUMERIC:
                    return cell.getStringCellValue();
                case Cell.CELL_TYPE_STRING:
                    return cell.getStringCellValue();
                case Cell.CELL_TYPE_FORMULA:
                    return cell.getStringCellValue();
                case Cell.CELL_TYPE_BLANK:
                    return  "";
                case Cell.CELL_TYPE_BOOLEAN:
                    return cell.getStringCellValue();
                case Cell.CELL_TYPE_ERROR:
                    return  "";

            }
            article.add("\n" + sheetName + " Zeile " + zeile.toString() + ", column >" + spalte + "< konnte in checkCellString nicht aufgelöst werden. Typ=" + cell.getCellType());
            textArea.add(article);

        }
        catch(Exception e){
            switch (e.getMessage()) {
                case "Cannot get a text value from a error formula cell":

                    article = new Article();
                    article.setText("\n" + sheetName + ": Info: row >" + zeile.toString() + "<, column " + spalte + ": formula cell error => replaced to empty cell");
                    textArea.add(article);

                    return "";

            }
            System.out.println("Zeile " + zeile.toString() + ", Spalte " + spalte + " konnte in checkCellString nicht aufgelöst werden. Typ=" + cell.getCellType() + e.getMessage());
        }


        return  "######";

    }

    private Date checkCellDate(String sheetName, Cell cell, Integer zeile, String spalte) {
        Date date=null;
        try{


            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_NUMERIC:
                    if (cell.getNumericCellValue() != 0) {
                        //Get date
                        date = (Date) cell.getDateCellValue();



                        //Get datetime
                        cell.getDateCellValue();

                    }
                    break;
            }


            return date;

         /*   if (cell.getCellType()!=Cell.CELL_TYPE_STRING && !cell.getStringCellValue().isEmpty())
            {
                System.out.println("Zeile " + zeile.toString() + ", Spalte " + spalte + " konnte nicht gelesen werden, da ExcelTyp Numeric!");
                //detailsText.setValue(detailsText.getValue() + "\nZeile " + zeile.toString() + ", Spalte " + spalte + "  konnte nicht gelesen werden, da ExcelTyp Numeric!");

                article.add("\nZeile " + zeile.toString() + ", Spalte " + spalte + "  konnte nicht gelesen werden, da ExcelTyp Numeric!");
                textArea.add(article);

                return "";
            }
            else
            {
                if (cell.getStringCellValue().isEmpty())
                {
                    //System.out.println("Info: Zeile " + zeile.toString() + ", Spalte " + spalte + " ist leer");
                    //detailsText.setValue(detailsText.getValue() + "\nZeile " + zeile.toString() + ", Spalte " + spalte + " ist leer");
                    article.add("\nZeile " + zeile.toString() + ", Spalte " + spalte + " ist leer");
                    textArea.add(article);
                }
                return  cell.getStringCellValue();

            }*/
        }
        catch(Exception e) {
            System.out.println("Exception" + e.getMessage());
            //detailsText.setValue(detailsText.getValue() + "\nZeile " + zeile.toString() + ", Spalte " + spalte + "  konnte nicht gelesen werden, da ExcelTyp Numeric!");
            article.add("\nZeile " + zeile.toString() + ", Spalte " + spalte + "  konnte nicht gelesen werden, da ExcelTyp Numeric!");
            textArea.add(article);
            return null;
        }
    }

    private Integer checkCellNumeric(String sheetName, Cell cell, Integer zeile, String spalte) {


        switch (cell.getCellType()){
            case Cell.CELL_TYPE_NUMERIC:
                return  (int) cell.getNumericCellValue();
            case Cell.CELL_TYPE_STRING:
                return 0;
            case Cell.CELL_TYPE_FORMULA:
                return 0;
            case Cell.CELL_TYPE_BLANK:
                return 0;
            case Cell.CELL_TYPE_BOOLEAN:
                return 0;
            case Cell.CELL_TYPE_ERROR:
                return 0;

        }

        return 0;


/*
        if (cell.getCellType()!=Cell.CELL_TYPE_NUMERIC)
        {
            var CellType =cell.getCellType();

            System.out.println("Zeile " + zeile.toString() + ", Spalte " + spalte + " konnte nicht gelesen werden, da ExcelTyp nicht numerisch, sonder hat Typ: " + CellType );
            //     textArea.setValue(textArea.getValue() + "\n" + LocalDateTime.now().format(formatter) + ": Error: Zeile " + zeile.toString() + ", Spalte " + spalte + " konnte nicht gelesen werden, da ExcelTyp nicht Numeric!");
            article.add("\nZeile " + zeile.toString() + ", Spalte " + spalte + "  konnte nicht gelesen werden, da ExcelTyp nicht Numeric!");
            textArea.add(article);
            return 0;
        }
        else
        {
            //System.out.println("Spalte: " + spalte + " Zeile: " + zeile.toString() + " Wert: " + cell.getNumericCellValue());
            return  (int) cell.getNumericCellValue();
        }

 */

    }




    public class KPI_Fact {

        private int row;


        private String NT_ID ;

        private String Scenario = "";
        private String Runrate = "";

        private Date Date;

        private Double Wert;

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public String getNT_ID() {
            return NT_ID;
        }

        public void setNT_ID(String NT_ID) {
            this.NT_ID = NT_ID;
        }

        public String getScenario() {
            return Scenario;
        }

        public void setScenario(String scenario) {
            Scenario = scenario;
        }

        public Date getDate() {
            return Date;
        }

        public void setDate(Date date) {
            Date = date;
        }

        public Double getWert()
        {
            return Wert;
        }

        public void setWert(Double wert) {
            Wert = wert;
        }

        public String getRunrate() {
            return Runrate;
        }

        public void setRunrate(String runrate) {
            Runrate = runrate;
        }
    }

    public class KPI_Actuals {

        private int row;
        private String NT_ID="" ;

        private String WTAC_ID="" ;

        private Integer sort=0;
        private String M2_Area="" ;

        private String M1_Network="" ;

        private String M3_Service="";

        private String M4_Dimension="";

        private String M5_Tech="";

        private String M6_Detail="";

        private String KPI_long="";

        private String Runrate="";

        private String Unit="";
        private String Description="";
        private String SourceReport;
        private String SourceInput="";
        private String SourceComment="";
        private String SourceContact="";
        private String SourceLink="";

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public String getNT_ID() {
            return NT_ID;
        }

        public void setNT_ID(String NT_ID) {
            this.NT_ID = NT_ID;
        }

        public String getWTAC_ID() {
            return WTAC_ID;
        }

        public void setWTAC_ID(String WTAC_ID) {
            this.WTAC_ID = WTAC_ID;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;

        }

        public String getM2_Area() {
            return M2_Area;
        }

        public void setM2_Area(String m2_Area) {
            M2_Area = m2_Area;
        }

        public String getM1_Network() {
            return M1_Network;
        }

        public void setM1_Network(String m1_Network) {
            M1_Network = m1_Network;
        }

        public String getM3_Service() {
            return M3_Service;
        }

        public void setM3_Service(String m3_Service) {
            M3_Service = m3_Service;
        }

        public String getM4_Dimension() {
            return M4_Dimension;
        }

        public void setM4_Dimension(String m4_Dimension) {
            M4_Dimension = m4_Dimension;
        }

        public String getM5_Tech() {
            return M5_Tech;
        }

        public void setM5_Tech(String m5_Tech) {
            M5_Tech = m5_Tech;
        }

        public String getM6_Detail() {
            return M6_Detail;
        }

        public void setM6_Detail(String m6_Detail) {
            M6_Detail = m6_Detail;
        }

        public String getKPI_long() {
            return KPI_long;
        }

        public void setKPI_long(String KPI_long) {
            this.KPI_long = KPI_long;
        }

        public String getRunrate() {
            return Runrate;
        }

        public void setRunrate(String runrate) {
            Runrate = runrate;
        }

        public String getUnit() {
            return Unit;
        }

        public void setUnit(String unit) {
            Unit = unit;
        }

        public String getDescription() {
            return Description;
        }

        public void setDescription(String description) {
            Description = description;
        }

        public String getSourceReport() {
            return SourceReport;
        }

        public void setSourceReport(String sourceReport) {
            SourceReport = sourceReport;
        }

        public String getSourceInput() {
            return SourceInput;
        }

        public void setSourceInput(String sourceInput) {
            SourceInput = sourceInput;
        }

        public String getSourceComment() {
            return SourceComment;
        }

        public void setSourceComment(String sourceComment) {
            SourceComment = sourceComment;
        }

        public String getSourceContact() {
            return SourceContact;
        }

        public void setSourceContact(String sourceContact) {
            SourceContact = sourceContact;
        }

        public String getSourceLink() {
            return SourceLink;
        }

        public void setSourceLink(String sourceLink) {
            SourceLink = sourceLink;
        }
    }

    public class KPI_Plan {

        private int row;

        private String NT_ID ;

        private String Spalte1 ;

        private String Scenario;

        private Date VersionDate;

        private String VersionComment;

        private String Runrate;

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public String getNT_ID() {
            return NT_ID;
        }

        public void setNT_ID(String NT_ID) {
            this.NT_ID = NT_ID;
        }

        public String getSpalte1() {
            return Spalte1;
        }

        public void setSpalte1(String spalte1) {
            Spalte1 = spalte1;
        }

        public String getScenario() {
            return Scenario;
        }

        public void setScenario(String scenario) {
            Scenario = scenario;
        }

        public Date getVersionDate() {
            return VersionDate;
        }

        public void setVersionDate(Date versionDate) {
            VersionDate = versionDate;
        }

        public String getVersionComment() {
            return VersionComment;
        }

        public void setVersionComment(String versionComment) {
            VersionComment = versionComment;
        }

        public String getRunrate() {
            return Runrate;
        }

        public void setRunrate(String runrate) {
            Runrate = runrate;
        }
    }

    public class QS_Status{
        String Sheet;
        String QSName;
        String Status;

        public QS_Status(String sheet, String QSName, String status) {
            Sheet = sheet;
            this.QSName = QSName;
            Status = status;
        }

        public String getSheet() {
            return Sheet;
        }

        public void setSheet(String sheet) {
            Sheet = sheet;
        }

        public String getQSName() {
            return QSName;
        }

        public void setQSName(String QSName) {
            this.QSName = QSName;
        }

        public String getStatus() {
            return Status;
        }

        public void setStatus(String status) {
            Status = status;
        }
    }

}
