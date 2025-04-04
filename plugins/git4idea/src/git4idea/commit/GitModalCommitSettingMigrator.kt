// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package git4idea.commit

import com.intellij.ide.util.runOnceForApp
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vcs.VcsApplicationSettings

internal class GitModalCommitSettingMigrator : ProjectActivity {
  override suspend fun execute(project: Project) {
    runOnceForApp("migrate-modal-commit-setting") {
      if (!VcsApplicationSettings.getInstance().COMMIT_FROM_LOCAL_CHANGES) {
        AdvancedSettings.setBoolean("vcs.commit.tool.window", false)
        AdvancedSettings.setBoolean("vcs.non.modal.commit.toggle.ui", true)
      }
    }
  }
}