// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.idea.devkit.testAssistant.vfs;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.ex.dummy.DummyCachingFileSystem;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

final class TestDataGroupFileSystem extends DummyCachingFileSystem<VirtualFile> {
  /**
   * We must have a separator for two arbitrary file paths, considering that almost all symbols are possible in Unix paths.
   * It is very unlikely that this UUID will be present in file path so it's a pretty reliable separator.
   */
  private static final @NonNls String GROUP_FILES_SEPARATOR = "33d0ee30-8c8f-11e7-bb31-be2e44b06b34";
  private static final @NonNls String PROTOCOL = "testdata";

  TestDataGroupFileSystem() {
    super(PROTOCOL);
  }

  public static TestDataGroupFileSystem getTestDataGroupFileSystem() {
    return (TestDataGroupFileSystem)VirtualFileManager.getInstance().getFileSystem(PROTOCOL);
  }

  public static String getPath(VirtualFile beforeFile, VirtualFile afterFile) {
    return beforeFile.getPath() + GROUP_FILES_SEPARATOR + afterFile.getPath();
  }

  @Override
  public @NotNull String extractPresentableUrl(@NotNull String path) {
    String[] parts = path.split(GROUP_FILES_SEPARATOR);
    if (parts.length != 2) {
      return super.extractPresentableUrl(path);
    }

    // extractPresentableUrl is called from PsiAwareFileEditorManagerImpl#getFileTooltipText  and  CopyPathsAction#getPaths.
    // First one wraps the return value with FileUtil#getLocationRelativeToUserHome so we have to wrap the second path with it too.
    // But this causes copying first path as absolute and second one as user home-relative. So need to wrap both paths.
    return FileUtil.getLocationRelativeToUserHome(parts[0]) + "\n" + FileUtil.getLocationRelativeToUserHome(parts[1]);
  }

  @Override
  protected VirtualFile findFileByPathInner(@NotNull String path) {
    String[] parts = path.split(GROUP_FILES_SEPARATOR);
    if (parts.length != 2) {
      return null;
    }

    String beforePath = parts[0];
    String afterPath = parts[1];
    if (StringUtil.isEmpty(beforePath) || StringUtil.isEmpty(afterPath)) {
      return null;
    }

    LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
    VirtualFile beforeFile = localFileSystem.refreshAndFindFileByPath(beforePath);
    VirtualFile afterFile = localFileSystem.refreshAndFindFileByPath(afterPath);
    if (beforeFile == null || afterFile == null) {
      return null;
    }

    return new TestDataGroupVirtualFile(beforeFile, afterFile);
  }
}
