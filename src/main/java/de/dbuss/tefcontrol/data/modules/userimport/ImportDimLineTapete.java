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

    MemoryBuffer memoryBuffer = new MemoryBuffer();
    Upload singleFileUpload = new Upload(memoryBuffer);


    private JdbcTemplate jdbcTemplate;
    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private AuthenticatedUser authenticatedUser;
    private final ProjectConnectionService projectConnectionService;
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
        logView.logMessage(Constants.INFO, "Starting DimLineTapete");
        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.DimLineTapete.equals(projectParameter.getNamespace()))
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

        textArea.setClassName("Info");
        textArea.add("please upload Excel file...");

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
            textArea.setClassName("Info");

            // Get information about the uploaded file
            fileDataFact = memoryBuffer.getInputStream();

            fileName = event.getFileName();
            contentLength = event.getContentLength();
            mimeType = event.getMIMEType();

            article=new Article();
            article.setText("Uploaded File: >>" + fileName + "<< (Size: " + contentLength/1024 + " KB)");
            textArea.add(article);

            logView.logMessage(Constants.INFO, "contentLenght: >" + contentLength + "<");
            logView.logMessage(Constants.INFO, "mimeType: >" + mimeType + "<");

            singleFileUpload.clearFileList();

            listOfDim_Line_Tapete = parseExcelFile(fileDataFact, fileName,"DimLineTapete");

            if (listOfDim_Line_Tapete == null){
                article=new Article();
                article.setText("Error: no Sheet with name >>DimLineTapete<< found!");
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
                projectUpload.setModulName("ImportDimLineTapete");

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


    public List<Dim_Line_Tapete> parseExcelFile(InputStream fileData, String fileName, String sheetName) {
        logView.logMessage(Constants.INFO, "Starting parseExcelFile for parse uploaded file");

        List<Dim_Line_Tapete> listOfDimLineTapete = new ArrayList<>();
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
                Dim_Line_Tapete kPI_Fact = new Dim_Line_Tapete();
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
                        if (row.getLastCellNum()<45)
                        {
                            article=new Article();
                            article.setText("Error: Count Columns: " + row.getLastCellNum() + " Expected: 45!");
                            textArea.add(article);
                            errors_Fact=1;
                        }

                        break;
                    }


                    Cell cell = cellIterator.next();
                    kPI_Fact.setRow(RowNumber);

                    if(cell.getColumnIndex()==0)
                    {
                        String ColumnName="PL_Line";

                        try {
                            //kPI_Fact.setPeriod(checkCellNumeric(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line(defaultUtils.getCellString(cell));
                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;
                        }

                        //System.out.println("Column: " + ColumnName + " Value: " + kPI_Fact.getPL_Line());

                    }

                    if(cell.getColumnIndex()==1)
                    {
                        String ColumnName="PL_Line_Name";
                        try {
                            //kPI_Fact.setScenario(checkCellString(sheetName, cell, RowNumber,ColumnName));
                            //kPI_Fact.setScenario(getCellString(cell));

                            kPI_Fact.setPL_Line_Name(defaultUtils.getCellString(cell));

                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }

                       // System.out.println("Column: " + ColumnName + " Value: " + kPI_Fact.getPL_Line_Name());

                    }
                    if(cell.getColumnIndex()==2)
                    {
                        String ColumnName="PL_Line_Gen01";
                        try {
                            //kPI_Fact.setSegment(checkCellString(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen01(defaultUtils.getCellString(cell));
                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }

                        //System.out.println("Column: " + ColumnName + " Value: " + kPI_Fact.getPL_Line_Gen01());

                    }

                    if(cell.getColumnIndex()==3)
                    {
                        String ColumnName="PL_Line_Gen01_Name";
                        try {
                            //kPI_Fact.setCC_KPI(checkCellString(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen01_Name(defaultUtils.getCellString(cell));
                        }
                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }

                        //System.out.println("Column: " + ColumnName + " Value: " + kPI_Fact.getPL_Line_Gen01_Name());

                    }

                    if(cell.getColumnIndex()==4)
                    {
                        String ColumnName="PL_Line_Gen02";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen02(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }

                        //System.out.println("Column: " + ColumnName + " Value: " + kPI_Fact.getPL_Line_Gen02());
                    }

                    if(cell.getColumnIndex()==5)
                    {
                        String ColumnName="PL_Line_Gen02_Name";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen02_Name(defaultUtils.getCellString(cell));
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
                        String ColumnName="PL_Line_Gen03";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen03(defaultUtils.getCellString(cell));
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
                        String ColumnName="PL_Line_Gen03_Name";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen03_Name(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==8)
                    {
                        String ColumnName="PL_Line_Gen03_Sortierung";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            //kPI_Fact.setPL_Line_Gen03_Sortierung(defaultUtils.getCellNumeric(cell));
                            kPI_Fact.setPL_Line_Gen03_Sortierung(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==9)
                    {
                        String ColumnName="PL_Line_Gen04";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen04(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==10)
                    {
                        String ColumnName="PL_Line_Gen04_Name";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen04_Name(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==11)
                    {
                        String ColumnName="PL_Line_Gen04_Sortierung";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            //kPI_Fact.setPL_Line_Gen04_Sortierung(defaultUtils.getCellNumeric(cell));
                            kPI_Fact.setPL_Line_Gen04_Sortierung(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==12)
                    {
                        String ColumnName="PL_Line_Gen05";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen05(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==13)
                    {
                        String ColumnName="PL_Line_Gen05_Name";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen05_Name(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                        //System.out.println("Column: " + ColumnName + " Value: " + kPI_Fact.getPL_Line_Gen05_Name());

                    }
                    if(cell.getColumnIndex()==14)
                    {
                        String ColumnName="PL_Line_Gen05_Sortierung";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            //kPI_Fact.setPL_Line_Gen05_Sortierung(defaultUtils.getCellNumeric(cell));
                            kPI_Fact.setPL_Line_Gen05_Sortierung(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }

                        //System.out.println("Column: " + ColumnName + " Value: " + kPI_Fact.getPL_Line_Gen05_Sortierung());
                    }
                    if(cell.getColumnIndex()==15)
                    {
                        String ColumnName="PL_Line_Gen06";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen06(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }

                        //System.out.println("Column: " + ColumnName + " Value: " + kPI_Fact.getPL_Line_Gen06());
                    }
                    if(cell.getColumnIndex()==16)
                    {
                        String ColumnName="PL_Line_Gen06_Name";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen06_Name(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }

                        //System.out.println("Column: " + ColumnName + " Value: " + kPI_Fact.getPL_Line_Gen06_Name());
                    }
                    if(cell.getColumnIndex()==17)
                    {
                        String ColumnName="PL_Line_Gen06_Sortierung";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                           // kPI_Fact.setPL_Line_Gen06_Sortierung(defaultUtils.getCellNumeric(cell));
                            kPI_Fact.setPL_Line_Gen06_Sortierung(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }

                        //System.out.println("Column: " + ColumnName + " Value: " + kPI_Fact.getPL_Line_Gen06_Sortierung());
                    }
                    if(cell.getColumnIndex()==18)
                    {
                        String ColumnName="PL_Line_Gen07";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen07(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==19)
                    {
                        String ColumnName="PL_Line_Gen07_Name";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen07_Name(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==20)
                    {
                        String ColumnName="PL_Line_Gen07_Sortierung";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            //kPI_Fact.setPL_Line_Gen07_Sortierung(defaultUtils.getCellNumeric(cell));
                            kPI_Fact.setPL_Line_Gen07_Sortierung(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==21)
                    {
                        String ColumnName="PL_Line_Gen08";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen08(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==22)
                    {
                        String ColumnName="PL_Line_Gen08_Name";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen08_Name(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==23)
                    {
                        String ColumnName="PL_Line_Gen08_Sortierung";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            //kPI_Fact.setPL_Line_Gen08_Sortierung(defaultUtils.getCellNumeric(cell));
                            kPI_Fact.setPL_Line_Gen08_Sortierung(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==24)
                    {
                        String ColumnName="PL_Line_Gen09";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen09(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==25)
                    {
                        String ColumnName="PL_Line_Gen09_Name";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen09_Name(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==26)
                    {
                        String ColumnName="PL_Line_Gen09_Sortierung";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            //kPI_Fact.setPL_Line_Gen09_Sortierung(defaultUtils.getCellNumeric(cell));
                            kPI_Fact.setPL_Line_Gen09_Sortierung(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==27)
                    {
                        String ColumnName="PL_Line_Gen10";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen10(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==28)
                    {
                        String ColumnName="PL_Line_Gen10_Name";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen10_Name(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==29)
                    {
                        String ColumnName="PL_Line_Gen10_Sortierung";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            //kPI_Fact.setPL_Line_Gen10_Sortierung(defaultUtils.getCellNumeric(cell));
                            kPI_Fact.setPL_Line_Gen10_Sortierung(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==30)
                    {
                        String ColumnName="PL_Line_Gen11";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen11(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==31)
                    {
                        String ColumnName="PL_Line_Gen11_Name";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen11_Name(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==32)
                    {
                        String ColumnName="PL_Line_Gen11_Sortierung";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            //kPI_Fact.setPL_Line_Gen11_Sortierung(defaultUtils.getCellNumeric(cell));
                            kPI_Fact.setPL_Line_Gen11_Sortierung(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==33)
                    {
                        String ColumnName="PL_Line_Gen12";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen12(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==34)
                    {
                        String ColumnName="PL_Line_Gen12_Name";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen12_Name(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==35)
                    {
                        String ColumnName="PL_Line_Gen12_Sortierung";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            //kPI_Fact.setPL_Line_Gen12_Sortierung(defaultUtils.getCellNumeric(cell));
                            kPI_Fact.setPL_Line_Gen12_Sortierung(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==36)
                    {
                        String ColumnName="PL_Line_Gen13";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen13(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==37)
                    {
                        String ColumnName="PL_Line_Gen13_Name";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen13_Name(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==38)
                    {
                        String ColumnName="PL_Line_Gen13_Sortierung";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            //kPI_Fact.setPL_Line_Gen13_Sortierung(defaultUtils.getCellNumeric(cell));
                            kPI_Fact.setPL_Line_Gen13_Sortierung(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==39)
                    {
                        String ColumnName="PL_Line_Gen14";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen14(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==40)
                    {
                        String ColumnName="PL_Line_Gen14_Name";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen14_Name(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==41)
                    {
                        String ColumnName="PL_Line_Gen14_Sortierung";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            //kPI_Fact.setPL_Line_Gen14_Sortierung(defaultUtils.getCellNumeric(cell));
                            kPI_Fact.setPL_Line_Gen14_Sortierung(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==42)
                    {
                        String ColumnName="PL_Line_Gen15";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen15(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==43)
                    {
                        String ColumnName="PL_Line_Gen15_Name";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            kPI_Fact.setPL_Line_Gen15_Name(defaultUtils.getCellString(cell));
                        }

                        catch(Exception e)
                        {
                            article=new Article();
                            article.setText("Sheet: " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==44)
                    {
                        String ColumnName="PL_Line_Gen15_Sortierung";
                        try {
                            //kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                            //kPI_Fact.setPL_Line_Gen15_Sortierung(defaultUtils.getCellNumeric(cell));
                            kPI_Fact.setPL_Line_Gen15_Sortierung(defaultUtils.getCellString(cell));
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

                if(kPI_Fact.isValid() )
                {
                    listOfDimLineTapete.add(kPI_Fact);
                }
                else
                {
                    System.out.println("Fact: skip empty row : " + kPI_Fact.getRow());
                }


            }


            article=new Article();
            article.setText("Count rows sheet: " + sheetName + " => " + listOfDimLineTapete.size());
            textArea.add(article);


            errors_Count+=errors_Fact;
            logView.logMessage(Constants.INFO, "Ending parseExcelFile_Fact() for parse uploaded file");
            return listOfDimLineTapete;

        } catch (Exception e) {
            article=new Article();
            article.setText("Error while parse file!: " + e.getMessage());
            textArea.add(article);
            textArea.setClassName("Error");
            logView.logMessage(Constants.ERROR, "Error while parse uploaded file");
            e.printStackTrace();
            return null;
        }

    }


    private String saveEntities() {
        logView.logMessage(Constants.INFO, "Starting saveEntities() for saving data in database");
        AtomicReference<String> returnStatus= new AtomicReference<>("false");
        int totalRows = listOfDim_Line_Tapete.size();

        //System.out.println("Upload Data to DB");

        projectConnectionService.getJdbcConnection(dbUrl, dbUser, dbPassword);

        String result = projectConnectionService.saveDimLineTapete(listOfDim_Line_Tapete, dimTableName, upload_id);
        returnStatus.set(result);

        if (returnStatus.toString().equals(Constants.OK)){
            logView.logMessage(Constants.INFO, "Result: " + returnStatus.toString());
        }
        else{
            logView.logMessage(Constants.ERROR, "ERROR: " + returnStatus.toString());
            textArea.setClassName("Error");
            return "Data not save to db: " +  returnStatus.toString();
        }

        logView.logMessage(Constants.INFO, "Ending saveEntities() for saving data in database");
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

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public String getPL_Line() {
            return PL_Line;
        }

        public void setPL_Line(String PL_Line) {
            this.PL_Line = PL_Line;
        }

        public String getPL_Line_Name() {
            return PL_Line_Name;
        }

        public void setPL_Line_Name(String PL_Line_Name) {
            this.PL_Line_Name = PL_Line_Name;
        }

        public String getPL_Line_Gen01() {
            return PL_Line_Gen01;
        }

        public void setPL_Line_Gen01(String PL_Line_Gen01) {
            this.PL_Line_Gen01 = PL_Line_Gen01;
        }

        public String getPL_Line_Gen01_Name() {
            return PL_Line_Gen01_Name;
        }

        public void setPL_Line_Gen01_Name(String PL_Line_Gen01_Name) {
            this.PL_Line_Gen01_Name = PL_Line_Gen01_Name;
        }

        public String getPL_Line_Gen02() {
            return PL_Line_Gen02;
        }

        public void setPL_Line_Gen02(String PL_Line_Gen02) {
            this.PL_Line_Gen02 = PL_Line_Gen02;
        }

        public String getPL_Line_Gen02_Name() {
            return PL_Line_Gen02_Name;
        }

        public void setPL_Line_Gen02_Name(String PL_Line_Gen02_Name) {
            this.PL_Line_Gen02_Name = PL_Line_Gen02_Name;
        }

        public String getPL_Line_Gen03() {
            return PL_Line_Gen03;
        }

        public void setPL_Line_Gen03(String PL_Line_Gen03) {
            this.PL_Line_Gen03 = PL_Line_Gen03;
        }

        public String getPL_Line_Gen03_Name() {
            return PL_Line_Gen03_Name;
        }

        public void setPL_Line_Gen03_Name(String PL_Line_Gen03_Name) {
            this.PL_Line_Gen03_Name = PL_Line_Gen03_Name;
        }

        public String getPL_Line_Gen03_Sortierung() {
            return PL_Line_Gen03_Sortierung;
        }

        public void setPL_Line_Gen03_Sortierung(String PL_Line_Gen03_Sortierung) {
            this.PL_Line_Gen03_Sortierung = PL_Line_Gen03_Sortierung;
        }

        public String getPL_Line_Gen04() {
            return PL_Line_Gen04;
        }

        public void setPL_Line_Gen04(String PL_Line_Gen04) {
            this.PL_Line_Gen04 = PL_Line_Gen04;
        }

        public String getPL_Line_Gen04_Name() {
            return PL_Line_Gen04_Name;
        }

        public void setPL_Line_Gen04_Name(String PL_Line_Gen04_Name) {
            this.PL_Line_Gen04_Name = PL_Line_Gen04_Name;
        }

        public String getPL_Line_Gen04_Sortierung() {
            return PL_Line_Gen04_Sortierung;
        }

        public void setPL_Line_Gen04_Sortierung(String PL_Line_Gen04_Sortierung) {
            this.PL_Line_Gen04_Sortierung = PL_Line_Gen04_Sortierung;
        }

        public String getPL_Line_Gen05() {
            return PL_Line_Gen05;
        }

        public void setPL_Line_Gen05(String PL_Line_Gen05) {
            this.PL_Line_Gen05 = PL_Line_Gen05;
        }

        public String getPL_Line_Gen05_Name() {
            return PL_Line_Gen05_Name;
        }

        public void setPL_Line_Gen05_Name(String PL_Line_Gen05_Name) {
            this.PL_Line_Gen05_Name = PL_Line_Gen05_Name;
        }

        public String getPL_Line_Gen05_Sortierung() {
            return PL_Line_Gen05_Sortierung;
        }

        public void setPL_Line_Gen05_Sortierung(String PL_Line_Gen05_Sortierung) {
            this.PL_Line_Gen05_Sortierung = PL_Line_Gen05_Sortierung;
        }

        public String getPL_Line_Gen06() {
            return PL_Line_Gen06;
        }

        public void setPL_Line_Gen06(String PL_Line_Gen06) {
            this.PL_Line_Gen06 = PL_Line_Gen06;
        }

        public String getPL_Line_Gen06_Name() {
            return PL_Line_Gen06_Name;
        }

        public void setPL_Line_Gen06_Name(String PL_Line_Gen06_Name) {
            this.PL_Line_Gen06_Name = PL_Line_Gen06_Name;
        }

        public String getPL_Line_Gen06_Sortierung() {
            return PL_Line_Gen06_Sortierung;
        }

        public void setPL_Line_Gen06_Sortierung(String PL_Line_Gen06_Sortierung) {
            this.PL_Line_Gen06_Sortierung = PL_Line_Gen06_Sortierung;
        }

        public String getPL_Line_Gen07() {
            return PL_Line_Gen07;
        }

        public void setPL_Line_Gen07(String PL_Line_Gen07) {
            this.PL_Line_Gen07 = PL_Line_Gen07;
        }

        public String getPL_Line_Gen07_Name() {
            return PL_Line_Gen07_Name;
        }

        public void setPL_Line_Gen07_Name(String PL_Line_Gen07_Name) {
            this.PL_Line_Gen07_Name = PL_Line_Gen07_Name;
        }

        public String getPL_Line_Gen07_Sortierung() {
            return PL_Line_Gen07_Sortierung;
        }

        public void setPL_Line_Gen07_Sortierung(String PL_Line_Gen07_Sortierung) {
            this.PL_Line_Gen07_Sortierung = PL_Line_Gen07_Sortierung;
        }

        public String getPL_Line_Gen08() {
            return PL_Line_Gen08;
        }

        public void setPL_Line_Gen08(String PL_Line_Gen08) {
            this.PL_Line_Gen08 = PL_Line_Gen08;
        }

        public String getPL_Line_Gen08_Name() {
            return PL_Line_Gen08_Name;
        }

        public void setPL_Line_Gen08_Name(String PL_Line_Gen08_Name) {
            this.PL_Line_Gen08_Name = PL_Line_Gen08_Name;
        }

        public String getPL_Line_Gen08_Sortierung() {
            return PL_Line_Gen08_Sortierung;
        }

        public void setPL_Line_Gen08_Sortierung(String PL_Line_Gen08_Sortierung) {
            this.PL_Line_Gen08_Sortierung = PL_Line_Gen08_Sortierung;
        }

        public String getPL_Line_Gen09() {
            return PL_Line_Gen09;
        }

        public void setPL_Line_Gen09(String PL_Line_Gen09) {
            this.PL_Line_Gen09 = PL_Line_Gen09;
        }

        public String getPL_Line_Gen09_Name() {
            return PL_Line_Gen09_Name;
        }

        public void setPL_Line_Gen09_Name(String PL_Line_Gen09_Name) {
            this.PL_Line_Gen09_Name = PL_Line_Gen09_Name;
        }

        public String getPL_Line_Gen09_Sortierung() {
            return PL_Line_Gen09_Sortierung;
        }

        public void setPL_Line_Gen09_Sortierung(String PL_Line_Gen09_Sortierung) {
            this.PL_Line_Gen09_Sortierung = PL_Line_Gen09_Sortierung;
        }

        public String getPL_Line_Gen10() {
            return PL_Line_Gen10;
        }

        public void setPL_Line_Gen10(String PL_Line_Gen10) {
            this.PL_Line_Gen10 = PL_Line_Gen10;
        }

        public String getPL_Line_Gen10_Name() {
            return PL_Line_Gen10_Name;
        }

        public void setPL_Line_Gen10_Name(String PL_Line_Gen10_Name) {
            this.PL_Line_Gen10_Name = PL_Line_Gen10_Name;
        }

        public String getPL_Line_Gen10_Sortierung() {
            return PL_Line_Gen10_Sortierung;
        }

        public void setPL_Line_Gen10_Sortierung(String PL_Line_Gen10_Sortierung) {
            this.PL_Line_Gen10_Sortierung = PL_Line_Gen10_Sortierung;
        }

        public String getPL_Line_Gen11() {
            return PL_Line_Gen11;
        }

        public void setPL_Line_Gen11(String PL_Line_Gen11) {
            this.PL_Line_Gen11 = PL_Line_Gen11;
        }

        public String getPL_Line_Gen11_Name() {
            return PL_Line_Gen11_Name;
        }

        public void setPL_Line_Gen11_Name(String PL_Line_Gen11_Name) {
            this.PL_Line_Gen11_Name = PL_Line_Gen11_Name;
        }

        public String getPL_Line_Gen11_Sortierung() {
            return PL_Line_Gen11_Sortierung;
        }

        public void setPL_Line_Gen11_Sortierung(String PL_Line_Gen11_Sortierung) {
            this.PL_Line_Gen11_Sortierung = PL_Line_Gen11_Sortierung;
        }

        public String getPL_Line_Gen12() {
            return PL_Line_Gen12;
        }

        public void setPL_Line_Gen12(String PL_Line_Gen12) {
            this.PL_Line_Gen12 = PL_Line_Gen12;
        }

        public String getPL_Line_Gen12_Name() {
            return PL_Line_Gen12_Name;
        }

        public void setPL_Line_Gen12_Name(String PL_Line_Gen12_Name) {
            this.PL_Line_Gen12_Name = PL_Line_Gen12_Name;
        }

        public String getPL_Line_Gen12_Sortierung() {
            return PL_Line_Gen12_Sortierung;
        }

        public void setPL_Line_Gen12_Sortierung(String PL_Line_Gen12_Sortierung) {
            this.PL_Line_Gen12_Sortierung = PL_Line_Gen12_Sortierung;
        }

        public String getPL_Line_Gen13() {
            return PL_Line_Gen13;
        }

        public void setPL_Line_Gen13(String PL_Line_Gen13) {
            this.PL_Line_Gen13 = PL_Line_Gen13;
        }

        public String getPL_Line_Gen13_Name() {
            return PL_Line_Gen13_Name;
        }

        public void setPL_Line_Gen13_Name(String PL_Line_Gen13_Name) {
            this.PL_Line_Gen13_Name = PL_Line_Gen13_Name;
        }

        public String getPL_Line_Gen13_Sortierung() {
            return PL_Line_Gen13_Sortierung;
        }

        public void setPL_Line_Gen13_Sortierung(String PL_Line_Gen13_Sortierung) {
            this.PL_Line_Gen13_Sortierung = PL_Line_Gen13_Sortierung;
        }

        public String getPL_Line_Gen14() {
            return PL_Line_Gen14;
        }

        public void setPL_Line_Gen14(String PL_Line_Gen14) {
            this.PL_Line_Gen14 = PL_Line_Gen14;
        }

        public String getPL_Line_Gen14_Name() {
            return PL_Line_Gen14_Name;
        }

        public void setPL_Line_Gen14_Name(String PL_Line_Gen14_Name) {
            this.PL_Line_Gen14_Name = PL_Line_Gen14_Name;
        }

        public String getPL_Line_Gen14_Sortierung() {
            return PL_Line_Gen14_Sortierung;
        }

        public void setPL_Line_Gen14_Sortierung(String PL_Line_Gen14_Sortierung) {
            this.PL_Line_Gen14_Sortierung = PL_Line_Gen14_Sortierung;
        }

        public String getPL_Line_Gen15() {
            return PL_Line_Gen15;
        }

        public void setPL_Line_Gen15(String PL_Line_Gen15) {
            this.PL_Line_Gen15 = PL_Line_Gen15;
        }

        public String getPL_Line_Gen15_Name() {
            return PL_Line_Gen15_Name;
        }

        public void setPL_Line_Gen15_Name(String PL_Line_Gen15_Name) {
            this.PL_Line_Gen15_Name = PL_Line_Gen15_Name;
        }

        public String getPL_Line_Gen15_Sortierung() {
            return PL_Line_Gen15_Sortierung;
        }

        public void setPL_Line_Gen15_Sortierung(String PL_Line_Gen15_Sortierung) {
            this.PL_Line_Gen15_Sortierung = PL_Line_Gen15_Sortierung;
        }
    }


}
