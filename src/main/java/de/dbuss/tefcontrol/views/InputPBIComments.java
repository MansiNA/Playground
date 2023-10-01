package de.dbuss.tefcontrol.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.crud.CrudFilter;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.provider.*;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.naturalOrder;


@PageTitle("PowerBI Comments")
@Route(value = "InputPBIComments", layout = MainLayout.class)
@RolesAllowed("USER")
public class InputPBIComments extends VerticalLayout {

    MemoryBuffer memoryBuffer = new MemoryBuffer();
    Upload singleFileUpload = new Upload(memoryBuffer);
    Button importButton = new Button("Freigabe");

    Article article = new Article();
    Div textArea = new Div();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    InputStream fileData;
    String fileName = "";
    long contentLength = 0;
    String mimeType = "";
    private List<Financials> listOfFinancials = new ArrayList<Financials>();
    private List<Subscriber> listOfSubscriber = new ArrayList<Subscriber>();
    private List<UnitsDeepDive> listOfUnitsDeepDive = new ArrayList<UnitsDeepDive>();

    private Crud<Financials> crudFinancials;
    private Grid<Financials> gridFinancials = new Grid<>(Financials.class);
    private Crud<Subscriber> crudSubscriber;
    private Grid<Subscriber> gridSubscriber = new Grid<>(Subscriber.class);
    private Crud<UnitsDeepDive> crudUnitsDeepDive;
    private Grid<UnitsDeepDive> gridUnitsDeepDive = new Grid<>(UnitsDeepDive.class);

    public InputPBIComments() {

        Div htmlDiv = new Div();
        htmlDiv.getElement().setProperty("innerHTML", "<h2>Input Frontend for PBI Comments");

        // Div zur Ansicht hinzufügen
        add(htmlDiv);

        HorizontalLayout hl = new HorizontalLayout();
        hl.setAlignItems(Alignment.CENTER);

        hl.add(singleFileUpload,importButton);
        add(hl);
        add(textArea);

        setupUploader();

        add(getTabsheet());
    }

    private TabSheet getTabsheet() {
        TabSheet tabSheet = new TabSheet();

        tabSheet.add("Financials", getFinancialsGrid());
        tabSheet.add("Subscriber", getSubscriberGrid());
        tabSheet.add("UnitsDeepDive", getUnitsDeepDiveGrid());

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();
        return tabSheet;
    }

    private Component getFinancialsGrid() {
        VerticalLayout content = new VerticalLayout();
        crudFinancials = new Crud<>(Financials.class, createFinancialsEditor());
        setupFinancialsGrid();
        content.add(crudFinancials);
        return content;
    }

    private Component getSubscriberGrid() {
        VerticalLayout content = new VerticalLayout();
        crudSubscriber = new Crud<>(Subscriber.class, createSubscriberEditor());
        setupSubscriberGrid();
        content.add(crudSubscriber);
        return content;
    }

    private Component getUnitsDeepDiveGrid() {
        VerticalLayout content = new VerticalLayout();
        crudUnitsDeepDive = new Crud<>(UnitsDeepDive.class, createUnitsDeepDiveEditor());
        setupUnitsDeepDiveGrid();
        content.add(crudUnitsDeepDive);
        return content;
    }

    private CrudEditor<Financials> createFinancialsEditor() {

        IntegerField zeile = new IntegerField  ("Zeile");
        IntegerField month = new IntegerField ("Month");
        TextField category = new TextField("Category");
        TextArea comment = new TextArea("Comment");
        TextField scenario = new TextField("Scenario");
        TextField xtd = new TextField("XTD");

        FormLayout editForm = new FormLayout(zeile, month, category, scenario, xtd, comment);

        editForm.setColspan(comment, 2);

        Binder<Financials> binder = new Binder<>(Financials.class);
        //binder.forField(month).withNullRepresentation("202301"").withConverter(new StringToIntegerConverter("Not a Number")).asRequired().bind(Financials::setMonth, Financials::setMonth);
        //  binder.forField(monat_ID).asRequired().bind(CLTV_HW_Measures::getMonat_ID, CLTV_HW_Measures::setMonat_ID);
        binder.forField(month).asRequired().bind(Financials::getMonth, Financials::setMonth);
        binder.forField(category).asRequired().bind(Financials::getCategory, Financials::setCategory);
        binder.forField(comment).asRequired().bind(Financials::getComment, Financials::setComment);
        binder.forField(scenario).asRequired().bind(Financials::getScenario, Financials::setScenario);
        binder.forField(xtd).asRequired().bind(Financials::getXtd, Financials::setXtd);
        binder.forField(zeile).asRequired().bind(Financials::getRow, Financials::setRow);

        return new BinderCrudEditor<>(binder, editForm);
    }

