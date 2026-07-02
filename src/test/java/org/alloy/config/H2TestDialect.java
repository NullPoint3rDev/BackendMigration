package org.alloy.config;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.type.SqlTypes;

/**
 * ponytail: Hibernate 6 maps small {@code @Enumerated(ORDINAL)} enums to TINYINT; H2 has no TINYINT type.
 */
public class H2TestDialect extends H2Dialect {

    @Override
    protected String columnType(int sqlTypeCode) {
        if (sqlTypeCode == SqlTypes.TINYINT) {
            return "smallint";
        }
        return super.columnType(sqlTypeCode);
    }
}
