package mongofx.ui.main;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.script.ScriptException;

import org.bson.Document;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import mongofx.js.api.ObjectListPresentationIterables;
import mongofx.js.api.TextPresentation;
import mongofx.service.MongoConnection;
import mongofx.service.MongoDatabase;

public class QueryTabController {

  private static final Logger log = LoggerFactory.getLogger(QueryTabController.class);

  @FXML
  private CodeArea codeArea;

  private MongoConnection connection;

  @FXML
  private TreeTableView<DocumentTreeValue> queryResultTree;

  private MongoDatabase mongoDatabase;

  @FXML
  private TextField limitResult;

  @FXML
  protected void initialize() {
    CodeAreaBuilder.setup(codeArea);
  }

  public void setConnection(MongoConnection connection) {
    this.connection = connection;
  }

  public void setDb(MongoDatabase mongoDatabase, String collectionName) {
    this.mongoDatabase = mongoDatabase;
    codeArea.replaceText("db.getCollection('" + collectionName + "').find({})");
  }

  @FXML
  public void codeAreaOnKeyReleased(KeyEvent ev) {
    if (ev.getCode() == KeyCode.F5) {
      executeScript();
    }
  }

  public void executeScript() {
    try {
      Optional<Object> documents = connection.eval(mongoDatabase, codeArea.getText());
      buildResultView(documents);
    }
    catch (ScriptException e) {
      log.error("Error execute script", e);
    }
  }

  private void buildResultView(Optional<Object> documents) {
    TreeItem<DocumentTreeValue> root = new TreeItem<>();
    if (documents.isPresent()) {
      Object result = documents.get();
      if (result instanceof TextPresentation) {
        root.getChildren().add(new TreeItem<>(new DocumentTreeValue("Result", String.valueOf(result))));
      }
      else {
        buildTreeFromDocuments(root,
            StreamSupport.stream(((ObjectListPresentationIterables)result).spliterator(), false)//
            .limit(Integer.parseInt(limitResult.getText())));
      }
    }
    queryResultTree.setRoot(root);
  }

  private void buildTreeFromDocuments(TreeItem<DocumentTreeValue> root, Stream<? extends Object> documents) {
    root.getChildren().addAll(documents.map(d -> new TreeItem<>(new DocumentTreeValue(null, d)))
        .peek(i -> buildChilds(i)).collect(Collectors.toList()));
  }

  private void buildChilds(TreeItem<DocumentTreeValue> i) {
    Object value = i.getValue().getValue();
    if (value instanceof Document) {
      i.getChildren()
      .addAll(((Document)value).entrySet().stream().map(f -> mapFieldToItem(f)).collect(Collectors.toList()));
    }
  }

  @SuppressWarnings("unchecked")
  private TreeItem<DocumentTreeValue> mapFieldToItem(Entry<String, Object> f) {
    Object value = f.getValue();
    if (value instanceof Document) {
      TreeItem<DocumentTreeValue> root = new TreeItem<>(new DocumentTreeValue(f.getKey(), value));
      buildChilds(root);
      return root;
    }
    if (value instanceof List) {
      TreeItem<DocumentTreeValue> root = new TreeItem<>(new DocumentTreeValue(f.getKey(), value));
      buildTreeFromArray(root, (List<Object>)value);
      return root;
    }
    return new TreeItem<>(new DocumentTreeValue(f.getKey(), f.getValue()));
  }

  private void buildTreeFromArray(TreeItem<DocumentTreeValue> root, List<Object> documents) {
    for (int i = 0; i < documents.size(); i++) {
      Object item = documents.get(i);
      TreeItem<DocumentTreeValue> treeItem = new TreeItem<>(new DocumentTreeValue(String.valueOf(i), item));
      root.getChildren().add(treeItem);
      buildChilds(treeItem);
    }
  }

  public void startTab() {
    int startIdx = codeArea.getText().indexOf("{");
    if (startIdx < 0) {
      startIdx = 0;
    }
    startIdx++;
    codeArea.selectRange(startIdx, startIdx);
    codeArea.requestFocus();
    executeScript();
  }
}