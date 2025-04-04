// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.javaFX.actions;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.actions.CreateFromTemplateActionBase;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;
import org.jetbrains.plugins.javaFX.fxml.JavaFxFileTypeFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.intellij.psi.search.GlobalSearchScope.moduleWithDependenciesAndLibrariesScope;
import static org.jetbrains.plugins.javaFX.fxml.JavaFxCommonNames.JAVAFX_APPLICATION_APPLICATION;

public final class CreateFxmlFileAction extends CreateFromTemplateActionBase {
  private static final String INTERNAL_TEMPLATE_NAME = "FxmlFile.fxml";

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  protected FileTemplate getTemplate(Project project, PsiDirectory dir) {
    return FileTemplateManager.getInstance(project).getInternalTemplate(INTERNAL_TEMPLATE_NAME);
  }

  @Override
  protected Map<String, String> getLiveTemplateDefaults(DataContext dataContext, @NotNull PsiFile file) {
    String packageName = ReadAction.compute(() -> {
      PsiDirectory psiDirectory = file.getContainingDirectory();
      if (psiDirectory != null) {
        VirtualFile vDirectory = psiDirectory.getVirtualFile();
        ProjectFileIndex index = ProjectRootManager.getInstance(file.getProject()).getFileIndex();
        if (index.isInSourceContent(vDirectory)) {
          return PackageIndex.getInstance(file.getProject()).getPackageNameByDirectory(vDirectory);
        }
      }
      return null;
    });
    @NonNls String name = file.getName();
    name = PathUtil.getFileName(name);
    if (JavaFxFileTypeFactory.FXML_EXTENSION.equals(PathUtil.getFileExtension(name))) {
      name = name.substring(0, name.length() - JavaFxFileTypeFactory.FXML_EXTENSION.length() - 1);
    }

    name = toClassName(name);
    name = !StringUtil.isEmpty(packageName) ? packageName + "." + name : name;
    return Collections.singletonMap("CONTROLLER_NAME", name);
  }

  private static String toClassName(String name) {
    int start;
    for (start = 0; start < name.length(); start++) {
      char c = name.charAt(start);
      if (Character.isJavaIdentifierStart(c) && c != '_' && c != '$') {
        break;
      }
    }
    StringBuilder className = new StringBuilder();
    boolean skip = true;

    for (int i = start; i < name.length(); i++) {
      char c = name.charAt(i);
      if (!Character.isJavaIdentifierPart(c) || c == '_' || c == '$') {
        skip = true;
        continue;
      }
      if (skip) {
        skip = false;
        className.append(Character.toUpperCase(c));
      }
      else {
        className.append(c);
      }
    }
    return className.toString();
  }

  @Override
  public void update(final @NotNull AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Presentation presentation = e.getPresentation();

    presentation.setEnabledAndVisible(isJavaFxTemplateAvailable(dataContext, JavaModuleSourceRootTypes.PRODUCTION));
  }

  static boolean isJavaFxTemplateAvailable(DataContext dataContext, Set<? extends JpsModuleSourceRootType<?>> requiredRootTypes) {
    var project = CommonDataKeys.PROJECT.getData(dataContext);
    var view = LangDataKeys.IDE_VIEW.getData(dataContext);
    if (project == null || view == null) {
      return false;
    }

    var directories = view.getDirectories();
    if (directories.length == 0) return false;

    var index = ProjectRootManager.getInstance(project).getFileIndex();
    var underRoot = Arrays.stream(directories)
      .map(PsiDirectory::getVirtualFile)
      .anyMatch(virtualFile -> index.isUnderSourceRootOfType(virtualFile, requiredRootTypes));

    if (!underRoot) return false;

    // root types check is less expensive on start toolbar update than checking module dependencies
    var module = ModuleUtilCore.findModuleForFile(directories[0].getVirtualFile(), project);
    return hasJavaFxDependency(module);
  }

  private static boolean hasJavaFxDependency(@Nullable Module module) {
    if (module == null || module.isDisposed()) return false;

    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      boolean hasClass = JavaPsiFacade.getInstance(module.getProject())
                           .findClass(JAVAFX_APPLICATION_APPLICATION, moduleWithDependenciesAndLibrariesScope(module)) != null;
      return Result.create(hasClass, ProjectRootManager.getInstance(module.getProject()));
    });
  }
}
