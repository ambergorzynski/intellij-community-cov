/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package com.intellij.psi.impl.search;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.RepositoryElementsManager;
import com.intellij.psi.impl.cache.RepositoryIndex;
import com.intellij.psi.impl.cache.RepositoryManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.DirectClassInheritorsSearch;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;

/**
 * @author max
 */
public class JavaDirectInheritorsSearcher implements QueryExecutor<PsiClass, DirectClassInheritorsSearch.SearchParameters> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.search.JavaDirectInheritorsSearcher");

  public boolean execute(final DirectClassInheritorsSearch.SearchParameters p, final Processor<PsiClass> consumer) {
    final PsiClass aClass = p.getClassToProcess();
    PsiManagerImpl psiManager = (PsiManagerImpl)PsiManager.getInstance(aClass.getProject());

    final SearchScope useScope = ApplicationManager.getApplication().runReadAction(new Computable<SearchScope>() {
      public SearchScope compute() {
        return aClass.getUseScope();
      }
    });

    if ("java.lang.Object".equals(aClass.getQualifiedName())) {
      return psiManager.getSearchHelper().processAllClasses(new PsiElementProcessor<PsiClass>() {
        public boolean execute(final PsiClass psiClass) {
          return consumer.process(psiClass);
        }
      }, useScope);
    }
    else {
      final RepositoryManager repositoryManager = psiManager.getRepositoryManager();
      final RepositoryElementsManager repositoryElementsManager = psiManager.getRepositoryElementsManager();

      long[] candidateIds = ApplicationManager.getApplication().runReadAction(new Computable<long[]>() {
        public long[] compute() {
          RepositoryIndex repositoryIndex = repositoryManager.getIndex();
          final VirtualFileFilter rootFilter;
          if (useScope instanceof GlobalSearchScope) {
            rootFilter = repositoryIndex.rootFilterBySearchScope((GlobalSearchScope)useScope);
          }
          else {
            rootFilter = null;
          }
          return repositoryIndex.getNameOccurrencesInExtendsLists(aClass.getName(), rootFilter);
        }
      });
      for (final long candidateId : candidateIds) {
        PsiClass candidate = ApplicationManager.getApplication().runReadAction(new Computable<PsiClass>() {
          public PsiClass compute() {
            return (PsiClass)repositoryElementsManager.findOrCreatePsiElementById(candidateId);
          }
        });
        LOG.assertTrue(candidate.isValid());
        if (!consumer.process(candidate)) {
          return false;
        }
      }
    }

    return true;
  }
}
