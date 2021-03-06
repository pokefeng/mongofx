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
package mongofx.ui.main;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.event.ActionEvent;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.wellbehaved.event.EventHandlerHelper;
import org.fxmisc.wellbehaved.event.EventHandlerHelper.Builder;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.reactfx.EventStreams;

import com.google.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import mongofx.service.MongoService;
import mongofx.service.settings.ConnectionSettings;
import mongofx.ui.dbtree.DbTreeValue;
import mongofx.ui.dbtree.DbTreeValue.TreeValueType;
import mongofx.ui.dbtree.DBTreeController;
import mongofx.ui.msg.PopupMessageController;
import mongofx.ui.msg.PopupService;

public class MainFrameController {

  @FXML
  public ToggleButton consoleButton;

  @FXML
  private PopupMessageController popupMessageController;

  @FXML
  private CodeArea consoleLog;

  @Inject
  private PopupService popupService;

  @Inject
  private UIBuilder uiBuilder;

  @Inject
  private ConsoleController consoleController;

  @Inject
  private DBTreeController DBTreeController;

  @Inject
  private MongoService mongoService;

  @FXML
  private TreeView<DbTreeValue> treeView;

  @FXML
  private TabPane queryTabs;

  private final Map<Node, QueryTabController> tabData = new HashMap<>();

  @FXML
  private StackPane mainFrame;

  private BooleanProperty codeBufferActive = new SimpleBooleanProperty(false);

  @FXML
  protected void initialize() {
    codeBufferActive.bind(Bindings.createBooleanBinding(() -> !queryTabs.getTabs().isEmpty(), queryTabs.getTabs()));

    DBTreeController.initialize(treeView, this);
    consoleController.initialize(consoleLog);
    EventStreams.simpleChangesOf(queryTabs.getTabs())
    .subscribe(e -> e.getRemoved().stream().forEach(t -> tabData.remove(t.getContent()).stopEval()));

    Builder<KeyEvent> mainEvents = EventHandlerHelper.on(EventPattern.keyPressed(KeyCode.S, KeyCombination.CONTROL_DOWN)).act(a -> saveBuffer())//
        .on(EventPattern.keyPressed(KeyCode.O, KeyCombination.CONTROL_DOWN)).act(a -> openBuffer());
    EventHandlerHelper.install(mainFrame.onKeyPressedProperty(), mainEvents.create());
    popupService.register(popupMessageController, consoleButton);
  }

  public void addConnectionSettings(ConnectionSettings connectionSettings) {
    DBTreeController.addDbConnect(mongoService.connect(connectionSettings));
  }

  public void openTab() {
    TreeItem<DbTreeValue> selectedItem = treeView.getSelectionModel().getSelectedItem();
    if (selectedItem != null) {
      DbTreeValue value = selectedItem.getValue();
      if (value.getValueType() == TreeValueType.COLLECTION || value.getValueType() == TreeValueType.DATABASE) {
        TreeItem<DbTreeValue> dbItem = findParentNode(selectedItem, TreeValueType.CONNECTION);
        Entry<Node, QueryTabController> tabEntry = uiBuilder.buildQueryNode(dbItem.getValue().getHostConnect(), value);
        tabData.put(tabEntry.getKey(), tabEntry.getValue());
        queryTabs.getTabs().add(new Tab(value.getDisplayValue(), tabEntry.getKey()));
        queryTabs.getSelectionModel().selectLast();
        tabEntry.getValue().startTab();
      }
    }
  }

  private TreeItem<DbTreeValue> findParentNode(TreeItem<DbTreeValue> item, TreeValueType type) {
    while(true) {
      if (item.getValue().getValueType() == type) {
        return item;
      }
      item = item.getParent();
    }
  }

  @FXML
  public void runCommand() {
    runCommand(false);
  }

  @FXML
  public void runSelectedCommand() {
    runCommand(true);
  }

  private void runCommand(boolean selected) {
    Tab selectedTab = queryTabs.getSelectionModel().getSelectedItem();
    if (selectedTab != null) {
      tabData.get(selectedTab.getContent()).executeScript(selected);
    }
  }

  @FXML
  public void showConnectionSettings() throws IOException {
    uiBuilder.showSettingsWindow(this);
  }


  @FXML
  public void reloadSelectedTreeItem() {
    DBTreeController.reloadSelectedTreeItem();
  }

  @FXML
  public void saveBuffer() {
    Tab selectedTab = queryTabs.getSelectionModel().getSelectedItem();
    if (selectedTab != null) {
      tabData.get(selectedTab.getContent()).saveCurrentBuffer();
    }
  }

  @FXML
  public void openBuffer() {
    Tab selectedTab = queryTabs.getSelectionModel().getSelectedItem();
    if (selectedTab != null) {
      tabData.get(selectedTab.getContent()).loadToBuffer();
    }
  }

  @FXML
  public void saveBufferAs() {
    Tab selectedTab = queryTabs.getSelectionModel().getSelectedItem();
    if (selectedTab != null) {
      tabData.get(selectedTab.getContent()).saveCurrentBufferAs();
    }
  }

  @FXML
  public void formatBuffer() {
    Tab selectedTab = queryTabs.getSelectionModel().getSelectedItem();
    if (selectedTab != null) {
      tabData.get(selectedTab.getContent()).formatCode();
    }
  }

  public Boolean getCodeBufferActive() {
    return codeBufferActive.get();
  }

  public ObservableBooleanValue codeBufferActiveProperty() {
    return codeBufferActive;
  }
}
