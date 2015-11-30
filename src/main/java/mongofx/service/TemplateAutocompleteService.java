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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;


@Singleton
public class TemplateAutocompleteService {
  private static final Logger log = LoggerFactory.getLogger(TypeAutocompleteService.class);

  private final NavigableMap<String, InsertTemplate> jsTemplates = new TreeMap<>();
  private boolean initialized = false;

  private void initialize() {
    if (initialized) {
      return;
    }

    loadJsTemplates("/templates");

    initialized = true;
  }

  public List<Suggest> find(String value) {
    initialize();

    return jsTemplates.tailMap(value, true).entrySet().stream().filter(v -> v.getKey().startsWith(value))
        .map(v -> new Suggest(v.getKey() + "(T)", v.getValue().getText())).collect(Collectors.toList());
  }

  private void loadJsTemplates(String path) {
    try {
      try (BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(path)))) {
        String line;
        while ((line = in.readLine()) != null) {
          if (line.endsWith(".js")) {
            try (InputStream templateIn = getClass().getResourceAsStream(String.format("%s/%s", path, line))) {
              byte[] buf = new byte[templateIn.available()];
              templateIn.read(buf);
              processTemplate(line.substring(0, line.length() - ".js".length()), new String(buf));
            }
          }
        }
      }
    }
    catch (IOException e) {
      log.error("IOException:", e);
    }
  }

  private void processTemplate(String name, String text) {
    jsTemplates.put(name, new InsertTemplate(text, 0));
  }

  public static class InsertTemplate {
    private final String text;
    private final int cursorPos;

    public InsertTemplate(String text, int cursorPos) {
      this.text = text;
      this.cursorPos = cursorPos;
    }

    public int getCursorPos() {
      return cursorPos;
    }

    public String getText() {
      return text;
    }
  }
}
