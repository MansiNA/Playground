package de.dbuss.tefcontrol.data.modules.userimport;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.*;
import de.dbuss.tefcontrol.components.DefaultUtils;
import de.dbuss.tefcontrol.components.LogView;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.modules.kpi.Strategic_KPIView;
import de.dbuss.tefcontrol.data.service.*;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@PageTitle("Import DimLineTabete | TEF-Control")
@Route(value = "DimLineTapete/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "FLIP"})
public class ImportDimLineTapete extends VerticalLayout implements BeforeEnterObserver {

    private Optional<Projects> projects;
    private List<ProjectAttachmentsDTO> listOfProjectAttachments;
    Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);
    private LogView logView;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String agentName;
    private final ProjectsService projectsService;
    private int projectId;
    private Boolean isLogsVisible = false;
    long contentLength = 0;
    String mimeType = "";
    Integer errors_Fact=0;
    private Boolean isVisible = false;
    private Button uploadBtn;
    private int upload_id;
    private DefaultUtils defaultUtils;

    private List<Dim_Line_Tapete> listOfDim_Line_Tapete = new ArrayList<Dim_Line_Tapete>();
    Integer errors_Count=0;
    InputStream fileDataFact;
    InputStream fileDataDim;

    String fileName = "";
    Article article = new Article();
    Div textArea = new Div();
    private QS_Grid qsGrid;
    private final ProjectAttachmentsService projectAttachmentsService;
    MemoryBuffer memoryBuffer = new MemoryBuffer();
    Upload singleFileUpload = new Upload(memoryBuffer);
    private AuthenticatedUser authenticatedUser;
    private final ProjectConnectionService projectConnectionService;
    private JdbcTemplate jdbcTemplate;
    private final BackendService backendService;
    private String dimTableName;
    public ImportDimLineTapete(JdbcTemplate jdbcTemplate, ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService, ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService, AuthenticatedUser authenticatedUser, BackendService backendService) {

        this.jdbcTemplate = jdbcTemplate;
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;
        this.authenticatedUser = authenticatedUser;
        this.projectConnectionService = projectConnectionService;
        this.backendService = backendService;

        logView = new LogView();
        logView.logMessage(Constants.INFO, "Starting Strategic_KPIView");
        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.Strategic_KPI.equals(projectParameter.getNamespace()))
                .collect(Collectors.toList());

        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : filteredProjectParameters) {
            //  if(projectParameter.getNamespace().equals(Constants.TECH_KPI)) {
            if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                dbServer = projectParameter.getValue();
            } else if (Constants.DB_NAME.equals(projectParameter.getName())) {
                dbName = projectParameter.getValue();
            } else if (Constants.DB_USER.equals(projectParameter.getName())) {
                dbUser = projectParameter.getValue();
            } else if (Constants.DB_PASSWORD.equals(projectParameter.getName())) {
                dbPassword = projectParameter.getValue();
            }
            else if (Constants.DIM_TABLE.equals(projectParameter.getName())) {
                dimTableName = projectParameter.getValue();
            }
            else if (Constants.DB_JOBS.equals(projectParameter.getName())) {
                agentName = projectParameter.getValue();
            }
            // }
        }
        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";

        setProjectParameterGrid(filteredProjectParameters);
        defaultUtils = new DefaultUtils(projectsService, projectAttachmentsService);

        qsGrid = new QS_Grid(projectConnectionService, backendService);

        uploadBtn = new Button("QS and Start Job");
        uploadBtn.setEnabled(false);

        HorizontalLayout hl = new HorizontalLayout();
        hl.add(getTabsheet());
        hl.setHeightFull();
        hl.setSizeFull();

        setHeightFull();
        setSizeFull();
        getStyle().set("overflow", "auto");
        add(hl, parameterGrid);

        parameterGrid.setVisible(false);
        logView.setVisible(false);
        add(logView);
        if(MainLayout.isAdmin) {
            UI.getCurrent().addShortcutListener(
                    () -> {
                        isLogsVisible = !isLogsVisible;
                        logView.setVisible(isLogsVisible);
                    },
                    Key.KEY_V, KeyModifier.ALT);
            UI.getCurrent().addShortcutListener(
                    () -> {
                        isVisible = !isVisible;
                        parameterGrid.setVisible(isVisible);
                    },
                    Key.KEY_I, KeyModifier.ALT);
        }
        logView.logMessage(Constants.INFO, "Ending Tech_KPIView");


    }

    private TabSheet getTabsheet() {
        logView.logMessage(Constants.INFO, "Starting getTabsheet() for Tabs");
        //log.info("Starting getTabsheet() for Tabsheet");
        TabSheet tabSheet = new TabSheet();

        tabSheet.add("Upload", getUpladTab());
        tabSheet.add("Description", getDescriptionTab());
        tabSheet.add("Attachments", getAttachmentTab());

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();
        //log.info("Ending getTabsheet() for Tabsheet");
        logView.logMessage(Constants.INFO, "Ending getTabsheet() for Tabs");
        return tabSheet;
    }

    private Component getUpladTab() {
        logView.logMessage(Constants.INFO, "Sarting getUpladTab() for set upload data");
        VerticalLayout content = new VerticalLayout();

        setupUploader();

        HorizontalLayout hl=new HorizontalLayout(singleFileUpload, qsGrid);
        content.add(hl, textArea, uploadBtn, parameterGrid);


        uploadBtn.addClickListener(e->{


            //Aufruf QS-Grid:
            logView.logMessage(Constants.INFO, "executing QS-Grid");
            //   if (qsGrid.projectId != projectId) {
            hl.remove(qsGrid);
            qsGrid = new QS_Grid(projectConnectionService, backendService);
            hl.add(qsGrid);
            CallbackHandler callbackHandler = new CallbackHandler();
            qsGrid.createDialog(callbackHandler, projectId, upload_id);
            //   }
            qsGrid.showDialog(true);





        });


        content.setSizeFull();
        content.setHeightFull();
        content.getStyle().set("overflow", "auto");


        return content;
    }

    public class CallbackHandler implements QS_Callback {
        // Die Methode, die aufgerufen wird, wenn die externe Methode abgeschlossen ist
        @Override
        public void onComplete(String result) {
            logView.logMessage(Constants.INFO, "Starting CallbackHandler onComplete for execute Start Job");
            if(!result.equals("Cancel")) {
                qsGrid.executeStartJobSteps(upload_id, agentName);
            }
            logView.logMessage(Constants.INFO, "Ending CallbackHandler onComplete for execute Start Job");
        }
    }

    private void setupUploader() {
        logView.logMessage(Constants.INFO, "Starting setupUploader() for setup file uploader");
        singleFileUpload.setWidth("450px");

        singleFileUpload.addStartedListener(e->{
            errors_Count=0;
            //textArea.setText("");
            uploadBtn.setEnabled(false);
            //qsBtn.setEnabled(false);
        });

        singleFileUpload.addSucceededListener(event -> {
            logView.logMessage(Constants.INFO, "File Uploaded: >" + event.getFileName() + "<");

            textArea.removeAll();

            // Get information about the uploaded file
            fileDataFact = memoryBuffer.getInputStream();
            fileDataDim = memoryBuffer.getInputStream();

            fileName = event.getFileName();
            contentLength = event.getContentLength();
            mimeType = event.getMIMEType();

            article=new Article();
            article.setText("Uploaded File: >>" + fileName + "<< (Size: " + contentLength/1024 + " KB)");
            textArea.add(article);

            logView.logMessage(Constants.INFO, "contentLenght: >" + contentLength + "<");
            logView.logMessage(Constants.INFO, "mimeType: >" + mimeType + "<");

            singleFileUpload.clearFileList();

       //     listOfFact_CC_KPI = parseExcelFile_Fact(fileDataFact, fileName,"Fact_CC_KPI");
       //     listOfDim_CC_KPI = parseExcelFile_Dim(fileDataDim, fileName,"DIM_CC_KPI");

            logView.logMessage(Constants.INFO, "error_Count: " + errors_Count);

            if (errors_Count==0)
            {
                logView.logMessage(Constants.INFO, "Uploading in uploadBtn.addClickListener");
                //    ui.setPollInterval(500);

                ProjectUpload projectUpload = new ProjectUpload();
                projectUpload.setFileName(fileName);
                //projectUpload.setUserName(MainLayout.userName);
                Optional<User> maybeUser = authenticatedUser.get();
                if (maybeUser.isPresent()) {
                    User user = maybeUser.get();
                    projectUpload.setUserName(user.getUsername());
                }
                projectUpload.setModulName("Strategic_KPI");

                logView.logMessage(Constants.INFO, "Get file upload id from database");
                projectConnectionService.getJdbcConnection(dbUrl, dbUser, dbPassword); // Set Connection to target DB
                upload_id = projectConnectionService.saveUploadedGenericFileData(projectUpload);

                projectUpload.setUploadId(upload_id);

                logView.logMessage(Constants.INFO, "upload id: " + upload_id);
                System.out.println("Upload_ID: " + upload_id);

                String erg= saveEntities();

                if (erg.contains("successfully") )
                {
                    uploadBtn.setEnabled(true);
                }

                article=new Article();
                article.setText(erg);
                textArea.add(article);



            }
            else {
                article=new Article();
                article.setText("data not saved to db");
                textArea.add(article);

            }

        });
        System.out.println("setup uploader................over");
        logView.logMessage(Constants.INFO, "Ending setupUploader() for setup file uploader");
    }

    private String saveEntities() {
        logView.logMessage(Constants.INFO, "Starting saveFactEntities() for saving Fact file data in database");
        AtomicReference<String> returnStatus= new AtomicReference<>("false");
        int totalRows = listOfDim_Line_Tapete.size();

        //System.out.println("Upload Data to DB");

        projectConnectionService.getJdbcConnection(dbUrl, dbUser, dbPassword);

        //String resultKPIFact = projectConnectionService.saveDimLineTapete(listOfDim_Line_Tapete, dimTableName, upload_id);
       // returnStatus.set(resultKPIFact);

        if (returnStatus.toString().equals(Constants.OK)){
            logView.logMessage(Constants.INFO, "ResultKPIFact: " + returnStatus.toString());
        }
        else{
            logView.logMessage(Constants.ERROR, "ERROR: " + returnStatus.toString());
            return "Data not save to db: " +  returnStatus.toString();
        }

        logView.logMessage(Constants.INFO, "Ending saveFactEntities() for saving Fact file data in database");
        return "Data with upload_id " + upload_id + " saved successfully to db...";
    }

    private Component getDescriptionTab() {
        logView.logMessage(Constants.INFO, "Set Description in getDescriptionTab()");
        return defaultUtils.getProjectDescription();
    }

    private Component getAttachmentTab() {
        logView.logMessage(Constants.INFO, "Set Attachment in getAttachmentTab()");
        return defaultUtils.getProjectAttachements();
    }

    private void updateAttachmentGrid(List<ProjectAttachmentsDTO> projectAttachmentsDTOS) {
        logView.logMessage(Constants.INFO, "Update Description in updateAttachmentGrid()");
        defaultUtils.setProjectId(projectId);
        defaultUtils.setAttachmentGridItems(projectAttachmentsDTOS);
    }
    private void updateDescription() {
        logView.logMessage(Constants.INFO, "Update Attachment in updateDescription()");
        defaultUtils.setProjectId(projectId);
        defaultUtils.setDescription();
    }
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        logView.logMessage(Constants.INFO, "Starting beforeEnter() for update");
        RouteParameters parameters = event.getRouteParameters();
        projectId = Integer.parseInt(parameters.get("project_Id").orElse(null));
        projects = projectsService.findById(projectId);
        projects.ifPresent(value -> listOfProjectAttachments = projectsService.getProjectAttachmentsWithoutFileContent(value));

        updateDescription();
        updateAttachmentGrid(listOfProjectAttachments);
        logView.logMessage(Constants.INFO, "Ending beforeEnter() for update");
    }

    private void setProjectParameterGrid(List<ProjectParameter> listOfProjectParameters) {
        logView.logMessage(Constants.INFO, "Starting setProjectParameterGrid() for set database detail in Grid");
        parameterGrid = new Grid<>(ProjectParameter.class, false);
        parameterGrid.addColumn(ProjectParameter::getName).setHeader("Name").setAutoWidth(true).setResizable(true);
        parameterGrid.addColumn(ProjectParameter::getValue).setHeader("Value").setAutoWidth(true).setResizable(true);
        parameterGrid.addColumn(ProjectParameter::getDescription).setHeader("Description").setAutoWidth(true).setResizable(true);

        parameterGrid.setItems(listOfProjectParameters);
        parameterGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        parameterGrid.setHeight("200px");
        parameterGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        logView.logMessage(Constants.INFO, "Ending setProjectParameterGrid() for set database detail in Grid");
    }

    public class Dim_Line_Tapete {

        private int row;
        private String PL_Line;
        private String PL_Line_Name;
        private String PL_Line_Gen01;
        private String PL_Line_Gen01_Name;
        private String PL_Line_Gen02;
        private String PL_Line_Gen02_Name;
        private String PL_Line_Gen03;
        private String PL_Line_Gen03_Name;
        private String PL_Line_Gen03_Sortierung;
        private String PL_Line_Gen04;
        private String PL_Line_Gen04_Name;
        private String PL_Line_Gen04_Sortierung;
        private String PL_Line_Gen05;
        private String PL_Line_Gen05_Name;
        private String PL_Line_Gen05_Sortierung;
        private String PL_Line_Gen06;
        private String PL_Line_Gen06_Name;
        private String PL_Line_Gen06_Sortierung;
        private String PL_Line_Gen07;
        private String PL_Line_Gen07_Name;
        private String PL_Line_Gen07_Sortierung;
        private String PL_Line_Gen08;
        private String PL_Line_Gen08_Name;
        private String PL_Line_Gen08_Sortierung;
        private String PL_Line_Gen09;
        private String PL_Line_Gen09_Name;
        private String PL_Line_Gen09_Sortierung;
        private String PL_Line_Gen10;
        private String PL_Line_Gen10_Name;
        private String PL_Line_Gen10_Sortierung;
        private String PL_Line_Gen11;
        private String PL_Line_Gen11_Name;
        private String PL_Line_Gen11_Sortierung;
        private String PL_Line_Gen12;
        private String PL_Line_Gen12_Name;
        private String PL_Line_Gen12_Sortierung;
        private String PL_Line_Gen13;
        private String PL_Line_Gen13_Name;
        private String PL_Line_Gen13_Sortierung;
        private String PL_Line_Gen14;
        private String PL_Line_Gen14_Name;
        private String PL_Line_Gen14_Sortierung;
        private String PL_Line_Gen15;
        private String PL_Line_Gen15_Name;
        private String PL_Line_Gen15_Sortierung;


        public boolean isValid() {
            if (PL_Line == null || PL_Line.isEmpty()) {
                return false;
               }
            return true;
        }

    }


}
