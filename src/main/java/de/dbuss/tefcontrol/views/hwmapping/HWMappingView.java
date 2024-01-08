package de.dbuss.tefcontrol.views.hwmapping;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import de.dbuss.tefcontrol.data.entity.CLTV_HW_Measures;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectConnection;
import de.dbuss.tefcontrol.data.service.CLTV_HW_MeasureService;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.vaadin.addons.componentfactory.PivotTable;

import javax.sql.DataSource;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@PageTitle("HW Mapping")
@Route(value = "HWMapping/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "MAPPING"})
public class HWMappingView extends VerticalLayout {
    private final CLTV_HW_MeasureService cltvHwMeasureService;
    private final ProjectConnectionService projectConnectionService;
    private List<CLTV_HW_Measures> listOfCLTVMeasures;
    private Crud<CLTV_HW_Measures> crud;
    private Grid<CLTV_HW_Measures> grid;
    MemoryBuffer memoryBuffer = new MemoryBuffer();
    Upload singleFileUpload = new Upload(memoryBuffer);
    long contentLength = 0;
    String mimeType = "";
    private String selectedDbName;
    Button addRowsBT = new Button("Add Rows");
    Button replaceRowsBT = new Button("Replace Rows");
    Div textArea = new Div();
    ProgressBar spinner = new ProgressBar();
    Article article = new Article();
    private Button exportButton = new Button("Export");
    private String exportPath;

    Checkbox checkbox;
    HorizontalLayout hl;
    List<CLTV_HW_Measures> fetchListOfCLTVMeasures;
    private Anchor anchor = new Anchor(getStreamResource("CLTV_HW_Mapping.xls", "default content"), "click to download");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public HWMappingView(@Value("${csv_exportPath}") String p_exportPath, CLTV_HW_MeasureService cltvHwMeasureService, ProjectConnectionService projectConnectionService, JdbcTemplate jdbcTemplate) {

        this.exportPath = p_exportPath;
        this.cltvHwMeasureService = cltvHwMeasureService;
        this.projectConnectionService = projectConnectionService;

        crud = new Crud<>(CLTV_HW_Measures.class, createEditor());
        listOfCLTVMeasures = new ArrayList<CLTV_HW_Measures>();

        Button downloadButton = new Button("Download");
        Button uploadButton = new Button("Save");

        setupGrid();
        setupUploader();
        setUpExportButton();

        crud.setHeight("600px");

        HorizontalLayout horl = new HorizontalLayout();
        horl.setWidthFull();

        VerticalLayout verl = new VerticalLayout();
        verl.add(addRowsBT, replaceRowsBT, spinner);

        ComboBox<String> databaseConnectionCB = new ComboBox<>();
        databaseConnectionCB.setAllowCustomValue(true);

        List<ProjectConnection> listOfProjectConnections = projectConnectionService.findAll();
        if (listOfProjectConnections.isEmpty()) {
            Notification.show("Project Connections is empty in database", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        } else {
            List<String> connectionNames = listOfProjectConnections.stream()
                    .flatMap(connection -> {
                        String category = connection.getCategory();
                        if (category == null) {
                            return Stream.of(connection.getName());
                        }
                        return Stream.empty();
                    })
                    .collect(Collectors.toList());

            if (connectionNames.isEmpty()) {
                Notification.show("Connections not found in database", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            } else {
                databaseConnectionCB.setItems(connectionNames);
                databaseConnectionCB.setValue(connectionNames.get(0));
                selectedDbName = connectionNames.get(0);
            }
        }

        horl.add(databaseConnectionCB, downloadButton, uploadButton, singleFileUpload, verl, exportButton, anchor);
        horl.setAlignItems(Alignment.CENTER);

        Icon icon = VaadinIcon.EXCLAMATION_CIRCLE.create();
        icon.getStyle().set("width", "var(--lumo-icon-size-s)");
        icon.getStyle().set("height", "var(--lumo-icon-size-s)");

        //     detailsText.setWidthFull();
        //     detailsText.setHeight("300px");
        //     details = new Details("Details",detailsText);
        //     details.setOpened(false);
        //     details.setWidthFull();
        addRowsBT.setEnabled(false);
        replaceRowsBT.setEnabled(false);

        addRowsBT.setWidth("120px");
        addRowsBT.setHeight("20px");
        replaceRowsBT.setWidth("120px");
        replaceRowsBT.setHeight("20px");
        addRowsBT.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        replaceRowsBT.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        databaseConnectionCB.addValueChangeListener(event -> {
            log.info("executing databaseConnectionCB.addValueChangeListener for database selection "+ event.getValue());
            selectedDbName = event.getValue();
        });

        downloadButton.addClickListener(clickEvent -> {
            // Get the selected database connection from the ComboBox
            log.info("executing downloadButton.addClickListener for fetch data from DB");
            if (selectedDbName == null || selectedDbName.isEmpty()) {
                Notification notification = Notification.show("Please select a database connection", 3000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } else {

                DataSource dataSource = projectConnectionService.getDataSource(selectedDbName);

                try {
                    // Perform fetch operations using the selected data source

                    fetchListOfCLTVMeasures = projectConnectionService.getCLTVHWMeasuresData(selectedDbName);
                    GenericDataProvider dataProvider = new GenericDataProvider(fetchListOfCLTVMeasures);

                    crud.setDataProvider(dataProvider);
                    setupDataProviderEvent();

                    Notification notification = Notification.show(" Rows fetch successfully", 3000, Notification.Position.MIDDLE);
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);


                } catch (Exception e) {
                    Notification notification = Notification.show("Error during fetch: " + e.getMessage(), 4000, Notification.Position.MIDDLE);
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });

        uploadButton.addClickListener(clickEvent -> {
            log.info("executing uploadButton.addClickListener for Save data in DB");
            List<CLTV_HW_Measures>allItems = getDataProviderAllItems();

            DataSource dataSource = projectConnectionService.getDataSource(selectedDbName);

            Notification notification = Notification.show(allItems.size() + " Rows Uploaded start",2000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            String result = projectConnectionService.write2DB(allItems, selectedDbName);
            if (result.equals(Constants.OK)){
                notification = Notification.show(allItems.size() + " Rows Uploaded successfully",3000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            else
            {
                notification = Notification.show("Error during upload!",4000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        addRowsBT.addClickListener(clickEvent -> {
            log.info("executing addRowsBT.addClickListener for Append data in Crud Grid");
            List<CLTV_HW_Measures> oldListOfCLTVMeasures = getDataProviderAllItems();

            if (listOfCLTVMeasures != null && !listOfCLTVMeasures.isEmpty()) {
                List<CLTV_HW_Measures> uniqueItems = listOfCLTVMeasures.stream()
                        .filter(newItem -> oldListOfCLTVMeasures.stream()
                                .noneMatch(existingItem -> existingItem.getId().equals(newItem.getId())))
                        .collect(Collectors.toList());

                oldListOfCLTVMeasures.addAll(uniqueItems);
                GenericDataProvider dataProvider = new GenericDataProvider(oldListOfCLTVMeasures);
                crud.setDataProvider(dataProvider);

                Notification notification = Notification.show("New data was appended with grid data successfully.", 4000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification notification = Notification.show("No data to display.", 4000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            replaceRowsBT.setEnabled(false);
            addRowsBT.setEnabled(false);
            //singleFileUpload.clearFileList();
        });

        replaceRowsBT.addClickListener(clickEvent -> {
            log.info("executing replaceRowsBT.addClickListener for Replace data in Crud Grid");
            if (listOfCLTVMeasures != null && !listOfCLTVMeasures.isEmpty()) {
                GenericDataProvider dataProvider = new GenericDataProvider(listOfCLTVMeasures);
                crud.setDataProvider(dataProvider);
                Notification notification = Notification.show("New data was replaced with grid data successfully.", 4000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification notification = Notification.show("No data to display.", 4000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            addRowsBT.setEnabled(false);
            replaceRowsBT.setEnabled(false);
        });

        spinner.setIndeterminate(true);
        spinner.setVisible(false);

        Details details = new Details("details", textArea);
        details.setOpened(false);

        checkbox = new Checkbox();
        checkbox.setLabel("Show Pivot");
        checkbox.addClickListener(e->{showPivot();});

        add(horl, details ,crud,checkbox);


        // Damit Gesamtsummen (Totals) nicht angezeigt werden in styles.css einfügen:
        //  .pvtTotal, .pvtTotalLabel, .pvtGrandTotal {display: none}


    }


    private void showPivot()
    {
        if(!checkbox.getValue())
        {
            remove(hl);
            return;
        }

        PivotTable.PivotData pivotData = new PivotTable.PivotData();
        pivotData.addColumn("Monat", String.class);
        pivotData.addColumn("Device", String.class);
        pivotData.addColumn("Measure", String.class);
        pivotData.addColumn("Channel", String.class);
        pivotData.addColumn("Wert", Double.class);
        // pivotData.addColumn("filled", Boolean.class);


        PivotTable.PivotOptions pivotOptions = new PivotTable.PivotOptions();
        pivotOptions.setCols("Monat");
        pivotOptions.setRows("Device","Measure","Channel");
        pivotOptions.setAggregator("Sum","Wert");


        fetchListOfCLTVMeasures.forEach(e->{
            System.out.println(e.getMonat_ID() + "|" + e.getDevice() + "|" + e.getMeasure_Name() + "|" +e.getChannel() + "|" + e.getValue());

            pivotData.addRow(e.getMonat_ID().toString(),e.getDevice(), e.getMeasure_Name(),e.getChannel(), Double.parseDouble(e.getValue()) );

        });


        //PivotTable table = new PivotTable(pivotData, pivotOptions, PivotTable.PivotMode.INTERACTIVE);
        PivotTable pivotTable = new PivotTable(pivotData, pivotOptions, PivotTable.PivotMode.NONINTERACTIVE);

        hl =new HorizontalLayout();
        hl.add(pivotTable);
        add(hl);

        // Damit Gesamtsummen (Totals) nicht angezeigt werden in styles.css einfügen:
        //  .pvtTotal, .pvtTotalLabel, .pvtGrandTotal {display: none}

    }

    public StreamResource getStreamResource(String filename, String content) {
        log.info("executing getStreamResource() for file");
        return new StreamResource(filename,
                () -> new ByteArrayInputStream(content.getBytes()));
    }

    private void setUpExportButton() {
        log.info("Starting setUpExportButton() prepare excel file for export");
        String exportFileName = "HW_Mapping.xls";
        exportButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        exportButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        exportButton.addClickListener(clickEvent -> {
            System.out.println("export..click.....");
            Notification.show("Exportiere Daten ");
            try {
                generateAndExportExcel(exportPath + exportFileName);

                File file = new File(exportPath + exportFileName);
                StreamResource streamResource = new StreamResource(file.getName(), () -> getStream(file));

                anchor.setHref(streamResource);
                anchor.setEnabled(true);
                exportButton.setEnabled(false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        anchor.getElement().setAttribute("download", true);
        anchor.setEnabled(false);
        log.info("Ending setUpExportButton() prepare excel file for export");
    }
    public void generateAndExportExcel(String file) {
        log.info("Starting generateAndExportExcel() generate excel file");
        try {
            // List<CLTV_HW_Measures> dataToExport = projectConnectionService.fetchDataFromDatabase(selectedDbName);
            List<CLTV_HW_Measures> dataToExport = getDataProviderAllItems();

            // Create a new Excel workbook and sheet using Apache POI
            Workbook workBook = new HSSFWorkbook();
            Sheet sheet1 = workBook.createSheet("Sheet1");

            // Create CellStyle for headers
            CellStyle cs = workBook.createCellStyle();
            Font f = workBook.createFont();
            f.setBold(true);
            f.setFontHeightInPoints((short) 12);
            cs.setFont(f);

            // Create headers in the first row
            Row headerRow = sheet1.createRow(0);
            String[] headers = {"id", "monat_id", "device", "measure_name", "channel", "value"};
            for (int i = 0; i < headers.length; i++) {
                Cell headerCell = headerRow.createCell(i);
                headerCell.setCellValue(headers[i]);
                headerCell.setCellStyle(cs);
            }

            // Fill data in subsequent rows
            int rowIndex = 1;
            for (CLTV_HW_Measures measure : dataToExport) {
                Row dataRow = sheet1.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(measure.getId());
                dataRow.createCell(1).setCellValue(measure.getMonat_ID());
                dataRow.createCell(2).setCellValue(measure.getDevice());
                dataRow.createCell(3).setCellValue(measure.getMeasure_Name());
                dataRow.createCell(4).setCellValue(measure.getChannel());
                dataRow.createCell(5).setCellValue(measure.getValue());
            }

            // Write the workbook to the specified file
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workBook.write(fileOut);
            }

            // Close the workbook
            workBook.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("Ending generateAndExportExcel() generate excel file");
    }

    private InputStream getStream(File file) {
        log.info("Starting getStream() for excel file");
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        log.info("Starting getStream() for excel file");
        return stream;
    }

    private Double checkCellDouble(Cell cell, Integer zeile, String spalte) {
        log.info("Executing checkCellDouble() for excel file");
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
    }

    private String checkCellString(Cell cell, Integer zeile, String spalte) {
        log.info("Executing checkCellString() for excel file");
        try{

            if (cell.getCellType()!=Cell.CELL_TYPE_STRING && !cell.getStringCellValue().isEmpty())
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

            }
        }
        catch(Exception e) {
            System.out.println("Exception" + e.getMessage());
            //detailsText.setValue(detailsText.getValue() + "\nZeile " + zeile.toString() + ", Spalte " + spalte + "  konnte nicht gelesen werden, da ExcelTyp Numeric!");
            article.add("\nZeile " + zeile.toString() + ", Spalte " + spalte + "  konnte nicht gelesen werden, da ExcelTyp Numeric!");
            textArea.add(article);
            return "";
        }
    }

    private Integer checkCellNumeric(Cell cell, Integer zeile, String spalte) {
        log.info("Executing checkCellNumeric() for excel file");
        if (cell.getCellType()!=Cell.CELL_TYPE_NUMERIC)
        {
            System.out.println("Zeile " + zeile.toString() + ", Spalte " + spalte + " konnte nicht gelesen werden, da ExcelTyp nicht numerisch!");
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

    }



    private void setupUploader() {
        log.info("Starting setupUploader() for upload excel file");
        singleFileUpload.setWidth("600px");
        singleFileUpload.addSucceededListener(event -> {
            // Get information about the uploaded file
            InputStream fileData = memoryBuffer.getInputStream();
            String fileName = event.getFileName();
            contentLength = event.getContentLength();
            mimeType = event.getMIMEType();
            addRowsBT.setEnabled(true);
            replaceRowsBT.setEnabled(true);
            textArea.setText("Warten auf Button \"Hochladen\"");

            listOfCLTVMeasures = parseExcelFile(fileData, fileName);
        });
        log.info("Ending setupUploader() for upload excel file");
    }

    public List<CLTV_HW_Measures> parseExcelFile(InputStream fileData, String fileName) {
        log.info("Starting parseExcelFile() for parse excel file");
        List<CLTV_HW_Measures> listOfCLTVMeasures = new ArrayList<>();
        try {
            if(fileName.isEmpty() || fileName.length()==0)
            {
                article=new Article();
                article.setText(LocalDateTime.now().format(formatter) + ": Error: Keine Datei angegeben!");
                textArea.add(article);
            }

            if(!mimeType.contains("application/vnd.ms-excel"))
            {
                article=new Article();
                article.setText(LocalDateTime.now().format(formatter) + ": Error: ungültiges Dateiformat!");
                textArea.add(article);
            }

            System.out.println("Excel import: "+  fileName + " => Mime-Type: " + mimeType  + " Größe " + contentLength + " Byte");
            textArea.setText(LocalDateTime.now().format(formatter) + ": Info: Verarbeite Datei: " + fileName + " (" + contentLength + " Byte)");


            //addRowsBT.setEnabled(false);
            //replaceRowsBT.setEnabled(false);
            //spinner.setVisible(true);

            HSSFWorkbook my_xls_workbook = new HSSFWorkbook(fileData);
            HSSFSheet my_worksheet = my_xls_workbook.getSheetAt(0);
            Iterator<Row> rowIterator = my_worksheet.iterator();

            Integer RowNumber=0;
            Boolean isError=false;

            while(rowIterator.hasNext() && !isError)
            {
                CLTV_HW_Measures elaFavoriten = new CLTV_HW_Measures();
                Row row = rowIterator.next();
                RowNumber++;

                Iterator<Cell> cellIterator = row.cellIterator();
                while(cellIterator.hasNext()) {

                    if(RowNumber==1) //Überschrift nicht betrachten
                    {
                        break;
                    }

                    Cell cell = cellIterator.next();

                    try {
                        elaFavoriten.setId(checkCellNumeric(cell, RowNumber,"ID"));
                    }
                    catch(Exception e)
                    {
                        article=new Article();
                        article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte ID nicht vorhanden!");
                        textArea.add(article);
                        isError=true;
                        break;
                    }

                    try {
                        cell = cellIterator.next();
                        elaFavoriten.setMonat_ID(checkCellNumeric(cell, RowNumber,"Monat_ID"));
                    }
                    catch(Exception e)
                    {
                        article=new Article();
                        article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte Monat_ID nicht vorhanden!");
                        textArea.add(article);
                        isError=true;
                        break;
                    }

                    try {
                        cell = cellIterator.next();
                        elaFavoriten.setDevice(checkCellString(cell, RowNumber,"Device"));
                    }
                    catch(Exception e)
                    {
                        article=new Article();
                        article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte Device nicht vorhanden! " + e.getMessage());
                        textArea.add(article);
                        isError=true;
                        break;
                    }

                    try {
                        cell = cellIterator.next();
                        elaFavoriten.setMeasure_Name(checkCellString(cell, RowNumber,"Measure_Name"));
                    }
                    catch(Exception e)
                    {
                        article=new Article();
                        article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte Measure_Name nicht vorhanden!");
                        textArea.add(article);
                        isError=true;
                        break;
                    }



                    try {
                        cell = cellIterator.next();
                        elaFavoriten.setChannel(checkCellString(cell, RowNumber,"Channel"));
                    }
                    catch(Exception e)
                    {
                        article=new Article();
                        article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte Channel nicht vorhanden!");
                        textArea.add(article);
                        isError=true;
                        break;
                    }


                    try {
                        cell = cellIterator.next();
                        elaFavoriten.setValue(checkCellNumeric(cell, RowNumber,"Value").toString());
                    }
                    catch(Exception e)
                    {
                        article=new Article();
                        article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte value nicht vorhanden!");
                        textArea.add(article);
                        isError=true;
                        break;
                    }



                    listOfCLTVMeasures.add(elaFavoriten);
                    System.out.println(listOfCLTVMeasures.size()+".............parse");
                }

            }

            if(isError)
            {
                //    button.setEnabled(true);
                spinner.setVisible(false);
                fileName="";
            }
            log.info("Ending parseExcelFile() for parse excel file");
            article=new Article();
            article.getStyle().set("white-space","pre-line");
            article.add(LocalDateTime.now().format(formatter) + ": Info: Anzahl Zeilen: " + listOfCLTVMeasures.size());
            article.add("\n");
            article.add(LocalDateTime.now().format(formatter) + ": Info: Start Upload to DB");
            textArea.add(article);

            System.out.println("Anzahl Zeilen im Excel: " + listOfCLTVMeasures.size());

            return listOfCLTVMeasures;
        } catch (Exception e) {
            log.error("error...Ending parseExcelFile() for parse excel file");
            e.printStackTrace();
            return null;
        }

    }

    private CrudEditor<CLTV_HW_Measures> createEditor() {
        log.info("Starting createEditor() for crude editor");
        IntegerField id = new IntegerField  ("Id");
        TextField monat_ID = new TextField ("Monat_ID");
        TextField device = new TextField("Device");
        TextField measure_name = new TextField("Measure Name");
        TextField channel = new TextField("Channel");
        TextField value = new TextField("Value");

        FormLayout editForm = new FormLayout(id, monat_ID, device, measure_name, channel, value);

        Binder<CLTV_HW_Measures> binder = new Binder<>(CLTV_HW_Measures.class);
        binder.forField(monat_ID).withNullRepresentation("202301").withConverter(new StringToIntegerConverter("Not a Number")).asRequired().bind(CLTV_HW_Measures::getMonat_ID, CLTV_HW_Measures::setMonat_ID);
        //  binder.forField(monat_ID).asRequired().bind(CLTV_HW_Measures::getMonat_ID, CLTV_HW_Measures::setMonat_ID);
        binder.forField(measure_name).asRequired().bind(CLTV_HW_Measures::getMeasure_Name,
                CLTV_HW_Measures::setMeasure_Name);
        binder.forField(device).asRequired().bind(CLTV_HW_Measures::getDevice,
                CLTV_HW_Measures::setDevice);
        binder.forField(channel).asRequired().bind(CLTV_HW_Measures::getChannel,
                CLTV_HW_Measures::setChannel);
        binder.forField(value).asRequired().bind(CLTV_HW_Measures::getValue,
                CLTV_HW_Measures::setValue);
        binder.forField(id).asRequired().bind(CLTV_HW_Measures::getId,
                CLTV_HW_Measures::setId);
        log.info("Ending createEditor() for crude editor");
        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupGrid() {
        log.info("Starting setupGrid() for crude editor");
        String ID = "Id";

        String MONAT_ID = "monat_ID";

        String DEVICE = "device";

        String MEASURE_NAME = "measure_Name";

        String CHANNEL = "channel";

        String VALUE = "value";
        String EDIT_COLUMN = "vaadin-crud-edit-column";
        grid = crud.getGrid();

        grid.getColumnByKey("id").setHeader("ID").setWidth("10px");

        // Reorder the columns (alphabetical by default)
        grid.setColumnOrder( grid.getColumnByKey("id"), grid.getColumnByKey(MONAT_ID), grid.getColumnByKey(DEVICE), grid.getColumnByKey(MEASURE_NAME), grid.getColumnByKey(CHANNEL)
                , grid.getColumnByKey(VALUE)
                , grid.getColumnByKey(EDIT_COLUMN));

        log.info("Ending setupGrid() for crude editor");
    }

    private void setupDataProviderEvent() {
        log.info("Starting setupDataProviderEvent() for crude editor save, delete events");
        GenericDataProvider dataProvider = new GenericDataProvider(getDataProviderAllItems());

        article=new Article();
        article.setText(LocalDateTime.now().format(formatter) + ": Info: Download from Database");
        textArea.add(article);

        crud.addDeleteListener(
                deleteEvent -> {dataProvider.delete(deleteEvent.getItem());
                    log.info("Executing crud.addDeleteListener for crude editor delete events");
                    crud.setDataProvider(dataProvider);
                });
        crud.addSaveListener(
                saveEvent -> {
                    dataProvider.persist(saveEvent.getItem());
                    log.info("Executing crud.addDeleteListener for crude editor save events");
                    crud.setDataProvider(dataProvider);
                });
        log.info("Ending setupDataProviderEvent() for crude editor save, delete events");
    }

    private List<CLTV_HW_Measures> getDataProviderAllItems() {
        log.info("Starting getDataProviderAllItems for grid dataprovider list");
        DataProvider<CLTV_HW_Measures, Void> existDataProvider = (DataProvider<CLTV_HW_Measures, Void>) grid.getDataProvider();
        List<CLTV_HW_Measures> listOfCLTVMeasures = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        log.info("Ending getDataProviderAllItems for grid dataprovider list");
        return listOfCLTVMeasures;
    }
}
