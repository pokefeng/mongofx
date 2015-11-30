// This file is part of MongoFX.
//
// MongoFX is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
// MongoFX is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with MongoFX.  If not, see <http://www.gnu.org/licenses/>.

//
// Copyright (c) Andrey Dubravin, 2015
//
package mongofx.service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.inject.Singleton;

import mongofx.js.api.DB;
import mongofx.js.api.JsIgnore;

@Singleton
public class TypeAutocompleteService {
  private final Map<Class<?>, NavigableMap<String, FieldDescription>> jsInfo = new HashMap<>();
  private final NavigableMap<String, FieldDescription> jsRootFields = new TreeMap<>();
  private boolean initialized = false;

  private void initialize() {
    if (initialized) {
      return;
    }

    jsRootFields.put("db", new FieldDescription("db", DB.class));
    loadJsInfo(DB.class);

    initialized = true;
  }

  public List<Suggest> find(List<String> paths, Map<Class<?>, NavigableMap<String, FieldDescription>> dynamicJsInfo) {
    initialize();

    if (paths.isEmpty()) {
      return Collections.emptyList();
    }

    String firstPathElement = paths.get(0);
    if (firstPathElement.isEmpty()) {
      return getRootFields();
    }
    NavigableMap<String, FieldDescription> root = jsRootFields;

    if (paths.size() > 1) {
      for (String path : paths.subList(0, paths.size() - 1)) {
        FieldDescription fieldDescription = root.get(path);
        if (fieldDescription == null) {
          return Collections.emptyList();
        }
        root = getJoinedTypeInfo(fieldDescription, dynamicJsInfo);
        if (root == null) {
          break;
        }
      }
    }

    if (root == null) {
      return Collections.emptyList();
    }

    return find(root, paths.get(paths.size() - 1));
  }

  private NavigableMap<String, FieldDescription> getJoinedTypeInfo(FieldDescription fieldDescription,
      Map<Class<?>, NavigableMap<String, FieldDescription>> dynamicJsInfo) {
    NavigableMap<String, FieldDescription> staticItemType = jsInfo.get(fieldDescription.fieldType);
    NavigableMap<String, FieldDescription> dynamicItemType = dynamicJsInfo.get(fieldDescription.fieldType);
    return joinTypeInfo(staticItemType, dynamicItemType);
  }

  private NavigableMap<String, FieldDescription> joinTypeInfo(NavigableMap<String, FieldDescription> staticItemType,
      NavigableMap<String, FieldDescription> dynamicItemType) {
    if (dynamicItemType == null) {
      return staticItemType;
    }
    if (staticItemType == null) {
      return dynamicItemType;
    }

    NavigableMap<String, FieldDescription> joinedMap = new TreeMap<>();
    joinedMap.putAll(staticItemType);
    joinedMap.putAll(dynamicItemType);
    return joinedMap;
  }

  private List<Suggest> getRootFields() {
    return jsRootFields.values().stream().map(e -> new Suggest(e)).collect(Collectors.toList());
  }

  private List<Suggest> find(NavigableMap<String, FieldDescription> root, String path) {
    NavigableMap<String, FieldDescription> tailMap = root.tailMap(path, true);
    return tailMap.entrySet().stream().filter(e -> e.getKey().startsWith(path)) //
        .map(e -> new Suggest(e.getValue().name, e.getValue().name.substring(path.length())))
        .collect(Collectors.toList());
  }

  private void loadJsInfo(Class<?> clazz) {
    if (jsInfo.containsKey(clazz)) {
      return;
    }

    NavigableMap<String, FieldDescription> fieldsInfo = new TreeMap<>();
    jsInfo.put(clazz, fieldsInfo);

    for (Method method : clazz.getDeclaredMethods()) {
      Class<?> returnType = method.getReturnType();
      if (!Modifier.isPublic(method.getModifiers()) || method.getAnnotation(JsIgnore.class) != null) {
        continue;
      }
      fieldsInfo.put(method.getName(), new FieldDescription(method.getName(), returnType));
      Package package1 = returnType.getPackage();
      if (package1 != null && "mongofx.js.api".equals(package1.getName())) {
        loadJsInfo(returnType);
      }
    }
  }

  public static class FieldDescription {
    private final Class<?> fieldType;
    final String name;

    public FieldDescription(String name, Class<?> returnType) {
      this.name = name;
      fieldType = returnType;
    }

    public Class<?> getFieldType() {
      return fieldType;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
