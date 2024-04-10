package de.dbuss.tefcontrol.data.modules;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@PageTitle("Import Underlying Cobi | TEF-Control")
@Route(value = "underlying_cobi/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "FLIP"})
public class underlying_cobi extends VerticalLayout implements BeforeEnterObserver {

    private LogView logView;
    private int projectId;
    private QS_Grid qsGrid;
    private Optional<Projects> projects;
    private List<ProjectAttachmentsDTO> listOfProjectAttachments;
    private DefaultUtils defaultUtils;

    Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);
    private Boolean isLogsVisible = false;
    private Boolean isVisible = false;

    private Button uploadBtn;
    private int upload_id;
    MemoryBuffer memoryBuffer = new MemoryBuffer();
    InputStream fileData;
    String fileName = "";
    long contentLength = 0;
    String mimeType = "";
    private List<underlyingFact> listOfFact = new ArrayList<underlyingFact>();
    Upload singleFileUpload = new Upload(memoryBuffer);

    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String agentName;
    Div textArea = new Div();
    Article article = new Article();

    private String targetTableName;
    Integer errors_Count = 0;
    Integer errors_Fact=0;

    private JdbcTemplate jdbcTemplate;
    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private AuthenticatedUser authenticatedUser;
    private final ProjectConnectionService projectConnectionService;
    private final BackendService backendService;

    public underlying_cobi(JdbcTemplate jdbcTemplate, ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService, ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService, AuthenticatedUser authenticatedUser, BackendService backendService) {

        this.jdbcTemplate = jdbcTemplate;
        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;
        this.authenticatedUser = authenticatedUser;
        this.projectConnectionService = projectConnectionService;
        this.backendService = backendService;

        logView = new LogView();
        logView.logMessage(Constants.INFO, "Starting Strategic_KPIView");

        uploadBtn = new Button("QS and Start Job");
        uploadBtn.setEnabled(false);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.UnderlyingCobi.equals(projectParameter.getNamespace()))
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
            } else if (Constants.TARGET_TABLE.equals(projectParameter.getName())) {
                targetTableName = projectParameter.getValue();
            } else if (Constants.DB_JOBS.equals(projectParameter.getName())) {
                agentName = projectParameter.getValue();
            }
            // }
        }
        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";

        setProjectParameterGrid(filteredProjectParameters);
        defaultUtils = new DefaultUtils(projectsService, projectAttachmentsService);

        qsGrid = new QS_Grid(projectConnectionService, backendService);

        HorizontalLayout hl = new HorizontalLayout();
        hl.add(getTabsheet());
        hl.setHeightFull();
        hl.setSizeFull();

        textArea.setClassName("Info");
        textArea.add("please upload Excel file...");

        setHeightFull();
        setSizeFull();
        getStyle().set("overflow", "auto");
        add(hl, parameterGrid);

        parameterGrid.setVisible(false);
        logView.setVisible(false);
        add(logView);
        if (MainLayout.isAdmin) {
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

        HorizontalLayout hl = new HorizontalLayout(singleFileUpload, qsGrid);
        content.add(hl, textArea, uploadBtn, parameterGrid);


        uploadBtn.addClickListener(e -> {


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

    private void setupUploader() {
        logView.logMessage(Constants.INFO, "Starting setupUploader() for setup file uploader");
        singleFileUpload.setWidth("450px");

        singleFileUpload.addStartedListener(e -> {
            errors_Count = 0;
            //textArea.setText("");
            uploadBtn.setEnabled(false);
            //qsBtn.setEnabled(false);
        });


        singleFileUpload.addSucceededListener(event -> {
            logView.logMessage(Constants.INFO, "File Uploaded: >" + event.getFileName() + "<");

            textArea.removeAll();
            textArea.setClassName("Info");

            // Get information about the uploaded file
            fileData = memoryBuffer.getInputStream();


            fileName = event.getFileName();
            contentLength = event.getContentLength();
            mimeType = event.getMIMEType();

            article=new Article();
            article.setText("Uploaded File: >>" + fileName + "<< (Size: " + contentLength/1024 + " KB)");
            textArea.add(article);

            logView.logMessage(Constants.INFO, "contentLenght: >" + contentLength + "<");
            logView.logMessage(Constants.INFO, "mimeType: >" + mimeType + "<");

            singleFileUpload.clearFileList();

            listOfFact = parseExcelFile(fileData, fileName,"UnderlyingCobi");

            if (listOfFact == null){
                article=new Article();
                article.setText("Error: no Sheet with name >>UnderlyingCobi<< found!");
                textArea.add(article);
                textArea.setClassName("Error");
                return;
            }

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
                projectUpload.setModulName("Underlying_Cobi");

                logView.logMessage(Constants.INFO, "Get file upload id from database");
                projectConnectionService.getJdbcConnection(dbUrl, dbUser, dbPassword); // Set Connection to target DB
                upload_id = projectConnectionService.saveUploadedGenericFileData(projectUpload);

                if (upload_id == -1){
                    article=new Article();
                    article.setText("Error: could not generate upload_id !");
                    textArea.add(article);
                    textArea.setClassName("Error");
                    return;
                }

                projectUpload.setUploadId(upload_id);

                logView.logMessage(Constants.INFO, "upload id: " + upload_id);
                System.out.println("Upload_ID: " + upload_id);

                String erg= saveEntities();

                if (erg.contains("successfully") )
                {
                    uploadBtn.setEnabled(true);
                }

                Paragraph p = new Paragraph(erg);
                textArea.add(p);



            }
            else {
                Paragraph p = new Paragraph("data not saved to db");
                textArea.add(p);
                textArea.setClassName("Error");

            }

        });
        System.out.println("setup uploader................over");
        logView.logMessage(Constants.INFO, "Ending setupUploader() for setup file uploader");



    }

    public List<underlyingFact> parseExcelFile(InputStream fileData, String fileName, String sheetName) {
        logView.logMessage(Constants.INFO, "Starting parseExcelFile() for parse uploaded file");

        List<underlyingFact> listOfFact = new ArrayList<>();
        try {
            if(fileName.isEmpty() || fileName.length()==0)
            {
                article=new Article();
                article.setText("Error: Keine Datei angegeben!");
                textArea.add(article);
            }

            if(!mimeType.contains("openxmlformats-officedocument"))
            {
                article=new Article();
                article.setText("Error: ungültiges Dateiformat!");
                textArea.add(article);
            }

            System.out.println("Excel import: "+  fileName + " => Mime-Type: " + mimeType  + " Größe " + contentLength + " Byte");

            //  HSSFWorkbook my_xls_workbook = new HSSFWorkbook(fileData);
            XSSFWorkbook my_xls_workbook = new XSSFWorkbook(fileData);
            //   HSSFSheet my_worksheet = my_xls_workbook.getSheetAt(0);
            XSSFSheet my_worksheet = my_xls_workbook.getSheet(sheetName);

            if (my_worksheet == null){
                return null;
            }

            Iterator<Row> rowIterator = my_worksheet.iterator();

            Integer RowNumber=0;
            errors_Fact=0;

            while(rowIterator.hasNext() )
            {
                underlyingFact Fact = new underlyingFact();
                Row row = rowIterator.next();
                RowNumber++;

                if (errors_Fact >0 | errors_Count!=0){
                    errors_Count++;
                    article=new Article();
                    article.setText("Abort further processing...");
                    textArea.add(article);
                    return null;
                }

                Iterator<Cell> cellIterator = row.cellIterator();

                while(cellIterator.hasNext()) {

                    if(RowNumber==1 ) //Überschrift nicht betrachten, aber Anzahl Spalten kontrollieren
                    {
                        if (row.getLastCellNum()<7)
                        {
                            article=new Article();
                            article.setText("Error: Count Columns: " + row.getLastCellNum() + " Expected: 7! (Segment | ProfitCenter | PL_LINE | Month | Scenario | TypeofData | Amount)");
                            textArea.add(article);
                            errors_Fact=1;
                        }

                        break;
                    }


                    Cell cell = cellIterator.next();
                    Fact.setRow(RowNumber);

                    if(cell.getColumnIndex()==0)
                    {
                        String ColumnName="Block";
                        try {
                            Fact.setBlock(defaultUtils.getCellString(cell));
                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;
                        }
                    }

                    if(cell.getColumnIndex()==1)
                    {
                        String ColumnName="Segment";
                        try {
                            Fact.setSegment(defaultUtils.getCellString(cell));
                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;
                        }
                    }

                    if(cell.getColumnIndex()==2)
                    {
                        String ColumnName="ProfitCenter";
                        try {
                            Fact.setProfitCenter(defaultUtils.getCellString(cell));

                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==3)
                    {
                        String ColumnName="PL_LINE";
                        try {
                            Fact.setPl_Line(defaultUtils.getCellString(cell));
                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }

                    if(cell.getColumnIndex()==4)
                    {
                        String ColumnName="Month";
                        try {
                            //kPI_Fact.setCC_KPI(checkCellString(sheetName, cell, RowNumber,ColumnName));
                            Fact.setMonth(defaultUtils.getCellNumeric(cell));
                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }

                    if(cell.getColumnIndex()==5)
                    {
                        String ColumnName="Scenario";
                        try {
                            //kPI_Fact.setCC_KPI(checkCellString(sheetName, cell, RowNumber,ColumnName));
                            Fact.setScenario(defaultUtils.getCellString(cell));
                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }

                    if(cell.getColumnIndex()==6)
                    {
                        String ColumnName="TypeofData";
                        try {
                            //kPI_Fact.setCC_KPI(checkCellString(sheetName, cell, RowNumber,ColumnName));
                            Fact.setTypeOfData(defaultUtils.getCellString(cell));
                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }


                    if(cell.getColumnIndex()==7)
                    {
                        String ColumnName="Amount";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            Fact.setAmount(defaultUtils.getCellDouble(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }

                }

                if(Fact.isValid() )
                {
                    listOfFact.add(Fact);
                }
                else
                {
                    System.out.println("Fact: skip empty row : " + Fact.getRow());
                }


            }


            article=new Article();
            article.setText("Count rows sheet: " + sheetName + " => " + listOfFact.size());
            textArea.add(article);


            errors_Count+=errors_Fact;
            logView.logMessage(Constants.INFO, "Ending parseExcelFile() for parse uploaded file");
            return listOfFact;

        } catch (Exception e) {
            article=new Article();
            article.setText("Error while parse file!: " + e.getMessage());
            textArea.add(article);
            logView.logMessage(Constants.ERROR, "Error while parse uploaded file");
            textArea.setClassName("Error");
            e.printStackTrace();
            return null;
        }

    }

    private String saveEntities() {
        logView.logMessage(Constants.INFO, "Starting saveFactEntities() for saving Fact file data in database");
        AtomicReference<String> returnStatus= new AtomicReference<>("false");
        int totalRows = listOfFact.size();

        //System.out.println("Upload Data to DB");

        projectConnectionService.getJdbcConnection(dbUrl, dbUser, dbPassword);

        String resultFact = projectConnectionService.saveUnderlyingCobi(listOfFact, targetTableName, upload_id);
        returnStatus.set(resultFact);

        if (returnStatus.toString().equals(Constants.OK)){
            logView.logMessage(Constants.INFO, "ResultKPIFact: " + returnStatus.toString());
        }
        else{
            logView.logMessage(Constants.ERROR, "ERROR: " + returnStatus.toString());
            textArea.setClassName("Error");
            return "Data not save to db: " +  returnStatus.toString();
        }


        logView.logMessage(Constants.INFO, "Ending saveFactEntities() for saving Fact file data in database");
        return "Data with upload_id " + upload_id + " saved successfully to db...";
    }

    public class CallbackHandler implements QS_Callback {
        // Die Methode, die aufgerufen wird, wenn die externe Methode abgeschlossen ist
        @Override
        public void onComplete(String result) {
            logView.logMessage(Constants.INFO, "Starting CallbackHandler onComplete for execute Start Job");
            if(!result.equals("Cancel")) {
                qsGrid.executeStartJobSteps(upload_id, agentName);
                article=new Article();
                article.setText("Job" + agentName + " started...");
                textArea.add(article);

            }
            else
            {
                article=new Article();
                article.setText("Job" + agentName + " not started");
                textArea.add(article);
            }

            logView.logMessage(Constants.INFO, "Ending CallbackHandler onComplete for execute Start Job");
        }
    }

    private Component getDescriptionTab() {
        logView.logMessage(Constants.INFO, "Set Description in getDescriptionTab()");
        return defaultUtils.getProjectDescription();
    }

    private Component getAttachmentTab() {
        logView.logMessage(Constants.INFO, "Set Attachment in getAttachmentTab()");
        return defaultUtils.getProjectAttachements();
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


    public class underlyingFact {

        private int row;
        private int month ;

        private String block = "";
        private String segment = "";
        private String profitCenter = "";
        private String pl_Line = "";
        private String scenario = "";
        private String typeOfData = "";
        private double amount;

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public String getBlock() {
            return block;
        }

        public void setBlock(String block) {
            this.block = block;
        }
        public String getSegment() {
            return segment;
        }

        public void setSegment(String segment) {
            this.segment = segment;
        }

        public String getProfitCenter() {
            return profitCenter;
        }

        public void setProfitCenter(String profitCenter) {
            this.profitCenter = profitCenter;
        }

        public String getPl_Line() {
            return pl_Line;
        }

        public void setPl_Line(String pl_Line) {
            this.pl_Line = pl_Line;
        }

        public String getScenario() {
            return scenario;
        }

        public void setScenario(String scenario) {
            this.scenario = scenario;
        }

        public String getTypeOfData() {
            return typeOfData;
        }

        public void setTypeOfData(String typeOfData) {
            this.typeOfData = typeOfData;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public boolean isValid() {
            if (profitCenter == null || profitCenter.isEmpty()) {
                if (pl_Line == null || pl_Line.isEmpty()) {
                    if (scenario == null || scenario.isEmpty()) {
                        return false;
                    }
                }
            }
            return true;
        }

    }





}
