/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.plugins.groovy.lang.completion;


import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.completion.CompletionData;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionVariant;
import com.intellij.codeInsight.lookup.*;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.filters.*;
import com.intellij.psi.filters.position.LeftNeighbour;
import com.intellij.psi.filters.position.ParentElementFilter;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.completion.filters.classdef.ExtendsFilter;
import org.jetbrains.plugins.groovy.lang.completion.filters.classdef.ImplementsFilter;
import org.jetbrains.plugins.groovy.lang.completion.filters.control.BranchFilter;
import org.jetbrains.plugins.groovy.lang.completion.filters.control.ControlStructureFilter;
import org.jetbrains.plugins.groovy.lang.completion.filters.control.additional.CaseDefaultFilter;
import org.jetbrains.plugins.groovy.lang.completion.filters.control.additional.CatchFinallyFilter;
import org.jetbrains.plugins.groovy.lang.completion.filters.control.additional.ElseFilter;
import org.jetbrains.plugins.groovy.lang.completion.filters.exprs.InstanceOfFilter;
import org.jetbrains.plugins.groovy.lang.completion.filters.exprs.SimpleExpressionFilter;
import org.jetbrains.plugins.groovy.lang.completion.filters.modifiers.*;
import org.jetbrains.plugins.groovy.lang.completion.filters.types.BuiltInTypeAsArgumentFilter;
import org.jetbrains.plugins.groovy.lang.completion.filters.types.BuiltInTypeFilter;
import org.jetbrains.plugins.groovy.lang.completion.getters.SuggestedVariableNamesGetter;
import org.jetbrains.plugins.groovy.lang.groovydoc.lexer.GroovyDocTokenTypes;
import org.jetbrains.plugins.groovy.lang.groovydoc.psi.api.GrDocInlinedTag;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierList;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotation;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrApplicationStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.impl.PsiImplUtil;

import java.util.Set;

/**
 * @author ilyas
 */
public class GroovyCompletionData extends CompletionData {
  public static final String[] BUILT_IN_TYPES = {"boolean", "byte", "char", "short", "int", "float", "long", "double", "void"};
  public static final String[] MODIFIERS = new String[]{"private", "public", "protected", "transient", "abstract", "native", "volatile", "strictfp"};
  static final String[] INLINED_DOC_TAGS = {"code", "docRoot", "inheritDoc", "link", "linkplain", "literal"};
  static final String[] DOC_TAGS = {"author", "deprecated", "exception", "param", "return", "see", "serial", "serialData",
      "serialField", "since", "throws", "version"};

  public GroovyCompletionData() {
    registerAllCompletions();
  }

  public static void addGroovyKeywords(CompletionParameters parameters, CompletionResultSet result) {
    PsiElement position = parameters.getPosition();
    if (!PlatformPatterns.psiElement().afterLeaf(".", ".&").accepts(position)) {
      if (suggestPackage(position)) {
        result.addElement(keyword("package"));
      }
      if (suggestImport(position)) {
        result.addElement(keyword("import"));
      }
      //---
      if (suggestClassInterfaceEnum(position)) {
        result.addElement(keyword("class"));
        result.addElement(keyword("interface"));
        result.addElement(keyword("enum"));
      }
      if (afterAtInType(position)) {
        result.addElement(keyword("interface"));
      }
      //---


    }
  }

  private static TailTypeDecorator<LookupElementBuilder> keyword(final String keyword) {
    return TailTypeDecorator
      .withTail(LookupElementBuilder.create(keyword).setBold().setInsertHandler(GroovyInsertHandler.INSTANCE), TailType.SPACE);
  }

  /**
   * Registers completions on top level of Groovy script file
   */
  private void registerAllCompletions() {
    registerClassInterfaceEnumAnnotationCompletion();
    registerControlCompletion();
    registerSimpleExprsCompletion();
    registerBuiltInTypeCompletion();
    registerBuiltInTypesAsArgumentCompletion();
    registerInstanceofCompletion();
    registerThrowsCompletion();
    registerBranchCompletion();
    registerModifierCompletion();
    registerSynchronizedCompletion();
    registerFinalCompletion();

    registerSuggestVariableNameCompletion();
  }

  private void registerSuggestVariableNameCompletion() {
    CompletionVariant variant = new CompletionVariant(new ParentElementFilter(new ClassFilter(GrVariable.class)));
    variant.includeScopeClass(LeafPsiElement.class);
    variant.addCompletion(new SuggestedVariableNamesGetter(), TailType.NONE);
    registerVariant(variant);
  }


