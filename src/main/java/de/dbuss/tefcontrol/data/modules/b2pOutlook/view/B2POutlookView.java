package de.dbuss.tefcontrol.data.modules.b2pOutlook.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.dbuss.tefcontrol.data.Role;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.entity.User;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.entity.OutlookMGSR;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.Financials;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.Subscriber;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.UnitsDeepDive;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@PageTitle("PowerBI Comments")
@Route(value = "B2P_Outlook_Excel", layout = MainLayout.class)
@RolesAllowed({"MAPPING", "ADMIN"})
public class B2POutlookView extends VerticalLayout {
    private MemoryBuffer memoryBuffer = new MemoryBuffer();
    private Upload singleFileUpload = new Upload(memoryBuffer);
    private HashMap<String, List<OutlookMGSR>> listOfAllSheets = new HashMap<>();
    private Crud<OutlookMGSR> crudMGSR;
    private Grid<OutlookMGSR> gridMGSR = new Grid<>(OutlookMGSR.class, false);
    private Button saveButton;
    private String selectedDbName;
    private String tableName;
    private long contentLength = 0;
    private String mimeType = "";
    private Div textArea = new Div();

    public B2POutlookView (ProjectParameterService projectParameterService) {

        saveButton = new Button(Constants.SAVE);
        saveButton.setEnabled(false);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        String dbServer = null;

        for (ProjectParameter projectParameter : listOfProjectParameters) {
            if(projectParameter.getNamespace().equals(Constants.B2P_OUTLOOK)) {
                if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                    dbServer = projectParameter.getValue();
                } else if (Constants.DB_NAME.equals(projectParameter.getName())) {
                    selectedDbName = projectParameter.getValue();
                } else if (Constants.TABLE.equals(projectParameter.getName())) {
                    tableName = projectParameter.getValue();
                }
            }
        }

        HorizontalLayout hl = new HorizontalLayout();
        hl.setAlignItems(Alignment.BASELINE);
        hl.add(singleFileUpload,saveButton);
        add(hl);

