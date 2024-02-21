package de.dbuss.tefcontrol.data.modules.kpi;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.*;
import de.dbuss.tefcontrol.components.DefaultUtils;
import de.dbuss.tefcontrol.components.LogView;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.dto.ProjectAttachmentsDTO;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.service.ProjectAttachmentsService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.data.service.ProjectsService;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Strategic KPI | TEF-Control")
@Route(value = "Strategic_KPI/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "FLIP"})
public class Strategic_KPIView extends VerticalLayout implements BeforeEnterObserver {

    private LogView logView;
    private int projectId;
    private Optional<Projects> projects;
    private List<ProjectAttachmentsDTO> listOfProjectAttachments;
    private Boolean isLogsVisible = false;
    private Boolean isVisible = false;
    Integer errors_Count=0;
    InputStream fileData_Fact;
    InputStream  fileData_Dim;
    String fileName = "";
    long contentLength = 0;
    String mimeType = "";
    Integer errors_Fact=0;
    Grid<ProjectParameter> parameterGrid = new Grid<>(ProjectParameter.class, false);

    private final ProjectsService projectsService;
    private final ProjectAttachmentsService projectAttachmentsService;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String agentName;
    MemoryBuffer memoryBuffer = new MemoryBuffer();
    Upload singleFileUpload = new Upload(memoryBuffer);

    private Button uploadBtn;
    private DefaultUtils defaultUtils;

    private List<Fact_CC_KPI> listOfFact_CC_KPI = new ArrayList<Fact_CC_KPI>();
    public Strategic_KPIView(ProjectParameterService projectParameterService, ProjectsService projectsService, ProjectAttachmentsService projectAttachmentsService) {

        this.projectsService = projectsService;
        this.projectAttachmentsService = projectAttachmentsService;


        logView = new LogView();
        logView.logMessage(Constants.INFO, "Starting Strategic_KPIView");

        uploadBtn = new Button("Upload");
        uploadBtn.setEnabled(false);

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
            } else if (Constants.DB_JOBS.equals(projectParameter.getName())) {
                agentName = projectParameter.getValue();
            }
            // }
        }
        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";

        setProjectParameterGrid(filteredProjectParameters);
        defaultUtils = new DefaultUtils(projectsService, projectAttachmentsService);

        HorizontalLayout hl = new HorizontalLayout();
        hl.add(getTabsheet());
        hl.setHeightFull();
        hl.setSizeFull();

        setHeightFull();
        setSizeFull();
        getStyle().set("overflow", "auto");
        add(hl,parameterGrid);

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

        HorizontalLayout hl=new HorizontalLayout(singleFileUpload, uploadBtn);
        content.add(hl, parameterGrid);



        content.setSizeFull();
        content.setHeightFull();
        content.getStyle().set("overflow", "auto");


        return content;
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
            // Get information about the uploaded file
            fileData_Fact = memoryBuffer.getInputStream();
            fileData_Dim = memoryBuffer.getInputStream();

            fileName = event.getFileName();
            contentLength = event.getContentLength();
            mimeType = event.getMIMEType();

            logView.logMessage(Constants.INFO, "contentLenght: >" + contentLength + "<");
            logView.logMessage(Constants.INFO, "mimeType: >" + mimeType + "<");

            singleFileUpload.clearFileList();

            listOfFact_CC_KPI = parseExcelFile_Fact(fileData_Fact, fileName,"Fact_CC_KPI");

//            factInfo="Fact (" + listOfKPI_Fact.size() + " rows)";
//            actualsInfo="Fact (" + listOfKPI_Actuals.size() + " rows)";

            if (errors_Count==0)
            {
                uploadBtn.setEnabled(true);
//                qsBtn.setEnabled(false);
            }


        });
        System.out.println("setup uploader................over");  logView.logMessage(Constants.INFO, "Ending setupUploader() for setup file uploader");
    }

    public List<Fact_CC_KPI> parseExcelFile_Fact(InputStream fileData, String fileName, String sheetName) {
        logView.logMessage(Constants.INFO, "Starting parseExcelFile_Fact() for parse uploaded file");

        List<Fact_CC_KPI> listOfKPI_Fact = new ArrayList<>();
        try {
            if(fileName.isEmpty() || fileName.length()==0)
            {
              //  article=new Article();
              //  article.setText(LocalDateTime.now().format(formatter) + ": Error: Keine Datei angegeben!");
              //  textArea.add(article);
            }

            if(!mimeType.contains("openxmlformats-officedocument"))
            {
              //  article=new Article();
              //  article.setText(LocalDateTime.now().format(formatter) + ": Error: ungültiges Dateiformat!");
              //  textArea.add(article);
            }

            System.out.println("Excel import: "+  fileName + " => Mime-Type: " + mimeType  + " Größe " + contentLength + " Byte");
            //textArea.setText(LocalDateTime.now().format(formatter) + ": Info: Verarbeite Datei: " + fileName + " (" + contentLength + " Byte)");
            //message.setText(LocalDateTime.now().format(formatter) + ": Info: reading file: " + fileName);

            //addRowsBT.setEnabled(false);
            //replaceRowsBT.setEnabled(false);
            //spinner.setVisible(true);

            //  HSSFWorkbook my_xls_workbook = new HSSFWorkbook(fileData);
            XSSFWorkbook my_xls_workbook = new XSSFWorkbook(fileData);
            //   HSSFSheet my_worksheet = my_xls_workbook.getSheetAt(0);
            XSSFSheet my_worksheet = my_xls_workbook.getSheet("Fact_CC_KPI");
            Iterator<Row> rowIterator = my_worksheet.iterator();

            Integer RowNumber=0;
            errors_Fact=0;

            while(rowIterator.hasNext() )
            {
                Fact_CC_KPI kPI_Fact = new Fact_CC_KPI();
                Row row = rowIterator.next();
                RowNumber++;

                if (errors_Fact>0){ break; } //Wenn bereits Fehler aufgetreten ist, beenden.



                Iterator<Cell> cellIterator = row.cellIterator();



                while(cellIterator.hasNext()) {

                    if(RowNumber==1 ) //Überschrift nicht betrachten, aber Anzahl Spalten kontrollieren
                    {


                        if (row.getLastCellNum()<5)
                        {
                         //   article=new Article();
                         //   article.setText(LocalDateTime.now().format(formatter) + ": Error: Count Columns: " + row.getLastCellNum() + " Expected: 5! (NT ID | XTD | Scenario_Name | Date | Amount)");
                         //   textArea.add(article);
                            errors_Fact=1;
                        }

                        break;
                    }


                    Cell cell = cellIterator.next();
                    kPI_Fact.setRow(RowNumber);

                    if(cell.getColumnIndex()==0)
                    {
                        String ColumnName="Period";
                        try {
                            kPI_Fact.setPeriod(checkCellNumeric(sheetName, cell, RowNumber,ColumnName));
                        }
                        catch(Exception e)
                        {
                          //  article=new Article();
                          //  article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                          //  textArea.add(article);
                            errors_Fact++;

                        }
                    }

                    if(cell.getColumnIndex()==1)
                    {
                        String ColumnName="Scenario";
                        try {
                            kPI_Fact.setScenario(checkCellString(sheetName, cell, RowNumber,ColumnName));
                        }
                        catch(Exception e)
                        {
                           // article=new Article();
                           // article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                           // textArea.add(article);
                            errors_Fact++;

                        }
                    }
                    if(cell.getColumnIndex()==2)
                    {
                        String ColumnName="Segment";
                        try {
                            kPI_Fact.setSegment(checkCellString(sheetName, cell, RowNumber,ColumnName));
                        }
                        catch(Exception e)
                        {
                            //article=new Article();
                            //article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                            //textArea.add(article);
                            errors_Fact++;

                        }
                    }

                    if(cell.getColumnIndex()==3)
                    {
                        String ColumnName="CC_KPI";
                        try {
                            kPI_Fact.setCC_KPI(checkCellString(sheetName, cell, RowNumber,ColumnName));
                        }
                        catch(Exception e)
                        {
                          //  article=new Article();
                          //  article.setText(LocalDateTime.now().format(formatter) + " " + sheetName + ": Error: Zeile " + RowNumber.toString() + ", Spalte " + ColumnName + ": " + e.getMessage());
                          //  textArea.add(article);
                            errors_Fact++;

                        }
                    }

                    if(cell.getColumnIndex()==4)
                    {
                        String ColumnName="Amount";
                        try {
                            kPI_Fact.setAmount(checkCellDouble(sheetName, cell, RowNumber,ColumnName));
                        }

                        catch(Exception e)
                        {
                          //  article=new Article();
                          //  article.setText(sheetName + ": Error: row " + RowNumber.toString() + ", column >" + ColumnName + "<  " + e.getMessage());
                          //  textArea.add(article);
                            errors_Fact++;

                        }
                    }

                }

                if(kPI_Fact.isValid() )
                {
                    listOfKPI_Fact.add(kPI_Fact);
                }
                else
                {
                    System.out.println("Fact: skip empty row : " + kPI_Fact.getRow());
                }


            }

     //       article=new Article();
     //       article.getStyle().set("white-space","pre-line");
     //       article.add("\n");
            //article.add(LocalDateTime.now().format(formatter) + " ==> " + sheetName + ": Count Rows: " + listOfKPI_Fact.size() + " Count Errrors: " + errors_Fact);
     //       article.add("==> Summary: " + sheetName + ": Count Rows: " + listOfKPI_Fact.size() + " Count Errrors: " + errors_Fact);
     //       article.add("\n");
     //       textArea.add(article);

     //       planInfo = "KPI Plan 0 rows";

            System.out.println("Anzahl Zeilen im Excel: " + listOfKPI_Fact.size());
            //      accordion.remove(factPanel);
            //       factPanel = new AccordionPanel( "KPI_Fact (" + listOfKPI_Fact.size()+ " rows)", gridFact);
            //       accordion.add(factPanel);

            errors_Count+=errors_Fact;
            logView.logMessage(Constants.INFO, "Ending parseExcelFile_Fact() for parse uploaded file");
            return listOfKPI_Fact;

        } catch (Exception e) {
            logView.logMessage(Constants.ERROR, "Error while parse uploaded file");
            e.printStackTrace();
            return null;
        }

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
        //    article.add("\n" + sheetName + " Error: row  >" + zeile.toString() + "<, column >" + spalte + "< konnte in checkCellString nicht aufgelöst werden. Typ=" + cell.getCellType());
        //    textArea.add(article);

        }
        catch(Exception e){
            switch (e.getMessage()) {
                case "Cannot get a text value from a error formula cell":

          //          article = new Article();
          //          article.setText("\n" + sheetName + ": Info: row >" + zeile.toString() + "<, column >" + spalte + "<  formula cell error (replaced to empty string)");
          //          textArea.add(article);

                    return "";

            }
            //System.out.println("Zeile " + zeile.toString() + ", Spalte " + spalte + " konnte in checkCellString nicht aufgelöst werden. Typ=" + cell.getCellType() + e.getMessage());
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
        //    article.add("\nZeile " + zeile.toString() + ", Spalte " + spalte + "  konnte nicht gelesen werden, da ExcelTyp Numeric!");
        //    textArea.add(article);
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

    private Double checkCellDouble(String sheetName, Cell cell, Integer zeile, String spalte)  {

        Double wert;

        wert = (double) cell.getNumericCellValue();
        return  wert;

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

    public class Fact_CC_KPI {

        private int row;

        private int Period ;

        private String Scenario = "";
        private String Segment = "";
        private String CC_KPI = "";

        private java.util.Date Date;

        private Double Amount;

        public int getPeriod(){return Period;};
        public void setPeriod(int period){this.Period=period;}
        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public String getSegment() {
            return Segment;
        }

        public void setSegment(String NT_ID) {
            this.Segment = Segment;
        }

        public String getCC_KPI() {
            return CC_KPI;
        }

        public void setCC_KPI(String CC_KPI) {
            this.CC_KPI = CC_KPI;
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

        public Double getAmount() {return Amount;}

        public void setAmount(Double amount) {
            Amount = amount;
        }

        public boolean isValid() {
            if (Segment == null || Segment.isEmpty()) {
                if (Scenario == null || Scenario.isEmpty()) {
                    if (CC_KPI == null || CC_KPI.isEmpty()) {
                        return false;
                    }
                }
            }
            return true;
        }

    }


}
