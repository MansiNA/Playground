package de.dbuss.tefcontrol.data.entity;

import com.vaadin.flow.component.crud.CrudFilter;
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;
import de.dbuss.tefcontrol.data.service.CLTV_HW_MeasureService;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Comparator.naturalOrder;

public class CLTV_HW_MeasuresDataProvider  extends AbstractBackEndDataProvider<CLTV_HW_Measures, CrudFilter> {

    // A real app should hook up something like JPA
    private static CLTV_HW_MeasureService cltvHwMeasureService;
    final List<CLTV_HW_Measures> DATABASE;

    public CLTV_HW_MeasuresDataProvider(CLTV_HW_MeasureService cltvHwMeasureService) {
        this.cltvHwMeasureService = cltvHwMeasureService;
        DATABASE = createCltv_hw_MeasureList();
    }

    public CLTV_HW_MeasuresDataProvider(List<CLTV_HW_Measures> listOfCLTV_HW_Measures) {
        this.DATABASE = listOfCLTV_HW_Measures;
    }
    private static List<CLTV_HW_Measures> createCltv_hw_MeasureList() {
        //return IntStream
        //        .rangeClosed(1, 50)
        //        .mapToObj(i -> new CLTV_HW_Measures(i, 20230106, "Device A", "Measure B", "Channel XY", 3L ))
        //        .collect(toList());

        List<CLTV_HW_Measures> xx = cltvHwMeasureService.findAllProducts("");
        return xx;
    }


    private Consumer<Long> sizeChangeListener;

    @Override
    protected Stream<CLTV_HW_Measures> fetchFromBackEnd(Query<CLTV_HW_Measures, CrudFilter> query) {
        int offset = query.getOffset();
        int limit = query.getLimit();

        Stream<CLTV_HW_Measures> stream = DATABASE.stream();

        if (query.getFilter().isPresent()) {
            stream = stream.filter(predicate(query.getFilter().get()))
                    .sorted(comparator(query.getFilter().get()));
        }

        return stream.skip(offset).limit(limit);
    }

    @Override
    protected int sizeInBackEnd(Query<CLTV_HW_Measures, CrudFilter> query) {
        // For RDBMS just execute a SELECT COUNT(*) ... WHERE query
        long count = fetchFromBackEnd(query).count();

        if (sizeChangeListener != null) {
            sizeChangeListener.accept(count);
        }

        return (int) count;
    }

    void setSizeChangeListener(Consumer<Long> listener) {
        sizeChangeListener = listener;
    }

    private static Predicate<CLTV_HW_Measures> predicate(CrudFilter filter) {
        // For RDBMS just generate a WHERE clause
        return filter.getConstraints().entrySet().stream()
                .map(constraint -> (Predicate<CLTV_HW_Measures>) person -> {
                    try {
                        Object value = valueOf(constraint.getKey(), person);
                        return value != null && value.toString().toLowerCase()
                                .contains(constraint.getValue().toLowerCase());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }).reduce(Predicate::and).orElse(e -> true);
    }

    private static Comparator<CLTV_HW_Measures> comparator(CrudFilter filter) {
        // For RDBMS just generate an ORDER BY clause
        return filter.getSortOrders().entrySet().stream().map(sortClause -> {
            try {
                Comparator<CLTV_HW_Measures> comparator = Comparator.comparing(
                        person -> (Comparable) valueOf(sortClause.getKey(),
                                person));

                if (sortClause.getValue() == SortDirection.DESCENDING) {
                    comparator = comparator.reversed();
                }

                return comparator;

            } catch (Exception ex) {
                return (Comparator<CLTV_HW_Measures>) (o1, o2) -> 0;
            }
        }).reduce(Comparator::thenComparing).orElse((o1, o2) -> 0);
    }

    private static Object valueOf(String fieldName, CLTV_HW_Measures person) {
        try {
            Field field = CLTV_HW_Measures.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(person);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void persist(CLTV_HW_Measures item) {

        if (item.getId() == null) {
            item.setId(DATABASE.stream().map(CLTV_HW_Measures::getId).max(naturalOrder())
                    .orElse(0) + 1);
        }

        final Optional<CLTV_HW_Measures> existingItem = find(item.getId());
        if (existingItem.isPresent()) {
            int position = DATABASE.indexOf(existingItem.get());
            DATABASE.remove(existingItem.get());
            DATABASE.add(position, item);
        } else {
            DATABASE.add(item);
        }
    }

    Optional<CLTV_HW_Measures> find(Integer id) {
        return DATABASE.stream().filter(entity -> entity.getId().equals(id))
                .findFirst();
    }

    public void delete(CLTV_HW_Measures item) {
        DATABASE.removeIf(entity -> entity.getId().equals(item.getId()));
    }
}
