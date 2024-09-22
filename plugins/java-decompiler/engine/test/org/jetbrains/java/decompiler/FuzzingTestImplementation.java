package org.jetbrains.java.decompiler;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.AnnotationConsumer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


class FuzzingTestImplementation {

    private static final Path TEST_SPECS_DIR;
    static {
        TEST_SPECS_DIR = Paths.get("testSpecs");
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ArgumentsSource(FuzzerClassFileTestDataProvider.class)
    @interface FuzzerClassFileTestDataSource {
        /** Name of the file containing the class files test specifications */
        String value();
    }

    static class FuzzerClassFileTestDataProvider implements ArgumentsProvider, AnnotationConsumer<FuzzerClassFileTestDataSource> {
        private String configFilePath;
        private DocumentBuilderFactory dbf;

        @Override
        public void accept(FuzzerClassFileTestDataSource annotation) {
            configFilePath = annotation.value();
            dbf = DocumentBuilderFactory.newInstance();
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {

            List<Arguments> res = new ArrayList<>();

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(TEST_SPECS_DIR.resolve(configFilePath).toString());

            //Document doc = db.parse(configFilePath.toString());

            /* discover tests we want to do from config file; this is expected to match the
             * binary files in the relevant directory.
             */
            doc.getDocumentElement().normalize();
            NodeList classes = doc.getElementsByTagName("class");

            for (int x=0;x<classes.getLength();++x) {
                Element clazz = (Element)classes.item(x);
                String classFilePath = clazz.getElementsByTagName("path").item(0).getTextContent();
                String name = clazz.getElementsByTagName("name").item(0).getTextContent();
                Path expectedSource = Paths.get(classFilePath);
                res.add(Arguments.of(expectedSource));

            }
            return res.stream();
        }
    }

}