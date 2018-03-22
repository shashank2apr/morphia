package org.mongodb.morphia.mapping;


import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CollectionOfValuesTest extends TestBase {

    @Test
    public void testCity() {
        getMorphia().map(City.class);

        City city = new City();
        city.name = "My city";
        city.array = new byte[]{4, 5};
        for (byte i = 0; i < 2; i++) {
            for (byte j = 0; j < 2; j++) {
                city.cells[i][j] = (i * 100 + j);
            }
        }

        getDatastore().save(city);
        City loaded = getDatastore().get(city);
        Assert.assertEquals(city.name, loaded.name);
        compare(city.array, loaded.array);
        for (int i = 0; i < city.cells.length; i++) {
            compare(city.cells[i], loaded.cells[i]);
        }
    }

    @Test
    public void testListOfListMapping() {
        getMorphia().map(ContainsListOfList.class);
        getDatastore().delete(getDatastore().find(ContainsListOfList.class));
        final ContainsListOfList entity = new ContainsListOfList();

        entity.strings = new ArrayList<>();
        entity.strings.add(Arrays.asList("element1", "element2"));
        entity.strings.add(Collections.singletonList("element3"));
        entity.integers = new ArrayList<>();
        entity.integers.add(Arrays.asList(1, 2));
        entity.integers.add(Collections.singletonList(3));
        getDatastore().save(entity);

        final ContainsListOfList loaded = getDatastore().get(entity);

        Assert.assertNotNull(loaded.strings);
        Assert.assertEquals(entity.strings, loaded.strings);
        Assert.assertEquals(entity.strings.get(0), loaded.strings.get(0));
        Assert.assertEquals(entity.strings.get(0).get(0), loaded.strings.get(0).get(0));

        Assert.assertNotNull(loaded.integers);
        Assert.assertEquals(entity.integers, loaded.integers);
        Assert.assertEquals(entity.integers.get(0), loaded.integers.get(0));
        Assert.assertEquals(entity.integers.get(0).get(0), loaded.integers.get(0).get(0));

        Assert.assertNotNull(loaded.id);
    }

    @Test
    public void testTwoDimensionalArrayMapping() {
        getMorphia().map(ContainsTwoDimensionalArray.class);
        final ContainsTwoDimensionalArray entity = new ContainsTwoDimensionalArray();
        entity.oneDimArray = "Joseph".getBytes();
        entity.twoDimArray = new byte[][]{"Joseph".getBytes(), "uwe".getBytes()};
        getDatastore().save(entity);
        final ContainsTwoDimensionalArray loaded = getDatastore().get(ContainsTwoDimensionalArray.class, entity.id);
        Assert.assertNotNull(loaded.id);
        Assert.assertNotNull(loaded.oneDimArray);
        Assert.assertNotNull(loaded.twoDimArray);

        compare(entity.oneDimArray, loaded.oneDimArray);

        compare(entity.twoDimArray[0], loaded.twoDimArray[0]);
        compare(entity.twoDimArray[1], loaded.twoDimArray[1]);
    }

    private void compare(final byte[] left, final byte[] right) {
        Assert.assertArrayEquals(left, right);
    }

    private void compare(final int[] left, final int[] right) {
        Assert.assertArrayEquals(left, right);
    }

    private static class ContainsListOfList {
        @Id
        private ObjectId id;
        private List<List<String>> strings;
        private List<List<Integer>> integers;
    }

    private static class ContainsTwoDimensionalArray {
        @Id
        private ObjectId id;
        private byte[] oneDimArray;
        private byte[][] twoDimArray;
    }

    @Entity(noClassnameStored = true)
    public static class City {
        @Id
        private ObjectId id;
        private String name;
        @Embedded
        private byte[] array;
        private int[][] cells = new int[2][2];
    }

}
