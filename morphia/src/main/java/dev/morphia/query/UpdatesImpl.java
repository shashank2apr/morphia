package dev.morphia.query;

import dev.morphia.Datastore;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.morphia.utils.ReflectionUtils.iterToList;
import static java.util.Collections.singletonList;

@SuppressWarnings("unchecked")
abstract class UpdatesImpl<Updater extends Updates> implements Updates<Updater>  {

    protected Datastore datastore;
    protected final Mapper mapper;
    protected final Class clazz;
    private Map<String, Object> ops = new HashMap<>();
    private boolean validateNames = true;

    UpdatesImpl(final Datastore datastore, final Mapper mapper, final Class clazz) {
        this.datastore = datastore;
        this.mapper = mapper;
        this.clazz = clazz;
    }

    @Override
    public Updater addToSet(final String field, final Object value) {
        if (value == null) {
            throw new QueryException("Value cannot be null.");
        }

        add(UpdateOperator.ADD_TO_SET, field, value, true);
        return (Updater)this;
    }

    @Override
    public Updater addToSet(final String field, final List<?> values) {
        if (values == null || values.isEmpty()) {
            throw new UpdateException("Values cannot be null or empty.");
        }

        add(UpdateOperator.ADD_TO_SET_EACH, field, values, true);
        return (Updater)this;
    }

    @Override
    public Updater addToSet(final String field, final Iterable<?> values) {
        return addToSet(field, iterToList(values));
    }

    @Override
    public Updater push(final String field, final Object value) {
        return push(field, value instanceof List ? (List<?>) value : singletonList(value), new PushOptions());
    }

    @Override
    public Updater push(final String field, final Object value, final PushOptions options) {
        return push(field, value instanceof List ? (List<?>) value : singletonList(value), options);
    }

    @Override
    public Updater push(final String field, final List<?> values) {
        return push(field, values, new PushOptions());
    }

    @Override
    public Updater push(final String field, final List<?> values, final PushOptions options) {
        if (values == null || values.isEmpty()) {
            throw new QueryException("Values cannot be null or empty.");
        }

        PathTarget pathTarget = new PathTarget(mapper, mapper.getMappedClass(clazz), field, validateNames);

        Document dbObject = new Document(UpdateOperator.EACH.val(), mapper.toMongoObject(pathTarget.getTarget(), null, values));
        options.update(dbObject);
        addOperation(UpdateOperator.PUSH, pathTarget.translatedPath(), dbObject);

        return (Updater)this;
    }

    @Override
    public Updater dec(final String field) {
        return inc(field, -1);
    }

    @Override
    public Updater dec(final String field, final Number value) {
        if ((value instanceof Long) || (value instanceof Integer)) {
            return inc(field, (value.longValue() * -1));
        }
        if ((value instanceof Double) || (value instanceof Float)) {
            return inc(field, (value.doubleValue() * -1));
        }
        throw new IllegalArgumentException(
            "Currently only the following types are allowed: integer, long, double, float.");
    }

    @Override
    public Updater disableValidation() {
        validateNames = false;
        return (Updater)this;
    }

    @Override
    public Updater enableValidation() {
        validateNames = true;
        return (Updater)this;
    }

    @Override
    public Updater inc(final String field) {
        return inc(field, 1);
    }

    @Override
    public Updater inc(final String field, final Number value) {
        if (value == null) {
            throw new QueryException("Value cannot be null.");
        }
        add(UpdateOperator.INC, field, value, false);
        return (Updater)this;
    }

    @Override
    public Updater max(final String field, final Number value) {
        add(UpdateOperator.MAX, field, value, false);
        return (Updater)this;
    }

    @Override
    public Updater min(final String field, final Number value) {
        add(UpdateOperator.MIN, field, value, false);
        return (Updater)this;
    }

    @Override
    public Updater removeAll(final String field, final Object value) {
        if (value == null) {
            throw new QueryException("Value cannot be null.");
        }
        add(UpdateOperator.PULL, field, value, true);
        return (Updater)this;
    }

    @Override
    public Updater removeAll(final String field, final List<?> values) {
        if (values == null || values.isEmpty()) {
            throw new QueryException("Value cannot be null or empty.");
        }

        add(UpdateOperator.PULL_ALL, field, values, true);
        return (Updater)this;
    }

    @Override
    public Updater removeFirst(final String field) {
        return remove(field, true);
    }

    @Override
    public Updater removeLast(final String field) {
        return remove(field, false);
    }

    @Override
    public Updater set(final String field, final Object value) {
        if (value == null) {
            throw new QueryException("Value for field [" + field + "] cannot be null.");
        }

        add(UpdateOperator.SET, field, value, true);
        return (Updater)this;
    }

    @Override
    public Updater setOnInsert(final String field, final Object value) {
        if (value == null) {
            throw new QueryException("Value cannot be null.");
        }

        add(UpdateOperator.SET_ON_INSERT, field, value, true);
        return (Updater)this;
    }

    @Override
    public Updater unset(final String field) {
        add(UpdateOperator.UNSET, field, 1, false);
        return (Updater)this;
    }

    /**
     * @return the operations listed
     */
    protected Document getOps() {
        return new Document(ops);
    }

    /**
     * Sets the operations for this UpdateOpsImpl
     *
     * @param ops the operations
     */
    @SuppressWarnings("unchecked")
    void setOps(final Document ops) {
        this.ops = ops;
    }

    //TODO Clean this up a little.
    private void add(final UpdateOperator op, final String f, final Object value, final boolean convert) {
        if (value == null) {
            throw new QueryException("Val cannot be null");
        }

        Object val = value;
        PathTarget pathTarget = new PathTarget(mapper, clazz, f, validateNames);
        MappedField mf = pathTarget.getTarget();

        if (convert) {
            if (UpdateOperator.PULL_ALL.equals(op) && value instanceof List) {
                val = toDBObjList(mf, (List<?>) value);
            } else {
                val = mapper.toMongoObject(mf, null, value);
            }
        }


        if (UpdateOperator.ADD_TO_SET_EACH.equals(op)) {
            val = new Document(UpdateOperator.EACH.val(), val);
        }

        addOperation(op, pathTarget.translatedPath(), val);
    }

    protected Updater remove(final String fieldExpr, final boolean firstNotLast) {
        add(UpdateOperator.POP, fieldExpr, (firstNotLast) ? -1 : 1, false);
        return (Updater)this;
    }

    @Override
    public String toString() {
        return getOps().toString();
    }

    protected List<Object> toDBObjList(final MappedField mf, final List<?> values) {
        final List<Object> list = new ArrayList<>(values.size());
        for (final Object obj : values) {
            list.add(mapper.toMongoObject(mf, null, obj));
        }

        return list;
    }

    private void addOperation(final UpdateOperator op, final String fieldName, final Object val) {
        final String opString = op.val();

        if (!ops.containsKey(opString)) {
            ops.put(opString, new Document());
        }
        ((Document) ops.get(opString)).put(fieldName, val);
    }

}