  private void registerClassInterfaceEnumAnnotationCompletion() {
    registerStandardCompletion(new ExtendsFilter(), "extends");
    registerStandardCompletion(new ImplementsFilter(), "implements");
  }

  private void registerControlCompletion() {
    String[] controlKeywords = {"try", "while", "with", "switch", "for", "return", "throw", "assert", "synchronized",};

    registerStandardCompletion(new ControlStructureFilter(), controlKeywords);
    registerStandardCompletion(new CaseDefaultFilter(), "case", "default");
    registerStandardCompletion(new CatchFinallyFilter(), "catch", "finally");
    registerStandardCompletion(new ElseFilter(), "else");


  }

  private void registerBuiltInTypeCompletion() {
    registerStandardCompletion(new AndFilter(new BuiltInTypeFilter(), new NotFilter(new ThrowsFilter())), BUILT_IN_TYPES);
  }

  private void registerBuiltInTypesAsArgumentCompletion() {
    AndFilter filter = new AndFilter(new BuiltInTypeAsArgumentFilter(), new NotFilter(new ThrowsFilter()));
    registerStandardCompletion(filter, BUILT_IN_TYPES);
  }

  private void registerSimpleExprsCompletion() {
    String[] exprs = {"true", "false", "null", "super", "new", "this", "as"};
    registerStandardCompletion(new SimpleExpressionFilter() {
      @Override
      public boolean isAcceptable(Object element, PsiElement context) {
        return super.isAcceptable(element, context) && !(context.getParent() instanceof GrLiteral);
      }
    }, exprs);
  }

  private void registerThrowsCompletion() {
    registerStandardCompletion(new ThrowsFilter(), "throws");
  }

  private void registerFinalCompletion() {
    registerStandardCompletion(new AndFilter(new FinalFilter(), new NotFilter(new ThrowsFilter())), "final", "def");
  }

  private void registerSynchronizedCompletion() {
    registerStandardCompletion(new SynchronizedFilter(), "synchronized");
  }

  private void registerInstanceofCompletion() {
    registerStandardCompletion(new InstanceOfFilter(), "instanceof", "in");
  }

  private void registerBranchCompletion() {
    registerStandardCompletion(new BranchFilter(), "break", "continue");
  }

  private void registerModifierCompletion() {
    registerStandardCompletion(new ModifiersFilter(), MODIFIERS);
    registerStandardCompletion(new LeftNeighbour(new PreviousModifierFilter()), "private", "public", "protected", "transient", "abstract",
                               "native", "volatile", "strictfp", "synchronized", "static");
    registerStandardCompletion(new StaticFilter(), "static");
  }


  @Override
  public void completeReference(final PsiReference reference,
                                final Set<LookupElement> set,
                                @NotNull final PsiElement position,
                                final PsiFile file,
                                final int offset) {
    super.completeReference(reference, set, position, file, offset);
    Set<LookupElement> result = new THashSet<LookupElement>();
    for (final LookupElement element : set) {
      result.add(LookupElementDecorator.withInsertHandler(element, new GroovyInsertHandlerAdapter()));
    }
    set.clear();
    set.addAll(result);
  }


  /**
   * Template to add all standard keywords completions
   *
   * @param filter   - Semantic filter for given keywords
   * @param keywords - Keywords to be completed
   */
  private void registerStandardCompletion(ElementFilter filter, String... keywords) {
    LeftNeighbour afterDotFilter = new LeftNeighbour(new TextFilter(".", ".&"));
    CompletionVariant variant = new CompletionVariant(new AndFilter(new NotFilter(afterDotFilter), filter));
    variant.setItemProperty(LookupItem.HIGHLIGHTED_ATTR, "");
    variant.includeScopeClass(LeafPsiElement.class);
    variant.setInsertHandler(new GroovyInsertHandlerAdapter());
    addCompletions(variant, keywords);
    registerVariant(variant);
  }


  public String findPrefix(PsiElement insertedElement, int offset) {
    if (insertedElement == null) return "";
    final String text = insertedElement.getText();
    final int offsetInElement = offset - insertedElement.getTextRange().getStartOffset();
    int start = offsetInElement - 1;
    while (start >= 0) {
      final char c = text.charAt(start);
      if (!Character.isJavaIdentifierPart(c) && c != '\'') break;
      --start;
    }

    return text.substring(start + 1, offsetInElement).trim();
  }

