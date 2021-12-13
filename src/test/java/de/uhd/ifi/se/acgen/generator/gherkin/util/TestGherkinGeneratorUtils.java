package de.uhd.ifi.se.acgen.generator.gherkin.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

public class TestGherkinGeneratorUtils {

    @Test
    public void testInstantiationOfUtilClasses() {
        assertDoesNotThrow(() -> {
            Constructor<PreprocessingUtils> constructor = PreprocessingUtils.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(constructor.getModifiers()));
            constructor.setAccessible(true);
            InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> constructor.newInstance());
            assertEquals(UnsupportedOperationException.class, e.getTargetException().getClass());
        });

        assertDoesNotThrow(() -> {
            Constructor<UIPreconditionUtils> constructor = UIPreconditionUtils.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(constructor.getModifiers()));
            constructor.setAccessible(true);
            InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> constructor.newInstance());
            assertEquals(UnsupportedOperationException.class, e.getTargetException().getClass());
        });

        assertDoesNotThrow(() -> {
            Constructor<ActionUtils> constructor = ActionUtils.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(constructor.getModifiers()));
            constructor.setAccessible(true);
            InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> constructor.newInstance());
            assertEquals(UnsupportedOperationException.class, e.getTargetException().getClass());
        });

        assertDoesNotThrow(() -> {
            Constructor<ResultUtils> constructor = ResultUtils.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(constructor.getModifiers()));
            constructor.setAccessible(true);
            InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> constructor.newInstance());
            assertEquals(UnsupportedOperationException.class, e.getTargetException().getClass());
        });

        assertDoesNotThrow(() -> {
            Constructor<PostprocessingUtils> constructor = PostprocessingUtils.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(constructor.getModifiers()));
            constructor.setAccessible(true);
            InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> constructor.newInstance());
            assertEquals(UnsupportedOperationException.class, e.getTargetException().getClass());
        });

        assertDoesNotThrow(() -> {
            Constructor<SharedUtils> constructor = SharedUtils.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(constructor.getModifiers()));
            constructor.setAccessible(true);
            InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> constructor.newInstance());
            assertEquals(UnsupportedOperationException.class, e.getTargetException().getClass());
        });
    }

}
