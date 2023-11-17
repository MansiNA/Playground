package de.dbuss.tefcontrol.data.modules.inputpbicomments.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.*;
import com.vaadin.flow.router.*;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.Role;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.Financials;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.Subscriber;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.UnitsDeepDive;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.data.service.ProjectQsService;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@PageTitle("PowerBI Central Comments")
@Route(value = "PBI_Central_Comments/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "MAPPING"})
public class PBICentralComments extends VerticalLayout implements BeforeEnterObserver {
    private final ProjectConnectionService projectConnectionService;
    MemoryBuffer memoryBuffer = new MemoryBuffer();
    Upload singleFileUpload = new Upload(memoryBuffer);
    private final AuthenticatedUser authenticatedUser;
    private List<Financials> listOfFinancials = new ArrayList<Financials>();
    private List<Subscriber> listOfSubscriber = new ArrayList<Subscriber>();
    private List<UnitsDeepDive> listOfUnitsDeepDive = new ArrayList<UnitsDeepDive>();
    private Crud<Financials> crudFinancials;
    private Grid<Financials> gridFinancials = new Grid<>(Financials.class, false);
    private Crud<Subscriber> crudSubscriber;
    private Grid<Subscriber> gridSubscriber = new Grid<>(Subscriber.class);
    private Crud<UnitsDeepDive> crudUnitsDeepDive;
    private Grid<UnitsDeepDive> gridUnitsDeepDive = new Grid<>(UnitsDeepDive.class);
    private ProgressBar progressBar = new ProgressBar();
    private String financialsTableName;
    private String subscriberTableName;
    private String unitTableName;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    long contentLength = 0;
    String mimeType = "";
    private Button saveButton;
    Div textArea = new Div();
    private String idKey = "row";
    private int projectId;
    private QS_Grid qsGrid;
    private Button qsBtn;

    public PBICentralComments(AuthenticatedUser authenticatedUser, ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService) {

        this.authenticatedUser = authenticatedUser;
        this.projectConnectionService = projectConnectionService;

        qsBtn = new Button("Start Job");
        qsBtn.setEnabled(false);

        progressBar.setWidth("15em");
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        Div htmlDiv = new Div();
        htmlDiv.getElement().setProperty("innerHTML", "<h2>Input Frontend for Central Comments");
        add(htmlDiv);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : listOfProjectParameters) {
            if(projectParameter.getNamespace().equals(Constants.PBI_CENTRAL_COMMENTS)) {
                if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                    dbServer = projectParameter.getValue();
                } else if (Constants.DB_NAME.equals(projectParameter.getName())) {
                    dbName = projectParameter.getValue();
                } else if (Constants.DB_USER.equals(projectParameter.getName())) {
                    dbUser = projectParameter.getValue();
                } else if (Constants.DB_PASSWORD.equals(projectParameter.getName())) {
                    dbPassword = projectParameter.getValue();
                }else if (Constants.TABLE_FINANCIALS.equals(projectParameter.getName())) {
                    financialsTableName = projectParameter.getValue();
                } else if (Constants.TABLE_SUBSCRIBER.equals(projectParameter.getName())) {
                    subscriberTableName = projectParameter.getValue();
                } else if (Constants.TABLE_UNITDEEPDIVE.equals(projectParameter.getName())) {
                    unitTableName = projectParameter.getValue();
                }
            }
        }
        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";
        //Text databaseDetail = new Text("Connected to: "+ dbServer+ ", Database: " + dbName+ ", Table Financials: " + financialsTableName + ", Table Subscriber: " + subscriberTableName+ ", Table Unitdeepdive: "+ unitTableName);
        Text databaseDetail = new Text("Connected to: "+ dbServer+ ", Database: " + dbName);

        //Componente QS-Grid:
        qsGrid = new QS_Grid(projectConnectionService);

        HorizontalLayout hl = new HorizontalLayout();
        hl.setAlignItems(Alignment.BASELINE);
        // hl.add(singleFileUpload, saveButton, databaseDetail, progressBar);
        hl.add(singleFileUpload, qsBtn, databaseDetail, progressBar, qsGrid);
        add(hl);

        qsBtn.addClickListener(e ->{
            if (qsGrid.projectId != projectId) {
                CallbackHandler callbackHandler = new CallbackHandler();
                qsGrid.createDialog(callbackHandler, projectId);
            }
            qsGrid.showDialog(true);
        });

        add(textArea);
        setupUploader();
        add(getTabsheet());
        setHeightFull();
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

                Notification notification = Notification.show(" Rows Uploaded start",2000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                UI ui = UI.getCurrent();
                progressBar.setVisible(true);

                ListenableFuture<String> future = upload2db();;
                future.addCallback(
                        successResult -> updateUi(ui, "Task finished: " + successResult),
                        failureException -> updateUi(ui, "Task failed: " + failureException.getMessage())
                );
            }
        }
    }
    private void updateUi(UI ui, String result) {

        ui.access(() -> {
            Notification.show(result);
            progressBar.setVisible(false);
        });
    }

    private ListenableFuture<String> upload2db() {
        List<Financials> allFinancialsItems = getFinancialsDataProviderAllItems();
        List<Subscriber> allSubscriber = getSubscriberDataProviderAllItems();
        List<UnitsDeepDive> allUnitsDeepDive = getUnitsDeepDiveDataProviderAllItems();

        Notification notification;

        String resultFinancial = projectConnectionService.saveFinancials(allFinancialsItems, financialsTableName, dbUrl, dbUser, dbPassword);
        if (resultFinancial.contains("ok")){
            notification = Notification.show(allFinancialsItems.size() + " Financials Rows Uploaded successfully",5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            notification = Notification.show("Error during Financials upload: " + resultFinancial ,5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        String resultSubscriber = projectConnectionService.saveSubscriber(allSubscriber, subscriberTableName, dbUrl, dbUser, dbPassword);
        if (resultSubscriber.contains("ok")){
            notification = Notification.show(allSubscriber.size() + " Subscriber Rows Uploaded successfully",5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            notification = Notification.show("Error during Subscriber upload: " + resultSubscriber,5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }

        String resultUnits = projectConnectionService.saveUnitsDeepDive(allUnitsDeepDive, unitTableName, dbUrl, dbUser, dbPassword);
        if (resultUnits.contains("ok")){
            notification = Notification.show(allUnitsDeepDive.size() + " UnitsDeepDive Rows Uploaded successfully",5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            notification = Notification.show("Error during UnitsDeepDive upload: " + resultUnits,5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        return AsyncResult.forValue("Some result");

    }

    private TabSheet getTabsheet() {
        boolean isAdmin=false;

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            Set<Role> roles = user.getRoles();
            isAdmin = roles.stream()
                    .anyMatch(role -> role == Role.ADMIN);
        }

        Component getFinancials=getFinancialsGrid();
        Component getSubscriber=getSubscriberGrid();
        Component getUnitsDeepDive = getUnitsDeepDiveGrid();

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Financials", getFinancials);
        tabSheet.add("Subscriber", getSubscriber);
        tabSheet.add("UnitsDeepDive", getUnitsDeepDive);

        if(!isAdmin) {
            tabSheet.getTab(getSubscriber).setEnabled(false);
            tabSheet.getTab(getUnitsDeepDive).setEnabled(false);
        }


        tabSheet.setSizeFull();
        tabSheet.setHeightFull();
        tabSheet.addThemeVariants(TabSheetVariant.MATERIAL_BORDERED);
        return tabSheet;
    }

    private Component getFinancialsGrid() {
        VerticalLayout content = new VerticalLayout();
        crudFinancials = new Crud<>(Financials.class, createFinancialsEditor());
        setupFinancialsGrid();
        content.add(crudFinancials);

        crudFinancials.setToolbarVisible(false);

        gridFinancials.addItemDoubleClickListener(event -> {
            Financials selectedEntity = event.getItem();
            crudFinancials.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            crudFinancials.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        crudFinancials.setHeightFull();
        content.setHeightFull();
        return content;
    }

    private Component getSubscriberGrid() {
        VerticalLayout content = new VerticalLayout();
        crudSubscriber = new Crud<>(Subscriber.class, createSubscriberEditor());
        setupSubscriberGrid();
        content.add(crudSubscriber);

        crudSubscriber.setToolbarVisible(false);

        gridSubscriber.addItemDoubleClickListener(event -> {
            Subscriber selectedEntity = event.getItem();
            crudSubscriber.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            crudSubscriber.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        crudSubscriber.setHeightFull();
        content.setHeightFull();
        return content;
    }

    private Component getUnitsDeepDiveGrid() {
        VerticalLayout content = new VerticalLayout();
        crudUnitsDeepDive = new Crud<>(UnitsDeepDive.class, createUnitsDeepDiveEditor());
        setupUnitsDeepDiveGrid();
        content.add(crudUnitsDeepDive);

        crudUnitsDeepDive.setToolbarVisible(false);

        gridUnitsDeepDive.addItemDoubleClickListener(event -> {
            UnitsDeepDive selectedEntity = event.getItem();
            crudUnitsDeepDive.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            crudUnitsDeepDive.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        crudUnitsDeepDive.setHeightFull();
        content.setHeightFull();
        return content;
    }

    private CrudEditor<Financials> createFinancialsEditor() {

        TextArea comment = new TextArea("Comment");

        comment.setHeight("250px");
        comment.setWidth("1200px");
        FormLayout editForm = new FormLayout(comment);
        editForm.setColspan(comment, 2);

        editForm.setHeight("250px");
        editForm.setWidth("1200px");

        Binder<Financials> binder = new Binder<>(Financials.class);
        binder.forField(comment).asRequired().bind(Financials::getComment, Financials::setComment);

        return new BinderCrudEditor<>(binder, editForm);
    }

    private CrudEditor<Subscriber> createSubscriberEditor() {

        TextArea comment = new TextArea("Comment");

        comment.setHeight("250px");
        comment.setWidth("1200px");
        FormLayout editForm = new FormLayout(comment);
        editForm.setColspan(comment, 2);

        editForm.setHeight("250px");
        editForm.setWidth("1200px");

        Binder<Subscriber> binder = new Binder<>(Subscriber.class);
        binder.forField(comment).asRequired().bind(Subscriber::getComment, Subscriber::setComment);

        return new BinderCrudEditor<>(binder, editForm);
    }

    private CrudEditor<UnitsDeepDive> createUnitsDeepDiveEditor() {

        TextArea comment = new TextArea("Comment");
        comment.setHeight("250px");
        comment.setWidth("1200px");
        FormLayout editForm = new FormLayout(comment);
        editForm.setColspan(comment, 3);

        editForm.setHeight("350px");
        editForm.setWidth("800px");

        Binder<UnitsDeepDive> binder = new Binder<>(UnitsDeepDive.class);
        binder.forField(comment).asRequired().bind(UnitsDeepDive::getComment, UnitsDeepDive::setComment);

        return new BinderCrudEditor<>(binder, editForm);
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

            GenericDataProvider dataFinancialsProvider = new GenericDataProvider(listOfFinancials, idKey);
            gridFinancials.setDataProvider(dataFinancialsProvider);
            GenericDataProvider dataSubscriberProvider = new GenericDataProvider(listOfSubscriber, idKey);
            crudSubscriber.setDataProvider(dataSubscriberProvider);
            GenericDataProvider dataUnitsDeepDiveProvider = new GenericDataProvider(listOfUnitsDeepDive, idKey);
            crudUnitsDeepDive.setDataProvider(dataUnitsDeepDiveProvider);
            setupDataProviderEvent();

            singleFileUpload.clearFileList();
            qsBtn.setEnabled(true);
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

            XSSFSheet sheet1 = my_xls_workbook.getSheet("Comments Financials");
            listOfFinancials = parseSheet(sheet1, Financials.class);

            XSSFSheet sheet2 = my_xls_workbook.getSheet("Comments Subscriber");
            listOfSubscriber = parseSheet(sheet2, Subscriber.class);

            XSSFSheet sheet3 = my_xls_workbook.getSheet("Comments Units Deep Dive");
            listOfUnitsDeepDive = parseSheet(sheet3, UnitsDeepDive.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupFinancialsGrid() {

        String ZEILE = "row";

        String MONTH = "month";

        String COMMENT = "comment";

        String SCENARIO = "scenario";

        String CATEGORY = "category";

        String XTD = "xtd";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        gridFinancials = crudFinancials.getGrid();

        gridFinancials.getColumnByKey("row").setHeader("Zeile").setWidth("10px");

        gridFinancials.removeColumn(gridFinancials.getColumnByKey(EDIT_COLUMN));

        gridFinancials.getColumnByKey("row").setHeader("Zeile").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridFinancials.getColumnByKey("month").setHeader("Month").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridFinancials.getColumnByKey("scenario").setHeader("Scenario").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridFinancials.getColumnByKey("category").setHeader("Category").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridFinancials.getColumnByKey("xtd").setHeader("XTD").setWidth("80px").setFlexGrow(0).setResizable(true);



        // Reorder the columns (alphabetical by default)
        gridFinancials.setColumnOrder( gridFinancials.getColumnByKey(ZEILE)
                , gridFinancials.getColumnByKey(MONTH)
                , gridFinancials.getColumnByKey(CATEGORY)
                , gridFinancials.getColumnByKey(SCENARIO)
                , gridFinancials.getColumnByKey(XTD)
                , gridFinancials.getColumnByKey(COMMENT));
            //    , gridFinancials.getColumnByKey(EDIT_COLUMN));

    }

    private void setupSubscriberGrid() {

        String ZEILE = "row";

        String MONTH = "month";

        String COMMENT = "comment";

        String CATEGORY = "category";

        String PAYMENTTYPE = "paymentType";

        String SEGMENT = "segment";

        String SCENARIO = "scenario";

        String XTD = "xtd";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        gridSubscriber = crudSubscriber.getGrid();

        gridSubscriber.removeColumn(gridSubscriber.getColumnByKey(EDIT_COLUMN));

        gridSubscriber.getColumnByKey("row").setHeader("Zeile").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridSubscriber.getColumnByKey("month").setHeader("Month").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridSubscriber.getColumnByKey("category").setHeader("Category").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridSubscriber.getColumnByKey("paymentType").setHeader("Payment Type").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridSubscriber.getColumnByKey("segment").setHeader("Segment").setWidth("150px").setFlexGrow(0).setResizable(true);
        gridSubscriber.getColumnByKey("scenario").setHeader("Scenario").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridSubscriber.getColumnByKey("xtd").setHeader("XTD").setWidth("80px").setFlexGrow(0).setResizable(true);

        // Reorder the columns (alphabetical by default)
        gridSubscriber.setColumnOrder( gridSubscriber.getColumnByKey(ZEILE)
                , gridSubscriber.getColumnByKey(MONTH)
                , gridSubscriber.getColumnByKey(CATEGORY)
                , gridSubscriber.getColumnByKey(SEGMENT)
                , gridSubscriber.getColumnByKey(PAYMENTTYPE)
                , gridSubscriber.getColumnByKey(SCENARIO)
                , gridSubscriber.getColumnByKey(XTD)
                , gridSubscriber.getColumnByKey(COMMENT));
              // , gridSubscriber.getColumnByKey(EDIT_COLUMN));

    }

    private void setupUnitsDeepDiveGrid() {

        String ZEILE = "row";

        String MONTH = "month";

        String COMMENT = "comment";

        String CATEGORY = "category";

        String SEGMENT = "segment";
        String SCENARIO = "scenario";
        String XTD = "xtd";


        String EDIT_COLUMN = "vaadin-crud-edit-column";

        gridUnitsDeepDive = crudUnitsDeepDive.getGrid();

        gridUnitsDeepDive.getColumnByKey("row").setHeader("Zeile").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridUnitsDeepDive.getColumnByKey("month").setHeader("Month").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridUnitsDeepDive.getColumnByKey("category").setHeader("Category").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridUnitsDeepDive.getColumnByKey("segment").setHeader("Segment").setWidth("150px").setFlexGrow(0).setResizable(true);
        gridUnitsDeepDive.getColumnByKey("scenario").setHeader("Scenario").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridUnitsDeepDive.getColumnByKey("xtd").setHeader("XTD").setWidth("80px").setFlexGrow(0).setResizable(true);

        gridUnitsDeepDive.removeColumn(gridUnitsDeepDive.getColumnByKey(EDIT_COLUMN));

        // Reorder the columns (alphabetical by default)
        gridUnitsDeepDive.setColumnOrder( gridUnitsDeepDive.getColumnByKey(ZEILE)
                , gridUnitsDeepDive.getColumnByKey(MONTH)
                , gridUnitsDeepDive.getColumnByKey(CATEGORY)
                , gridUnitsDeepDive.getColumnByKey(SEGMENT)
                , gridUnitsDeepDive.getColumnByKey(SCENARIO)
                , gridUnitsDeepDive.getColumnByKey(XTD)
                , gridUnitsDeepDive.getColumnByKey(COMMENT));
            //    , gridUnitsDeepDive.getColumnByKey(EDIT_COLUMN));

    }

    public <T> List<T>  parseSheet(XSSFSheet my_worksheet, Class<T> targetType) {

        try {
            List<T> resultList = new ArrayList<>();
            Iterator<Row> rowIterator = my_worksheet.iterator();

            int RowNumber=0;
            Integer Error_count=0;
            System.out.println(my_worksheet.getPhysicalNumberOfRows()+"$$$$$$$$$");

            while (rowIterator.hasNext() ) {
                Row row = rowIterator.next();
                T entity = targetType.newInstance();
                RowNumber++;

                if (RowNumber == 1) {
                    continue;
                }
                if(row.getCell(0).toString().isEmpty()) {
                    break;
                }

                Field[] fields = targetType.getDeclaredFields();
                for (int index = 0; index < fields.length; index++) {
                    Cell cell = null;
                    if(index != 0) {
                        cell = row.getCell(index -1);
                    } else {
                        cell = row.getCell(index);
                    }

                    if (cell != null && !cell.toString().isEmpty()) {
                        Field field = fields[index];
                        field.setAccessible(true);

                        if (index == 0) {
                            field.set(entity, RowNumber);
                        } else {
                            if (field.getType() == int.class || field.getType() == Integer.class) {
                                field.set(entity, (int) cell.getNumericCellValue());
                            } else if (field.getType() == double.class || field.getType() == Double.class) {
                                field.set(entity, cell.getNumericCellValue());
                            } else if (field.getType() == String.class) {
                                field.set(entity, cell.getStringCellValue());
                            }
                        }
                    }
                }
                resultList.add(entity);
            }
            return resultList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private void setupDataProviderEvent() {
        GenericDataProvider financialsdataProvider = new GenericDataProvider(getFinancialsDataProviderAllItems(), idKey);
        GenericDataProvider  subscriberdataProvider = new GenericDataProvider(getSubscriberDataProviderAllItems(), idKey);
        GenericDataProvider  unitsDeepDivedataProvider = new GenericDataProvider(getUnitsDeepDiveDataProviderAllItems(), idKey);

        crudFinancials.addDeleteListener(
                deleteEvent -> {financialsdataProvider.delete(deleteEvent.getItem());
                    crudFinancials.setDataProvider(financialsdataProvider);

               });
       crudFinancials.addSaveListener(
                saveEvent -> {
                    financialsdataProvider.persist(saveEvent.getItem());
                    crudFinancials.setDataProvider(financialsdataProvider);
                });

        crudSubscriber.addDeleteListener(
                deleteEvent -> {subscriberdataProvider.delete(deleteEvent.getItem());
                    crudSubscriber.setDataProvider(subscriberdataProvider);

                });
        crudSubscriber.addSaveListener(
                saveEvent -> {
                    subscriberdataProvider.persist(saveEvent.getItem());
                    crudSubscriber.setDataProvider(subscriberdataProvider);
                });

        crudUnitsDeepDive.addDeleteListener(
                deleteEvent -> {unitsDeepDivedataProvider.delete(deleteEvent.getItem());
                    crudUnitsDeepDive.setDataProvider(unitsDeepDivedataProvider);

                });
        crudUnitsDeepDive.addSaveListener(
                saveEvent -> {
                    unitsDeepDivedataProvider.persist(saveEvent.getItem());
                    crudUnitsDeepDive.setDataProvider(unitsDeepDivedataProvider);
                });
    }

    private List<Financials> getFinancialsDataProviderAllItems() {
        DataProvider<Financials, Void> existDataProvider = (DataProvider<Financials, Void>) gridFinancials.getDataProvider();
        List<Financials> listOfFinancials = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        return listOfFinancials;
    }

    private List<Subscriber> getSubscriberDataProviderAllItems() {
        DataProvider<Subscriber, Void> existDataProvider = (DataProvider<Subscriber, Void>) gridSubscriber.getDataProvider();
        List<Subscriber> listOfSubscriber = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        return listOfSubscriber;
    }

    private List<UnitsDeepDive> getUnitsDeepDiveDataProviderAllItems() {
        DataProvider<UnitsDeepDive, Void> existDataProvider = (DataProvider<UnitsDeepDive, Void>) gridUnitsDeepDive.getDataProvider();
        List<UnitsDeepDive>  listOfUnitsDeepDive = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        return listOfUnitsDeepDive;
    }

}
