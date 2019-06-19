package dev.morphia.converters;


import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.MappingException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class CharacterConverter extends TypeConverter implements SimpleValueConverter {
    /**
     * Creates the Converter.
     */
    public CharacterConverter() {
        super(char.class, Character.class);
    }

    @Override
    public Object decode(final Class targetClass, final Object fromDocument, final MappedField optionalExtraInfo) {
        if (fromDocument == null) {
            return null;
        }

        if (fromDocument instanceof String) {
            final char[] chars = ((String) fromDocument).toCharArray();
            if (chars.length == 1) {
                return chars[0];
            } else if (chars.length == 0) {
                return (char) 0;
            }
        }
        throw new MappingException("Trying to map multi-character data to a single character: " + fromDocument);
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        return value == null || value.equals('\0') ? null : String.valueOf(value);
    }
}