        setupUploader();
        add(getTabsheet());
    }
    private TabSheet getTabsheet() {
        boolean isAdmin=false;

        Component getMGSR = getMGSRGrid();

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("MGSR", getMGSR);
        tabSheet.add("all", new Div(new Text("new data import")));


        tabSheet.setSizeFull();
        tabSheet.setHeightFull();
        tabSheet.addThemeVariants(TabSheetVariant.MATERIAL_BORDERED);
        return tabSheet;
    }

    private Component getMGSRGrid() {
        VerticalLayout content = new VerticalLayout();
        crudMGSR = new Crud<>(OutlookMGSR.class, createMGSREditor());
        setupMGSRGrid();
        content.add(crudMGSR);

        crudMGSR.setToolbarVisible(false);

        gridMGSR.addItemDoubleClickListener(event -> {
            OutlookMGSR selectedEntity = event.getItem();
            crudMGSR.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            crudMGSR.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        crudMGSR.setHeightFull();
        content.setHeightFull();
        return content;
    }

    private CrudEditor<OutlookMGSR> createMGSREditor() {

        FormLayout editForm = new FormLayout();
        Binder<OutlookMGSR> binder = new Binder<>(OutlookMGSR.class);
        return new BinderCrudEditor<>(binder, editForm);
    }
    private void setupMGSRGrid_old() {
        String ZEILE = "zeile";
        String MONTH = "month";
        String PLLINE = "pl_Line";
        String PROFITCENTER = "profitCenter";
        String SCENARIO = "scenario";
        String SEGMENT = "segment";
        String PAYMENTTYPE = "paymentType";
        String TYPEOFDATA = "typeOfData";
        String VALUE = "value";

        gridMGSR = crudMGSR.getGrid();

        // Define the columns first
        Grid.Column<OutlookMGSR> zeileColumn = gridMGSR.addColumn(OutlookMGSR::getZeile);
        Grid.Column<OutlookMGSR> monthColumn = gridMGSR.addColumn(OutlookMGSR::getMonth);
        Grid.Column<OutlookMGSR> plLineColumn = gridMGSR.addColumn(OutlookMGSR::getPl_Line);
        Grid.Column<OutlookMGSR> profitCenterColumn = gridMGSR.addColumn(OutlookMGSR::getProfitCenter);
        Grid.Column<OutlookMGSR> scenarioColumn = gridMGSR.addColumn(OutlookMGSR::getScenario);
        Grid.Column<OutlookMGSR> segmentColumn = gridMGSR.addColumn(OutlookMGSR::getSegment);
        Grid.Column<OutlookMGSR> paymentTypeColumn = gridMGSR.addColumn(OutlookMGSR::getPaymentType);
        Grid.Column<OutlookMGSR> typeOfDataColumn = gridMGSR.addColumn(OutlookMGSR::getTypeOfData);
        Grid.Column<OutlookMGSR> valueColumn = gridMGSR.addColumn(OutlookMGSR::getValue);

        // Set headers and other properties for the columns
        zeileColumn.setHeader("Zeile").setWidth("80px").setFlexGrow(0).setResizable(true);
        monthColumn.setHeader("Month").setWidth("100px").setFlexGrow(0).setResizable(true);
        plLineColumn.setHeader("PL_Line").setWidth("80px").setFlexGrow(0).setResizable(true);
        profitCenterColumn.setHeader("ProfitCenter").setWidth("100px").setFlexGrow(0).setResizable(true);
        scenarioColumn.setHeader("Scenario").setWidth("200px").setFlexGrow(0).setResizable(true);
        segmentColumn.setHeader("Segment").setWidth("200px").setFlexGrow(0).setResizable(true);
        paymentTypeColumn.setHeader("PaymentType").setWidth("80px").setFlexGrow(0).setResizable(true);
        typeOfDataColumn.setHeader("TypeOfData").setWidth("200px").setFlexGrow(0).setResizable(true);
        valueColumn.setHeader("Value").setWidth("80px").setFlexGrow(0).setResizable(true);

        // Reorder the columns (if needed)
        gridMGSR.setColumnOrder(
                zeileColumn,
                monthColumn,
                plLineColumn,
                profitCenterColumn,
                scenarioColumn,
                segmentColumn,
                paymentTypeColumn,
                typeOfDataColumn,
                valueColumn
        );
    }

    private void setupMGSRGrid() {

        String ZEILE = "zeile";
        String MONTH = "month";
        String PLLINE = "pl_Line";
        String PROFITCENTER = "profitCenter";
        String SCENARIO = "scenario";
        String SEGMENT = "segment";
        String PAYMENTTYPE = "paymentType";
        String TYPEOFDATA = "typeOfData";
        String VALUE = "value";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        gridMGSR = crudMGSR.getGrid();

        //gridMGSR.getColumnByKey(ZEILE).setHeader("Zeile").setWidth("10px");
        gridMGSR.setColumns(ZEILE, MONTH, PLLINE, PROFITCENTER, SCENARIO, SEGMENT, PAYMENTTYPE, TYPEOFDATA, VALUE);

      //  gridMGSR.removeColumn(gridMGSR.getColumnByKey(EDIT_COLUMN));

        gridMGSR.getColumnByKey(ZEILE).setHeader("Zeile").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(MONTH).setHeader("Month").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(PLLINE).setHeader("PL_Line").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(PROFITCENTER).setHeader("ProfitCenter").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(SCENARIO).setHeader("Scenario").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(SEGMENT).setHeader("Segment").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(PAYMENTTYPE).setHeader("PaymentType").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(TYPEOFDATA).setHeader("TypeOfData").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(VALUE).setHeader("Value").setWidth("80px").setFlexGrow(0).setResizable(true);

        // Reorder the columns (alphabetical by default)
        gridMGSR.setColumnOrder( gridMGSR.getColumnByKey(ZEILE)
                , gridMGSR.getColumnByKey(MONTH)
                , gridMGSR.getColumnByKey(PLLINE)
                , gridMGSR.getColumnByKey(PROFITCENTER)
                , gridMGSR.getColumnByKey(SCENARIO)
                , gridMGSR.getColumnByKey(SEGMENT)
                , gridMGSR.getColumnByKey(PAYMENTTYPE)
                , gridMGSR.getColumnByKey(TYPEOFDATA)
                , gridMGSR.getColumnByKey(VALUE));
        //    , gridFinancials.getColumnByKey(EDIT_COLUMN));
    }
    private void setupUploader() {
        singleFileUpload.setWidth("600px");

        singleFileUpload.addSucceededListener(event -> {
            // Get information about the uploaded file
            InputStream fileData = memoryBuffer.getInputStream();
            String fileName = event.getFileName();
            contentLength = event.getContentLength();
            mimeType = event.getMIMEType();

            parseExcelFile(fileData,fileName);
            GenericDataProvider dataFinancialsProvider = new GenericDataProvider(listOfAllSheets.get("MGSR"), "Zeile");
            gridMGSR.setDataProvider(dataFinancialsProvider);

            singleFileUpload.clearFileList();
        });
    }

    private void parseExcelFile(InputStream fileData, String fileName) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        Article article = new Article();

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
            textArea.setText(LocalDateTime.now().format(formatter) + ": Info: Verarbeite Datei: " + fileName + " (" + contentLength + " Byte)");

            XSSFWorkbook my_xls_workbook = new XSSFWorkbook(fileData);

            List<OutlookMGSR> sheetData = new ArrayList<>();

            XSSFSheet sheet1 = my_xls_workbook.getSheet("MGSR");
            sheetData = parseSheet(sheet1, OutlookMGSR.class);
            listOfAllSheets.put("MGSR", sheetData);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T> List<T>  parseSheet(XSSFSheet sheet, Class<T> targetType) {
        List<T> resultList = new ArrayList<>();
        try {
            // List<T> resultList = new ArrayList<>();
            Iterator<Row> rowIterator = sheet.iterator();

            int rowNumber=0;
            Integer Error_count=0;
            System.out.println(sheet.getPhysicalNumberOfRows()+"$$$$$$$$$");

            while (rowIterator.hasNext() ) {
                Row row = rowIterator.next();
                if(row != null) {
                    T entity = targetType.newInstance();
                    rowNumber++;

                    if (rowNumber == 1) {
                        continue;
                    }
                    if (row.getCell(0).toString().isEmpty()) {
                        break;
                    }

                    Field[] fields = targetType.getDeclaredFields();
                    for (int index = 0; index < fields.length; index++) {
                        Cell cell = null;
                        if (index != 0) {
                            cell = row.getCell(index - 1);
                        } else {
                            cell = row.getCell(index);
                        }
                        System.out.println(cell);
                        if (cell != null && !cell.toString().isEmpty()) {
                            Field field = fields[index];
                            field.setAccessible(true);

                            if (index == 0) {
                                field.set(entity, rowNumber);
                            } else {
                                if (field.getType() == int.class || field.getType() == Integer.class) {
                                    field.set(entity, (int) cell.getNumericCellValue());
                                } else if (field.getType() == long.class || field.getType() == Long.class) {
                                    field.set(entity, (long) cell.getNumericCellValue());
                                } else if (field.getType() == double.class || field.getType() == Double.class) {
                                    field.set(entity, cell.getNumericCellValue());
                                } else if (field.getType() == String.class) {
                                    field.set(entity, cell.getStringCellValue());
                                }
                            }
                        }
                    }
                    System.out.println(rowNumber + "..............");
                    resultList.add(entity);
                } else {
                    System.out.println(row+"++++++++++++++");
                    break;
                }
            }
            return resultList;
        } catch (Exception e) {
           // e.printStackTrace();
            return resultList;
        }

    }
}
