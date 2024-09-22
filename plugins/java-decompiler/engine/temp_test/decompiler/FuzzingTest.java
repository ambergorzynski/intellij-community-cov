// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.java.decompiler;

import org.jetbrains.java.decompiler.FuzzingTestImplementation.FuzzerClassFileTestDataSource;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.ClassFormatException;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jetbrains.java.decompiler.DecompilerTestFixture.assertFilesEqual;
import static org.junit.Assert.assertTrue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;



public class FuzzingTest {
  private DecompilerTestFixture fixture;

  @Before
  public void setUp() throws IOException {
    fixture = new DecompilerTestFixture();
    fixture.setUp(Map.of(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1",
                         IFernflowerPreferences.DUMP_ORIGINAL_LINES, "1",
                         IFernflowerPreferences.IGNORE_INVALID_BYTECODE, "1",
                         IFernflowerPreferences.VERIFY_ANONYMOUS_CLASSES, "1",
                         IFernflowerPreferences.CONVERT_PATTERN_SWITCH, "1",
                         IFernflowerPreferences.CONVERT_RECORD_PATTERN, "1"
    ));
  }

  @After
  public void tearDown() throws IOException {
    fixture.tearDown();
    fixture = null;
  }

  //@Test public void testPrimitiveNarrowing() { doTest("pkg/TestPrimitiveNarrowing"); }

  //@ParameterizedTest(name = "[{index}] {0}")
  //@FuzzerClassFileTestDataSource("/data/dev/fernflower/intellij-community-cov/plugins/java-decompiler/engine/testData/fuzzer_classes.xml")
  //@ParameterizedTest()
  //@ValueSource(ints = {1,3,5})
  @Test
  public void decompile() {
        Path classFilePath = Paths.get("/data/work/fuzzflesh/coverage/fuzzflesh_graph_gen/out/decompiler_input/graph_9_path_2");
        doTest(classFilePath);
        assertTrue(true);
  }


  private void doTest(Path classFilePath) {
    var decompiler = fixture.getDecompiler();
    
    //var classFile = fixture.getTestDataDir().resolve("classes/" + testFile + ".class");
    var classFile = classFilePath.resolve("TestCase.class");

    assertThat(classFile).isRegularFile();
    for (var file : collectClasses(classFile)) {
      decompiler.addSource(file.toFile());
    }

    decompiler.decompileContext();

    var decompiledFile = fixture.getTargetDir().resolve(classFile.getFileName().toString().replace(".class", ".java"));
    assertThat(decompiledFile).isRegularFile();
    assertTrue(Files.isRegularFile(decompiledFile));
  }

  static List<Path> collectClasses(Path classFile) {
    var files = new ArrayList<Path>();
    files.add(classFile);

    var parent = classFile.getParent();
    if (parent != null) {
      var glob = classFile.getFileName().toString().replace(".class", "$*.class");
      try (DirectoryStream<Path> inner = Files.newDirectoryStream(parent, glob)) {
        inner.forEach(files::add);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    return files;
  }


  
}


