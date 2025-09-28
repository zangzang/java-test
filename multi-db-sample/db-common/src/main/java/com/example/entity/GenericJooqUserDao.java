package com.example.entity;

import com.example.base.entity.User;
import com.example.base.dao.UserDao;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;

import java.util.List;
import java.util.Optional;

/**
 * Generic JOOQ-based UserDao implementation that is table/field-agnostic.
 * Pass the module-generated USER table and its fields when constructing.
 */
/**
 * Generic JOOQ-based UserDao implementation that is table/field-agnostic.
 * Pass the module-generated USER table and its fields when constructing.
 *
 * Extension points:
 * - override {@link #convertId(Long)} and {@link #convertNumberToLong(Object)} to
 *   support non-numeric id types (UUID, String, etc.) in concrete subclasses.
 */
public class GenericJooqUserDao<R extends Record, I> implements UserDao {
    protected final DSLContext dsl;
    protected final Table<R> userTable;
    protected final Field<I> idField;
    protected final Field<String> nameField;
    protected final Field<String> emailField;
    protected final IdConverter<I> idConverter;

    public GenericJooqUserDao(DSLContext dsl,
                              Table<R> userTable,
                              Field<I> idField,
                              Field<String> nameField,
                              Field<String> emailField) {
        this.dsl = dsl;
        this.userTable = userTable;
        this.idField = idField;
        this.nameField = nameField;
        this.emailField = emailField;
        this.idConverter = new DefaultIdConverter<>(idField);
    }

    /**
     * Create with a custom IdConverter (for UUID or other id types).
     */
    public GenericJooqUserDao(DSLContext dsl,
                              Table<R> userTable,
                              Field<I> idField,
                              Field<String> nameField,
                              Field<String> emailField,
                              IdConverter<I> idConverter) {
        this.dsl = dsl;
        this.userTable = userTable;
        this.idField = idField;
        this.nameField = nameField;
        this.emailField = emailField;
        this.idConverter = idConverter;
    }

    /**
     * Package-visible constructor for tests that do not need a real DSLContext.
     * Allows testing id conversion helpers without a jOOQ runtime.
     */
    GenericJooqUserDao(Table<R> userTable, Field<I> idField, Field<String> nameField, Field<String> emailField) {
        this.dsl = null;
        this.userTable = userTable;
        this.idField = idField;
        this.nameField = nameField;
        this.emailField = emailField;
        this.idConverter = new DefaultIdConverter<>(idField);
    }

    GenericJooqUserDao(Table<R> userTable, Field<I> idField, Field<String> nameField, Field<String> emailField, IdConverter<I> idConverter) {
        this.dsl = null;
        this.userTable = userTable;
        this.idField = idField;
        this.nameField = nameField;
        this.emailField = emailField;
        this.idConverter = idConverter;
    }

    @Override
    public Optional<User> findById(Long id) {
        try {
            var record = dsl.selectFrom(userTable).where(idField.eq(convertId(id))).fetchOne();
            if (record != null) {
                User user = new User();
                user.setId(convertNumberToLong(record.get(idField)));
                user.setName(record.get(nameField));
                user.setEmail(record.get(emailField));
                return Optional.of(user);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding user by id", e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        try {
            return dsl.selectFrom(userTable)
                    .fetch()
                    .map(r -> {
                        User user = new User();
                        user.setId(convertNumberToLong(r.get(idField)));
                        user.setName(r.get(nameField));
                        user.setEmail(r.get(emailField));
                        return user;
                    });
        } catch (Exception e) {
            throw new RuntimeException("Error finding all users", e);
        }
    }

    /**
     * Convert a long id to the concrete field type. Delegates to configured IdConverter.
     * Subclasses can override convertId/convertNumberToLong for custom behavior, but
     * prefer supplying a custom IdConverter via constructor override if needed.
     */
    protected I convertId(Long id) {
        return idConverter.fromLong(id);
    }

    protected long convertNumberToLong(I value) {
        return idConverter.toLong(value);
    }

    /**
     * Strategy interface to convert between repository id types and Long used by the
     * application layer. Supply a custom implementation to support UUIDs or other ids.
     */
    public interface IdConverter<T> {
        T fromLong(Long id);
        long toLong(T value);
    }

    /** Default numeric converter that handles Integer/Long/Short and falls back to toString parsing. */
    private static final class DefaultIdConverter<T> implements IdConverter<T> {
        private final Field<?> idField;

        DefaultIdConverter(Field<?> idField) {
            this.idField = idField;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T fromLong(Long id) {
            try {
                if (Integer.class.isAssignableFrom(idField.getType())) {
                    return (T) Integer.valueOf(id.intValue());
                }
                if (Long.class.isAssignableFrom(idField.getType())) {
                    return (T) Long.valueOf(id.longValue());
                }
                if (Short.class.isAssignableFrom(idField.getType())) {
                    return (T) Short.valueOf(id.shortValue());
                }
            } catch (Exception ignored) {
            }
            return (T) id;
        }

        @Override
        public long toLong(T value) {
            if (value == null) return 0L;
            if (value instanceof Number) return ((Number) value).longValue();
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException e) {
                throw new RuntimeException("Unable to convert id field to long", e);
            }
        }
    }

    @Override
    public void save(User user) {
        try {
            var inserted = dsl.insertInto(userTable, nameField, emailField)
                    .values(user.getName(), user.getEmail())
                    .returning(idField)
                    .fetchOne();

            if (inserted != null) {
                user.setId(convertNumberToLong(inserted.get(idField)));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error saving user", e);
        }
    }

    @Override
    public void update(User user) {
        try {
            dsl.update(userTable)
                    .set(nameField, user.getName())
                    .set(emailField, user.getEmail())
                    .where(idField.eq(convertId(user.getId())))
                    .execute();
        } catch (Exception e) {
            throw new RuntimeException("Error updating user", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            dsl.deleteFrom(userTable).where(idField.eq(convertId(id))).execute();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting user", e);
        }
    }
}
