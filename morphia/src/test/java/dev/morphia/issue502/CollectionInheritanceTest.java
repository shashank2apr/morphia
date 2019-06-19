package dev.morphia.issue502;

import dev.morphia.TestBase;
import dev.morphia.annotations.Id;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Can't inherit HashSet : generic is lost...
 */
public class CollectionInheritanceTest extends TestBase {

    private static Book newBook() {
        final Book book = new Book();
        book.authors.add(new Author("Hergé"));
        book.authors.add(new Author("George R. R. Martin"));
        return book;
    }

    /**
     * Issue's details...
     */
    @Test
    public void testMappingBook() {
        // Mapping...
        getMorphia().map(Book.class /* , Authors.class, Author.class */);

        // Test mapping : author objects must be converted into Document (but wasn't)
        final Document dbBook = getMorphia().getMapper().toDocument(newBook());
        final Object firstBook = ((List<?>) dbBook.get("authors")).iterator().next();
        assertTrue("Author wasn't converted : expected instanceof <Document>, but was <" + firstBook.getClass() + ">",
                   firstBook instanceof Document);

    }

    /**
     * Real test
     */
    @Test
    public void testSavingBook() {
        // Test saving
        getDs().save(newBook());

        assertEquals(1, getDs().getCollection(Book.class).countDocuments());
    }

    private static class Author {
        private String name;

        Author(final String name) {
            this.name = name;
        }

    }

    private static class Authors extends HashSet<Author> {
    }

    private static class Book {
        @Id
        private ObjectId id;

        private Authors authors = new Authors();
    }
}
