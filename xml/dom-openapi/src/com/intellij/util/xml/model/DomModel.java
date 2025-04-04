/*
 * Copyright 2000-2019 JetBrains s.r.o.
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

package com.intellij.util.xml.model;

import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
public interface DomModel<T extends DomElement> {

  /**
   * @deprecated Using this method may result in a large memory usage, since it will keep all the DOM and PSI for all the config files
   */
  @NotNull
  @Deprecated(forRemoval = true)
  T getMergedModel();

  @NotNull
  Set<XmlFile> getConfigFiles();
  
  @Unmodifiable
  List<DomFileElement<T>> getRoots();
}
