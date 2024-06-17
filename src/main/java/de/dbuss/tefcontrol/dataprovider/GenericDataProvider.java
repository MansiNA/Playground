package de.dbuss.tefcontrol.dataprovider;

import com.vaadin.flow.component.crud.CrudFilter;
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class GenericDataProvider<T> extends AbstractBackEndDataProvider<T, CrudFilter> {

    private final List<T> DATABASE;
    private Consumer<Long> sizeChangeListener;
    private String idKey = "id";

    public GenericDataProvider(List<T> data) {
        this.DATABASE = data;
    }

    public GenericDataProvider(List<T> data, String idKey) {
        this.DATABASE = data;
        this.idKey = idKey;
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
                                entity), Comparator.nullsFirst(Comparator.naturalOrder()));

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
            Field field = item.getClass().getDeclaredField(idKey);
            field.setAccessible(true);
            Integer row = (Integer) field.get(item);

            if (row == null) {
                row = DATABASE.stream().map(entity -> {
                    try {
                        Field entityField = entity.getClass().getDeclaredField(idKey);
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
                Field field = entity.getClass().getDeclaredField(idKey);
                field.setAccessible(true);
                return field.get(entity).equals(id);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).findFirst();
    }

    public void delete(T item) {
        try {
            Field field = item.getClass().getDeclaredField(idKey);
            field.setAccessible(true);
            Integer row = (Integer) field.get(item);

            DATABASE.removeIf(entity -> {
                try {
                    Field entityField = entity.getClass().getDeclaredField(idKey);
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
