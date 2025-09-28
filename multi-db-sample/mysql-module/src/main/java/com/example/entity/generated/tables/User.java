package com.example.entity.generated.tables;

import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

public class User extends TableImpl<Record> {
    public static final User USER = new User();

    public final TableField<Record, Integer> ID = createField(DSL.name("id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).identity(true), this, "");
    public final TableField<Record, String> NAME = createField(DSL.name("name"), org.jooq.impl.SQLDataType.VARCHAR(255), this, "");
    public final TableField<Record, String> EMAIL = createField(DSL.name("email"), org.jooq.impl.SQLDataType.VARCHAR(255), this, "");

    private User(Name alias, Table<Record> aliased) {
        super(alias, null, aliased, null);
    }

    private User() {
        this(DSL.name("user"), null);
    }
}