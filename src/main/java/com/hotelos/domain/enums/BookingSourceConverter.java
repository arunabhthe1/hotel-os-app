package com.hotelos.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BookingSourceConverter implements AttributeConverter<BookingSource, String> {

    @Override
    public String convertToDatabaseColumn(BookingSource attribute) {
        return attribute == null ? null : attribute.toDbValue();
    }

    @Override
    public BookingSource convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BookingSource.fromDbValue(dbData);
    }
}
