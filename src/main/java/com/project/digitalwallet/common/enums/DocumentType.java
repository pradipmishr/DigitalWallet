package com.project.digitalwallet.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DocumentType {
//    CITIZENSHIP,
//    PASSPORT,
//    DRIVING_LICENSE,
    NATIONAL_ID;

    @JsonCreator
    public static DocumentType fromValue(String value) {
        return DocumentType.valueOf(value.toUpperCase());
    }
}