    private CrudEditor<Subscriber> createSubscriberEditor() {

        IntegerField zeile = new IntegerField  ("Zeile");
        IntegerField month = new IntegerField ("Month");
        TextField category = new TextField("Category");
        TextField comment = new TextField("Comment");
        TextField paymentType = new TextField("Payment Type");
        TextField segment = new TextField("Segment");

        FormLayout editForm = new FormLayout(zeile, month, category, paymentType, segment, comment);
        editForm.setColspan(comment, 2);

        Binder<Subscriber> binder = new Binder<>(Subscriber.class);
        binder.forField(month).asRequired().bind(Subscriber::getMonth, Subscriber::setMonth);
        binder.forField(category).asRequired().bind(Subscriber::getCategory, Subscriber::setCategory);
        binder.forField(comment).asRequired().bind(Subscriber::getComment, Subscriber::setComment);
        binder.forField(paymentType).asRequired().bind(Subscriber::getPaymentType, Subscriber::setPaymentType);
        binder.forField(segment).asRequired().bind(Subscriber::getSegment, Subscriber::setSegment);
        binder.forField(zeile).asRequired().bind(Subscriber::getRow, Subscriber::setRow);

        return new BinderCrudEditor<>(binder, editForm);
    }

    private CrudEditor<UnitsDeepDive> createUnitsDeepDiveEditor() {

        IntegerField zeile = new IntegerField  ("Zeile");
        IntegerField month = new IntegerField ("Month");
        TextField category = new TextField("Category");
        TextField comment = new TextField("Comment");
        TextField segment = new TextField("Segment");

        FormLayout editForm = new FormLayout(zeile, month, category, segment, comment);
        editForm.setColspan(comment, 2);

        Binder<UnitsDeepDive> binder = new Binder<>(UnitsDeepDive.class);
        binder.forField(month).asRequired().bind(UnitsDeepDive::getMonth, UnitsDeepDive::setMonth);
        binder.forField(category).asRequired().bind(UnitsDeepDive::getCategory, UnitsDeepDive::setCategory);
        binder.forField(comment).asRequired().bind(UnitsDeepDive::getComment, UnitsDeepDive::setComment);
        binder.forField(segment).asRequired().bind(UnitsDeepDive::getSegment, UnitsDeepDive::setSegment);
        binder.forField(zeile).asRequired().bind(UnitsDeepDive::getRow, UnitsDeepDive::setRow);

        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupUploader() {
        singleFileUpload.setWidth("600px");

        singleFileUpload.addSucceededListener(event -> {
            // Get information about the uploaded file
            fileData = memoryBuffer.getInputStream();
            fileName = event.getFileName();
            contentLength = event.getContentLength();
            mimeType = event.getMIMEType();

            parseExcelFile(fileData,fileName);
            GenericDataProvider dataFinancialsProvider = new GenericDataProvider(listOfFinancials);
            crudFinancials.setDataProvider(dataFinancialsProvider);
            GenericDataProvider dataSubscriberProvider = new GenericDataProvider(listOfSubscriber);
            crudSubscriber.setDataProvider(dataSubscriberProvider);
            GenericDataProvider dataUnitsDeepDiveProvider = new GenericDataProvider(listOfUnitsDeepDive);
            crudUnitsDeepDive.setDataProvider(dataUnitsDeepDiveProvider);
            setupDataProviderEvent();

            singleFileUpload.clearFileList();
            crudFinancials.setHeight("600px");
        });
        System.out.println("setup uploader................over");
    }

    private void parseExcelFile(InputStream fileData, String fileName) {

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

        // Reorder the columns (alphabetical by default)
        gridFinancials.setColumnOrder( gridFinancials.getColumnByKey(ZEILE)
                , gridFinancials.getColumnByKey(MONTH)
                , gridFinancials.getColumnByKey(COMMENT)
                , gridFinancials.getColumnByKey(SCENARIO)
                , gridFinancials.getColumnByKey(CATEGORY)
                , gridFinancials.getColumnByKey(XTD)
                , gridFinancials.getColumnByKey(EDIT_COLUMN));

        gridFinancials.addItemDoubleClickListener(e->{
            System.out.println("Zeile: " + e.getItem().getRow());
        });

    }

    private void setupSubscriberGrid() {

        String ZEILE = "row";

        String MONTH = "month";

        String COMMENT = "comment";

        String CATEGORY = "category";

        String PAYMENTTYPE = "paymentType";

        String SEGMENT = "segment";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        gridSubscriber = crudSubscriber.getGrid();

        gridSubscriber.getColumnByKey("row").setHeader("Zeile").setWidth("10px");

        // Reorder the columns (alphabetical by default)
        gridSubscriber.setColumnOrder( gridSubscriber.getColumnByKey(ZEILE)
                , gridSubscriber.getColumnByKey(MONTH)
                , gridSubscriber.getColumnByKey(COMMENT)
                , gridSubscriber.getColumnByKey(CATEGORY)
                , gridSubscriber.getColumnByKey(PAYMENTTYPE)
                , gridSubscriber.getColumnByKey(SEGMENT)
                , gridSubscriber.getColumnByKey(EDIT_COLUMN));

        gridSubscriber.addItemDoubleClickListener(e->{
            System.out.println("Zeile: " + e.getItem().getRow());
        });
    }

    private void setupUnitsDeepDiveGrid() {

        String ZEILE = "row";

        String MONTH = "month";

        String COMMENT = "comment";

        String CATEGORY = "category";

        String SEGMENT = "segment";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        gridUnitsDeepDive = crudUnitsDeepDive.getGrid();

        gridUnitsDeepDive.getColumnByKey("row").setHeader("Zeile").setWidth("10px");

        // Reorder the columns (alphabetical by default)
        gridUnitsDeepDive.setColumnOrder( gridUnitsDeepDive.getColumnByKey(ZEILE)
                , gridUnitsDeepDive.getColumnByKey(MONTH)
                , gridUnitsDeepDive.getColumnByKey(COMMENT)
                , gridUnitsDeepDive.getColumnByKey(CATEGORY)
                , gridUnitsDeepDive.getColumnByKey(SEGMENT)
                , gridUnitsDeepDive.getColumnByKey(EDIT_COLUMN));

        gridSubscriber.addItemDoubleClickListener(e->{
            System.out.println("Zeile: " + e.getItem().getRow());
        });
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
                for (int index = 0; index < (fields.length -1); index++) {
                    Cell cell = row.getCell(index);

                    if (cell != null && !cell.toString().isEmpty()) {
                        Field field = fields[index];
                        if( index == 0) {
                            field.set(entity, RowNumber);
                        }

                        field = fields[index+1];
                        field.setAccessible(true);

                        if (field.getType() == int.class || field.getType() == Integer.class) {
                            field.set(entity, (int) cell.getNumericCellValue());
                        } else if (field.getType() == double.class || field.getType() == Double.class) {
                            field.set(entity, cell.getNumericCellValue());
                        } else if (field.getType() == String.class) {
                            field.set(entity, cell.getStringCellValue());
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
        GenericDataProvider  financialsdataProvider = new GenericDataProvider(getFinancialsDataProviderAllItems());
        GenericDataProvider  subscriberdataProvider = new GenericDataProvider(getSubscriberDataProviderAllItems());
        GenericDataProvider  unitsDeepDivedataProvider = new GenericDataProvider(getUnitsDeepDiveDataProviderAllItems());

        article=new Article();
        article.setText(LocalDateTime.now().format(formatter) + ": Info: Download from Database");
        textArea.add(article);

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


    public static class Subscriber {

        public Subscriber() {
        }

        private Integer row;

        private Integer month;

        private String category;

        private String comment;

        private String paymentType;

        private String segment;

        public Integer getRow() {
            return row;
        }

        public void setRow(Integer row) {
            this.row = row;
        }

        public Integer getMonth() {
            return month;
        }

        public void setMonth(Integer month) {
            this.month = month;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getPaymentType() {
            return paymentType;
        }

        public void setPaymentType(String paymentType) {
            this.paymentType = paymentType;
        }

        public String getSegment() {
            return segment;
        }

        public void setSegment(String segment) {
            this.segment = segment;
        }
    }

    public static class UnitsDeepDive {

        public UnitsDeepDive() {
        }

        private Integer row;

        private Integer month;

        private String category;

        private String comment;

        private String segment;

        public Integer getRow() {
            return row;
        }

        public void setRow(Integer row) {
            this.row = row;
        }

        public Integer getMonth() {
            return month;
        }

        public void setMonth(Integer month) {
            this.month = month;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getSegment() {
            return segment;
        }

        public void setSegment(String segment) {
            this.segment = segment;
        }
    }

    public static class Financials {

        public Financials() {
        }

        private Integer row;

        private Integer month;

        private String category;

        private String comment;

        private String scenario;

        private String xtd;

        public Integer getRow() {
            return row;
        }

        public void setRow(Integer row) {
            this.row = row;
        }

        public Integer getMonth() {
            return month;
        }

        public void setMonth(Integer month) {
            this.month = month;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getScenario() {
            return scenario;
        }

        public void setScenario(String scenario) {
            this.scenario = scenario;
        }

        public String getXtd() {
            return xtd;
        }

        public void setXtd(String xtd) {
            this.xtd = xtd;
        }
    }

    public class GenericDataProvider<T> extends AbstractBackEndDataProvider<T, CrudFilter> {

        private final List<T> DATABASE;
        private Consumer<Long> sizeChangeListener;

        public GenericDataProvider(List<T> data) {
            this.DATABASE = data;
        }

        public void setSizeChangeListener(Consumer<Long> sizeChangeListener) {
            this.sizeChangeListener = sizeChangeListener;
        }

        @Override
        protected Stream<T> fetchFromBackEnd(Query<T, CrudFilter> query) {
            int offset = query.getOffset();
            int limit = query.getLimit();

            Stream<T> stream = DATABASE.stream();

            if (query.getFilter().isPresent()) {
                stream = stream.filter(predicate(query.getFilter().get()))
                        .sorted(comparator(query.getFilter().get()));
            }

            return stream.skip(offset).limit(limit);
        }

        private Predicate<T> predicate(CrudFilter filter) {
            return filter.getConstraints().entrySet().stream()
                    .map(constraint -> (Predicate<T>) entity -> {
                        try {
                            Object value = valueOf(constraint.getKey(), entity);
                            return value != null && value.toString().toLowerCase()
                                    .contains(constraint.getValue().toLowerCase());
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                    }).reduce(Predicate::and).orElse(entity -> true);
        }

        private Object valueOf(String fieldName, T entity) {
            try {
                Field field = entity.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(entity);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        private Comparator<T> comparator(CrudFilter filter) {
            return filter.getSortOrders().entrySet().stream().map(sortClause -> {
                try {
                    Comparator<T> comparator = Comparator.comparing(
                            entity -> (Comparable) valueOf(sortClause.getKey(),
                                    entity));

                    if (sortClause.getValue() == SortDirection.DESCENDING) {
                        comparator = comparator.reversed();
                    }

                    return comparator;

                } catch (Exception ex) {
                    return (Comparator<T>) (o1, o2) -> 0;
                }
            }).reduce(Comparator::thenComparing).orElse((o1, o2) -> 0);
        }

        @Override
        protected int sizeInBackEnd(Query<T, CrudFilter> query) {
            long count = fetchFromBackEnd(query).count();

            if (sizeChangeListener != null) {
                sizeChangeListener.accept(count);
            }

            return (int) count;
        }
        public void persist(T item) {
            try {
                Field field = item.getClass().getDeclaredField("row");
                field.setAccessible(true);
                Integer row = (Integer) field.get(item);

                if (row == null) {
                    row = DATABASE.stream().map(entity -> {
                        try {
                            Field entityField = entity.getClass().getDeclaredField("row");
                            entityField.setAccessible(true);
                            return (Integer) entityField.get(entity);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return 0;
                        }
                    }).max(Comparator.naturalOrder()).orElse(0) + 1;
                    field.set(item, row);
                }

                Optional<T> existingItem = find(row);
                if (existingItem.isPresent()) {
                    int position = DATABASE.indexOf(existingItem.get());
                    DATABASE.remove(existingItem.get());
                    DATABASE.add(position, item);
                } else {
                    DATABASE.add(item);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public Optional<T> find(Integer id) {
            return DATABASE.stream().filter(entity -> {
                try {
                    Field field = entity.getClass().getDeclaredField("row");
                    field.setAccessible(true);
                    return field.get(entity).equals(id);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).findFirst();
        }

        public void delete(T item) {
            try {
                Field field = item.getClass().getDeclaredField("row");
                field.setAccessible(true);
                Integer row = (Integer) field.get(item);

                DATABASE.removeIf(entity -> {
                    try {
                        Field entityField = entity.getClass().getDeclaredField("row");
                        entityField.setAccessible(true);
                        return entityField.get(entity).equals(row);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
