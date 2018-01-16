/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.github.api;

import hudson.XmlFile;
import org.apache.commons.io.IOUtils;
import org.junit.AssumptionViolatedException;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Verifies that the whitelisted classes can be actually saved on disk.
 */
@RunWith(Parameterized.class)
public class ClassFilterTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @ClassRule
    public static TemporaryFolder tmp = new TemporaryFolder();

    private final String className;

    public ClassFilterTest(String className) {
        this.className = className;
    }

    @Override
    public String toString() {
        return super.toString() + " " + className;
    }

    public static Set<String> readWhitelist() throws AssertionError {
        HashSet<String> classes = new HashSet<>();

        try (InputStream is = ClassFilterTest.class.getResourceAsStream("/META-INF/hudson.remoting.ClassFilter")) {
            for (String line : IOUtils.readLines(is, StandardCharsets.UTF_8)) {
                if (line.matches("#.*|\\s*")) { // skip comments
                    continue;
                } else if (line.endsWith(".*")) { // regexp, we assume that it is a pattern like foo.bar.* or foo.bar.Foo.*
                    throw new AssertionError("Regular expressions are not supported so far");
                } else { // just class entry
                    classes.add(line);
                }
            }
        } catch (IOException ex) {
            throw new AssertionError("Failed to read /META-INF/hudson.remoting.ClassFilter", ex);
        }

        return classes;
    }

    @Parameterized.Parameters(name = "{index}: class={0}")
    public static Collection<String> data() throws AssertionError {
        return readWhitelist();
    }

    @Test
    public void testClass() throws AssertionError, IOException {
        final Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new AssertionError("Whitelisted class does not exist", ex);
        }

        Object obj;
        try {
            obj = clazz.getConstructor().newInstance();
        } catch (NoSuchMethodException ex) {
            throw new AssumptionViolatedException("The class has no default constructor. Cannot check its serialization", ex);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException | NoClassDefFoundError ex) {
            // NoClassDefFoundError is for OkHttp
            throw new AssumptionViolatedException("Cannot instantinate the class", ex);
        }

        XmlFile f = new XmlFile(new File(tmp.getRoot(), className + ".xml"));
        f.write(obj);
    }
}
