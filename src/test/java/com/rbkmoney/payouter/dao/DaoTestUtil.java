package com.rbkmoney.payouter.dao;

import org.apache.commons.lang.WordUtils;
import org.jooq.Field;
import org.jooq.Table;

import java.util.Arrays;
import java.util.function.Predicate;

public class DaoTestUtil {

    public static String[] getNonNullColumnNames(Table table) {
        return getColumnNames(field -> !field.getDataType().nullable(), table);
    }

    public static String[] getNullColumnNames(Table table) {
        return getColumnNames(field -> field.getDataType().nullable(), table);
    }

    public static String[] getColumnNames(Predicate<Field> predicate, Table table) {

        return Arrays.stream(table.fields())
                .filter(predicate)
                .map(field ->
                        WordUtils.uncapitalize(
                                WordUtils.capitalizeFully(field.getName(), new char[]{'_'})
                                        .replaceAll("_", "")
                        )
                )
                .toArray(size -> new String[size]);
    }

}