  /**
   * Adds all completion variants in sequence
   *
   * @param comps   Given completions
   * @param variant Variant for completions
   */
  private void addCompletions(CompletionVariant variant, String... comps) {
    for (String completion : comps) {
      variant.addCompletion(completion, TailType.SPACE);
    }
  }


  public static void addGroovyDocKeywords(CompletionParameters parameters, CompletionResultSet result) {
    PsiElement position = parameters.getPosition();
    if (PlatformPatterns.psiElement(GroovyDocTokenTypes.mGDOC_TAG_NAME).andNot(PlatformPatterns.psiElement().afterLeaf(".")).accepts(position)) {
      String[] tags = position.getParent() instanceof GrDocInlinedTag ? INLINED_DOC_TAGS : DOC_TAGS;
      for (String docTag : tags) {
        result.addElement(TailTypeDecorator.withTail(LookupElementBuilder.create(docTag), TailType.SPACE));
      }
    }
  }

  private static boolean suggestPackage(PsiElement context) {
    if (context.getParent() != null &&
        !(context.getParent() instanceof PsiErrorElement) &&
        context.getParent().getParent() instanceof GroovyFile &&
        ((GroovyFile) context.getParent().getParent()).getPackageDefinition() == null) {
      if (context.getParent() instanceof GrReferenceExpression) {
        return true;
      }
      if (context.getParent() instanceof GrApplicationStatement &&
          ((GrApplicationStatement) context.getParent()).getExpressionArguments()[0] instanceof GrReferenceExpression) {
        return true;
      }
      return false;
    }
    if (context.getTextRange().getStartOffset() == 0 && !(context instanceof OuterLanguageElement)) {
      return true;
    }

    final PsiElement leaf = GroovyCompletionUtil.getLeafByOffset(context.getTextRange().getStartOffset() - 1, context);
    if (leaf != null) {
      PsiElement parent = leaf.getParent();
      if (parent instanceof GroovyFile) {
        GroovyFile groovyFile = (GroovyFile) parent;
        if (groovyFile.getPackageDefinition() == null) {
          return GroovyCompletionUtil.isNewStatement(context, false);
        }
      }
    }

    return false;
  }

  private static boolean suggestImport(PsiElement context) {
    if (context.getParent() != null &&
        !(context.getParent() instanceof PsiErrorElement) &&
        GroovyCompletionUtil.isNewStatement(context, false) &&
        context.getParent().getParent() instanceof GroovyFile) {
      return true;
    }
    final PsiElement leaf = GroovyCompletionUtil.getLeafByOffset(context.getTextRange().getStartOffset() - 1, context);
    if (leaf != null) {
      PsiElement parent = leaf.getParent();
      if (parent instanceof GroovyFile) {
        return GroovyCompletionUtil.isNewStatement(context, false);
      }
    }
    return context.getTextRange().getStartOffset() == 0 && !(context instanceof OuterLanguageElement);
  }

  private static boolean suggestClassInterfaceEnum(PsiElement context) {
    if (context.getParent() != null &&
        (context.getParent() instanceof GrReferenceExpression) &&
        context.getParent().getParent() instanceof GroovyFile) {
      return true;
    }
    if (context.getParent() != null &&
        (context.getParent() instanceof GrReferenceExpression) &&
        (context.getParent().getParent() instanceof GrApplicationStatement ||
            context.getParent().getParent() instanceof GrCall) &&
        context.getParent().getParent().getParent() instanceof GroovyFile) {
      return true;
    }
    final PsiElement leaf = GroovyCompletionUtil.getLeafByOffset(context.getTextRange().getStartOffset() - 1, context);
    if (leaf != null) {
      PsiElement prev = leaf;
      prev = PsiImplUtil.realPrevious(prev);
      if (prev instanceof GrModifierList &&
          prev.getParent() != null &&
          prev.getParent().getParent() instanceof GroovyFile)
        return true;

      PsiElement parent = leaf.getParent();
      if (parent instanceof GroovyFile) {
        return GroovyCompletionUtil.isNewStatement(context, false);
      }
    }

    return false;
  }

  private static boolean afterAtInType(PsiElement context) {
    PsiElement previous = PsiImplUtil.realPrevious(context.getPrevSibling());
    if (previous != null &&
        GroovyTokenTypes.mAT.equals(previous.getNode().getElementType()) &&
        context.getParent() != null &&
        context.getParent().getParent() instanceof GroovyFile) {
      return true;
    }
    if (context.getParent() instanceof PsiErrorElement &&
        context.getParent().getParent() instanceof GrAnnotation) {
      return true;
    }
    return false;
  }
}
