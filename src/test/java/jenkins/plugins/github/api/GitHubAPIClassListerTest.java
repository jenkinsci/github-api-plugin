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

import jenkins.plugins.github.api.util.ClassLister;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

// TODO: Move this tool to JTH?
/**
 * Lists classes in GitHub API
 */
public class GitHubAPIClassListerTest {

    // No sense to blacklist base classes. GitHub object apparently includes all the crap, and it's needed for serialization of objects

    // TODO: add objects?
    private static final List<Class<?>> BLACKLISTED_ANONYMOUS_CLASSES =  Arrays.asList(Iterable.class, Iterator.class);

    @Test
    public void verifyThatAllClassesAreWhitelisted() {

        TreeSet<String> entries = new TreeSet<>();
        Map<String, String> ignores = new HashMap<>();
        try {
            for (Class<?> clazz : ClassLister.getClassesForPackage("org.kohsuke.github")) {
                if (clazz.isEnum() || clazz.isInterface() || clazz.isAnnotation()) {
                    continue; // No need to list enums, interfaces and annotations
                }

                if (Throwable.class.isAssignableFrom(clazz)) {
                    continue; // All exceptions are whitelisted
                }

                String className = clazz.getName();
                if (clazz.isAnonymousClass()) {
                    boolean skipped = false;
                    for (Class<?> base : BLACKLISTED_ANONYMOUS_CLASSES) {
                        if (base.isAssignableFrom(clazz)) {
                            skipped = true;
                        }
                    }

                    if (skipped) {
                        System.out.println("Skipping anonymous class " + clazz + " of type " + clazz.getSuperclass());
                        continue;
                    } else {
                        System.out.println("Adding anonymous class " + clazz + " of type " + clazz.getSuperclass());
                    }
                }

                entries.add(className);
            }
        } catch (ClassNotFoundException ex) {
            throw new AssertionError(ex);
        }

        for (String className : entries) {
            if (ignores.containsKey(className)) {
                System.out.println("# Ignore: " + className + ". " + ignores.get(className));
            } else {
                System.out.println(className);
            }
        }
        System.out.println("Total entries: " + entries.size() + ". Whitelisted: " + (entries.size() - ignores.size()));

        Set<String> whitelisted = ClassFilterTest.readWhitelist();
        for (String entry : entries) {
            if (ignores.containsKey(entry)) {
                continue;
            }

            Assert.assertTrue("Class is not whitelisted: " + entry, whitelisted.contains(entry));
        }
    }
}
